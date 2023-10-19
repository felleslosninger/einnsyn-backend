package no.einnsyn.apiv3.entities.innsynskrav;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.innsynskrav.models.Innsynskrav;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDel;
import no.einnsyn.clients.ip.IPSender;

@Service
public class InnsynskravSenderService {


  private final JavaMailSender mailSender;

  private IPSender ipSender;

  @Value("${email.from}")
  private String emailFrom;

  @Value("${email.baseUrl}")
  private String emailBaseUrl;

  @Value("${application.expectedResponseTimeoutDays:30}")
  private int expectedResponseTimeoutDays;

  @Value("${application.integrasjonspunktOrgnummer:000000000}")
  private String integrasjonspunktOrgnummer;

  MustacheFactory mustacheFactory = new DefaultMustacheFactory();
  Mustache orderConfirmationToEnhetTemplateHTML =
      mustacheFactory.compile("mailtemplates/orderConfirmationToEnhet.html.mustache");
  Mustache orderConfirmationToEnhetTemplateTXT =
      mustacheFactory.compile("mailtemplates/orderConfirmationToEnhet.txt.mustache");


  public InnsynskravSenderService(JavaMailSender mailSender, IPSender ipSender) {
    this.mailSender = mailSender;
    this.ipSender = ipSender;
  }


  public void sendInnsynskrav(Innsynskrav innsynskrav) {
    // Get a map of innsynskravDel by enhet
    var innsynskravDelMap = innsynskrav.getInnsynskravDel().stream()
        .collect(Collectors.groupingBy(InnsynskravDel::getEnhet));

    // Split sending into each enhet
    innsynskravDelMap.forEach((enhet, innsynskravDelList) -> {
      sendInnsynskrav(enhet, innsynskrav, innsynskravDelList);
    });
  }


  /**
   * Input is a list
   * 
   * @param enhet
   * @param innsynskrav
   * @param innsynskravDelList
   * @return
   */
  @Async
  @Transactional
  public void sendInnsynskrav(Enhet enhet, Innsynskrav innsynskrav,
      @Nullable List<InnsynskravDel> innsynskravDelList) {

    boolean success = false;

    // Remove successfully sent innsynskravDels
    innsynskravDelList = innsynskravDelList.stream()
        .filter(innsynskravDel -> innsynskravDel.getSent() == null).collect(Collectors.toList());

    // Return early if there are no innsynskravDels
    if (innsynskravDelList.size() == 0) {
      return;
    }

    // Find number of retries from first item in list
    int retryCount = innsynskravDelList.get(0).getRetryCount();

    // Check if we should send through eFormidling. Retry up to 3 times
    if (enhet.getEFormidling() != null && enhet.getEFormidling() == true && retryCount < 3) {
      success = sendInnsynskravThroughEFormidling(enhet, innsynskrav, innsynskravDelList);
    }

    // Send email
    else {
      success = sendInnsynskravByEmail(enhet, innsynskrav, innsynskravDelList);
    }

    if (success) {
      Instant now = Instant.now();
      innsynskravDelList.forEach(innsynskravDel -> {
        innsynskravDel.setSent(now);
      });
    } else {
      innsynskravDelList.forEach(innsynskravDel -> {
        innsynskravDel.setRetryCount(retryCount + 1);
      });
    }
  }


  /**
   * Get language bundle for a language
   * 
   * @param language
   * @return
   */
  public ResourceBundle getLanguageBundle(String language) {
    var locale = Locale.forLanguageTag(language);
    return ResourceBundle.getBundle("mailtemplates/orderConfirmationToEnhet", locale);
  }


  /**
   * Render mail template
   * 
   * @param enhet
   * @param innsynskrav
   * @param innsynskravDelList
   * @param mustache
   * @return
   */
  public String renderMail(Enhet enhet, Innsynskrav innsynskrav,
      List<InnsynskravDel> innsynskravDelList, Mustache mustache) {
    var language = "nb"; // Language should possibly be fetched from Enhet?
    var languageBundle = getLanguageBundle(language);
    var labels = new HashMap<String, String>();
    languageBundle.keySet().forEach(key -> {
      labels.put(key, languageBundle.getString(key));
    });

    var context = new HashMap<String, Object>();
    context.put("labels", labels);
    context.put("enhet", enhet);
    context.put("innsynskrav", innsynskrav);
    context.put("innsynskravDelList", innsynskravDelList);

    StringWriter writer = new StringWriter();
    mustache.execute(writer, context);

    return writer.toString();
  }


  /**
   * Send innsynskrav through email
   * 
   * @param enhet
   * @param innsynskrav
   * @param innsynskravDelList
   * @return
   */
  public boolean sendInnsynskravByEmail(Enhet enhet, Innsynskrav innsynskrav,
      List<InnsynskravDel> innsynskravDelList) {
    try {
      var mimeMessage = mailSender.createMimeMessage();
      var message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
      var language = "nb"; // Language should possibly be fetched from Enhet?
      var languageBundle = getLanguageBundle(language);
      String orderxml = OrderFileGenerator.toOrderXML(enhet, innsynskrav, innsynskravDelList);

      message.setSubject(languageBundle.getString("subject"));
      message.setFrom(emailFrom);
      // TODO: Set recipient when we are sure things are working. This should not be sent in test.
      // message.setTo(enhet.getInnsynskravEpost());
      message.setTo("gisle@gisle.net");

      String html =
          renderMail(enhet, innsynskrav, innsynskravDelList, orderConfirmationToEnhetTemplateHTML);
      String txt =
          renderMail(enhet, innsynskrav, innsynskravDelList, orderConfirmationToEnhetTemplateTXT);

      var byteArrayResource = new ByteArrayResource(orderxml.getBytes(StandardCharsets.UTF_8));
      message.addAttachment("order.xml", byteArrayResource, "text/xml");
      message.setText(txt, html);

      // TODO: Run in async thread
      mailSender.send(mimeMessage);
    } catch (Exception e) {
      // TODO: Real logging
      System.out.println(e);
      return false;
    }
    return true;
  }


  /**
   * Send innsynskrav through eFormidling
   * 
   * @param enhet
   * @param innsynskrav
   * @param innsynskravDelList
   * @return
   */
  public boolean sendInnsynskravThroughEFormidling(Enhet enhet, Innsynskrav innsynskrav,
      List<InnsynskravDel> innsynskravDelList) {

    String orderxml = OrderFileGenerator.toOrderXML(enhet, innsynskrav, innsynskravDelList);
    String transactionId = UUID.randomUUID().toString();
    Enhet handteresAv = enhet.getHandteresAv();
    if (handteresAv == null) {
      handteresAv = enhet;
    }
    String mailMessage =
        renderMail(enhet, innsynskrav, innsynskravDelList, orderConfirmationToEnhetTemplateTXT);

    try {
      // @formatter:off
      ipSender.sendInnsynskrav(
        orderxml,
        transactionId,
        handteresAv.getOrgnummer(),
        enhet.getOrgnummer(), // Data owner
        enhet.getInnsynskravEpost(),
        mailMessage,
        integrasjonspunktOrgnummer,
        expectedResponseTimeoutDays
      );
      // @formatter:on
    } catch (Exception e) {
      // TODO: Real error handling
      System.out.println(e);
      return false;
    }
    return true;
  }

}

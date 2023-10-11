package no.einnsyn.apiv3.entities.innsynskrav;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
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

@Service
public class InnsynskravSenderService {


  private final JavaMailSender mailSender;

  @Value("${email.from}")
  private String emailFrom;

  @Value("${email.baseUrl}")
  private String emailBaseUrl;

  MustacheFactory mustacheFactory = new DefaultMustacheFactory();
  Mustache orderConfirmationToEnhetTemplateHTML =
      mustacheFactory.compile("mailtemplates/orderConfirmationToEnhet.html.mustache");
  Mustache orderConfirmationToEnhetTemplateTXT =
      mustacheFactory.compile("mailtemplates/orderConfirmationToEnhet.txt.mustache");


  public InnsynskravSenderService(JavaMailSender mailSender) {
    this.mailSender = mailSender;
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

    // Get list of innsynskravDel for this enhet, if it's not already given
    if (innsynskravDelList == null) {
      innsynskravDelList = innsynskrav.getInnsynskravDel().stream()
          .filter(innsynskravDel -> innsynskravDel.getEnhet().equals(enhet))
          .collect(Collectors.toList());
    }

    // Filter away successfully sent innsynskravDels
    innsynskravDelList = innsynskravDelList.stream()
        .filter(innsynskravDel -> innsynskravDel.getSent() == null).collect(Collectors.toList());

    // Check if there are any innsynskravDel for this enhet
    if (innsynskravDelList.size() == 0) {
      return;
    }

    // Generate order.xml
    String orderxml = null;
    if (enhet.getOrderXmlVersjon() == 2) {
      orderxml = OrderFileGenerator.toOrderXMLV2(enhet, innsynskrav, innsynskravDelList);
    } else {
      orderxml = OrderFileGenerator.toOrderXMLV1(enhet, innsynskrav, innsynskravDelList);
    }

    // Find number of retries from first item in list
    int retryCount = innsynskravDelList.get(0).getRetryCount();

    // Check if we should send through eFormidling. Retry up to 3 times
    if (enhet.getEFormidling() == true && retryCount < 3) {
      // Send through eFormidling
      try {
        // @formatter:off
        // ipSender.sendInnsynskrav(
        //   xml,
        //   transactionId,
        //   orgnummerMottakar,
        //   orgnummerDataeigar,
        //   epostTilVirksomhet,
        //   epostTekst,
        //   applicationSettings.getOrgnummer(),
        //   applicationSettings.getExpectedResponseTimeoutDays());
        // @formatter:on

        success = true;
      } catch (Exception e) {
        // TODO: Real logging
        System.out.println(e);
      }

    }

    // Send email
    else {
      try {
        var language = "nb"; // Language should possibly be fetched from Enhet?
        var locale = Locale.forLanguageTag(language);
        var languageBundle =
            ResourceBundle.getBundle("mailtemplates/orderConfirmationToEnhet", locale);

        var mimeMessage = mailSender.createMimeMessage();
        var message = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        // Convert language bundle to map
        var labels = new HashMap<String, String>();
        languageBundle.keySet().forEach(key -> {
          labels.put(key, languageBundle.getString(key));
        });

        message.setSubject(languageBundle.getString("subject"));
        message.setFrom(emailFrom);
        // TODO: Set recipient when we are sure things are working. This should not be sent in test.
        // message.setTo(enhet.getInnsynskravEpost());
        message.setTo("gisle@gisle.net");

        var context = new HashMap<String, Object>();
        context.put("labels", labels);
        context.put("enhet", enhet);
        context.put("innsynskrav", innsynskrav);
        context.put("innsynskravDelList", innsynskravDelList);

        StringWriter htmlWriter = new StringWriter();
        orderConfirmationToEnhetTemplateHTML.execute(htmlWriter, context);
        StringWriter plainWriter = new StringWriter();
        orderConfirmationToEnhetTemplateTXT.execute(plainWriter, context);

        var byteArrayResource = new ByteArrayResource(orderxml.getBytes(StandardCharsets.UTF_8));
        message.addAttachment("order.xml", byteArrayResource, "text/xml");
        message.setText(plainWriter.toString(), htmlWriter.toString());
        mailSender.send(mimeMessage);

        success = true;
      } catch (Exception e) {
        // TODO: Real logging
        System.out.println(e);
      }
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

}

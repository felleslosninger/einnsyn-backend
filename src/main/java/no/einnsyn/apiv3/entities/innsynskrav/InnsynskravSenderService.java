package no.einnsyn.apiv3.entities.innsynskrav;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.innsynskrav.models.Innsynskrav;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDel;
import no.einnsyn.apiv3.utils.MailRenderer;
import no.einnsyn.apiv3.utils.MailSender;
import no.einnsyn.clients.ip.IPSender;

@Service
public class InnsynskravSenderService {

  private final MailSender mailSender;

  private final MailRenderer mailRenderer;

  private final InnsynskravRepository innsynskravRepository;

  private final IPSender ipSender;

  private final OrderFileGenerator orderFileGenerator;

  @Value("${application.email.from}")
  private String emailFrom;

  @URL
  @Value("${application.baseUrl}")
  private String emailBaseUrl;

  @Value("${application.integrasjonspunkt.expectedResponseTimeoutDays:30}")
  private int expectedResponseTimeoutDays;

  @Value("${application.integrasjonspunkt.orgnummer:000000000}")
  private String integrasjonspunktOrgnummer;


  public InnsynskravSenderService(MailRenderer mailRenderer, MailSender mailSender,
      IPSender ipSender, InnsynskravRepository innsynskravRepository,
      OrderFileGenerator orderFileGenerator) {
    this.mailRenderer = mailRenderer;
    this.mailSender = mailSender;
    this.ipSender = ipSender;
    this.innsynskravRepository = innsynskravRepository;
    this.orderFileGenerator = orderFileGenerator;
  }


  @Transactional
  public void sendInnsynskrav(String innsynskravId) {
    var innsynskrav = innsynskravRepository.findById(innsynskravId);
    sendInnsynskrav(innsynskrav);
  }


  /**
   * Send innsynskrav to all enhets in an Innsynskrav.
   * 
   * @param innsynskrav
   */
  @Transactional
  public void sendInnsynskrav(Innsynskrav innsynskrav) {
    // Get a map of innsynskravDel by enhet
    var innsynskravDelMap = innsynskrav.getInnsynskravDel().stream()
        .collect(Collectors.groupingBy(InnsynskravDel::getEnhet));

    // Split sending into each enhet
    innsynskravDelMap.forEach(
        (enhet, innsynskravDelList) -> sendInnsynskrav(enhet, innsynskrav, innsynskravDelList));
  }


  /**
   * Input is a list
   * 
   * @param enhet
   * @param innsynskrav
   * @param innsynskravDelList
   */
  @Transactional
  public void sendInnsynskrav(Enhet enhet, Innsynskrav innsynskrav,
      List<InnsynskravDel> innsynskravDelList) {

    boolean success;

    // Remove successfully sent innsynskravDels
    innsynskravDelList = innsynskravDelList.stream()
        .filter(innsynskravDel -> innsynskravDel.getSent() == null).toList();

    // Return early if there are no innsynskravDels
    if (innsynskravDelList.isEmpty()) {
      return;
    }

    // Find number of retries from first item in list
    int retryCount = innsynskravDelList.get(0).getRetryCount();

    // Check if we should send through eFormidling. Retry up to 3 times
    if (enhet.isEFormidling() && retryCount < 3) {
      success = sendInnsynskravThroughEFormidling(enhet, innsynskrav, innsynskravDelList);
    }

    // Send email
    else {
      success = sendInnsynskravByEmail(enhet, innsynskrav, innsynskravDelList);
    }

    if (success) {
      Instant now = Instant.now();
      innsynskravDelList.forEach(innsynskravDel -> innsynskravDel.setSent(now));
    } else {
      innsynskravDelList.forEach(innsynskravDel -> {
        innsynskravDel.setRetryCount(retryCount + 1);
        innsynskravDel.setRetryTimestamp(Instant.now());
      });
    }
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
      var language = "nb"; // Language should possibly be fetched from Enhet?
      var context = new HashMap<String, Object>();
      context.put("enhet", enhet);
      context.put("innsynskrav", innsynskrav);
      context.put("innsynskravDelList", innsynskravDelList);

      // Create attachment
      String orderxml = orderFileGenerator.toOrderXML(enhet, innsynskrav, innsynskravDelList);
      var byteArrayResource = new ByteArrayResource(orderxml.getBytes(StandardCharsets.UTF_8));

      var emailTo = "gisle@gisle.net"; // TODO: Set recipient when we are sure things are working.
      mailSender.send(emailFrom, emailTo, "orderConfirmationToEnhet", language, context,
          byteArrayResource, "order.xml", "application/xml");
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

    String orderxml = orderFileGenerator.toOrderXML(enhet, innsynskrav, innsynskravDelList);
    String transactionId = UUID.randomUUID().toString();

    // Set handteresAv to "enhet" if it is null
    Enhet handteresAv = enhet.getHandteresAv();
    if (handteresAv == null) {
      handteresAv = enhet;
    }

    var context = new HashMap<String, Object>();
    context.put("enhet", enhet);
    context.put("innsynskrav", innsynskrav);
    context.put("innsynskravDelList", innsynskravDelList);

    String mailMessage;
    try {
      mailMessage =
          mailRenderer.render("mailtemplates/confirmAnonymousOrder.txt.mustache", context);
    } catch (Exception e) {
      // TODO: Decide what to do when we can't render template
      return false;
    }

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

package no.einnsyn.apiv3.entities.innsynskrav;

import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.innsynskrav.models.Innsynskrav;
import no.einnsyn.apiv3.entities.innsynskravdel.InnsynskravDelRepository;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDel;
import no.einnsyn.apiv3.utils.MailRenderer;
import no.einnsyn.apiv3.utils.MailSender;
import no.einnsyn.clients.ip.IPSender;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@SuppressWarnings("java:S1192")
public class InnsynskravSenderService {

  private final MailSender mailSender;
  private final MailRenderer mailRenderer;
  private final InnsynskravRepository innsynskravRepository;
  private final InnsynskravDelRepository innsynskravDelRepository;
  private final TransactionTemplate transactionTemplate;
  private final IPSender ipSender;
  private final OrderFileGenerator orderFileGenerator;
  private MeterRegistry meterRegistry;

  @SuppressWarnings("java:S6813")
  @Lazy
  @Autowired
  private InnsynskravSenderService proxy;

  @Value("${application.email.from}")
  private String emailFrom;

  @URL
  @Value("${application.baseUrl}")
  private String emailBaseUrl;

  @Value("${application.integrasjonspunkt.expectedResponseTimeoutDays:30}")
  private int expectedResponseTimeoutDays;

  @Value("${application.integrasjonspunkt.orgnummer:000000000}")
  private String integrasjonspunktOrgnummer;

  public InnsynskravSenderService(
      MailRenderer mailRenderer,
      MailSender mailSender,
      IPSender ipSender,
      InnsynskravRepository innsynskravRepository,
      OrderFileGenerator orderFileGenerator,
      MeterRegistry meterRegistry,
      InnsynskravDelRepository innsynskravDelRepository,
      TransactionTemplate transactionTemplate) {
    this.mailRenderer = mailRenderer;
    this.mailSender = mailSender;
    this.ipSender = ipSender;
    this.innsynskravRepository = innsynskravRepository;
    this.orderFileGenerator = orderFileGenerator;
    this.innsynskravDelRepository = innsynskravDelRepository;
    this.meterRegistry = meterRegistry;
    this.transactionTemplate = transactionTemplate;
  }

  @Transactional
  public void sendInnsynskrav(String innsynskravId) {
    var innsynskrav = innsynskravRepository.findById(innsynskravId).orElse(null);
    proxy.sendInnsynskrav(innsynskrav);
  }

  /**
   * Send innsynskrav to all enhets in an Innsynskrav.
   *
   * @param innsynskrav
   */
  @Transactional
  public void sendInnsynskrav(Innsynskrav innsynskrav) {
    // Get a map of innsynskravDel by enhet
    var innsynskravDelMap =
        innsynskrav.getInnsynskravDel().stream()
            .collect(Collectors.groupingBy(InnsynskravDel::getEnhet));

    // Split sending into each enhet
    innsynskravDelMap.forEach(
        (enhet, innsynskravDelList) ->
            proxy.sendInnsynskrav(enhet, innsynskrav, innsynskravDelList));
  }

  /**
   * Input is a list
   *
   * @param enhet
   * @param innsynskrav
   * @param innsynskravDelList
   */
  @Transactional
  @Async
  public void sendInnsynskrav(
      Enhet enhet, Innsynskrav innsynskrav, List<InnsynskravDel> unfilteredInnsynskravDelList) {

    boolean success;

    // Remove successfully sent innsynskravDels
    var filteredInnsynskravDelList =
        unfilteredInnsynskravDelList.stream()
            .filter(innsynskravDel -> innsynskravDel.getSent() == null)
            .toList();

    // Return early if there are no innsynskravDels
    if (filteredInnsynskravDelList.isEmpty()) {
      return;
    }

    // Find number of retries from first item in list
    int retryCount = filteredInnsynskravDelList.get(0).getRetryCount();

    // Check if we should send through eFormidling. Retry up to 3 times
    if (enhet.isEFormidling() && retryCount < 3) {
      log.debug(
          "Sending innsynskrav {} through eFormidling. Retries: {}",
          innsynskrav.getId(),
          retryCount);
      success =
          proxy.sendInnsynskravThroughEFormidling(enhet, innsynskrav, filteredInnsynskravDelList);
      meterRegistry
          .counter(
              "ein_innsynskrav_sender",
              "status",
              success ? "success" : "failed",
              "type",
              "eformidling")
          .increment();
    }

    // Send email
    else {
      log.debug("Sending innsynskrav {} over e-mail. Retries: {}", innsynskrav.getId(), retryCount);
      success = proxy.sendInnsynskravByEmail(enhet, innsynskrav, filteredInnsynskravDelList);
      meterRegistry
          .counter(
              "ein_innsynskrav_sender", "status", success ? "success" : "failed", "type", "mail")
          .increment();
    }

    if (success) {
      log.info("Innsynskrav {} sent to {}", innsynskrav.getId(), enhet.getOrgnummer());
    } else {
      log.error("Innsynskrav {} failed to {}", innsynskrav.getId(), enhet.getOrgnummer());
    }

    // We're in an async function, and the old transaction is already committed. Updates to
    // innsynskravDelList must be executed in a new transaction.
    transactionTemplate.execute(
        status -> {
          for (var oldInnsynskravDel : filteredInnsynskravDelList) {
            var innsynskravDel =
                innsynskravDelRepository.findById(oldInnsynskravDel.getId()).orElse(null);
            if (innsynskravDel == null) {
              log.error(
                  "Could not find innsynskravDel {}. It could have been deleted before sending was"
                      + " done.",
                  oldInnsynskravDel.getId());
              continue;
            }
            if (success) {
              innsynskravDel.setSent(Instant.now());
              log.trace("Set sent timestamp for {}", innsynskravDel.getId());
            } else {
              innsynskravDel.setRetryCount(retryCount + 1);
              innsynskravDel.setRetryTimestamp(Instant.now());
              log.trace("Set retry timestamp for {}", innsynskravDel.getId());
            }
          }
          return null;
        });
  }

  /**
   * Send innsynskrav through email
   *
   * @param enhet
   * @param innsynskrav
   * @param innsynskravDelList
   * @return
   */
  public boolean sendInnsynskravByEmail(
      Enhet enhet, Innsynskrav innsynskrav, List<InnsynskravDel> innsynskravDelList) {
    try {
      var language = "nb"; // Language should possibly be fetched from Enhet?
      var context = new HashMap<String, Object>();
      context.put("enhet", enhet);
      context.put("innsynskrav", innsynskrav);
      context.put("innsynskravDelList", innsynskravDelList);

      // Create attachment
      var orderxml = orderFileGenerator.toOrderXML(enhet, innsynskrav, innsynskravDelList);
      var byteArrayResource = new ByteArrayResource(orderxml.getBytes(StandardCharsets.UTF_8));

      var emailTo = "gisle@gisle.net"; // TODO: Set recipient when we are sure things are working.

      log.info("Sending innsynskrav to {}", emailTo, StructuredArguments.raw("payload", orderxml));
      mailSender.send(
          emailFrom,
          emailTo,
          "orderConfirmationToEnhet",
          language,
          context,
          byteArrayResource,
          "order.xml",
          "application/xml");
    } catch (Exception e) {
      log.error("Could not send innsynskrav by email", e);
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
  public boolean sendInnsynskravThroughEFormidling(
      Enhet enhet, Innsynskrav innsynskrav, List<InnsynskravDel> innsynskravDelList) {

    var orderxml = orderFileGenerator.toOrderXML(enhet, innsynskrav, innsynskravDelList);
    var transactionId = UUID.randomUUID().toString();

    // Set handteresAv to "enhet" if it is null
    var handteresAv = enhet.getHandteresAv();
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
      log.error("Could not render mail template", e);
      return false;
    }

    try {
      log.debug(
          "Sending innsynskrav {} to eFormidling",
          innsynskrav.getId(),
          StructuredArguments.raw("payload", orderxml));
      ipSender.sendInnsynskrav(
          orderxml,
          transactionId,
          handteresAv.getOrgnummer(),
          enhet.getOrgnummer(), // Data owner
          enhet.getInnsynskravEpost(),
          mailMessage,
          integrasjonspunktOrgnummer,
          expectedResponseTimeoutDays);
    } catch (Exception e) {
      log.error("Could not send innsynskrav through eFormidling", e);
      return false;
    }
    return true;
  }
}

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
import no.einnsyn.apiv3.entities.innsynskravdel.InnsynskravDelService;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDel;
import no.einnsyn.apiv3.utils.MailRenderer;
import no.einnsyn.apiv3.utils.MailSender;
import no.einnsyn.clients.ip.IPSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@SuppressWarnings({"java:S1192", "LoggingPlaceholderCountMatchesArgumentCount"})
public class InnsynskravSenderService {

  private final MailSender mailSender;
  private final MailRenderer mailRenderer;
  private final InnsynskravRepository innsynskravRepository;
  private final InnsynskravDelService innsynskravDelService;
  private final IPSender ipSender;
  private final OrderFileGenerator orderFileGenerator;
  private final MeterRegistry meterRegistry;

  @SuppressWarnings("java:S6813")
  @Lazy
  @Autowired
  private InnsynskravSenderService proxy;

  @Value("${application.email.from}")
  private String emailFrom;

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
      InnsynskravDelService innsynskravDelService) {
    this.mailRenderer = mailRenderer;
    this.mailSender = mailSender;
    this.ipSender = ipSender;
    this.innsynskravRepository = innsynskravRepository;
    this.innsynskravDelService = innsynskravDelService;
    this.orderFileGenerator = orderFileGenerator;
    this.meterRegistry = meterRegistry;
  }

  @Transactional
  public void sendInnsynskrav(String innsynskravId) {
    innsynskravRepository
        .findById(innsynskravId)
        .ifPresent(innsynskrav -> proxy.sendInnsynskrav(innsynskrav));
  }

  /**
   * Send innsynskrav to all enhets in an Innsynskrav.
   *
   * @param innsynskrav The innsynskrav
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
   * @param enhet The enhet
   * @param innsynskrav The innsynskrav
   * @param unfilteredInnsynskravDelList The innsynskravDel list before all successfully sent are
   *     removed
   */
  @Transactional
  @Async
  public void sendInnsynskrav(
      Enhet enhet, Innsynskrav innsynskrav, List<InnsynskravDel> unfilteredInnsynskravDelList) {
    log.trace(
        "sendInnsynskrav({}, {}, {})",
        enhet.getId(),
        innsynskrav.getId(),
        unfilteredInnsynskravDelList.size());

    // Remove successfully sent innsynskravDels
    var filteredInnsynskravDelList =
        unfilteredInnsynskravDelList.stream()
            .filter(innsynskravDel -> innsynskravDel.getSent() == null)
            .toList();
    log.trace("filteredInnsynskravDelList.size(): {}", filteredInnsynskravDelList.size());

    // Return early if there are no innsynskravDels
    if (filteredInnsynskravDelList.isEmpty()) {
      return;
    }

    // Find number of retries from first item in list
    int retryCount = filteredInnsynskravDelList.getFirst().getRetryCount();
    boolean sendThroughEformidling = enhet.isEFormidling() && retryCount < 3;
    boolean success = false;

    // Send through eFormidling
    if (sendThroughEformidling) {
      success =
          proxy.sendInnsynskravThroughEFormidling(enhet, innsynskrav, filteredInnsynskravDelList);
    }

    // Send email
    else {
      success = proxy.sendInnsynskravByEmail(enhet, innsynskrav, filteredInnsynskravDelList);
    }

    // Prometheus / grafana
    meterRegistry
        .counter(
            "ein_innsynskrav_sender",
            "status",
            success ? "success" : "failed",
            "type",
            sendThroughEformidling ? "eFormidling" : "email")
        .increment();

    // Log
    log.debug(
        "Send innsynskrav {} using {}. Retries: {}. Status: {}",
        innsynskrav.getId(),
        sendThroughEformidling ? "eFormidling" : "e-mail",
        retryCount,
        success ? "success" : "failed");

    for (var innsynskravDel : filteredInnsynskravDelList) {
      log.trace("Update sent status for {}", innsynskravDel.getId());
      proxy.updateInnsynskravDelRetryStatus(innsynskravDel.getId(), success);
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void updateInnsynskravDelRetryStatus(String innsynskravDelId, boolean success) {
    var innsynskravDel = innsynskravDelService.findById(innsynskravDelId);
    log.trace("Update innsynskravDelRetryStatus({}, {})", innsynskravDel, success);
    if (success) {
      innsynskravDel.setSent(Instant.now());
    } else {
      innsynskravDel.setRetryCount(innsynskravDel.getRetryCount() + 1);
      innsynskravDel.setRetryTimestamp(Instant.now());
    }
  }

  /**
   * Send innsynskrav through email
   *
   * @param enhet The receiving enhet
   * @param innsynskrav The innsynskrav
   * @param innsynskravDelList The innsynskravDel list
   * @return True if successful
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
   * @param enhet The receiving enhet
   * @param innsynskrav The innsynskrav
   * @param innsynskravDelList The innsynskravDel list
   * @return True if successful
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

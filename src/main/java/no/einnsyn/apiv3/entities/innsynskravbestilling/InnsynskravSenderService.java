package no.einnsyn.apiv3.entities.innsynskravbestilling;

import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.innsynskrav.InnsynskravRepository;
import no.einnsyn.apiv3.entities.innsynskrav.models.Innsynskrav;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravStatusValue;
import no.einnsyn.apiv3.entities.innsynskravbestilling.models.InnsynskravBestilling;
import no.einnsyn.apiv3.entities.journalpost.JournalpostService;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.utils.MailRenderer;
import no.einnsyn.apiv3.utils.MailSender;
import no.einnsyn.clients.ip.IPSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@SuppressWarnings({"java:S1192", "LoggingPlaceholderCountMatchesArgumentCount"})
public class InnsynskravSenderService {

  private final MailSender mailSender;
  private final MailRenderer mailRenderer;
  private final InnsynskravBestillingRepository innsynskravBestillingRepository;
  private final InnsynskravRepository innsynskravRepository;
  private final IPSender ipSender;
  private final MeterRegistry meterRegistry;
  private final JournalpostService journalpostService;
  private final SimpleDateFormat v1DateFormat = new SimpleDateFormat("dd.MM.yyyy");
  private final SimpleDateFormat v2DateFormat = new SimpleDateFormat("yyyy-MM-dd");

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

  @Value("${application.innsynskrav.debugRecipient}")
  private String debugRecipient;

  public InnsynskravSenderService(
      MailRenderer mailRenderer,
      MailSender mailSender,
      IPSender ipSender,
      InnsynskravBestillingRepository innsynskravBestillingRepository,
      MeterRegistry meterRegistry,
      InnsynskravRepository innsynskravRepository,
      JournalpostService journalpostService) {
    this.mailRenderer = mailRenderer;
    this.mailSender = mailSender;
    this.ipSender = ipSender;
    this.innsynskravBestillingRepository = innsynskravBestillingRepository;
    this.innsynskravRepository = innsynskravRepository;
    this.meterRegistry = meterRegistry;
    this.journalpostService = journalpostService;
  }

  @Transactional(rollbackFor = Exception.class)
  public void sendInnsynskravBestilling(String innsynskravBestillingId) {
    innsynskravBestillingRepository
        .findById(innsynskravBestillingId)
        .ifPresent(innsynskravBestilling -> proxy.sendInnsynskravBestilling(innsynskravBestilling));
  }

  /**
   * Send innsynskrav to all enhets in an InnsynskravBestilling.
   *
   * @param innsynskravBestilling The InnsynskravBestilling
   */
  @Transactional
  public void sendInnsynskravBestilling(InnsynskravBestilling innsynskravBestilling) {
    // Get a map of innsynskrav by enhet
    var innsynskravMap =
        innsynskravBestilling.getInnsynskrav().stream()
            .collect(Collectors.groupingBy(Innsynskrav::getEnhet));

    // Split sending into each enhet
    for (var entry : innsynskravMap.entrySet()) {
      var enhet = entry.getKey();
      var innsynskravList = entry.getValue();
      try {
        proxy.sendInnsynskravBestilling(enhet, innsynskravBestilling, innsynskravList);
      } catch (Exception e) {
        log.error("Could not send InnsynskravBestilling to enhet {}", enhet.getId(), e);
      }
    }
  }

  @Transactional
  @Async("requestSideEffectExecutor")
  public void sendInnsynskravBestillingAsync(String innsynskravBestillingId) {
    innsynskravBestillingRepository
        .findById(innsynskravBestillingId)
        .ifPresent(value -> proxy.sendInnsynskravBestilling(value));
  }

  /**
   * Input is a list
   *
   * @param enhet The enhet
   * @param innsynskravBestilling The InnsynskravBestilling
   * @param unfilteredInnsynskravList The innsynskrav list before all successfully sent are removed
   */
  @Transactional
  public void sendInnsynskravBestilling(
      Enhet enhet,
      InnsynskravBestilling innsynskravBestilling,
      List<Innsynskrav> unfilteredInnsynskravList) {
    log.trace(
        "sendInnsynskravBestilling({}, {}, {})",
        enhet.getId(),
        innsynskravBestilling.getId(),
        unfilteredInnsynskravList.size());

    // Remove successfully sent innsynskravs
    var filteredInnsynskravList =
        unfilteredInnsynskravList.stream()
            .filter(innsynskrav -> innsynskrav.getSent() == null)
            .toList();
    log.trace("filteredInnsynskravList.size(): {}", filteredInnsynskravList.size());

    // Return early if there are no innsynskravs
    if (filteredInnsynskravList.isEmpty()) {
      return;
    }

    // Find number of retries from first item in list
    int retryCount = filteredInnsynskravList.getFirst().getRetryCount();
    boolean sendThroughEformidling = enhet.isEFormidling() && retryCount < 3;
    boolean success = false;

    // Send through eFormidling
    if (sendThroughEformidling) {
      success =
          proxy.sendInnsynskravThroughEFormidling(
              enhet, innsynskravBestilling, filteredInnsynskravList);
    }

    // Send email
    else {
      success = proxy.sendInnsynskravByEmail(enhet, innsynskravBestilling, filteredInnsynskravList);
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
    log.info(
        "Send innsynskrav {} using {}. Retries: {}. Status: {}",
        innsynskravBestilling.getId(),
        sendThroughEformidling ? "eFormidling" : "e-mail",
        retryCount,
        success ? "success" : "failed");

    for (var innsynskrav : filteredInnsynskravList) {
      var innsynskravId = innsynskrav.getId();
      log.trace("Update sent status for {}", innsynskravId);
      if (success) {
        innsynskravRepository.setSent(innsynskravId);
        innsynskravRepository.insertLegacyStatusAtomic(
            innsynskrav.getLegacyId(), InnsynskravStatusValue.SENDT_TIL_VIRKSOMHET.name(), true);
      } else {
        innsynskravRepository.updateRetries(innsynskravId);
      }
    }
  }

  /**
   * Send innsynskrav through email
   *
   * @param enhet The receiving enhet
   * @param innsynskravBestilling The innsynskravBestilling
   * @param innsynskravList The innsynskrav list
   * @return True if successful
   */
  public boolean sendInnsynskravByEmail(
      Enhet enhet, InnsynskravBestilling innsynskravBestilling, List<Innsynskrav> innsynskravList) {
    try {
      var innsynskravTemplateWrapperList =
          innsynskravList.stream()
              .filter(idl -> idl.getJournalpost() != null)
              .map(idl -> new InnsynskravTemplateWrapper(idl, journalpostService))
              .toList();
      var language = "nb"; // Language should possibly be fetched from Enhet?
      var context = new HashMap<String, Object>();
      context.put("enhet", enhet);
      context.put("innsynskravBestilling", innsynskravBestilling);
      context.put("innsynskravList", innsynskravTemplateWrapperList);
      context.put("v1DateFormat", v1DateFormat.format(innsynskravBestilling.getOpprettetDato()));
      context.put("v2DateFormat", v2DateFormat.format(innsynskravBestilling.getOpprettetDato()));

      // Create attachment
      String orderxml;
      if (enhet.getOrderXmlVersjon() == null || enhet.getOrderXmlVersjon() == 1) {
        orderxml = mailRenderer.renderFile("orderXmlTemplates/order-v1.xml.mustache", context);
      } else {
        orderxml = mailRenderer.renderFile("orderXmlTemplates/order-v2.xml.mustache", context);
      }
      var byteArrayResource = new ByteArrayResource(orderxml.getBytes(StandardCharsets.UTF_8));

      // Set emailTo to debugRecipient if it is set (for testing)
      var emailTo =
          StringUtils.hasText(debugRecipient) ? debugRecipient : enhet.getInnsynskravEpost();
      if (emailTo == null) {
        log.error("No email address found for enhet {}", enhet.getId());
        return false;
      }

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
   * @param innsynskravBestilling The InnsynskravBestilling
   * @param innsynskravList The innsynskrav list
   * @return True if successful
   */
  public boolean sendInnsynskravThroughEFormidling(
      Enhet enhet, InnsynskravBestilling innsynskravBestilling, List<Innsynskrav> innsynskravList) {

    var transactionId = UUID.randomUUID().toString();

    var innsynskravTemplateWrapperList =
        innsynskravList.stream()
            .filter(idl -> idl.getJournalpost() != null)
            .map(idl -> new InnsynskravTemplateWrapper(idl, journalpostService))
            .toList();

    // Set handteresAv to "enhet" if it is null
    var handteresAv = enhet.getHandteresAv();
    if (handteresAv == null) {
      handteresAv = enhet;
    }

    var context = new HashMap<String, Object>();
    context.put("enhet", enhet);
    context.put("innsynskravBestilling", innsynskravBestilling);
    context.put("innsynskravList", innsynskravTemplateWrapperList);
    context.put("v1DateFormat", v1DateFormat.format(innsynskravBestilling.getOpprettetDato()));
    context.put("v2DateFormat", v2DateFormat.format(innsynskravBestilling.getOpprettetDato()));

    String mailMessage;
    String orderxml;
    try {
      if (enhet.getOrderXmlVersjon() == null || enhet.getOrderXmlVersjon() == 1) {
        orderxml = mailRenderer.renderFile("orderXmlTemplates/order-v1.xml.mustache", context);
      } else {
        orderxml = mailRenderer.renderFile("orderXmlTemplates/order-v2.xml.mustache", context);
      }
      mailMessage =
          mailRenderer.renderFile("mailtemplates/confirmAnonymousOrder.txt.mustache", context);
    } catch (Exception e) {
      log.error("Could not render mail template", e);
      return false;
    }

    try {
      log.info(
          "Sending innsynskrav {} to eFormidling",
          innsynskravBestilling.getId(),
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

  /** Wrapper class to simplify the use of templates */
  @Getter
  private static class InnsynskravTemplateWrapper {
    private final String saksnr;
    private final int dokumentnr;
    private final String journalnr;
    private final String saksbehandler;
    // V2 extras:
    private final String fagsystemId;
    private String fagsystemDelId;
    private final String id;
    private final String systemid;
    private final String admEnhet;
    // Needed for other templates sharing the same context
    private final Journalpost journalpost;

    public InnsynskravTemplateWrapper(
        Innsynskrav innsynskrav, JournalpostService journalpostService) {
      journalpost = innsynskrav.getJournalpost();

      saksnr =
          String.join(
              "/",
              journalpost.getSaksmappe().getSaksaar().toString(),
              journalpost.getSaksmappe().getSakssekvensnummer().toString());
      dokumentnr = journalpost.getJournalpostnummer();
      journalnr =
          String.join(
              "/",
              journalpost.getJournalsekvensnummer().toString(),
              Integer.toString(journalpost.getJournalaar() % 100));
      saksbehandler =
          journalpostService.getSaksbehandler(journalpost.getId()) != null
              ? journalpostService.getSaksbehandler(journalpost.getId())
              : "[Ufordelt]";

      var saksmappe = journalpost.getSaksmappe();
      if (saksmappe.getParentKlasse() != null) {
        fagsystemId = saksmappe.getParentKlasse().getParentArkivdel().getParent().getSystemId();
        fagsystemDelId = saksmappe.getParentKlasse().getParentArkivdel().getSystemId();
      } else if (saksmappe.getParentArkivdel() != null) {
        fagsystemId = saksmappe.getParentArkivdel().getParent().getSystemId();
        fagsystemDelId = saksmappe.getParentArkivdel().getSystemId();
      } else {
        fagsystemId = saksmappe.getParentArkiv().getSystemId();
      }

      id = journalpost.getExternalId();
      systemid = journalpost.getSystemId();
      admEnhet = journalpostService.getAdministrativEnhetKode(journalpost.getId());
    }
  }
}

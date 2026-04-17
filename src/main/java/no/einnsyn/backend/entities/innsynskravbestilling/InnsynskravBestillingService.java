package no.einnsyn.backend.entities.innsynskravbestilling;

import jakarta.mail.MessagingException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.common.exceptions.models.AuthorizationException;
import no.einnsyn.backend.common.exceptions.models.BadRequestException;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.exceptions.models.NotFoundException;
import no.einnsyn.backend.common.exceptions.models.TooManyUnverifiedOrdersException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.paginators.Paginators;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.base.BaseService;
import no.einnsyn.backend.entities.bruker.models.ListByBrukerParameters;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import no.einnsyn.backend.entities.innsynskrav.InnsynskravRepository;
import no.einnsyn.backend.entities.innsynskrav.InnsynskravService;
import no.einnsyn.backend.entities.innsynskrav.models.Innsynskrav;
import no.einnsyn.backend.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestilling;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestillingDTO;
import no.einnsyn.backend.entities.innsynskravbestilling.models.ListByInnsynskravBestillingParameters;
import no.einnsyn.backend.utils.id.IdGenerator;
import no.einnsyn.backend.utils.mail.MailSenderService;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
public class InnsynskravBestillingService
    extends BaseService<InnsynskravBestilling, InnsynskravBestillingDTO> {

  @Getter(onMethod_ = @Override)
  private final InnsynskravBestillingRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter(onMethod_ = @Override)
  @Lazy
  @Autowired
  private InnsynskravBestillingService proxy;

  private final InnsynskravSenderService innsynskravSenderService;

  private final InnsynskravRepository innsynskravRepository;

  private final MailSenderService mailSender;

  @Value("${application.email.from}")
  private String emailFrom;

  @URL
  @Value("${application.baseUrl}")
  private String emailBaseUrl;

  @Value("${application.innsynskrav.maxInnsynskravPerInnsynskravBestilling:50}")
  private Integer maxInnsynskravPerInnsynskravBestilling;

  @Value("${application.innsynskrav.verificationQuarantineLimit:1}")
  private Integer verificationQuarantineLimit;

  @Value("${application.innsynskrav.verificationQuarantineHours:1}")
  private Integer verificationQuarantineHours;

  public InnsynskravBestillingService(
      InnsynskravBestillingRepository repository,
      InnsynskravRepository innsynskravRepository,
      InnsynskravService innsynskravService,
      InnsynskravSenderService innsynskravSenderService,
      MailSenderService mailSender) {
    super();
    this.repository = repository;
    this.innsynskravRepository = innsynskravRepository;
    this.innsynskravService = innsynskravService;
    this.innsynskravSenderService = innsynskravSenderService;
    this.mailSender = mailSender;
  }

  @Override
  public InnsynskravBestilling newObject() {
    return new InnsynskravBestilling();
  }

  @Override
  public InnsynskravBestillingDTO newDTO() {
    return new InnsynskravBestillingDTO();
  }

  /**
   * Override scheduleIndex to also trigger reindexing of Innsynskrav.
   *
   * @param innsynskravBestillingId ID of the InnsynskravBestilling
   * @param recurseDirection -1 for parents, 1 for children, 0 for both
   */
  @Override
  public boolean scheduleIndex(String innsynskravBestillingId, int recurseDirection) {
    var isScheduled = super.scheduleIndex(innsynskravBestillingId, recurseDirection);

    // Reindex innsynskrav
    if (recurseDirection >= 0 && !isScheduled) {
      try (var innsynskravStream =
          innsynskravRepository.streamIdByInnsynskravBestillingId(innsynskravBestillingId)) {
        innsynskravStream.forEach(id -> innsynskravService.scheduleIndex(id, 1));
      }
    }

    return true;
  }

  /**
   * Add a InnsynskravBestilling from a DTO object. A verification e-mail will be sent unless the
   * user is logged in.
   *
   * @param dto The DTO object
   * @return The entity object
   */
  @Override
  protected InnsynskravBestilling addEntity(InnsynskravBestillingDTO dto) throws EInnsynException {
    var brukerId = authenticationService.getBrukerId();
    if (brukerId != null) {
      dto.setVerified(true);
      dto.setBruker(new ExpandableField<>(brukerId));
    }

    var innsynskravBestilling = super.addEntity(dto);

    // TODO: We should not lock / send automatically on creation. We should allow saving unfinished
    // orders, and have a separate endpoint to lock/send when the user is ready.

    // No more Innsynskrav objects can be added
    innsynskravBestilling.setLocked(true);

    // Send email after the current transaction is persisted
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            if (brukerId != null) {
              innsynskravSenderService.sendInnsynskravBestillingAsync(
                  innsynskravBestilling.getId());
              proxy.sendOrderConfirmationToBruker(innsynskravBestilling.getId());
            } else {
              try {
                proxy.sendAnonymousConfirmationEmail(innsynskravBestilling.getId());
              } catch (EInnsynException e) {
                log.error(
                    "Failed to send anonymous confirmation email for InnsynskravBestilling {}",
                    innsynskravBestilling.getId(),
                    e);
              }
            }
          }
        });

    return innsynskravBestilling;
  }

  @Override
  protected InnsynskravBestilling fromDTO(
      InnsynskravBestillingDTO dto, InnsynskravBestilling innsynskravBestilling)
      throws EInnsynException {
    super.fromDTO(dto, innsynskravBestilling);

    // This should never pass through the controller, and is only set internally
    if (dto.getVerified() != null) {
      innsynskravBestilling.setVerified(dto.getVerified());
      log.trace("innsynskravBestilling.setVerified(" + innsynskravBestilling.isVerified() + ")");
    }

    // This should never pass through the controller, and is only set internally
    if (innsynskravBestilling.getId() == null && !innsynskravBestilling.isVerified()) {
      // Check if the user has too many unverified orders
      checkVerificationQuarantine(dto.getEmail());

      var secret = IdGenerator.generateId("issec");
      innsynskravBestilling.setVerificationSecret(secret);
      log.trace(
          "innsynskravBestilling.setVerificationSecret("
              + innsynskravBestilling.getVerificationSecret()
              + ")");
    }

    if (dto.getEmail() != null) {
      innsynskravBestilling.setEpost(dto.getEmail());
      log.trace("innsynskravBestilling.setEpost(" + innsynskravBestilling.getEpost() + ")");
    }

    if (dto.getLanguage() != null) {
      innsynskravBestilling.setLanguage(dto.getLanguage());
      log.trace("innsynskravBestilling.setLanguage(" + innsynskravBestilling.getLanguage() + ")");
    }

    var brukerField = dto.getBruker();
    if (brukerField != null) {
      var bruker = brukerService.findOrThrow(brukerField.getId());
      innsynskravBestilling.setBruker(bruker);
      log.trace("innsynskravBestilling.setBruker(" + bruker.getId() + ")");
    }

    // Persist before adding relations
    if (innsynskravBestilling.getId() == null) {
      log.trace("innsynskravBestilling.saveAndFlush()");
      innsynskravBestilling = repository.saveAndFlush(innsynskravBestilling);
    }

    // Add Innsynskrav list
    var innsynskravListField = dto.getInnsynskrav();
    if (innsynskravListField != null) {
      for (var innsynskravField : innsynskravListField) {
        var innsynskravDTO = innsynskravField.requireExpandedObject();
        innsynskravDTO.setInnsynskravBestilling(
            new ExpandableField<>(innsynskravBestilling.getId()));
        log.trace("innsynskravBestilling.addInnsynskrav(" + innsynskravDTO.getId() + ")");
        addInnsynskrav(innsynskravBestilling, innsynskravService.createOrThrow(innsynskravField));
      }
    }

    return innsynskravBestilling;
  }

  @Override
  protected InnsynskravBestillingDTO toDTO(
      InnsynskravBestilling innsynskravBestilling,
      InnsynskravBestillingDTO dto,
      Set<String> expandPaths,
      String currentPath) {
    super.toDTO(innsynskravBestilling, dto, expandPaths, currentPath);

    dto.setEmail(innsynskravBestilling.getEpost());
    dto.setVerified(innsynskravBestilling.isVerified());
    dto.setLanguage(innsynskravBestilling.getLanguage());

    // Add bruker
    dto.setBruker(
        brukerService.maybeExpand(
            innsynskravBestilling.getBruker(), "bruker", expandPaths, currentPath));

    // Add Innsynskrav list
    dto.setInnsynskrav(
        innsynskravService.maybeExpand(
            innsynskravBestilling.getInnsynskrav(), "innsynskrav", expandPaths, currentPath));

    return dto;
  }

  /**
   * Add an Innsynskrav to an InnsynskravBestilling, enforcing the configured maximum limit.
   *
   * @param innsynskravBestilling The InnsynskravBestilling entity
   * @param innsynskrav The Innsynskrav to add
   * @throws BadRequestException if the maximum number of Innsynskrav would be exceeded
   */
  public void addInnsynskrav(InnsynskravBestilling innsynskravBestilling, Innsynskrav innsynskrav)
      throws BadRequestException {
    var currentCount =
        innsynskravBestilling.getInnsynskrav() != null
            ? innsynskravBestilling.getInnsynskrav().size()
            : 0;
    if (currentCount >= maxInnsynskravPerInnsynskravBestilling) {
      throw new BadRequestException(
          "Too many Innsynskrav in a single InnsynskravBestilling. Maximum is "
              + maxInnsynskravPerInnsynskravBestilling);
    }
    innsynskravBestilling.addInnsynskrav(innsynskrav);
  }

  /**
   * Check if the user has too many unverified orders within the quarantine period.
   *
   * @param epost the email address to check
   * @throws EInnsynException if too many unverified orders exist
   */
  public void checkVerificationQuarantine(String epost) throws EInnsynException {
    var quarantineStartedAtInstant =
        Instant.now().minus(verificationQuarantineHours, ChronoUnit.HOURS);
    var numberOfUnverifiedOrdersWithinQuarantine =
        repository.countUnverifiedForUser(epost, quarantineStartedAtInstant);

    if (numberOfUnverifiedOrdersWithinQuarantine >= verificationQuarantineLimit)
      throw new TooManyUnverifiedOrdersException("Too many unverified orders for e-mail " + epost);
  }

  /**
   * Send e-mail to user, asking to verify the InnsynskravBestilling
   *
   * @param innsynskravBestillingId ID of the InnsynskravBestilling
   */
  @Async("requestSideEffectExecutor")
  @Transactional(readOnly = true)
  public void sendAnonymousConfirmationEmail(String innsynskravBestillingId)
      throws EInnsynException {
    var innsynskravBestilling = getProxy().findOrThrow(innsynskravBestillingId);
    var language = innsynskravBestilling.getLanguage();
    var context = new HashMap<String, Object>();
    context.put("baseUrl", emailBaseUrl);
    context.put("innsynskravBestillingId", innsynskravBestilling.getId());
    context.put("verificationSecret", innsynskravBestilling.getVerificationSecret());

    try {
      log.debug(
          "Send order verification email for InnsynskravBestilling {} to anonymous user {}",
          innsynskravBestilling.getId(),
          innsynskravBestilling.getEpost());
      mailSender.send(
          emailFrom, innsynskravBestilling.getEpost(), "confirmAnonymousOrder", language, context);
    } catch (MessagingException e) {
      log.error(
          "Could not send order verification email for InnsynskravBestilling {} to {}",
          innsynskravBestilling.getId(),
          innsynskravBestilling.getEpost(),
          e);
    }
  }

  /**
   * Send order confirmation e-mail to bruker
   *
   * @param innsynskravBestillingId The InnsynskravBestilling ID
   */
  @Async("requestSideEffectExecutor")
  @Transactional(readOnly = true)
  public void sendOrderConfirmationToBruker(String innsynskravBestillingId) {
    var innsynskravBestilling = innsynskravBestillingService.find(innsynskravBestillingId);
    var language = innsynskravBestilling.getLanguage();
    var sortedInnsynskrav =
        InnsynskravSenderService.getSortedInnsynskrav(innsynskravBestilling.getInnsynskrav());
    var context = new HashMap<String, Object>();
    context.put("baseUrl", emailBaseUrl);
    context.put("isAnonymous", innsynskravBestilling.getBruker() == null);
    context.put("innsynskravBestilling", innsynskravBestilling);
    context.put("innsynskravList", sortedInnsynskrav);
    context.put("innsynskravGroups", groupInnsynskravForBrukerMail(sortedInnsynskrav));

    try {
      log.debug(
          "Send order confirmation email for InnsynskravBestilling {} to user {}",
          innsynskravBestilling.getId(),
          innsynskravBestilling.getEpost());
      mailSender.send(
          emailFrom,
          innsynskravBestilling.getEpost(),
          "orderConfirmationToBruker",
          language,
          context);
    } catch (MessagingException e) {
      log.error(
          "Could not send order confirmation email for InnsynskravBestilling {} to {}",
          innsynskravBestilling.getId(),
          innsynskravBestilling.getEpost(),
          e);
    }
  }

  private List<InnsynskravBrukerMailGroup> groupInnsynskravForBrukerMail(
      List<Innsynskrav> innsynskravList) {
    var groups = new LinkedHashMap<String, InnsynskravBrukerMailGroup>();

    for (var innsynskrav : innsynskravList) {
      var virksomhet = getVirksomhetForInnsynskrav(innsynskrav);
      var groupKey = virksomhet == null ? "ukjent" : virksomhet.getId();
      var groupName = virksomhet == null ? "" : virksomhet.getNavn();

      groups
          .computeIfAbsent(groupKey, ignored -> new InnsynskravBrukerMailGroup(groupName))
          .addDocument(toBrukerMailDocument(innsynskrav, virksomhet));
    }

    return List.copyOf(groups.values());
  }

  private Enhet getVirksomhetForInnsynskrav(Innsynskrav innsynskrav) {
    var journalpost = innsynskrav.getJournalpost();
    if (journalpost == null) {
      return null;
    }
    if (journalpost.getAvhendetTil() != null) {
      return journalpost.getAvhendetTil();
    }
    return journalpost.getJournalenhet();
  }

  private InnsynskravBrukerMailDocument toBrukerMailDocument(
      Innsynskrav innsynskrav, Enhet virksomhet) {
    var journalpost = innsynskrav.getJournalpost();
    var saksmappe = journalpost.getSaksmappe();
    var saksnr = saksmappe.getSaksaar() + "/" + saksmappe.getSakssekvensnummer();
    var urlTilSak =
        emailBaseUrl
            + "/saksmappe?id="
            + saksmappe.getLegacyIri()
            + "&jid="
            + journalpost.getLegacyIri();

    return new InnsynskravBrukerMailDocument(
        saksmappe.getOffentligTittelSensitiv(),
        journalpost.getOffentligTittelSensitiv(),
        Integer.toString(journalpost.getJournalpostnummer()),
        saksnr,
        virksomhet == null ? "" : virksomhet.getNavn(),
        virksomhet == null ? "" : virksomhet.getInnsynskravEpost(),
        urlTilSak);
  }

  @Getter
  private static class InnsynskravBrukerMailGroup {
    private final String virksomhet;
    private final List<InnsynskravBrukerMailDocument> dokumenter = new ArrayList<>();

    private InnsynskravBrukerMailGroup(String virksomhet) {
      this.virksomhet = virksomhet;
    }

    private void addDocument(InnsynskravBrukerMailDocument dokument) {
      dokumenter.add(dokument);
    }
  }

  @Getter
  private static class InnsynskravBrukerMailDocument {
    private final String sakstittel;
    private final String journalposttittel;
    private final String doknr;
    private final String saksnr;
    private final String virksomhet;
    private final String virksomhetepost;
    private final String urlTilSak;

    private InnsynskravBrukerMailDocument(
        String sakstittel,
        String journalposttittel,
        String doknr,
        String saksnr,
        String virksomhet,
        String virksomhetepost,
        String urlTilSak) {
      this.sakstittel = sakstittel;
      this.journalposttittel = journalposttittel;
      this.doknr = doknr;
      this.saksnr = saksnr;
      this.virksomhet = virksomhet;
      this.virksomhetepost = virksomhetepost;
      this.urlTilSak = urlTilSak;
    }
  }

  /**
   * Verify an anonymous InnsynskravBestilling
   *
   * @param innsynskravBestillingId The InnsynskravBestilling ID
   * @param verificationSecret The verification secret
   * @return The InnsynskravBestilling
   */
  @Transactional(rollbackFor = Exception.class)
  @Retryable
  public InnsynskravBestillingDTO verify(String innsynskravBestillingId, String verificationSecret)
      throws EInnsynException {
    var innsynskravBestilling = innsynskravBestillingService.findOrThrow(innsynskravBestillingId);

    if (!innsynskravBestilling.isVerified()) {
      // Secret didn't match
      if (!innsynskravBestilling.getVerificationSecret().equals(verificationSecret)) {
        throw new AuthorizationException("Verification secret did not match");
      }

      innsynskravBestilling.setVerified(true);
      repository.saveAndFlush(innsynskravBestilling);

      // Ensure Elasticsearch documents (including child innsynskrav docs) are reindexed with the
      // updated verified state.
      scheduleIndex(innsynskravBestilling.getId());

      innsynskravSenderService.sendInnsynskravBestillingAsync(innsynskravBestilling.getId());
      proxy.sendOrderConfirmationToBruker(innsynskravBestilling.getId());
    }

    return proxy.toDTO(innsynskravBestilling);
  }

  /**
   * Delete an InnsynskravBestilling. Will detach any connected Innsynskrav first.
   *
   * @param innsynskravBestilling The entity object
   */
  @Override
  protected void deleteEntity(InnsynskravBestilling innsynskravBestilling) throws EInnsynException {
    // Delete all Innsynskrav objects
    var innsynskravList = innsynskravBestilling.getInnsynskrav();
    if (innsynskravList != null) {
      innsynskravBestilling.setInnsynskrav(null);
      for (var innsynskrav : innsynskravList) {
        innsynskrav.setInnsynskravBestilling(null);
      }
    }

    super.deleteEntity(innsynskravBestilling);
  }

  @Override
  protected Paginators<InnsynskravBestilling> getPaginators(ListParameters params)
      throws EInnsynException {
    if (params instanceof ListByBrukerParameters p && p.getBrukerId() != null) {
      var bruker = brukerService.findOrThrow(p.getBrukerId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(bruker, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(bruker, pivot, pageRequest));
    }
    return super.getPaginators(params);
  }

  protected PaginatedList<InnsynskravDTO> listInnsynskrav(
      String innsynskravBestillingId, ListByInnsynskravBestillingParameters query)
      throws EInnsynException {
    query.setInnsynskravBestillingId(innsynskravBestillingId);
    return innsynskravService.list(query);
  }

  /**
   * Authorize the list operation. Admins and users with access to the given bruker can list
   * InnsynskravBestilling.
   */
  @Override
  protected void authorizeList(ListParameters params) throws EInnsynException {
    if (authenticationService.isAdmin()) {
      return;
    }

    if (params instanceof ListByBrukerParameters p
        && p.getBrukerId() != null
        && authenticationService.isSelf(p.getBrukerId())) {
      return;
    }

    throw new AuthorizationException("Not authorized to list InnsynskravBestilling");
  }

  /**
   * Authorize the get operation. Admins and users with access to the given bruker can get
   * InnsynskravBestilling objects.
   *
   * @param id The InnsynskravBestilling ID
   * @throws AuthorizationException If the user is not authorized
   */
  @Override
  protected void authorizeGet(String id) throws EInnsynException {
    if (authenticationService.isAdmin()) {
      return;
    }

    var innsynskravBestilling =
        innsynskravBestillingService.findOrThrow(id, NotFoundException.class);

    var innsynskravBruker = innsynskravBestilling.getBruker();
    if (innsynskravBruker != null && authenticationService.isSelf(innsynskravBruker.getId())) {
      return;
    }

    throw new AuthorizationException("Not authorized to get " + id);
  }

  /**
   * Authorize the add operation. Anybody can add InnsynskravBestilling objects.
   *
   * @param dto The InnsynskravBestilling DTO
   * @throws AuthorizationException If the user is not authorized
   */
  @Override
  protected void authorizeAdd(InnsynskravBestillingDTO dto) throws EInnsynException {
    // No authorization needed
  }

  /**
   * Authorize the update operation. Only users representing a bruker that owns the object can
   * update.
   *
   * @param id The InnsynskravBestilling ID
   * @param dto The InnsynskravBestilling DTO
   * @throws AuthorizationException If the user is not authorized
   */
  @Override
  protected void authorizeUpdate(String id, InnsynskravBestillingDTO dto) throws EInnsynException {
    var innsynskravBestilling = innsynskravBestillingService.findOrThrow(id);
    if (innsynskravBestilling.isLocked()) {
      throw new AuthorizationException("Not authorized to update " + id + " (locked)");
    }

    if (authenticationService.isAdmin()) {
      return;
    }

    var innsynskravBruker = innsynskravBestilling.getBruker();
    if (innsynskravBruker != null && authenticationService.isSelf(innsynskravBruker.getId())) {
      return;
    }

    throw new AuthorizationException("Not authorized to update " + id);
  }

  /**
   * Authorize the delete operation. Only users representing a bruker that owns the object can
   * delete.
   *
   * @param id The InnsynskravBestilling ID
   * @throws AuthorizationException If the user is not authorized
   */
  @Override
  protected void authorizeDelete(String id) throws EInnsynException {
    if (authenticationService.isAdmin()) {
      return;
    }

    var innsynskravBestilling = innsynskravBestillingService.findOrThrow(id);
    var innsynskravBruker = innsynskravBestilling.getBruker();
    if (innsynskravBruker != null && authenticationService.isSelf(innsynskravBruker.getId())) {
      return;
    }

    throw new AuthorizationException("Not authorized to delete " + id);
  }
}

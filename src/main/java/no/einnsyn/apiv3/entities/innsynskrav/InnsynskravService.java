package no.einnsyn.apiv3.entities.innsynskrav;

import jakarta.mail.MessagingException;
import java.util.HashMap;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.paginators.Paginators;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.BaseService;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.innsynskrav.models.Innsynskrav;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravListQueryDTO;
import no.einnsyn.apiv3.entities.innsynskravdel.InnsynskravDelService;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelDTO;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelListQueryDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import no.einnsyn.apiv3.error.exceptions.ForbiddenException;
import no.einnsyn.apiv3.utils.MailSender;
import no.einnsyn.apiv3.utils.idgenerator.IdGenerator;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class InnsynskravService extends BaseService<Innsynskrav, InnsynskravDTO> {

  @Getter private final InnsynskravRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private InnsynskravService proxy;

  private final InnsynskravSenderService innsynskravSenderService;

  private final MailSender mailSender;

  @Value("${application.email.from}")
  private String emailFrom;

  @URL
  @Value("${application.baseUrl}")
  private String emailBaseUrl;

  public InnsynskravService(
      InnsynskravRepository repository,
      InnsynskravDelService innsynskravDelService,
      InnsynskravSenderService innsynskravSenderService,
      MailSender mailSender) {
    super();
    this.repository = repository;
    this.innsynskravDelService = innsynskravDelService;
    this.innsynskravSenderService = innsynskravSenderService;
    this.mailSender = mailSender;
  }

  public Innsynskrav newObject() {
    return new Innsynskrav();
  }

  public InnsynskravDTO newDTO() {
    return new InnsynskravDTO();
  }

  /**
   * Add a InnsynskravBestilling from a DTO object. A verification e-mail will be sent unless the
   * user is logged in.
   *
   * @param dto The DTO object
   * @return The entity object
   */
  @Override
  protected Innsynskrav addEntity(InnsynskravDTO dto) throws EInnsynException {
    var brukerId = authenticationService.getBrukerId();
    if (brukerId != null) {
      dto.setVerified(true);
      dto.setBruker(new ExpandableField<>(brukerId));
    }

    var innsynskrav = super.addEntity(dto);

    // No more InnsynskravDel objects can be added
    innsynskrav.setLocked(true);

    if (brukerId != null) {
      innsynskravSenderService.sendInnsynskrav(innsynskrav);
    } else {
      proxy.sendAnonymousConfirmationEmail(innsynskrav);
    }

    return innsynskrav;
  }

  @Override
  protected Innsynskrav fromDTO(InnsynskravDTO dto, Innsynskrav innsynskrav)
      throws EInnsynException {
    super.fromDTO(dto, innsynskrav);

    // This should never pass through the controller, and is only set internally
    if (dto.getVerified() != null) {
      innsynskrav.setVerified(dto.getVerified());
      log.trace("innsynskrav.setVerified(" + innsynskrav.isVerified() + ")");
    }

    // This should never pass through the controller, and is only set internally
    if (innsynskrav.getId() == null && !innsynskrav.isVerified()) {
      var secret = IdGenerator.generateId("issec");
      innsynskrav.setVerificationSecret(secret);
      log.trace("innsynskrav.setVerificationSecret(" + innsynskrav.getVerificationSecret() + ")");
    }

    if (dto.getEmail() != null) {
      innsynskrav.setEpost(dto.getEmail());
      log.trace("innsynskrav.setEpost(" + innsynskrav.getEpost() + ")");
    }

    if (dto.getLanguage() != null) {
      innsynskrav.setLanguage(dto.getLanguage());
      log.trace("innsynskrav.setLanguage(" + innsynskrav.getLanguage() + ")");
    }

    var brukerField = dto.getBruker();
    if (brukerField != null) {
      var bruker = brukerService.findById(brukerField.getId());
      innsynskrav.setBruker(bruker);
      log.trace("innsynskrav.setBruker(" + innsynskrav.getBruker() + ")");
    }

    // Persist before adding relations
    if (innsynskrav.getId() == null) {
      log.trace("innsynskrav.saveAndFlush()");
      innsynskrav = repository.saveAndFlush(innsynskrav);
    }

    // Add InnsynskravDel list
    var innsynskravDelListField = dto.getInnsynskravDel();
    if (innsynskravDelListField != null) {
      for (var innsynskravDelField : innsynskravDelListField) {
        var innsynskravDelDTO = innsynskravDelField.requireExpandedObject();
        innsynskravDelDTO.setInnsynskrav(new ExpandableField<>(innsynskrav.getId()));
        log.trace("innsynskrav.addInnsynskravDel(" + innsynskravDelDTO.getId() + ")");
        innsynskrav.addInnsynskravDel(innsynskravDelService.createOrThrow(innsynskravDelField));
      }
    }

    return innsynskrav;
  }

  @Override
  protected InnsynskravDTO toDTO(
      Innsynskrav innsynskrav, InnsynskravDTO dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(innsynskrav, dto, expandPaths, currentPath);

    dto.setEmail(innsynskrav.getEpost());
    dto.setVerified(innsynskrav.isVerified());

    // Add bruker
    dto.setBruker(
        brukerService.maybeExpand(innsynskrav.getBruker(), "bruker", expandPaths, currentPath));

    // Add InnsynskravDel list
    dto.setInnsynskravDel(
        innsynskravDelService.maybeExpand(
            innsynskrav.getInnsynskravDel(), "innsynskravDel", expandPaths, currentPath));

    return dto;
  }

  /**
   * Send e-mail to user, asking to verify the Innsynskrav
   *
   * @param innsynskrav The Innsynskrav
   */
  @Async
  public void sendAnonymousConfirmationEmail(Innsynskrav innsynskrav) {
    var language = innsynskrav.getLanguage();
    var context = new HashMap<String, Object>();
    context.put("baseUrl", emailBaseUrl);
    context.put("innsynskravId", innsynskrav.getId());
    context.put("verificationSecret", innsynskrav.getVerificationSecret());

    try {
      log.debug(
          "Send order confirmation email for innsynskrav {} to anonymous user {}",
          innsynskrav.getId(),
          innsynskrav.getEpost());
      mailSender.send(
          emailFrom, innsynskrav.getEpost(), "confirmAnonymousOrder", language, context);
    } catch (MessagingException e) {
      log.error(
          "Could not send confirmation email for innsynskrav {} to {}",
          innsynskrav.getId(),
          innsynskrav.getEpost(),
          e);
    }
  }

  /**
   * Send order confirmation e-mail to bruker
   *
   * @param innsynskrav The Innsynskrav
   */
  @Async
  public void sendOrderConfirmationToBruker(Innsynskrav innsynskrav) {
    var language = innsynskrav.getLanguage();
    var context = new HashMap<String, Object>();
    context.put("innsynskrav", innsynskrav);
    context.put("innsynskravDelList", innsynskrav.getInnsynskravDel());

    try {
      log.debug(
          "Send order confirmation email for innsynskrav {} to user {}",
          innsynskrav.getId(),
          innsynskrav.getEpost());
      mailSender.send(
          emailFrom, innsynskrav.getEpost(), "orderConfirmationToBruker", language, context);
    } catch (MessagingException e) {
      log.error(
          "Could not send order confirmation email for innsynskrav {} to {}",
          innsynskrav.getId(),
          innsynskrav.getEpost(),
          e);
    }
  }

  /**
   * Verify an anonymous innsynskrav
   *
   * @param innsynskravId The Innsynskrav ID
   * @param verificationSecret The verification secret
   * @return The Innsynskrav
   */
  @Transactional
  public InnsynskravDTO verifyInnsynskrav(String innsynskravId, String verificationSecret)
      throws ForbiddenException {
    var innsynskrav = innsynskravService.findById(innsynskravId);

    if (!innsynskrav.isVerified()) {
      // Secret didn't match
      if (!innsynskrav.getVerificationSecret().equals(verificationSecret)) {
        throw new ForbiddenException("Verification secret did not match");
      }

      innsynskrav.setVerified(true);
      repository.saveAndFlush(innsynskrav);
      innsynskravSenderService.sendInnsynskrav(innsynskrav);
      proxy.sendOrderConfirmationToBruker(innsynskrav);
    }

    return proxy.toDTO(innsynskrav);
  }

  /**
   * Delete an Innsynskrav
   *
   * @param innsynskrav The entity object
   */
  @Override
  protected void deleteEntity(Innsynskrav innsynskrav) throws EInnsynException {
    // Delete all InnsynskravDel objects
    var innsynskravDelList = innsynskrav.getInnsynskravDel();
    if (innsynskravDelList != null) {
      for (var innsynskravDel : innsynskravDelList) {
        innsynskravDelService.delete(innsynskravDel.getId());
      }
    }

    super.deleteEntity(innsynskrav);
  }

  @Override
  protected Paginators<Innsynskrav> getPaginators(BaseListQueryDTO params) {
    if (params instanceof InnsynskravListQueryDTO p && p.getBrukerId() != null) {
      var bruker = brukerService.findById(p.getBrukerId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(bruker, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(bruker, pivot, pageRequest));
    }
    return super.getPaginators(params);
  }

  protected ResultList<InnsynskravDelDTO> getInnsynskravDelList(
      String innsynskravId, InnsynskravDelListQueryDTO query) throws EInnsynException {
    query.setInnsynskravId(innsynskravId);
    return innsynskravDelService.list(query);
  }

  /**
   * Authorize the list operation. Admins and users with access to the given bruker can list
   * Innsynskrav.
   */
  @Override
  protected void authorizeList(BaseListQueryDTO params) throws ForbiddenException {
    if (authenticationService.isAdmin()) {
      return;
    }

    if (params instanceof InnsynskravListQueryDTO p
        && p.getBrukerId() != null
        && authenticationService.isSelf(p.getBrukerId())) {
      return;
    }

    throw new ForbiddenException("Not authorized to list Innsynskrav");
  }

  /**
   * Authorize the get operation. Admins and users with access to the given bruker can get
   * Innsynskrav objects.
   *
   * @param id The Innsynskrav ID
   * @throws ForbiddenException If the user is not authorized
   */
  @Override
  protected void authorizeGet(String id) throws ForbiddenException {
    if (authenticationService.isAdmin()) {
      return;
    }

    var innsynskrav = innsynskravService.findById(id);
    if (innsynskrav == null) {
      throw new ForbiddenException("Not authorized to get " + id);
    }

    var innsynskravBruker = innsynskrav.getBruker();
    if (innsynskravBruker != null && authenticationService.isSelf(innsynskravBruker.getId())) {
      return;
    }

    throw new ForbiddenException("Not authorized to get " + id);
  }

  /**
   * Authorize the add operation. Anybody can add Innsynskrav objects.
   *
   * @param dto The Innsynskrav DTO
   * @throws ForbiddenException If the user is not authorized
   */
  @Override
  protected void authorizeAdd(InnsynskravDTO dto) throws ForbiddenException {
    // No authorization needed
  }

  /**
   * Authorize the update operation. Only users representing a bruker that owns the object can
   * update.
   *
   * @param id The Innsynskrav ID
   * @param dto The Innsynskrav DTO
   * @throws ForbiddenException If the user is not authorized
   */
  @Override
  protected void authorizeUpdate(String id, InnsynskravDTO dto) throws ForbiddenException {
    if (authenticationService.isAdmin()) {
      return;
    }

    var innsynskrav = innsynskravService.findById(id);
    if (authenticationService.isSelf(innsynskrav.getBruker().getId())) {
      return;
    }

    throw new ForbiddenException("Not authorized to update " + id);
  }

  /**
   * Authorize the delete operation. Only users representing a bruker that owns the object can
   * delete.
   *
   * @param id The Innsynskrav ID
   * @throws ForbiddenException If the user is not authorized
   */
  @Override
  protected void authorizeDelete(String id) throws ForbiddenException {
    if (authenticationService.isAdmin()) {
      return;
    }

    var innsynskrav = innsynskravService.findById(id);
    var innsynskravBruker = innsynskrav.getBruker();
    if (innsynskravBruker != null && authenticationService.isSelf(innsynskravBruker.getId())) {
      return;
    }

    throw new ForbiddenException("Not authorized to delete " + id);
  }
}

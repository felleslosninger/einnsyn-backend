package no.einnsyn.apiv3.entities.innsynskravdel;

import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.common.paginators.Paginators;
import no.einnsyn.apiv3.entities.base.BaseService;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.innsynskravdel.models.*;
import no.einnsyn.apiv3.error.exceptions.BadRequestException;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import no.einnsyn.apiv3.error.exceptions.ForbiddenException;
import no.einnsyn.apiv3.utils.TimeConverter;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Getter
@Service
@Slf4j
public class InnsynskravDelService extends BaseService<InnsynskravDel, InnsynskravDelDTO> {

  private final InnsynskravDelRepository repository;

  @SuppressWarnings("java:S6813")
  @Lazy
  @Autowired
  private InnsynskravDelService proxy;

  public InnsynskravDelService(InnsynskravDelRepository repository) {
    super();
    this.repository = repository;
  }

  public InnsynskravDel newObject() {
    return new InnsynskravDel();
  }

  public InnsynskravDelDTO newDTO() {
    return new InnsynskravDelDTO();
  }

  /**
   * Convert DTO to InnsynskravDel.
   *
   * @param dto The DTO to convert
   * @param innsynskravDel The InnsynskravDel to fill
   * @return The InnsynskravDel
   */
  @Override
  protected InnsynskravDel fromDTO(InnsynskravDelDTO dto, InnsynskravDel innsynskravDel)
      throws EInnsynException {
    super.fromDTO(dto, innsynskravDel);

    // Set reference to innsynskrav
    if (innsynskravDel.getInnsynskrav() == null) {
      if (dto.getInnsynskrav() == null) {
        throw new BadRequestException("Innsynskrav is required");
      }
      var innsynskrav = innsynskravService.findById(dto.getInnsynskrav().getId());
      innsynskravDel.setInnsynskrav(innsynskrav);
      log.trace("innsynskravDel.setInnsynskrav({})", innsynskravDel.getInnsynskrav().getId());
    }

    // Set reference to journalpost
    if (innsynskravDel.getEnhet() == null) {
      if (dto.getJournalpost() == null) {
        throw new BadRequestException("Journalpost is required");
      }
      var journalpost = journalpostService.findById(dto.getJournalpost().getId());
      innsynskravDel.setJournalpost(journalpost);
      log.trace("innsynskravDel.setJournalpost({})", innsynskravDel.getJournalpost().getId());
    }

    // Set reference to the Journalpost's Journalenhet
    if (innsynskravDel.getEnhet() == null) {
      var journalpost = innsynskravDel.getJournalpost();
      // .journalenhet is lazy loaded, get an un-proxied object:
      var enhet = (Enhet) Hibernate.unproxy(journalpost.getJournalenhet());
      innsynskravDel.setEnhet(enhet);
      log.trace("innsynskravDel.setEnhet({})", innsynskravDel.getEnhet().getId());
    }

    // Create initial innsynskravDelStatus
    var createdStatus = new InnsynskravDelStatus();
    createdStatus.setStatus(InnsynskravDelStatusValue.OPPRETTET);
    createdStatus.setSystemgenerert(true);
    createdStatus.setOpprettetDato(new Date());

    var statusList = new ArrayList<InnsynskravDelStatus>();
    statusList.add(createdStatus);
    innsynskravDel.setStatus(statusList);

    // These are readOnly values in the API, but we use them internally
    if (dto.getSent() != null) {
      innsynskravDel.setSent(TimeConverter.timestampToInstant(dto.getSent()));
      log.trace("innsynskravDel.setSent({})", innsynskravDel.getSent());
    }

    if (dto.getRetryCount() != null) {
      innsynskravDel.setRetryCount(dto.getRetryCount());
      log.trace("innsynskravDel.setRetryCount({})", innsynskravDel.getRetryCount());
    }

    if (dto.getRetryTimestamp() != null) {
      innsynskravDel.setRetryTimestamp(TimeConverter.timestampToInstant(dto.getRetryTimestamp()));
      log.trace("innsynskravDel.setRetryTimestamp({})", innsynskravDel.getRetryTimestamp());
    }

    return innsynskravDel;
  }

  /**
   * Convert InnsynskravDel to DTO.
   *
   * @param innsynskravDel The InnsynskravDel to convert
   * @param dto The DTO to fill
   * @param expandPaths The paths to expandableFields that should be expanded
   * @param currentPath The current path in the tree
   * @return The DTO
   */
  @Override
  protected InnsynskravDelDTO toDTO(
      InnsynskravDel innsynskravDel,
      InnsynskravDelDTO dto,
      Set<String> expandPaths,
      String currentPath) {
    dto = super.toDTO(innsynskravDel, dto, expandPaths, currentPath);

    var journalpost = innsynskravDel.getJournalpost();
    dto.setJournalpost(
        journalpostService.maybeExpand(journalpost, "journalpost", expandPaths, currentPath));

    var enhet = innsynskravDel.getEnhet();
    dto.setEnhet(enhetService.maybeExpand(enhet, "enhet", expandPaths, currentPath));

    if (innsynskravDel.getSent() != null) {
      dto.setSent(innsynskravDel.getSent().toString());
    }

    dto.setEmail(innsynskravDel.getInnsynskrav().getEpost());

    return dto;
  }

  @Override
  protected Paginators<InnsynskravDel> getPaginators(BaseListQueryDTO params) {
    if (params instanceof InnsynskravDelListQueryDTO p) {
      if (p.getBrukerId() != null) {
        var bruker = brukerService.findById(p.getBrukerId());
        return new Paginators<>(
            (pivot, pageRequest) -> repository.paginateAsc(bruker, pivot, pageRequest),
            (pivot, pageRequest) -> repository.paginateDesc(bruker, pivot, pageRequest));
      } else if (p.getInnsynskravId() != null) {
        var innsynskrav = innsynskravService.findById(p.getInnsynskravId());
        return new Paginators<>(
            (pivot, pageRequest) -> repository.paginateAsc(innsynskrav, pivot, pageRequest),
            (pivot, pageRequest) -> repository.paginateDesc(innsynskrav, pivot, pageRequest));
      } else if (p.getEnhetId() != null) {
        var enhet = enhetService.findById(p.getEnhetId());
        return new Paginators<>(
            (pivot, pageRequest) -> repository.paginateAsc(enhet, pivot, pageRequest),
            (pivot, pageRequest) -> repository.paginateDesc(enhet, pivot, pageRequest));
      }
    }
    return super.getPaginators(params);
  }

  /**
   * Authorize the list operation. Admins and users with access to the innsynskrav can list
   * InnsynskravDel objects.
   *
   * @param params The list query
   * @throws ForbiddenException If not authorized
   */
  @Override
  protected void authorizeList(BaseListQueryDTO params) throws EInnsynException {
    if (authenticationService.isAdmin()) {
      return;
    }

    // Allow listing one's own InnsynskravDels
    if (params instanceof InnsynskravDelListQueryDTO p
        && p.getBrukerId() != null
        && authenticationService.isSelf(p.getBrukerId())) {
      return;
    }

    // Allow if the user is the owner of the Innsynskrav
    if (params instanceof InnsynskravDelListQueryDTO p && p.getInnsynskravId() != null) {
      var innsynskrav = innsynskravService.findById(p.getInnsynskravId());
      var innsynskravBruker = innsynskrav.getBruker();
      if (innsynskravBruker != null && authenticationService.isSelf(innsynskravBruker.getId())) {
        return;
      }
    }

    // Allow when authenticated as the Enhet
    if (params instanceof InnsynskravDelListQueryDTO p && p.getEnhetId() != null) {
      var loggedInAs = authenticationService.getJournalenhetId();
      if (enhetService.isAncestorOf(loggedInAs, p.getEnhetId())) {
        return;
      }
    }

    throw new ForbiddenException("Not authorized to list InnsynskravDel");
  }

  /**
   * Authorize the get operation. Admins and users with access to the innsynskrav can get
   * InnsynskravDel objects.
   *
   * @param id The id of the InnsynskravDel
   * @throws ForbiddenException If not authorized
   */
  @Override
  protected void authorizeGet(String id) throws EInnsynException {
    if (authenticationService.isAdmin()) {
      return;
    }

    var innsynskravDel = innsynskravDelService.findById(id);
    var innsynskrav = innsynskravDel.getInnsynskrav();
    var innsynskravBruker = innsynskrav.getBruker();
    if (innsynskravBruker != null && authenticationService.isSelf(innsynskravBruker.getId())) {
      return;
    }

    // Owning Enhet can get the innsynskrav
    var loggedInAs = authenticationService.getJournalenhetId();
    var innsynskravDelEnhet = innsynskravDel.getEnhet();
    if (loggedInAs != null
        && innsynskravDelEnhet != null
        && enhetService.isAncestorOf(loggedInAs, innsynskravDelEnhet.getId())) {
      return;
    }

    throw new ForbiddenException("Not authorized to get " + id);
  }

  /**
   * Authorize the add operation. A InnsynskravDel requires a Innsynskrav. If the Innsynskrav has a
   * Bruker, only the Bruker can add InnsynskravDel objects. If not, anybody can add unless the
   * Innsynskrav is sent.
   *
   * @param dto The InnsynskravDelDTO to add
   * @throws ForbiddenException If not authorized
   */
  @Override
  protected void authorizeAdd(InnsynskravDelDTO dto) throws EInnsynException {
    var innsynskravDTO = dto.getInnsynskrav();
    if (innsynskravDTO == null) {
      throw new ForbiddenException("Innsynskrav is required");
    }

    var innsynskrav = innsynskravService.findById(innsynskravDTO.getId());
    if (innsynskrav == null) {
      throw new ForbiddenException("Innsynskrav " + innsynskravDTO.getId() + " not found");
    }

    var innsynskravBruker = innsynskrav.getBruker();
    if (innsynskravBruker != null && !authenticationService.isSelf(innsynskravBruker.getId())) {
      throw new ForbiddenException(
          "Not authorized to add InnsynskravDel to " + innsynskravDTO.getId());
    }

    if (innsynskrav.isLocked()) {
      throw new ForbiddenException("Innsynskrav " + innsynskravDTO.getId() + " is already sent");
    }
  }

  /**
   * Authorize the update operation. Admins and users with access to the innsynskrav can update
   * InnsynskravDel objects.
   *
   * @param id The id of the InnsynskravDel
   * @param dto The InnsynskravDelDTO to update
   * @throws ForbiddenException If not authorized
   */
  @Override
  protected void authorizeUpdate(String id, InnsynskravDelDTO dto) throws EInnsynException {
    var innsynskravDel = innsynskravDelService.findById(id);
    var innsynskrav = innsynskravDel.getInnsynskrav();
    if (innsynskrav == null) {
      throw new ForbiddenException("Innsynskrav not found");
    }

    if (innsynskrav.isLocked()) {
      throw new ForbiddenException("Innsynskrav is already sent");
    }

    if (authenticationService.isAdmin()) {
      return;
    }

    var innsynskravBruker = innsynskrav.getBruker();
    if (innsynskravBruker != null && authenticationService.isSelf(innsynskravBruker.getId())) {
      return;
    }

    throw new ForbiddenException("Not authorized to update " + dto.getId());
  }

  /**
   * Authorize the delete operation. Admins and users with access to the innsynskrav can delete
   * InnsynskravDel objects.
   *
   * @param id The id of the InnsynskravDel
   * @throws ForbiddenException If not authorized
   */
  @Override
  protected void authorizeDelete(String id) throws EInnsynException {
    var innsynskravDel = innsynskravDelService.findById(id);
    var innsynskrav = innsynskravDel.getInnsynskrav();
    if (innsynskrav == null) {
      throw new ForbiddenException("Innsynskrav not found");
    }

    if (authenticationService.isAdmin()) {
      return;
    }

    // Owner of the Journalpost can delete
    var journalpost = innsynskravDel.getJournalpost();
    if (journalpost != null) {
      try {
        journalpostService.authorizeDelete(journalpost.getId());
        return;
      } catch (ForbiddenException e) {
      }
    }

    var innsynskravBruker = innsynskrav.getBruker();
    if (innsynskravBruker != null && authenticationService.isSelf(innsynskravBruker.getId())) {
      return;
    }

    throw new ForbiddenException("Not authorized to delete " + id);
  }
}

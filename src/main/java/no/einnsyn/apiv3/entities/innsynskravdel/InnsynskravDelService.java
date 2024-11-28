package no.einnsyn.apiv3.entities.innsynskravdel;

import java.util.Date;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.common.paginators.Paginators;
import no.einnsyn.apiv3.entities.base.BaseService;
import no.einnsyn.apiv3.entities.base.models.BaseES;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDel;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelDTO;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelES;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelListQueryDTO;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelStatus;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelStatusValue;
import no.einnsyn.apiv3.error.exceptions.BadRequestException;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import no.einnsyn.apiv3.error.exceptions.ForbiddenException;
import no.einnsyn.apiv3.utils.TimeConverter;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class InnsynskravDelService extends BaseService<InnsynskravDel, InnsynskravDelDTO> {

  @Getter private final InnsynskravDelRepository repository;

  @SuppressWarnings("java:S6813")
  @Lazy
  @Autowired
  @Getter
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

    // Set reference to InnsynskravBestilling
    if (innsynskravDel.getInnsynskravBestilling() == null) {
      if (dto.getInnsynskravBestilling() == null) {
        throw new BadRequestException("InnsynskravBestilling is required");
      }
      var innsynskravBestilling =
          innsynskravBestillingService.findById(dto.getInnsynskravBestilling().getId());
      innsynskravDel.setInnsynskravBestilling(innsynskravBestilling);
      log.trace(
          "innsynskravDel.setInnsynskravBestilling({})",
          innsynskravDel.getInnsynskravBestilling().getId());
    }

    // Set reference to journalpost
    if (innsynskravDel.getEnhet() == null) {
      if (dto.getJournalpost() == null) {
        throw new BadRequestException("Journalpost is required");
      }
      var journalpostId = dto.getJournalpost().getId();
      var journalpost = journalpostService.findById(journalpostId);
      innsynskravDel.setJournalpost(journalpost);
      log.trace("innsynskravDel.setJournalpost({})", journalpostId);
    }

    // Set reference to the Journalpost's Journalenhet
    if (innsynskravDel.getEnhet() == null) {
      var journalpost = innsynskravDel.getJournalpost();
      // .journalenhet is lazy loaded, get an un-proxied object:
      if (journalpost != null) {
        var enhet = (Enhet) Hibernate.unproxy(journalpost.getJournalenhet());
        innsynskravDel.setEnhet(enhet);
        log.trace("innsynskravDel.setEnhet({})", innsynskravDel.getEnhet().getId());
      }
    }

    // Create initial innsynskravDelStatus
    var createdStatus = new InnsynskravDelStatus();
    createdStatus.setStatus(InnsynskravDelStatusValue.OPPRETTET);
    createdStatus.setSystemgenerert(true);
    createdStatus.setOpprettetDato(new Date());
    innsynskravDel.getLegacyStatus().add(createdStatus);

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

    var innsynskravBestilling = innsynskravDel.getInnsynskravBestilling();
    dto.setInnsynskravBestilling(
        innsynskravBestillingService.maybeExpand(
            innsynskravBestilling, "innsynskravBestilling", expandPaths, currentPath));

    if (innsynskravDel.getSent() != null) {
      dto.setSent(innsynskravDel.getSent().toString());
    }

    dto.setEmail(innsynskravDel.getInnsynskravBestilling().getEpost());

    return dto;
  }

  @Override
  public BaseES toLegacyES(InnsynskravDel innsynskravDel) {
    return toLegacyES(innsynskravDel, new InnsynskravDelES());
  }

  @Override
  public BaseES toLegacyES(InnsynskravDel innsynskravDel, BaseES es) {
    super.toLegacyES(innsynskravDel, es);
    if (es instanceof InnsynskravDelES innsynskravDelES) {
      innsynskravDelES.setSorteringstype("innsynskrav");
      innsynskravDelES.setCreated(innsynskravDel.getCreated().toString());
      if (innsynskravDel.getSent() != null) {
        innsynskravDelES.setSent(innsynskravDel.getSent().toString());
      }
      innsynskravDelES.setVerified(innsynskravDel.getInnsynskravBestilling().isVerified());

      var statistics = new InnsynskravDelES.InnsynskravStat();
      var journalpost = innsynskravDel.getJournalpost();
      if (journalpost != null) {
        statistics.setParent(journalpost.getId());
      }
      innsynskravDelES.setStatRelation(statistics);

      var bruker = innsynskravDel.getInnsynskravBestilling().getBruker();
      if (bruker != null) {
        innsynskravDelES.setBruker(bruker.getId());
      }
    }
    return es;
  }

  @Override
  @Transactional(readOnly = true)
  public String getESParent(String id) {
    var innsynskravDel = getProxy().findById(id);
    if (innsynskravDel != null) {
      var journalpost = innsynskravDel.getJournalpost();
      if (journalpost != null) {
        return journalpost.getId();
      }
    }
    return null;
  }

  @Override
  protected Paginators<InnsynskravDel> getPaginators(BaseListQueryDTO params) {
    if (params instanceof InnsynskravDelListQueryDTO p) {
      if (p.getBrukerId() != null) {
        var bruker = brukerService.findById(p.getBrukerId());
        return new Paginators<>(
            (pivot, pageRequest) -> repository.paginateAsc(bruker, pivot, pageRequest),
            (pivot, pageRequest) -> repository.paginateDesc(bruker, pivot, pageRequest));
      } else if (p.getInnsynskravBestillingId() != null) {
        var innsynskravBestilling =
            innsynskravBestillingService.findById(p.getInnsynskravBestillingId());
        return new Paginators<>(
            (pivot, pageRequest) ->
                repository.paginateAsc(innsynskravBestilling, pivot, pageRequest),
            (pivot, pageRequest) ->
                repository.paginateDesc(innsynskravBestilling, pivot, pageRequest));
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
   * Authorize the list operation. Admins and users with access to the InnsynskravBestilling can
   * list InnsynskravDel objects.
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

    // Allow if the user is the owner of the InnsynskravBestilling
    if (params instanceof InnsynskravDelListQueryDTO p && p.getInnsynskravBestillingId() != null) {
      var innsynskravBestilling =
          innsynskravBestillingService.findById(p.getInnsynskravBestillingId());
      var innsynskravBruker = innsynskravBestilling.getBruker();
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
   * Authorize the get operation. Admins and users with access to the InnsynskravBestilling can get
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
    var innsynskravBestilling = innsynskravDel.getInnsynskravBestilling();
    var innsynskravBruker = innsynskravBestilling.getBruker();
    if (innsynskravBruker != null && authenticationService.isSelf(innsynskravBruker.getId())) {
      return;
    }

    // Owning Enhet can get the InnsynskravBestilling
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
   * Authorize the add operation. A InnsynskravDel requires a InnsynskravBestilling. If the
   * InnsynskravBestilling has a Bruker, only the Bruker can add InnsynskravDel objects. If not,
   * anybody can add unless the InnsynskravBestilling is sent.
   *
   * @param dto The InnsynskravDelDTO to add
   * @throws ForbiddenException If not authorized
   */
  @Override
  protected void authorizeAdd(InnsynskravDelDTO dto) throws EInnsynException {
    var innsynskravBestillingDTO = dto.getInnsynskravBestilling();
    if (innsynskravBestillingDTO == null) {
      throw new ForbiddenException("InnsynskravBestilling is required");
    }

    var innsynskravBestilling =
        innsynskravBestillingService.findById(innsynskravBestillingDTO.getId());
    if (innsynskravBestilling == null) {
      throw new ForbiddenException(
          "InnsynskravBestilling " + innsynskravBestillingDTO.getId() + " not found");
    }

    var innsynskravBruker = innsynskravBestilling.getBruker();
    if (innsynskravBruker != null && !authenticationService.isSelf(innsynskravBruker.getId())) {
      throw new ForbiddenException(
          "Not authorized to add InnsynskravDel to " + innsynskravBestillingDTO.getId());
    }

    if (innsynskravBestilling.isLocked()) {
      throw new ForbiddenException(
          "InnsynskravBestilling " + innsynskravBestillingDTO.getId() + " is already sent");
    }
  }

  /**
   * Authorize the update operation. Admins and users with access to the InnsynskravBestilling can
   * update InnsynskravDel objects.
   *
   * @param id The id of the InnsynskravDel
   * @param dto The InnsynskravDelDTO to update
   * @throws ForbiddenException If not authorized
   */
  @Override
  protected void authorizeUpdate(String id, InnsynskravDelDTO dto) throws EInnsynException {
    var innsynskravDel = innsynskravDelService.findById(id);
    var innsynskravBestilling = innsynskravDel.getInnsynskravBestilling();
    if (innsynskravBestilling == null) {
      throw new ForbiddenException("InnsynskravBestilling not found");
    }

    if (innsynskravBestilling.isLocked()) {
      throw new ForbiddenException("InnsynskravBestilling is already sent");
    }

    if (authenticationService.isAdmin()) {
      return;
    }

    var innsynskravBruker = innsynskravBestilling.getBruker();
    if (innsynskravBruker != null && authenticationService.isSelf(innsynskravBruker.getId())) {
      return;
    }

    throw new ForbiddenException("Not authorized to update " + dto.getId());
  }

  /**
   * Authorize the delete operation. Admins and users with access to the InnsynskravBestilling can
   * delete InnsynskravDel objects.
   *
   * @param id The id of the InnsynskravDel
   * @throws ForbiddenException If not authorized
   */
  @Override
  protected void authorizeDelete(String id) throws EInnsynException {
    var innsynskravDel = innsynskravDelService.findById(id);
    var innsynskravBestilling = innsynskravDel.getInnsynskravBestilling();
    if (innsynskravBestilling == null) {
      throw new ForbiddenException("InnsynskravBestilling not found");
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

    var innsynskravBruker = innsynskravBestilling.getBruker();
    if (innsynskravBruker != null && authenticationService.isSelf(innsynskravBruker.getId())) {
      return;
    }

    throw new ForbiddenException("Not authorized to delete " + id);
  }
}

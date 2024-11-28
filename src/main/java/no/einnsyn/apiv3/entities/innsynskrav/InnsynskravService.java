package no.einnsyn.apiv3.entities.innsynskrav;

import java.util.Date;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.common.paginators.Paginators;
import no.einnsyn.apiv3.entities.base.BaseService;
import no.einnsyn.apiv3.entities.base.models.BaseES;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.innsynskrav.models.Innsynskrav;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravES;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravListQueryDTO;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravStatus;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravStatusValue;
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
public class InnsynskravService extends BaseService<Innsynskrav, InnsynskravDTO> {

  @Getter private final InnsynskravRepository repository;

  @SuppressWarnings("java:S6813")
  @Lazy
  @Autowired
  @Getter
  private InnsynskravService proxy;

  public InnsynskravService(InnsynskravRepository repository) {
    super();
    this.repository = repository;
  }

  public Innsynskrav newObject() {
    return new Innsynskrav();
  }

  public InnsynskravDTO newDTO() {
    return new InnsynskravDTO();
  }

  /**
   * Convert DTO to Innsynskrav.
   *
   * @param dto The DTO to convert
   * @param innsynskrav The Innsynskrav to fill
   * @return The Innsynskrav
   */
  @Override
  protected Innsynskrav fromDTO(InnsynskravDTO dto, Innsynskrav innsynskrav)
      throws EInnsynException {
    super.fromDTO(dto, innsynskrav);

    // Set reference to InnsynskravBestilling
    if (innsynskrav.getInnsynskravBestilling() == null) {
      if (dto.getInnsynskravBestilling() == null) {
        throw new BadRequestException("InnsynskravBestilling is required");
      }
      var innsynskravBestilling =
          innsynskravBestillingService.findById(dto.getInnsynskravBestilling().getId());
      innsynskrav.setInnsynskravBestilling(innsynskravBestilling);
      log.trace(
          "innsynskrav.setInnsynskravBestilling({})",
          innsynskrav.getInnsynskravBestilling().getId());
    }

    // Set reference to journalpost
    if (innsynskrav.getEnhet() == null) {
      if (dto.getJournalpost() == null) {
        throw new BadRequestException("Journalpost is required");
      }
      var journalpostId = dto.getJournalpost().getId();
      var journalpost = journalpostService.findById(journalpostId);
      innsynskrav.setJournalpost(journalpost);
      log.trace("innsynskrav.setJournalpost({})", journalpostId);
    }

    // Set reference to the Journalpost's Journalenhet
    if (innsynskrav.getEnhet() == null) {
      var journalpost = innsynskrav.getJournalpost();
      // .journalenhet is lazy loaded, get an un-proxied object:
      if (journalpost != null) {
        var enhet = (Enhet) Hibernate.unproxy(journalpost.getJournalenhet());
        innsynskrav.setEnhet(enhet);
        log.trace("innsynskrav.setEnhet({})", innsynskrav.getEnhet().getId());
      }
    }

    // Create initial innsynskravStatus
    var createdStatus = new InnsynskravStatus();
    createdStatus.setStatus(InnsynskravStatusValue.OPPRETTET);
    createdStatus.setSystemgenerert(true);
    createdStatus.setOpprettetDato(new Date());
    innsynskrav.getLegacyStatus().add(createdStatus);

    // These are readOnly values in the API, but we use them internally
    if (dto.getSent() != null) {
      innsynskrav.setSent(TimeConverter.timestampToInstant(dto.getSent()));
      log.trace("innsynskrav.setSent({})", innsynskrav.getSent());
    }

    if (dto.getRetryCount() != null) {
      innsynskrav.setRetryCount(dto.getRetryCount());
      log.trace("innsynskrav.setRetryCount({})", innsynskrav.getRetryCount());
    }

    if (dto.getRetryTimestamp() != null) {
      innsynskrav.setRetryTimestamp(TimeConverter.timestampToInstant(dto.getRetryTimestamp()));
      log.trace("innsynskrav.setRetryTimestamp({})", innsynskrav.getRetryTimestamp());
    }

    return innsynskrav;
  }

  /**
   * Convert Innsynskrav to DTO.
   *
   * @param innsynskrav The Innsynskrav to convert
   * @param dto The DTO to fill
   * @param expandPaths The paths to expandableFields that should be expanded
   * @param currentPath The current path in the tree
   * @return The DTO
   */
  @Override
  protected InnsynskravDTO toDTO(
      Innsynskrav innsynskrav, InnsynskravDTO dto, Set<String> expandPaths, String currentPath) {
    dto = super.toDTO(innsynskrav, dto, expandPaths, currentPath);

    var journalpost = innsynskrav.getJournalpost();
    dto.setJournalpost(
        journalpostService.maybeExpand(journalpost, "journalpost", expandPaths, currentPath));

    var enhet = innsynskrav.getEnhet();
    dto.setEnhet(enhetService.maybeExpand(enhet, "enhet", expandPaths, currentPath));

    var innsynskravBestilling = innsynskrav.getInnsynskravBestilling();
    dto.setInnsynskravBestilling(
        innsynskravBestillingService.maybeExpand(
            innsynskravBestilling, "innsynskravBestilling", expandPaths, currentPath));

    if (innsynskrav.getSent() != null) {
      dto.setSent(innsynskrav.getSent().toString());
    }

    dto.setEmail(innsynskrav.getInnsynskravBestilling().getEpost());

    return dto;
  }

  @Override
  public BaseES toLegacyES(Innsynskrav innsynskrav) {
    return toLegacyES(innsynskrav, new InnsynskravES());
  }

  @Override
  public BaseES toLegacyES(Innsynskrav innsynskrav, BaseES es) {
    super.toLegacyES(innsynskrav, es);
    if (es instanceof InnsynskravES innsynskravES) {
      innsynskravES.setSorteringstype("innsynskrav");
      innsynskravES.setCreated(innsynskrav.getCreated().toString());
      if (innsynskrav.getSent() != null) {
        innsynskravES.setSent(innsynskrav.getSent().toString());
      }
      innsynskravES.setVerified(innsynskrav.getInnsynskravBestilling().isVerified());

      var statistics = new InnsynskravES.InnsynskravStat();
      var journalpost = innsynskrav.getJournalpost();
      if (journalpost != null) {
        statistics.setParent(journalpost.getId());
      }
      innsynskravES.setStatRelation(statistics);

      var bruker = innsynskrav.getInnsynskravBestilling().getBruker();
      if (bruker != null) {
        innsynskravES.setBruker(bruker.getId());
      }
    }
    return es;
  }

  @Override
  @Transactional(readOnly = true)
  public String getESParent(String id) {
    var innsynskrav = getProxy().findById(id);
    if (innsynskrav != null) {
      var journalpost = innsynskrav.getJournalpost();
      if (journalpost != null) {
        return journalpost.getId();
      }
    }
    return null;
  }

  @Override
  protected Paginators<Innsynskrav> getPaginators(BaseListQueryDTO params) {
    if (params instanceof InnsynskravListQueryDTO p) {
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
   * list Innsynskrav objects.
   *
   * @param params The list query
   * @throws ForbiddenException If not authorized
   */
  @Override
  protected void authorizeList(BaseListQueryDTO params) throws EInnsynException {
    if (authenticationService.isAdmin()) {
      return;
    }

    // Allow listing one's own Innsynskravs
    if (params instanceof InnsynskravListQueryDTO p
        && p.getBrukerId() != null
        && authenticationService.isSelf(p.getBrukerId())) {
      return;
    }

    // Allow if the user is the owner of the InnsynskravBestilling
    if (params instanceof InnsynskravListQueryDTO p && p.getInnsynskravBestillingId() != null) {
      var innsynskravBestilling =
          innsynskravBestillingService.findById(p.getInnsynskravBestillingId());
      var innsynskravBruker = innsynskravBestilling.getBruker();
      if (innsynskravBruker != null && authenticationService.isSelf(innsynskravBruker.getId())) {
        return;
      }
    }

    // Allow when authenticated as the Enhet
    if (params instanceof InnsynskravListQueryDTO p && p.getEnhetId() != null) {
      var loggedInAs = authenticationService.getJournalenhetId();
      if (enhetService.isAncestorOf(loggedInAs, p.getEnhetId())) {
        return;
      }
    }

    throw new ForbiddenException("Not authorized to list Innsynskrav");
  }

  /**
   * Authorize the get operation. Admins and users with access to the InnsynskravBestilling can get
   * Innsynskrav objects.
   *
   * @param id The id of the Innsynskrav
   * @throws ForbiddenException If not authorized
   */
  @Override
  protected void authorizeGet(String id) throws EInnsynException {
    if (authenticationService.isAdmin()) {
      return;
    }

    var innsynskrav = innsynskravService.findById(id);
    var innsynskravBestilling = innsynskrav.getInnsynskravBestilling();
    var innsynskravBruker = innsynskravBestilling.getBruker();
    if (innsynskravBruker != null && authenticationService.isSelf(innsynskravBruker.getId())) {
      return;
    }

    // Owning Enhet can get the InnsynskravBestilling
    var loggedInAs = authenticationService.getJournalenhetId();
    var innsynskravEnhet = innsynskrav.getEnhet();
    if (loggedInAs != null
        && innsynskravEnhet != null
        && enhetService.isAncestorOf(loggedInAs, innsynskravEnhet.getId())) {
      return;
    }

    throw new ForbiddenException("Not authorized to get " + id);
  }

  /**
   * Authorize the add operation. A Innsynskrav requires a InnsynskravBestilling. If the
   * InnsynskravBestilling has a Bruker, only the Bruker can add Innsynskrav objects. If not,
   * anybody can add unless the InnsynskravBestilling is sent.
   *
   * @param dto The InnsynskravDTO to add
   * @throws ForbiddenException If not authorized
   */
  @Override
  protected void authorizeAdd(InnsynskravDTO dto) throws EInnsynException {
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
          "Not authorized to add Innsynskrav to " + innsynskravBestillingDTO.getId());
    }

    if (innsynskravBestilling.isLocked()) {
      throw new ForbiddenException(
          "InnsynskravBestilling " + innsynskravBestillingDTO.getId() + " is already sent");
    }
  }

  /**
   * Authorize the update operation. Admins and users with access to the InnsynskravBestilling can
   * update Innsynskrav objects.
   *
   * @param id The id of the Innsynskrav
   * @param dto The InnsynskravDTO to update
   * @throws ForbiddenException If not authorized
   */
  @Override
  protected void authorizeUpdate(String id, InnsynskravDTO dto) throws EInnsynException {
    var innsynskrav = innsynskravService.findById(id);
    var innsynskravBestilling = innsynskrav.getInnsynskravBestilling();
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
   * delete Innsynskrav objects.
   *
   * @param id The id of the Innsynskrav
   * @throws ForbiddenException If not authorized
   */
  @Override
  protected void authorizeDelete(String id) throws EInnsynException {
    var innsynskrav = innsynskravService.findById(id);
    var innsynskravBestilling = innsynskrav.getInnsynskravBestilling();
    if (innsynskravBestilling == null) {
      throw new ForbiddenException("InnsynskravBestilling not found");
    }

    if (authenticationService.isAdmin()) {
      return;
    }

    // Owner of the Journalpost can delete
    var journalpost = innsynskrav.getJournalpost();
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

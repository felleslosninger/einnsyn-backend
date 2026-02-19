package no.einnsyn.backend.entities.innsynskrav;

import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.common.exceptions.models.AuthorizationException;
import no.einnsyn.backend.common.exceptions.models.BadRequestException;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.paginators.Paginators;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.entities.base.BaseService;
import no.einnsyn.backend.entities.base.models.BaseES;
import no.einnsyn.backend.entities.bruker.models.ListByBrukerParameters;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import no.einnsyn.backend.entities.enhet.models.ListByEnhetParameters;
import no.einnsyn.backend.entities.innsynskrav.models.Innsynskrav;
import no.einnsyn.backend.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.backend.entities.innsynskrav.models.InnsynskravES;
import no.einnsyn.backend.entities.innsynskrav.models.InnsynskravStatus;
import no.einnsyn.backend.entities.innsynskrav.models.InnsynskravStatusValue;
import no.einnsyn.backend.entities.innsynskravbestilling.models.ListByInnsynskravBestillingParameters;
import no.einnsyn.backend.utils.TimeConverter;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class InnsynskravService extends BaseService<Innsynskrav, InnsynskravDTO> {

  @Getter(onMethod_ = @Override)
  private final InnsynskravRepository repository;

  @SuppressWarnings("java:S6813")
  @Lazy
  @Autowired
  @Getter(onMethod_ = @Override)
  private InnsynskravService proxy;

  public InnsynskravService(InnsynskravRepository repository) {
    super();
    this.repository = repository;
  }

  @Override
  public Innsynskrav newObject() {
    return new Innsynskrav();
  }

  @Override
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
          innsynskravBestillingService.findByIdOrThrow(dto.getInnsynskravBestilling().getId());
      innsynskrav.setInnsynskravBestilling(innsynskravBestilling);
      log.trace(
          "innsynskrav.setInnsynskravBestilling({})",
          innsynskrav.getInnsynskravBestilling().getId());
    }

    // Set reference to journalpost
    if (innsynskrav.getJournalpost() == null) {
      if (dto.getJournalpost() == null) {
        throw new BadRequestException("Journalpost is required");
      }
      var journalpostId = dto.getJournalpost().getId();
      var journalpost = journalpostService.findByIdOrThrow(journalpostId);
      innsynskrav.setJournalpost(journalpost);
      log.trace("innsynskrav.setJournalpost({})", journalpostId);
    }

    // Set reference to the Journalpost's Journalenhet
    if (innsynskrav.getEnhet() == null) {
      var journalpost = innsynskrav.getJournalpost();
      // .journalenhet is lazy loaded, get an un-proxied object:
      if (journalpost != null) {
        // If "avhendetTil" is set, use that as the Enhet
        var avhendetTilEnhet = (Enhet) Hibernate.unproxy(journalpost.getAvhendetTil());
        if (avhendetTilEnhet != null) {
          innsynskrav.setEnhet(avhendetTilEnhet);
        } else {
          var enhet = (Enhet) Hibernate.unproxy(journalpost.getAdministrativEnhetObjekt());
          innsynskrav.setEnhet(enhet);
        }
        log.trace("innsynskrav.setEnhet({})", innsynskrav.getEnhet().getId());
      }
    }

    // Create initial innsynskravStatus
    var createdStatus = new InnsynskravStatus();
    createdStatus.setStatus(InnsynskravStatusValue.OPPRETTET);
    createdStatus.setSystemgenerert(true);
    createdStatus.setOpprettetDato(new Date());
    innsynskrav.getLegacyStatus().add(createdStatus);

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
    if (innsynskravBestilling != null) {
      dto.setEmail(innsynskravBestilling.getEpost());
    }

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
      if (innsynskrav.getSent() != null) {
        innsynskravES.setSent(TimeConverter.instantToTimestamp(innsynskrav.getSent()));
      }

      var parentId = getProxy().getESParent(innsynskrav, innsynskrav.getId());
      if (parentId != null) {
        var statistics = new InnsynskravES.InnsynskravStat();
        statistics.setParent(parentId);
        innsynskravES.setStatRelation(statistics);
      }

      var journalenhet = innsynskrav.getEnhet();
      if (journalenhet != null) {
        innsynskravES.setJournalenhet(journalenhet.getId());
      }

      var innsynskravBestilling = innsynskrav.getInnsynskravBestilling();
      if (innsynskravBestilling != null) {
        innsynskravES.setVerified(innsynskravBestilling.isVerified());
        var bruker = innsynskravBestilling.getBruker();
        if (bruker != null) {
          innsynskravES.setBruker(bruker.getId());
        }
      }
    }
    return es;
  }

  @Override
  @Transactional(readOnly = true)
  public String getESParent(Innsynskrav innsynskrav, String id) {
    if (innsynskrav != null) {
      var journalpost = innsynskrav.getJournalpost();
      if (journalpost != null) {
        return journalpost.getId();
      }
    }
    // Try to get the parent from the ES index. This is needed when the parent is deleted before the
    // child, and we need the parent ID to delete the child from ES.
    try {
      esClient.indices().refresh(r -> r.index(elasticsearchIndex));
      var esResponse =
          esClient.search(
              sr -> sr.index(elasticsearchIndex).query(q -> q.ids(ids -> ids.values(List.of(id)))),
              Void.class);
      return esResponse.hits().hits().getFirst().routing();
    } catch (Exception e) {
      if (innsynskrav != null) {
        var journalpost = innsynskrav.getJournalpost();
        var journalpostId = journalpost != null ? journalpost.getId() : null;
        if (journalpostId == null) {
          log.warn(
              "Failed to get parent for Innsynskrav: {}. Journalpost was not found.",
              innsynskrav.getId());
        } else {
          log.error(
              "Failed to get parent for Innsynskrav: {}, parent: {}",
              innsynskrav.getId(),
              journalpostId,
              e);
        }
      } else {
        log.error("Failed to get parent for Innsynskrav {}", id, e);
      }
    }

    return null;
  }

  @Override
  protected Paginators<Innsynskrav> getPaginators(ListParameters params) throws EInnsynException {
    if (params instanceof ListByBrukerParameters p && p.getBrukerId() != null) {
      var bruker = brukerService.findByIdOrThrow(p.getBrukerId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(bruker, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(bruker, pivot, pageRequest));
    }
    if (params instanceof ListByInnsynskravBestillingParameters p
        && p.getInnsynskravBestillingId() != null) {
      var innsynskravBestilling =
          innsynskravBestillingService.findByIdOrThrow(p.getInnsynskravBestillingId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(innsynskravBestilling, pivot, pageRequest),
          (pivot, pageRequest) ->
              repository.paginateDesc(innsynskravBestilling, pivot, pageRequest));
    }
    if (params instanceof ListByEnhetParameters p && p.getEnhetId() != null) {
      var enhet = enhetService.findByIdOrThrow(p.getEnhetId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(enhet, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(enhet, pivot, pageRequest));
    }

    return super.getPaginators(params);
  }

  /**
   * Authorize the list operation. Admins and users with access to the InnsynskravBestilling can
   * list Innsynskrav objects.
   *
   * @param params The list query
   * @throws AuthorizationException If not authorized
   */
  @Override
  protected void authorizeList(ListParameters params) throws EInnsynException {
    if (authenticationService.isAdmin()) {
      return;
    }

    // Allow listing one's own Innsynskravs
    if (params instanceof ListByBrukerParameters p
        && p.getBrukerId() != null
        && authenticationService.isSelf(p.getBrukerId())) {
      return;
    }

    // Allow if the user is the owner of the InnsynskravBestilling
    if (params instanceof ListByInnsynskravBestillingParameters p
        && p.getInnsynskravBestillingId() != null) {
      var innsynskravBestilling =
          innsynskravBestillingService.findByIdOrThrow(p.getInnsynskravBestillingId());
      var innsynskravBruker = innsynskravBestilling.getBruker();
      if (innsynskravBruker != null && authenticationService.isSelf(innsynskravBruker.getId())) {
        return;
      }
    }

    // Allow when authenticated as the Enhet
    if (params instanceof ListByEnhetParameters p && p.getEnhetId() != null) {
      var loggedInAs = authenticationService.getEnhetId();
      if (enhetService.isAncestorOf(loggedInAs, p.getEnhetId())) {
        return;
      }
    }

    throw new AuthorizationException("Not authorized to list Innsynskrav");
  }

  /**
   * Authorize the get operation. Admins and users with access to the InnsynskravBestilling can get
   * Innsynskrav objects.
   *
   * @param id The id of the Innsynskrav
   * @throws AuthorizationException If not authorized
   */
  @Override
  protected void authorizeGet(String id) throws EInnsynException {
    if (authenticationService.isAdmin()) {
      return;
    }

    var innsynskrav = innsynskravService.findByIdOrThrow(id);
    var innsynskravBestilling = innsynskrav.getInnsynskravBestilling();
    if (innsynskravBestilling != null) {
      var innsynskravBruker = innsynskravBestilling.getBruker();
      if (innsynskravBruker != null && authenticationService.isSelf(innsynskravBruker.getId())) {
        return;
      }
    }

    // Owning Enhet can get the InnsynskravBestilling
    var loggedInAs = authenticationService.getEnhetId();
    var innsynskravEnhet = innsynskrav.getEnhet();
    if (loggedInAs != null
        && innsynskravEnhet != null
        && enhetService.isAncestorOf(loggedInAs, innsynskravEnhet.getId())) {
      return;
    }

    throw new AuthorizationException("Not authorized to get " + id);
  }

  /**
   * Authorize the add operation. A Innsynskrav requires a InnsynskravBestilling. If the
   * InnsynskravBestilling has a Bruker, only the Bruker can add Innsynskrav objects. If not,
   * anybody can add unless the InnsynskravBestilling is sent.
   *
   * @param dto The InnsynskravDTO to add
   * @throws AuthorizationException If not authorized
   */
  @Override
  protected void authorizeAdd(InnsynskravDTO dto) throws EInnsynException {
    var innsynskravBestillingDTO = dto.getInnsynskravBestilling();
    if (innsynskravBestillingDTO == null) {
      throw new AuthorizationException("InnsynskravBestilling is required");
    }

    var innsynskravBestilling =
        innsynskravBestillingService.findByIdOrThrow(
            innsynskravBestillingDTO.getId(), AuthorizationException.class);

    var innsynskravBruker = innsynskravBestilling.getBruker();
    if (innsynskravBruker != null && !authenticationService.isSelf(innsynskravBruker.getId())) {
      throw new AuthorizationException(
          "Not authorized to add Innsynskrav to " + innsynskravBestillingDTO.getId());
    }

    if (innsynskravBestilling.isLocked()) {
      throw new AuthorizationException(
          "InnsynskravBestilling " + innsynskravBestillingDTO.getId() + " is already sent");
    }
  }

  /**
   * Authorize the update operation. Admins and users with access to the InnsynskravBestilling can
   * update Innsynskrav objects.
   *
   * @param id The id of the Innsynskrav
   * @param dto The InnsynskravDTO to update
   * @throws AuthorizationException If not authorized
   */
  @Override
  protected void authorizeUpdate(String id, InnsynskravDTO dto) throws EInnsynException {
    var innsynskrav = innsynskravService.findByIdOrThrow(id);
    var innsynskravBestilling = innsynskrav.getInnsynskravBestilling();
    if (innsynskravBestilling == null) {
      throw new AuthorizationException("InnsynskravBestilling not found");
    }

    if (innsynskravBestilling.isLocked()) {
      throw new AuthorizationException("InnsynskravBestilling is already sent");
    }

    if (authenticationService.isAdmin()) {
      return;
    }

    var innsynskravBruker = innsynskravBestilling.getBruker();
    if (innsynskravBruker != null && authenticationService.isSelf(innsynskravBruker.getId())) {
      return;
    }

    throw new AuthorizationException("Not authorized to update " + dto.getId());
  }

  /**
   * Authorize the delete operation. Admins and users with access to the InnsynskravBestilling can
   * delete Innsynskrav objects.
   *
   * @param id The id of the Innsynskrav
   * @throws AuthorizationException If not authorized
   */
  @Override
  protected void authorizeDelete(String id) throws EInnsynException {
    var innsynskrav = innsynskravService.findByIdOrThrow(id);
    var innsynskravBestilling = innsynskrav.getInnsynskravBestilling();
    if (authenticationService.isAdmin()) {
      return;
    }

    if (innsynskravBestilling == null) {
      throw new AuthorizationException("InnsynskravBestilling not found");
    }

    var innsynskravBruker = innsynskravBestilling.getBruker();
    if (innsynskravBruker != null && authenticationService.isSelf(innsynskravBruker.getId())) {
      return;
    }

    throw new AuthorizationException("Not authorized to delete " + id);
  }
}

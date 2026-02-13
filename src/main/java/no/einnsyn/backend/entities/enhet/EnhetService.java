package no.einnsyn.backend.entities.enhet;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.common.exceptions.models.AuthorizationException;
import no.einnsyn.backend.common.exceptions.models.BadRequestException;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.hasslug.HasSlugService;
import no.einnsyn.backend.common.paginators.Paginators;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.apikey.ApiKeyRepository;
import no.einnsyn.backend.entities.apikey.models.ApiKeyDTO;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.base.BaseService;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import no.einnsyn.backend.entities.enhet.models.ListByEnhetParameters;
import no.einnsyn.backend.entities.innsynskrav.InnsynskravRepository;
import no.einnsyn.backend.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.backend.entities.moetemappe.MoetemappeRepository;
import no.einnsyn.backend.entities.moetesak.MoetesakRepository;
import no.einnsyn.backend.entities.saksmappe.SaksmappeRepository;
import no.einnsyn.backend.utils.id.IdValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class EnhetService extends BaseService<Enhet, EnhetDTO>
    implements HasSlugService<Enhet, EnhetService> {

  @Getter(onMethod_ = @Override)
  private final EnhetRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter(onMethod_ = @Override)
  @Lazy
  @Autowired
  private EnhetService proxy;

  private final InnsynskravRepository innsynskravRepository;
  private final SaksmappeRepository saksmappeRepository;
  private final MoetemappeRepository moetemappeRepository;
  private final MoetesakRepository moetesakRepository;
  private final ApiKeyRepository apiKeyRepository;

  EnhetService(
      EnhetRepository repository,
      InnsynskravRepository innsynskravRepository,
      SaksmappeRepository saksmappeRepository,
      MoetemappeRepository moetemappeRepository,
      MoetesakRepository moetesakRepository,
      ApiKeyRepository apiKeyRepository) {
    this.repository = repository;
    this.innsynskravRepository = innsynskravRepository;
    this.saksmappeRepository = saksmappeRepository;
    this.moetemappeRepository = moetemappeRepository;
    this.moetesakRepository = moetesakRepository;
    this.apiKeyRepository = apiKeyRepository;
  }

  @Override
  public Enhet newObject() {
    return new Enhet();
  }

  @Override
  public EnhetDTO newDTO() {
    return new EnhetDTO();
  }

  /**
   * Extend findById to also lookup by orgnummer.
   *
   * @param id the id to lookup
   * @return the object
   */
  @Override
  @Transactional(readOnly = true)
  public Enhet findById(String id) {
    // Try to lookup by orgnummer if it's a valid orgnummer
    if (id != null && id.matches("\\d{9}")) {
      var enhet = repository.findByOrgnummer(id);
      if (enhet != null) {
        return enhet;
      }
    }

    if (id != null && !id.startsWith(idPrefix)) {
      var enhet = repository.findBySlug(id);
      if (enhet != null) {
        return enhet;
      }
    }
    return super.findById(id);
  }

  /**
   * Extend findPropertyAndObjectByDTO to also lookup by orgnummer.
   *
   * @param baseDTO the DTO to find
   * @return the object with the given orgnummer, or null if not found
   */
  @Override
  @Transactional(readOnly = true)
  public Pair<String, Enhet> findPropertyAndObjectByDTO(BaseDTO baseDTO) {
    if (baseDTO instanceof EnhetDTO dto && dto.getOrgnummer() != null) {
      var enhet = repository.findByOrgnummer(dto.getOrgnummer());
      if (enhet != null) {
        return Pair.of("orgnummer", enhet);
      }
    }

    return super.findPropertyAndObjectByDTO(baseDTO);
  }

  @Override
  @SuppressWarnings({"java:S3776", "java:S6541"}) // Method is "complex" due to many fields
  protected Enhet fromDTO(EnhetDTO dto, Enhet enhet) throws EInnsynException {
    super.fromDTO(dto, enhet);

    if (dto.getSlug() != null) {
      enhet.setSlug(dto.getSlug());
    }

    if (dto.getNavn() != null) {
      enhet.setNavn(dto.getNavn());
    }

    if (dto.getNavnNynorsk() != null) {
      enhet.setNavnNynorsk(dto.getNavnNynorsk());
    }

    if (dto.getNavnEngelsk() != null) {
      enhet.setNavnEngelsk(dto.getNavnEngelsk());
    }

    if (dto.getNavnSami() != null) {
      enhet.setNavnSami(dto.getNavnSami());
    }

    if (dto.getAvsluttetDato() != null) {
      enhet.setAvsluttetDato(LocalDate.parse(dto.getAvsluttetDato()));
    }

    if (dto.getInnsynskravEpost() != null) {
      enhet.setInnsynskravEpost(dto.getInnsynskravEpost());
    }

    if (dto.getKontaktpunktAdresse() != null) {
      enhet.setKontaktpunktAdresse(dto.getKontaktpunktAdresse());
    }

    if (dto.getKontaktpunktEpost() != null) {
      enhet.setKontaktpunktEpost(dto.getKontaktpunktEpost());
    }

    if (dto.getKontaktpunktTelefon() != null) {
      enhet.setKontaktpunktTelefon(dto.getKontaktpunktTelefon());
    }

    if (dto.getOrgnummer() != null) {
      enhet.setOrgnummer(dto.getOrgnummer());
    }

    if (dto.getEnhetskode() != null) {
      enhet.setEnhetskode(dto.getEnhetskode());
    }

    if (dto.getEnhetstype() != null) {
      enhet.setEnhetstype(EnhetDTO.EnhetstypeEnum.fromValue(dto.getEnhetstype()));
    }

    if (dto.getSkjult() != null) {
      enhet.setSkjult(dto.getSkjult());
    }

    if (dto.getEFormidling() != null) {
      enhet.setEFormidling(dto.getEFormidling());
    }

    if (dto.getVisToppnode() != null) {
      enhet.setVisToppnode(dto.getVisToppnode());
    }

    if (dto.getTeknisk() != null) {
      enhet.setErTeknisk(dto.getTeknisk());
    }

    if (dto.getSkalKonvertereId() != null) {
      enhet.setSkalKonvertereId(dto.getSkalKonvertereId());
    }

    if (dto.getSkalMottaKvittering() != null) {
      enhet.setSkalMottaKvittering(dto.getSkalMottaKvittering());
    }

    if (dto.getOrderXmlVersjon() != null) {
      enhet.setOrderXmlVersjon(dto.getOrderXmlVersjon());
    }

    if (dto.getParent() != null) {
      var parent = enhetService.findByIdOrThrow(dto.getParent().getId());
      enhet.setParent(parent);
    }

    if (dto.getHandteresAv() != null) {
      var handteresAv = returnExistingOrThrow(dto.getHandteresAv());
      enhet.setHandteresAv(handteresAv);
    }

    // Persist before adding relations
    if (enhet.getId() == null) {
      enhet = repository.saveAndFlush(enhet);
    }

    // Add underenhets
    var underenhetFieldList = dto.getUnderenhet();
    if (underenhetFieldList != null) {
      for (var underenhetField : underenhetFieldList) {
        if (underenhetField.getId() != null) {
          // TODO: THIS IS A MOVE OPERATION
          // - Check that we're allowed to update old parent and new
          // - Reindex old parent and new (with children)
          throw new BadRequestException("Move not implemented");
        } else {
          var underenhetDTO = underenhetField.getExpandedObject();
          underenhetDTO.setParent(new ExpandableField<>(enhet.getId()));
          var underenhet = enhetService.addEntity(underenhetDTO);
          enhet.addUnderenhet(underenhet);
        }
      }
    }

    var slugBase = getSlugBase(enhet);
    enhet = proxy.setSlug(enhet, slugBase);

    return enhet;
  }

  @Override
  public String getSlugBase(Enhet enhet) {
    var parent = enhet.getParent();
    while (parent != null && parent.getEnhetstype() == EnhetDTO.EnhetstypeEnum.DUMMYENHET) {
      parent = parent.getParent();
    }
    if (parent != null) {
      return getSlugBase(parent) + "/" + enhet.getNavn();
    }
    return enhet.getNavn();
  }

  @Override
  protected EnhetDTO toDTO(Enhet enhet, EnhetDTO dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(enhet, dto, expandPaths, currentPath);

    dto.setSlug(enhet.getSlug());
    dto.setNavn(enhet.getNavn());
    dto.setNavnNynorsk(enhet.getNavnNynorsk());
    dto.setNavnEngelsk(enhet.getNavnEngelsk());
    dto.setNavnSami(enhet.getNavnSami());
    if (enhet.getAvsluttetDato() != null) {
      dto.setAvsluttetDato(enhet.getAvsluttetDato().toString());
    }
    dto.setInnsynskravEpost(enhet.getInnsynskravEpost());
    dto.setKontaktpunktAdresse(enhet.getKontaktpunktAdresse());
    dto.setKontaktpunktEpost(enhet.getKontaktpunktEpost());
    dto.setKontaktpunktTelefon(enhet.getKontaktpunktTelefon());
    dto.setOrgnummer(enhet.getOrgnummer());
    dto.setEnhetskode(enhet.getEnhetskode());
    dto.setEnhetstype(enhet.getEnhetstype().toString());
    dto.setSkjult(enhet.isSkjult());
    dto.setEFormidling(enhet.isEFormidling());
    dto.setVisToppnode(enhet.isVisToppnode());
    dto.setTeknisk(enhet.isErTeknisk());
    dto.setSkalKonvertereId(enhet.isSkalKonvertereId());
    dto.setSkalMottaKvittering(enhet.isSkalMottaKvittering());
    dto.setOrderXmlVersjon(enhet.getOrderXmlVersjon());

    dto.setParent(maybeExpand(enhet.getParent(), "parent", expandPaths, currentPath));
    dto.setUnderenhet(maybeExpand(enhet.getUnderenhet(), "underenhet", expandPaths, currentPath));
    dto.setHandteresAv(
        maybeExpand(enhet.getHandteresAv(), "handteresAv", expandPaths, currentPath));

    return dto;
  }

  /**
   * Recursively check if an Enhet, or any of its ancestors, are hidden.
   *
   * @param enhetId The enhetId to check
   * @return True if hidden, false if not
   */
  @Transactional(readOnly = true)
  public boolean isSkjult(String enhetId) {
    return repository.isSkjult(enhetId);
  }

  /** Find hidden Enhet objects. */
  @Transactional(readOnly = true)
  public List<Enhet> findHidden() throws EInnsynException {
    return repository.findHidden();
  }

  /**
   * Search the subtree under `root` for an enhet with matching enhetskode.
   *
   * @param enhetskode The enhetskode to search for
   * @param root The root of the subtree to search
   * @return The Enhet object with matching enhetskode, or null if not found
   */
  @Transactional(readOnly = true, propagation = Propagation.MANDATORY)
  public Enhet findByEnhetskode(String enhetskode, Enhet root) {

    if (!StringUtils.hasText(enhetskode) || root == null) {
      return null;
    }

    return repository.findByEnhetskode(enhetskode, root.getId());
  }

  @Transactional(readOnly = true)
  public List<Enhet> getTransitiveEnhets(String enhetId) throws EInnsynException {
    var enhet = enhetService.findByIdOrThrow(enhetId);
    return getProxy().getTransitiveEnhets(enhet);
  }

  /**
   * Get a "transitive" list of ancestors for an Enhet object.
   *
   * @param enhet The Enhet object to get ancestors for
   * @return A list of Enhet objects, starting with the root and ending with the given Enhet
   */
  @Transactional(readOnly = true, propagation = Propagation.MANDATORY)
  public List<Enhet> getTransitiveEnhets(Enhet enhet) {
    var transitiveList = new ArrayList<Enhet>();
    var visited = new HashSet<Enhet>();
    var parent = enhet;
    while (parent != null && !visited.contains(parent)) {
      transitiveList.add(parent);
      visited.add(parent);
      parent = parent.getParent();
    }
    return transitiveList;
  }

  /**
   * Check if an enhetId is authorized to handle a given enhet.
   *
   * @param parentId The enhetId to check
   * @param potentialChildId The enhetId to check
   */
  @Transactional(readOnly = true)
  public boolean isAncestorOf(@Nullable String parentId, @Nullable String potentialChildId) {
    if (parentId == null || potentialChildId == null) {
      return false;
    }

    // If we have another identifier (e.g. orgnummer), look up the actual id
    if (!IdValidator.isValid(potentialChildId)) {
      var potentialChild = proxy.findById(potentialChildId);
      if (potentialChild == null) {
        return false;
      }
      potentialChildId = potentialChild.getId();
    }

    if (!IdValidator.isValid(parentId)) {
      return false;
    }

    return repository.isAncestorOf(parentId, potentialChildId);
  }

  /**
   * Check if an authenticated user is authorized to handle a given enhet.
   *
   * @param authenticatedId the ID of the authenticated user
   * @param enhetId the ID of the enhet to check
   * @return true if the authenticated user is authorized to handle the enhet
   */
  @Transactional(readOnly = true)
  public boolean isHandledBy(String authenticatedId, String enhetId) {
    var enhet = getProxy().findById(enhetId);
    if (enhet == null) {
      return false;
    }
    var handteresAv = enhet.getHandteresAv();
    return handteresAv != null && handteresAv.getId().equals(authenticatedId);
  }

  /**
   * Delete an Enhet and all its descendants
   *
   * @param enhet The Enhet object to delete
   */
  @Override
  protected void deleteEntity(Enhet enhet) throws EInnsynException {

    // Delete all underenhets
    var underenhetList = enhet.getUnderenhet();
    if (underenhetList != null) {
      enhet.setUnderenhet(null);
      for (var underenhet : underenhetList) {
        enhetService.delete(underenhet.getId());
      }
    }

    // Delete all Innsynskrav
    try (var innsynskravIdStream = innsynskravRepository.streamIdByEnhet(enhet)) {
      var innsynskravIdIterator = innsynskravIdStream.iterator();
      while (innsynskravIdIterator.hasNext()) {
        innsynskravService.delete(innsynskravIdIterator.next());
      }
    }

    // Delete all Saksmappe by this enhet
    try (var saksmappeIdSteram = saksmappeRepository.streamIdByAdministrativEnhetObjekt(enhet)) {
      var saksmappeIdIterator = saksmappeIdSteram.iterator();
      while (saksmappeIdIterator.hasNext()) {
        saksmappeService.delete(saksmappeIdIterator.next());
      }
    }

    // Delete all Moetemappe by this enhet
    try (var moetemappeIdStream = moetemappeRepository.streamIdByUtvalgObjekt(enhet)) {
      var moetemappeIdIterator = moetemappeIdStream.iterator();
      while (moetemappeIdIterator.hasNext()) {
        moetemappeService.delete(moetemappeIdIterator.next());
      }
    }

    // Delete all Moetesak by this enhet
    try (var moetesakIdStream = moetesakRepository.streamIdByUtvalgObjekt(enhet)) {
      var moetesakIdIterator = moetesakIdStream.iterator();
      while (moetesakIdIterator.hasNext()) {
        moetesakService.delete(moetesakIdIterator.next());
      }
    }

    // Delete all ApiKeys for this enhet
    try (var apiKeyStream = apiKeyRepository.streamIdByEnhet(enhet)) {
      var apiKeyIdIterator = apiKeyStream.iterator();
      while (apiKeyIdIterator.hasNext()) {
        apiKeyService.delete(apiKeyIdIterator.next());
      }
    }

    super.deleteEntity(enhet);
  }

  /**
   * Get a list of all enhetIds in the subtree under a given enhetId, including the given enhetId.
   *
   * @param enhetId The enhetId to get subtree for
   * @return A list of enhetIds
   */
  public List<String> getSubtreeIdList(String enhetId) {
    if (enhetId == null) {
      return List.of();
    }
    var idList = repository.getSubtreeIdList(enhetId);
    return idList;
  }

  /**
   * @param enhetId The enhetId to get underenhets for
   * @param query The query object
   * @return A list of Enhet objects
   */
  public PaginatedList<EnhetDTO> listUnderenhet(String enhetId, ListByEnhetParameters query)
      throws EInnsynException {
    query.setEnhetId(enhetId);
    return enhetService.list(query);
  }

  /**
   * @param enhetId The enhetId to add underenhets to
   * @param enhetField The EnhetDTO object to add, wrapped in an ExpandableField
   */
  public EnhetDTO addUnderenhet(String enhetId, ExpandableField<EnhetDTO> enhetField)
      throws EInnsynException {

    // If an ID is given, we're moving an existing Enhet. If not, we're creating a new child.
    var dto =
        enhetField.getId() != null
            ? enhetService.get(enhetField.getId())
            : enhetField.getExpandedObject();

    dto.setParent(new ExpandableField<>(enhetId));
    return enhetService.add(dto);
  }

  public PaginatedList<ApiKeyDTO> listApiKey(String enhetId, ListByEnhetParameters query)
      throws EInnsynException {
    query.setEnhetId(enhetId);
    return apiKeyService.list(query);
  }

  public ApiKeyDTO addApiKey(String enhetId, ApiKeyDTO dto) throws EInnsynException {
    dto.setEnhet(new ExpandableField<>(enhetId));
    return apiKeyService.add(dto);
  }

  public PaginatedList<ArkivDTO> listArkiv(String enhetId, ListByEnhetParameters query)
      throws EInnsynException {
    query.setEnhetId(enhetId);
    return arkivService.list(query);
  }

  public PaginatedList<InnsynskravDTO> listInnsynskrav(String enhetId, ListByEnhetParameters query)
      throws EInnsynException {
    query.setEnhetId(enhetId);
    return innsynskravService.list(query);
  }

  @Override
  protected Paginators<Enhet> getPaginators(ListParameters params) throws EInnsynException {
    if (params instanceof ListByEnhetParameters p && p.getEnhetId() != null) {
      var parent = enhetService.findByIdOrThrow(p.getEnhetId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(parent, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(parent, pivot, pageRequest));
    }
    return super.getPaginators(params);
  }

  /**
   * Authorize listing of Enhet.
   *
   * @param params The query object
   */
  @Override
  protected void authorizeList(ListParameters params) {
    // Anybody can list Enhet objects
  }

  /** No authorization required for get operation. */
  @Override
  protected void authorizeGet(String idToGet) throws EInnsynException {
    // Anybody can get Enhet objects
  }

  /**
   * Authorize the add operation. Only users with a journalenhet can add Enhet objects, and only
   * below the authenticated enhet.
   *
   * @param dto The EnhetDTO object to add
   * @throws AuthorizationException If not authorized
   */
  @Override
  protected void authorizeAdd(EnhetDTO dto) throws EInnsynException {
    var parent = dto.getParent();
    if (parent == null) {
      throw new AuthorizationException("Parent is required");
    }

    var loggedInAs = authenticationService.getEnhetId();
    if (enhetService.isAncestorOf(loggedInAs, parent.getId())) {
      return;
    }

    throw new AuthorizationException("Not authorized to add Enhet under parent " + parent.getId());
  }

  /**
   * Authorize the update operation. Only users representing a journalenhet that owns the object can
   * update.
   *
   * @param idToUpdate The enhetId to update
   * @param dto The EnhetDTO object to update
   * @throws AuthorizationException If not authorized
   */
  @Override
  protected void authorizeUpdate(String idToUpdate, EnhetDTO dto) throws EInnsynException {
    var loggedInAs = authenticationService.getEnhetId();
    if (enhetService.isAncestorOf(loggedInAs, idToUpdate)) {
      return;
    }

    throw new AuthorizationException("Not authorized to update " + idToUpdate);
  }

  /**
   * Authorize the delete operation. Only users representing a journalenhet that owns the object can
   * delete.
   *
   * @param idToDelete The enhetId to delete
   * @throws AuthorizationException If not authorized
   */
  @Override
  protected void authorizeDelete(String idToDelete) throws EInnsynException {
    var loggedInAs = authenticationService.getEnhetId();
    if (enhetService.isAncestorOf(loggedInAs, idToDelete)) {
      var enhet = proxy.findById(idToDelete);
      if (enhetHasData(enhet)) {
        throw new AuthorizationException(
            "Not authorized to delete " + idToDelete + ". Enhet or underenhet still has data.");
      }
      return;
    }

    throw new AuthorizationException("Not authorized to delete " + idToDelete);
  }

  protected boolean enhetHasData(Enhet enhet) {
    if (saksmappeRepository.existsByAdministrativEnhetObjekt(enhet)
        || moetemappeRepository.existsByUtvalgObjekt(enhet)
        || moetesakRepository.existsByUtvalgObjekt(enhet)) {
      return true;
    }
    // Check underenhets
    if (enhet.getUnderenhet() != null) {
      for (Enhet underenhet : enhet.getUnderenhet()) {
        if (enhetHasData(underenhet)) {
          return true;
        }
      }
    }
    return false;
  }
}

package no.einnsyn.apiv3.entities.enhet;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.paginators.Paginators;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.apikey.ApiKeyRepository;
import no.einnsyn.apiv3.entities.apikey.models.ApiKeyDTO;
import no.einnsyn.apiv3.entities.apikey.models.ApiKeyListQueryDTO;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivListQueryDTO;
import no.einnsyn.apiv3.entities.base.BaseService;
import no.einnsyn.apiv3.entities.base.models.BaseDTO;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.enhet.models.EnhetDTO;
import no.einnsyn.apiv3.entities.enhet.models.EnhetListQueryDTO;
import no.einnsyn.apiv3.entities.enhet.models.EnhetstypeEnum;
import no.einnsyn.apiv3.entities.innsynskravdel.InnsynskravDelRepository;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelDTO;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelListQueryDTO;
import no.einnsyn.apiv3.entities.moetemappe.MoetemappeRepository;
import no.einnsyn.apiv3.entities.moetesak.MoetesakRepository;
import no.einnsyn.apiv3.entities.saksmappe.SaksmappeRepository;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import no.einnsyn.apiv3.error.exceptions.ForbiddenException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EnhetService extends BaseService<Enhet, EnhetDTO> {

  @Getter private final EnhetRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private EnhetService proxy;

  private final InnsynskravDelRepository innsynskravDelRepository;
  private final SaksmappeRepository saksmappeRepository;
  private final MoetemappeRepository moetemappeRepository;
  private final MoetesakRepository moetesakRepository;
  private final ApiKeyRepository apiKeyRepository;

  EnhetService(
      EnhetRepository repository,
      InnsynskravDelRepository innsynskravDelRepository,
      SaksmappeRepository saksmappeRepository,
      MoetemappeRepository moetemappeRepository,
      MoetesakRepository moetesakRepository,
      ApiKeyRepository apiKeyRepository) {
    this.repository = repository;
    this.innsynskravDelRepository = innsynskravDelRepository;
    this.saksmappeRepository = saksmappeRepository;
    this.moetemappeRepository = moetemappeRepository;
    this.moetesakRepository = moetesakRepository;
    this.apiKeyRepository = apiKeyRepository;
  }

  public Enhet newObject() {
    return new Enhet();
  }

  public EnhetDTO newDTO() {
    return new EnhetDTO();
  }

  @Override
  @Transactional(readOnly = true)
  public Enhet findByDTO(BaseDTO baseDTO) {
    if (baseDTO instanceof EnhetDTO dto && dto.getOrgnummer() != null) {
      var found = repository.findByOrgnummer(dto.getOrgnummer());
      if (found != null) {
        return found;
      }
    }

    return super.findByDTO(baseDTO);
  }

  @Override
  @SuppressWarnings("java:S3776") // Method is "complex" due to many fields
  protected Enhet fromDTO(EnhetDTO dto, Enhet enhet) throws EInnsynException {
    super.fromDTO(dto, enhet);

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
      enhet.setEnhetstype(EnhetstypeEnum.fromValue(dto.getEnhetstype()));
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
      var parent = enhetService.findById(dto.getParent().getId());
      enhet.setParent(parent);
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
          throw new EInnsynException("Move not implemented");
        } else {
          var underenhetDTO = underenhetField.getExpandedObject();
          underenhetDTO.setParent(new ExpandableField<>(enhet.getId()));
          var underenhet = enhetService.addEntity(underenhetDTO);
          enhet.addUnderenhet(underenhet);
        }
      }
    }

    return enhet;
  }

  @Override
  protected EnhetDTO toDTO(Enhet enhet, EnhetDTO dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(enhet, dto, expandPaths, currentPath);

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

    var parent = enhet.getParent();
    if (parent != null) {
      dto.setParent(maybeExpand(parent, "parent", expandPaths, currentPath));
    }

    // Underenhets
    dto.setUnderenhet(maybeExpand(enhet.getUnderenhet(), "underenhet", expandPaths, currentPath));

    return dto;
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
  @SuppressWarnings("java:S6809") // We're already in a transaction
  public List<Enhet> getTransitiveEnhets(String enhetId) {
    var enhet = enhetService.findById(enhetId);
    if (enhet == null) {
      return new ArrayList<>();
    }
    return getTransitiveEnhets(enhet);
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

    return repository.isAncestorOf(parentId, potentialChildId);
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

    // Delete all InnsynskravDel
    var innsynskravDelStream = innsynskravDelRepository.findAllByEnhet(enhet);
    var innsynskravDelIterator = innsynskravDelStream.iterator();
    while (innsynskravDelIterator.hasNext()) {
      var innsynskravDel = innsynskravDelIterator.next();
      innsynskravDelService.delete(innsynskravDel.getId());
    }

    // Delete all Saksmappe by this enhet
    var saksmappeSteram = saksmappeRepository.findAllByAdministrativEnhetObjekt(enhet);
    var saksmappeIterator = saksmappeSteram.iterator();
    while (saksmappeIterator.hasNext()) {
      var saksmappe = saksmappeIterator.next();
      saksmappeService.delete(saksmappe.getId());
    }

    // Delete all Moetemappe by this enhet
    var moetemappeStream = moetemappeRepository.findAllByUtvalgObjekt(enhet);
    var moetemappeIterator = moetemappeStream.iterator();
    while (moetemappeIterator.hasNext()) {
      var moetemappe = moetemappeIterator.next();
      moetemappeService.delete(moetemappe.getId());
    }

    // Delete all Moetesak by this enhet
    var moetesakStream = moetesakRepository.findAllByUtvalgObjekt(enhet);
    var moetesakIterator = moetesakStream.iterator();
    while (moetesakIterator.hasNext()) {
      var moetesak = moetesakIterator.next();
      moetesakService.delete(moetesak.getId());
    }

    // Delete all ApiKeys for this enhet
    var apiKeyStream = apiKeyRepository.findAllByEnhet(enhet);
    var apiKeyIterator = apiKeyStream.iterator();
    while (apiKeyIterator.hasNext()) {
      var apiKey = apiKeyIterator.next();
      apiKeyService.delete(apiKey.getId());
    }

    super.deleteEntity(enhet);
  }

  /**
   * @param enhetId The enhetId to get underenhets for
   * @param query The query object
   * @return A list of Enhet objects
   */
  public ResultList<EnhetDTO> getUnderenhetList(String enhetId, EnhetListQueryDTO query)
      throws EInnsynException {
    query.setParentId(enhetId);
    return enhetService.list(query);
  }

  /**
   * @param enhetId The enhetId to add underenhets to
   * @param dto The EnhetDTO object to add
   */
  public EnhetDTO addUnderenhet(String enhetId, EnhetDTO dto) throws EInnsynException {
    dto.setParent(new ExpandableField<>(enhetId));
    return enhetService.add(dto);
  }

  public ResultList<ApiKeyDTO> getApiKeyList(String enhetId, ApiKeyListQueryDTO query)
      throws EInnsynException {
    query.setEnhetId(enhetId);
    return apiKeyService.list(query);
  }

  public ApiKeyDTO addApiKey(String enhetId, ApiKeyDTO dto) throws EInnsynException {
    dto.setEnhet(new ExpandableField<>(enhetId));
    return apiKeyService.add(dto);
  }

  public ResultList<ArkivDTO> getArkivList(String enhetId, ArkivListQueryDTO query)
      throws EInnsynException {
    query.setEnhetId(enhetId);
    return arkivService.list(query);
  }

  public ResultList<InnsynskravDelDTO> getInnsynskravDelList(
      String enhetId, InnsynskravDelListQueryDTO query) throws EInnsynException {
    query.setEnhetId(enhetId);
    return innsynskravDelService.list(query);
  }

  @Override
  protected Paginators<Enhet> getPaginators(BaseListQueryDTO params) {
    if (params instanceof EnhetListQueryDTO p && p.getParentId() != null) {
      var parent = enhetService.findById(p.getParentId());
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
  protected void authorizeList(BaseListQueryDTO params) {
    // Anybody can list Enhet objects
  }

  /**
   * Authorize the get operation. Admins can get any Enhet, otherwise only the ones under the
   * authenticated enhet.
   */
  @Override
  protected void authorizeGet(String idToGet) throws EInnsynException {
    var loggedInAs = authenticationService.getJournalenhetId();
    if (enhetService.isAncestorOf(loggedInAs, idToGet)) {
      return;
    }

    throw new ForbiddenException("Not authorized to get Enhet " + idToGet);
  }

  /**
   * Authorize the add operation. Only users with a journalenhet can add Enhet objects, and only
   * below the authenticated enhet.
   *
   * @param dto The EnhetDTO object to add
   * @throws ForbiddenException If not authorized
   */
  @Override
  protected void authorizeAdd(EnhetDTO dto) throws EInnsynException {
    var parent = dto.getParent();
    if (parent == null) {
      throw new ForbiddenException("Parent is required");
    }

    var loggedInAs = authenticationService.getJournalenhetId();
    if (enhetService.isAncestorOf(loggedInAs, parent.getId())) {
      return;
    }

    throw new ForbiddenException("Not authorized to add Enhet under parent " + parent.getId());
  }

  /**
   * Authorize the update operation. Only users representing a journalenhet that owns the object can
   * update.
   *
   * @param idToUpdate The enhetId to update
   * @param dto The EnhetDTO object to update
   * @throws ForbiddenException If not authorized
   */
  @Override
  protected void authorizeUpdate(String idToUpdate, EnhetDTO dto) throws EInnsynException {
    var loggedInAs = authenticationService.getJournalenhetId();
    if (enhetService.isAncestorOf(loggedInAs, idToUpdate)) {
      return;
    }

    throw new ForbiddenException("Not authorized to update " + idToUpdate);
  }

  /**
   * Authorize the delete operation. Only users representing a journalenhet that owns the object can
   * delete.
   *
   * @param idToDelete The enhetId to delete
   * @throws ForbiddenException If not authorized
   */
  @Override
  protected void authorizeDelete(String idToDelete) throws EInnsynException {
    var loggedInAs = authenticationService.getJournalenhetId();
    if (enhetService.isAncestorOf(loggedInAs, idToDelete)) {
      return;
    }

    throw new ForbiddenException("Not authorized to delete " + idToDelete);
  }
}

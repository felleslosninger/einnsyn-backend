package no.einnsyn.apiv3.entities.base;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.arkiv.ArkivService;
import no.einnsyn.apiv3.entities.arkivdel.ArkivdelService;
import no.einnsyn.apiv3.entities.base.models.Base;
import no.einnsyn.apiv3.entities.base.models.BaseDTO;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.behandlingsprotokoll.BehandlingsprotokollService;
import no.einnsyn.apiv3.entities.bruker.BrukerService;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.DokumentbeskrivelseService;
import no.einnsyn.apiv3.entities.dokumentobjekt.DokumentobjektService;
import no.einnsyn.apiv3.entities.enhet.EnhetService;
import no.einnsyn.apiv3.entities.identifikator.IdentifikatorService;
import no.einnsyn.apiv3.entities.innsynskrav.InnsynskravService;
import no.einnsyn.apiv3.entities.innsynskravdel.InnsynskravDelService;
import no.einnsyn.apiv3.entities.journalpost.JournalpostService;
import no.einnsyn.apiv3.entities.klasse.KlasseService;
import no.einnsyn.apiv3.entities.korrespondansepart.KorrespondansepartService;
import no.einnsyn.apiv3.entities.lagretsak.LagretSakService;
import no.einnsyn.apiv3.entities.lagretsoek.LagretSoekService;
import no.einnsyn.apiv3.entities.moetedeltaker.MoetedeltakerService;
import no.einnsyn.apiv3.entities.moetedokument.MoetedokumentService;
import no.einnsyn.apiv3.entities.moetemappe.MoetemappeService;
import no.einnsyn.apiv3.entities.moetesak.MoetesakService;
import no.einnsyn.apiv3.entities.moetesaksbeskrivelse.MoetesaksbeskrivelseService;
import no.einnsyn.apiv3.entities.saksmappe.SaksmappeService;
import no.einnsyn.apiv3.entities.skjerming.SkjermingService;
import no.einnsyn.apiv3.entities.tilbakemelding.TilbakemeldingService;
import no.einnsyn.apiv3.entities.utredning.UtredningService;
import no.einnsyn.apiv3.entities.vedtak.VedtakService;
import no.einnsyn.apiv3.entities.votering.VoteringService;
import no.einnsyn.apiv3.utils.IdGenerator;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Abstract base service class providing generic functionalities for entity services. This class is
 * designed to be extended by entity service implementations, providing a common framework for
 * handling entities and their data transfer objects (DTOs).
 *
 * @param <O> the type of the entity object
 * @param <D> the type of the data transfer object (DTO)
 */
@SuppressWarnings("java:S6813")
public abstract class BaseService<O extends Base, D extends BaseDTO> {

  @Lazy @Autowired protected ArkivService arkivService;
  @Lazy @Autowired protected ArkivdelService arkivdelService;
  @Lazy @Autowired protected BehandlingsprotokollService behandlingsprotokollService;
  @Lazy @Autowired protected BrukerService brukerService;
  @Lazy @Autowired protected DokumentbeskrivelseService dokumentbeskrivelseService;
  @Lazy @Autowired protected DokumentobjektService dokumentobjektService;
  @Lazy @Autowired protected EnhetService enhetService;
  @Lazy @Autowired protected IdentifikatorService identifikatorService;
  @Lazy @Autowired protected InnsynskravService innsynskravService;
  @Lazy @Autowired protected InnsynskravDelService innsynskravDelService;
  @Lazy @Autowired protected JournalpostService journalpostService;
  @Lazy @Autowired protected KlasseService klasseService;
  @Lazy @Autowired protected KorrespondansepartService korrespondansepartService;
  @Lazy @Autowired protected LagretSakService lagretSakService;
  @Lazy @Autowired protected LagretSoekService lagretSoekService;
  @Lazy @Autowired protected MoetedeltakerService moetedeltakerService;
  @Lazy @Autowired protected MoetedokumentService moetedokumentService;
  @Lazy @Autowired protected MoetemappeService moetemappeService;
  @Lazy @Autowired protected MoetesakService moetesakService;
  @Lazy @Autowired protected MoetesaksbeskrivelseService moetesakbeskrivelseService;
  @Lazy @Autowired protected SaksmappeService saksmappeService;
  @Lazy @Autowired protected SkjermingService skjermingService;
  @Lazy @Autowired protected TilbakemeldingService tilbakemeldingService;
  @Lazy @Autowired protected UtredningService utredningService;
  @Lazy @Autowired protected VedtakService vedtakService;
  @Lazy @Autowired protected VoteringService voteringService;

  protected final String idPrefix = IdGenerator.getPrefix(this.newObject().getClass());

  protected abstract BaseRepository<O> getRepository();

  protected abstract BaseService<O, D> getProxy();

  @Autowired private HttpServletRequest request;

  /**
   * Creates a new instance of the Data Transfer Object (DTO) associated with this service. This
   * method is typically used to instantiate a DTO for data conversion or initial population.
   *
   * @return a new instance of the DTO type associated with this service
   */
  public abstract D newDTO();

  /**
   * Creates a new instance of the entity object associated with this service. This method is
   * commonly used to instantiate an entity before persisting it to the database, or before
   * populating it with data for further processing.
   *
   * @return a new instance of the entity type associated with this service
   */
  public abstract O newObject();

  /**
   * Finds an entity by its unique identifier. If the ID does not start with the current entity's ID
   * prefix, it is treated as an external ID or a system ID. This method can be extended by entity
   * services to provide additional lookup logic, for instance lookup by email address.
   *
   * @param id The unique identifier of the entity
   * @return the entity object if found, or null
   */
  @Transactional
  public O findById(String id) {
    var repository = this.getRepository();
    // If the ID doesn't start with our prefix, it is an external ID or a system ID
    if (!id.startsWith(idPrefix)) {
      var object = repository.findByExternalId(id);
      if (object != null) {
        return object;
      }
    }

    return repository.findById(id).orElse(null);
  }

  /**
   * Checks whether an entity exists with the same logic as findById().
   *
   * @param id The unique identifier of the entity
   * @return true if the entity exists, false otherwise
   */
  @Transactional
  public boolean existsById(String id) {
    var repository = this.getRepository();
    // If the ID doesn't start with our prefix, it is an external ID or a system ID
    if (!id.startsWith(idPrefix)) {
      var exists = repository.existsByExternalId(id);
      if (exists) {
        return true;
      }
    }

    return repository.existsById(id);
  }

  public D get(String id) throws EInnsynException {
    return getProxy().get(id, new BaseGetQueryDTO());
  }

  /**
   * Retrieves a DTO representation of an entity based on a unique identifier.
   *
   * @param id The unique identifier of the entity
   * @return the DTO of the entity if found
   */
  @Transactional
  public D get(String id, BaseGetQueryDTO query) throws EInnsynException {
    var proxy = getProxy();
    var obj = proxy.findById(id);
    var expandSet = expandListToSet(query.getExpand());
    if (id == null) {
      throw new EInnsynException("No object found with id " + id);
    }
    return proxy.toDTO(obj, expandSet);
  }

  /**
   * Adds a new entity to the database. This is currently a wrapper for update() method, which
   * handles both new objects and updates.
   *
   * @param entity The entity object to add
   * @return the added entity
   */
  public D add(D dto) throws EInnsynException {
    return getProxy().update(null, dto);
  }

  public D update(D dto) throws EInnsynException {
    return getProxy().update(null, dto);
  }

  /**
   * Updates an existing entity in the database if an ID is given, or creates and persists a new
   * object if not. The method will handle persisting to the database, indexing to ElasticSearch,
   * and returning the updated entity's DTO.
   *
   * @param entity The entity object with updated data
   * @return the updated entity
   */
  @Transactional
  public D update(String id, D dto) throws EInnsynException {
    O obj = null;
    var proxy = this.getProxy();
    var repository = this.getRepository();

    // If ID is given, get the existing saksmappe from DB
    if (id != null) {
      obj = proxy.findById(id);
    } else {
      obj = newObject();
    }

    // Generate database object from JSON
    var paths = new HashSet<String>();
    obj = proxy.fromDTO(dto, obj, paths, "");
    repository.saveAndFlush(obj);

    // Add / update ElasticSearch document
    proxy.index(obj, true);

    // Generate a DTO containing all inserted objects
    return proxy.toDTO(obj, newDTO(), paths, "");
  }

  public void index(O obj) throws EInnsynException {
    this.index(obj, false);
  }

  /**
   * Index the object to ElasticSearch. This is a dummy placeholder for entities that shouldn't be
   * indexed. Specific logic should be implemented in the subclass, and should also implement logic
   * to update related objects that may contain the current object in the index.
   *
   * @param obj The entity object to index
   * @throws EInnsynException
   */
  public void index(O obj, boolean shouldUpdateRelatives) throws EInnsynException {}

  public O fromDTO(D dto) throws EInnsynException {
    return getProxy().fromDTO(dto, this.newObject(), new HashSet<>(), "");
  }

  public O fromDTO(D dto, O object) throws EInnsynException {
    return getProxy().fromDTO(dto, object, new HashSet<>(), "");
  }

  public O fromDTO(D dto, Set<String> paths, String currentPath) throws EInnsynException {
    return getProxy().fromDTO(dto, this.newObject(), paths, currentPath);
  }

  /**
   * Converts a Data Transfer Object (DTO) to its corresponding entity object (O). This method is
   * intended for reconstructing an entity from its DTO, typically used when persisting data
   * received in the form of a DTO to the database.
   *
   * @param dto the DTO to be converted to an entity
   * @return an entity object corresponding to the DTO
   */
  @SuppressWarnings({"java:S1172", "java:S1130"})
  public O fromDTO(D dto, O object, Set<String> paths, String currentPath) throws EInnsynException {
    if (dto.getExternalId() != null) {
      // TODO: Make sure external IDs don't have our ID prefix. This will make it fail on lookup
      object.setExternalId(dto.getExternalId());
    }

    return object;
  }

  public D toDTO(O object) {
    return getProxy().toDTO(object, newDTO(), new HashSet<>(), "");
  }

  public D toDTO(O object, Set<String> expandPaths) {
    return getProxy().toDTO(object, newDTO(), expandPaths, "");
  }

  public D toDTO(O object, Set<String> expandPaths, String currentPath) {
    return getProxy().toDTO(object, newDTO(), expandPaths, currentPath);
  }

  public D toDTO(O object, D dto) {
    return getProxy().toDTO(object, dto, new HashSet<>(), "");
  }

  /**
   * Converts an entity object (O) to its corresponding Data Transfer Object (DTO).
   *
   * @param object the entity object to be converted
   * @param object the target DTO object
   * @param expandPaths a set of paths indicating properties to expand
   * @param currentPath the current path in the object tree, used for nested expansions
   * @return a DTO representation of the entity
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public D toDTO(O object, D dto, Set<String> expandPaths, String currentPath) {

    dto.setId(object.getId());
    dto.setExternalId(object.getExternalId());
    dto.setCreated(object.getCreated().toString());
    dto.setUpdated(object.getUpdated().toString());

    return dto;
  }

  /**
   * Converts a JSONObject from Elasticsearch to its corresponding entity object (O). This method is
   * intended for reconstructing an entity from its Elasticsearch representation. In the future we
   * might make the ES document equal to the database document, to avoid // having to do an extra
   * lookup
   *
   * @param source The JSONObject representation of the entity from Elasticsearch
   * @return the converted entity object
   */
  public O esToEntity(JSONObject source) {
    var version = (Integer) source.get("_version");
    if (version == null) {
      version = 1;
    }

    // This is a legacy document
    if (version == 1) {
      var id = (String) source.get("id");
      return getProxy().findById(id);
    }

    return null;
  }

  /**
   * Retrieves a list of DTOs based on provided query parameters. This method uses the entity
   * service's getPage() implementation to get a paginated list of entities, and then converts the
   * page to a ResponseList.
   *
   * <p>Note: When searching using "endingBefore", the result list will be reversed. This is because
   * we use the "endingBefore" id as a pivot, and the DB will return the ordered list starting from
   * the pivot.
   *
   * @param params The query parameters for filtering and pagination
   * @return a ResultList containing DTOs that match the query criteria
   */
  @Transactional
  public ResultList<D> list(BaseListQueryDTO params) {
    var response = new ResultList<D>();
    var proxy = getProxy();
    var startingAfter = params.getStartingAfter();
    var endingBefore = params.getEndingBefore();
    var limit = params.getLimit();
    var pageRequest = PageRequest.of(0, limit + 2);
    var responsePage = proxy.getPage(params, pageRequest);
    var responseList = new LinkedList<>(responsePage.getContent());
    String nextId = null;
    String prevId = null;
    var uri = request.getRequestURI();
    var uriBuilder = UriComponentsBuilder.fromUriString(uri);
    uriBuilder.replaceQueryParam("startingAfter");
    uriBuilder.replaceQueryParam("endingBefore");

    if (responseList.isEmpty()) {
      return response;
    }

    // If starting after, remove the first item if it's the same as the startingAfter value
    if (startingAfter != null) {
      var firstItem = responseList.getFirst();
      if (firstItem.getId().equals(startingAfter)) {
        responseList.removeFirst();
        if (!responseList.isEmpty()) {
          prevId = responseList.getFirst().getId();
        } else {
          // This will always be the last item
          prevId = idPrefix + "_zzzzzzzzzzzzzzzzzzzzzzzzzz";
        }
      }

      // If there are more items, remove remaining items and set "nextId"
      if (responseList.size() > limit) {
        while (responseList.size() > limit) responseList.removeLast();
        nextId = responseList.getLast().getId();
      }
    }

    // If ending before, remove the first item if it's the same as the endingBefore value
    else if (endingBefore != null) {
      // Queries with endingBefore are reversed
      responseList = responseList.reversed();

      var lastItem = responseList.getLast();
      if (lastItem.getId().equals(endingBefore)) {
        responseList.removeLast();
        if (!responseList.isEmpty()) {
          nextId = responseList.getLast().getId();
        } else {
          nextId = "";
        }
      }

      if (responseList.size() > limit) {
        while (responseList.size() > limit) responseList.removeFirst();
        prevId = responseList.getFirst().getId();
      }
    }

    // If we don't have startingAfter or endingBefore, we're at the beginning of the list, but we
    // might have more items
    else if (responseList.size() > limit) {
      while (responseList.size() > limit) {
        responseList.removeLast();
      }
      nextId = responseList.getLast().getId();
    }

    if (nextId != null) {
      if (!nextId.equals("")) {
        uriBuilder.replaceQueryParam("startingAfter", nextId);
      }
      response.setNext(uriBuilder.build().toString());
    }

    if (prevId != null) {
      uriBuilder.replaceQueryParam("endingBefore", prevId);
      response.setPrevious(uriBuilder.build().toString());
    }

    // Convert to DTO
    var expandPaths = expandListToSet(params.getExpand());
    var responseDtoList = new ArrayList<D>();
    for (var responseObject : responseList) {
      responseDtoList.add(proxy.toDTO(responseObject, expandPaths));
    }

    response.setItems(responseDtoList);

    return response;
  }

  /**
   * Retrieves a paginated list of entity objects based on query parameters. Supports pagination
   * through 'startingAfter' and 'endingBefore' fields in the query DTO.
   *
   * <p>We will always fetch one item more than "limit", to make the list() method able to detect
   * whether there are more items available.
   *
   * @param params The query parameters for pagination
   * @return a Page object containing the list of entities
   */
  @Transactional
  public Page<O> getPage(BaseListQueryDTO params, PageRequest pageRequest) {
    Page<O> responsePage = null;
    var repository = this.getRepository();

    var startingAfter = params.getStartingAfter();
    var endingBefore = params.getEndingBefore();
    var sortOrder = params.getSortOrder();
    var hasStartingAfter = startingAfter != null;
    var hasEndingBefore = endingBefore != null;
    var ascending = "asc".equals(sortOrder);
    var descending = !ascending;
    var pivot = hasStartingAfter ? startingAfter : endingBefore;

    if ((hasStartingAfter && ascending) || (hasEndingBefore && descending)) {
      return repository.findByIdGreaterThanEqualOrderByIdAsc(pivot, pageRequest);
    }

    if (hasStartingAfter || hasEndingBefore) {
      return repository.findByIdLessThanEqualOrderByIdDesc(pivot, pageRequest);
    }

    if (ascending) {
      responsePage = repository.findAllByOrderByIdAsc(pageRequest);
    } else {
      responsePage = repository.findAllByOrderByIdDesc(pageRequest);
    }

    return responsePage;
  }

  /**
   * Optionally expands an entity with the given property name and expand paths. If the property is
   * within the expand paths, a full DTO is provided; otherwise, only the ID is returned.
   *
   * @param obj The entity object to expand
   * @param propertyName The property name to check for expansion
   * @param expandPaths A set of paths indicating properties to expand
   * @param currentPath The current path in the object tree, used for nested expansions
   * @return an ExpandableField containing either a full DTO or just the ID
   */
  public ExpandableField<D> maybeExpand(
      O obj, String propertyName, Set<String> expandPaths, String currentPath) {
    if (currentPath == null) currentPath = "";
    var updatedPath = currentPath.isEmpty() ? propertyName : currentPath + "." + propertyName;
    if (expandPaths != null && expandPaths.contains(updatedPath)) {
      return new ExpandableField<>(
          obj.getId(), getProxy().toDTO(obj, newDTO(), expandPaths, updatedPath));
    } else {
      return new ExpandableField<>(obj.getId(), null);
    }
  }

  /**
   * Converts a list of expand strings to a set. For entries that contain a dot, the method will
   * also add the parent path to the set, so you don't need to add all levels of expansion.
   *
   * <p>Example:
   *
   * <p>Input: ["journalpost.journalenhet"]
   *
   * <p>Output: ["journalpost", "journalpost.journalenhet"]
   *
   * @param list
   * @return
   */
  public Set<String> expandListToSet(List<String> list) {
    if (list == null || list.isEmpty()) {
      return new HashSet<>();
    }
    var set = new HashSet<>(list);
    for (var item : set) {
      var dotIndex = item.indexOf('.');
      while (dotIndex >= 0) {
        set.add(item.substring(0, dotIndex));
        dotIndex = item.indexOf('.', dotIndex);
      }
    }
    return set;
  }

  /**
   * Deletes an entity based on its ID. The method finds the entity, delegates to the abstract
   * delete method, and returns the deleted entity's DTO.
   *
   * @param id The unique identifier of the entity to delete
   * @return the DTO of the deleted entity
   */
  @Transactional
  public D delete(String id) throws EInnsynException {
    var proxy = getProxy();
    var obj = proxy.findById(id);
    return proxy.delete(obj);
  }

  /**
   * Abstract method for deleting an entity. This method should be implemented by subclasses to
   * define the specific deletion logic.
   *
   * @param obj The entity object to be deleted
   * @return the DTO of the deleted entity
   * @throws EInnsynException
   */
  @Transactional
  public abstract D delete(O obj) throws EInnsynException;
}

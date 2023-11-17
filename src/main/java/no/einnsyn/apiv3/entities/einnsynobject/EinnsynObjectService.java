package no.einnsyn.apiv3.entities.einnsynobject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.Resource;
import no.einnsyn.apiv3.entities.EinnsynRepository;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObject;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;
import no.einnsyn.apiv3.entities.enhet.EnhetRepository;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.requests.GetListRequestParameters;
import no.einnsyn.apiv3.responses.ResponseList;

public abstract class EinnsynObjectService<O extends EinnsynObject, J extends EinnsynObjectJSON> {

  // Temporarily use Oslo Kommune, since they have lots of subunits for testing. This will be
  // replaced by the logged in user's unit.
  public static String TEMPORARY_ADM_ENHET_ID = "enhet_01haf8swcbeaxt7s6spy92r7mq";

  @Resource
  private EnhetRepository enhetRepository;

  // Wildcard type is needed, because some repositories are EinnsynRepository<O, Integer> and some
  // are EinnsynRepository<O, UUID>
  protected abstract EinnsynRepository<O, ?> getRepository();

  protected abstract EinnsynObjectService<O, J> getService();

  public abstract J newJSON();

  public abstract O newObject();

  /**
   * Insert a new object from a JSON object, persist/index it to all relevant databases.
   * 
   * @param json
   * @return
   */
  public J update(J json) {
    return getService().update(null, json);
  }

  /**
   * Update a Dokumentbeskrivelse from a JSON object, persist/index it to all relevant databases. If
   * no ID is given, a new Dokumentbeskrivelse will be created.
   * 
   * @param id
   * @param json
   * @return
   */
  @Transactional
  public J update(String id, J json) {
    O obj = null;
    var repository = this.getRepository();

    // If ID is given, get the existing saksmappe from DB
    if (id != null) {
      obj = repository.findById(id);
    } else {
      obj = this.newObject();
    }

    // Generate database object from JSON
    Set<String> paths = new HashSet<>();
    obj = this.fromJSON(json, obj, paths, "");
    obj = repository.saveAndFlush(obj);

    // Add / update ElasticSearch document
    this.index(obj, true);

    // Generate JSON containing all inserted objects
    return this.toJSON(obj, paths, "");

  }

  /**
   * Index the object to ElasticSearch. Dummy placeholder for entities that shouldn't be indexed.
   * 
   * @param obj
   */
  public void index(O obj) {
    this.index(obj, false);
  }

  public void index(O obj, boolean shouldUpdateRelatives) {}


  /**
   * 
   * @param json
   * @return
   */
  public O fromJSON(J json) {
    return fromJSON(json, this.newObject(), new HashSet<>(), "");
  }

  /**
   * 
   * @param json
   * @param paths
   * @param currentPath
   * @return
   */
  public O fromJSON(J json, Set<String> paths, String currentPath) {
    return fromJSON(json, this.newObject(), paths, currentPath);
  }

  /**
   * Create a EinnsynObject object from a JSON description
   * 
   * @param einnsynObject
   * @param json
   */
  public O fromJSON(J json, O einnsynObject, Set<String> paths, String currentPath) {
    if (json.getExternalId() != null) {
      einnsynObject.setExternalId(json.getExternalId());
    }

    // This is an insert. Find journalenhet from authentication
    if (einnsynObject.getId() == null) {
      // TODO: Fetch journalenhet from authentication
      Enhet journalEnhet = enhetRepository.findById(TEMPORARY_ADM_ENHET_ID);
      einnsynObject.setJournalenhet(journalEnhet);
    }

    return einnsynObject;
  }


  public J toJSON(O einnsynObject) {
    return toJSON(einnsynObject, newJSON());
  }

  public J toJSON(O einnsynObject, Set<String> expandPaths) {
    return toJSON(einnsynObject, newJSON(), expandPaths, "");
  }

  public J toJSON(O einnsynObject, Set<String> expandPaths, String currentPath) {
    return toJSON(einnsynObject, newJSON(), expandPaths, currentPath);
  }

  public J toJSON(O einnsynObject, J json) {
    return toJSON(einnsynObject, json, new HashSet<>(), "");
  }

  public J toJSON(O einnsynObject, J json, Set<String> expandPaths, String currentPath) {
    json.setId(einnsynObject.getId());
    json.setExternalId(einnsynObject.getExternalId());
    json.setCreated(einnsynObject.getCreated());
    json.setUpdated(einnsynObject.getUpdated());
    return json;
  }


  /**
   * Convert a Saksmappe to an ES document
   * 
   * @param saksmappe
   * @return
   */
  public J toES(O saksmappe) {
    return toES(saksmappe, newJSON());
  }

  /**
   * Convert a EinnsynObject to an ES document
   * 
   * @param mappe
   * @return
   */
  public J toES(O object, J objectES) {
    return objectES;
  }


  /**
   * 
   * @param params
   * @return
   */
  @Transactional
  public ResponseList<J> list(GetListRequestParameters params) {
    return getService().list(params, null);
  }

  /**
   * Allows a parentId string that subclasses can use to filter the list
   */
  @Transactional
  public ResponseList<J> list(GetListRequestParameters params, Page<O> responsePage) {
    ResponseList<J> response = new ResponseList<>();

    // Fetch the requested list page
    if (responsePage == null) {
      responsePage = this.getPage(params);
    }

    List<O> responseList = new LinkedList<>(responsePage.getContent());

    // If there is one more item than requested, set hasMore and remove the last item
    if (responseList.size() > params.getLimit()) {
      response.setHasMore(true);
      responseList.remove(responseList.size() - 1);
    }

    // Convert to JSON
    Set<String> expandPaths = params.getExpand() != null ? params.getExpand() : new HashSet<>();
    List<J> responseJsonList = new ArrayList<>();
    responseList
        .forEach(responseObject -> responseJsonList.add(toJSON(responseObject, expandPaths)));

    response.setData(responseJsonList);

    return response;
  }


  /**
   * Get a single page of a paginated list of objects. This can be overridden by subclasses to allow
   * entity-specific filtering.
   * 
   * @param params
   * @return
   */
  public Page<O> getPage(GetListRequestParameters params) {
    Page<O> responsePage = null;
    var repository = this.getRepository();

    if (params.getStartingAfter() != null) {
      responsePage = repository.findByIdGreaterThanOrderByIdDesc(params.getStartingAfter(),
          PageRequest.of(0, params.getLimit() + 1));
    } else if (params.getEndingBefore() != null) {
      responsePage = repository.findByIdLessThanOrderByIdDesc(params.getEndingBefore(),
          PageRequest.of(0, params.getLimit() + 1));
    } else {
      responsePage = repository.findAllByOrderByIdDesc(PageRequest.of(0, params.getLimit() + 1));
    }

    return responsePage;
  }


  /**
   * Creates an ExpandableField object. If propertyName is in the expandPaths list, the object will
   * be expanded, if not, it will only contain the ID.
   * 
   * @param obj
   * @param propertyName Name of the property to expand, appended to currentPath for deeper steps
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return
   */
  public ExpandableField<J> maybeExpand(O obj, String propertyName, Set<String> expandPaths,
      String currentPath) {
    if (currentPath == null)
      currentPath = "";
    String updatedPath = currentPath.isEmpty() ? propertyName : currentPath + "." + propertyName;
    if (expandPaths != null && expandPaths.contains(updatedPath)) {
      return new ExpandableField<>(obj.getId(),
          this.toJSON(obj, newJSON(), expandPaths, updatedPath));
    } else {
      return new ExpandableField<>(obj.getId(), null);
    }
  }


  /**
   * Delete object by ID
   * 
   * @param id
   * @return
   */
  public abstract J delete(String id);

  /**
   * Delete object
   */
  public abstract J delete(O obj);

}

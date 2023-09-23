package no.einnsyn.apiv3.entities.einnsynobject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import no.einnsyn.apiv3.entities.IEinnsynRepository;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObject;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;
import no.einnsyn.apiv3.entities.enhet.EnhetRepository;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.requests.GetListRequestParameters;
import no.einnsyn.apiv3.responses.ResponseList;

public abstract class EinnsynObjectService<OBJECT extends EinnsynObject, JSON extends EinnsynObjectJSON> {


  @Autowired
  private EnhetRepository enhetRepository;

  // TODO: This might look weird for Enhet since it has UUID ids.
  // Most likely it won't be a problem, because we use the _id field for lookups instead of the
  // primary key.

  public EinnsynObjectService() {}

  protected abstract IEinnsynRepository<OBJECT, Long> getRepository();

  public abstract JSON newJSON();

  public abstract OBJECT newObject();


  /**
   * Update a Dokumentbeskrivelse from a JSON object, persist/index it to all relevant databases. If
   * no ID is given, a new Dokumentbeskrivelse will be created.
   * 
   * @param id
   * @param json
   * @return
   */
  @Transactional
  public JSON update(String id, JSON json) {
    OBJECT obj = null;
    IEinnsynRepository<OBJECT, Long> repository = this.getRepository();

    // If ID is given, get the existing saksmappe from DB
    if (id != null) {
      obj = repository.findById(id);
      if (obj == null) {
        throw new Error("Dokumentobjekt not found");
      }
    } else {
      obj = this.newObject();
    }

    // Generate database object from JSON
    Set<String> paths = new HashSet<String>();
    obj = this.fromJSON(json, obj, paths, "");
    repository.saveAndFlush(obj);

    // Generate JSON containing all inserted objects
    JSON responseJSON = this.toJSON(obj, paths, "");

    return responseJSON;
  }

  /**
   * 
   * @param json
   * @param paths
   * @param currentPath
   * @return
   */
  public OBJECT fromJSON(JSON json, Set<String> paths, String currentPath) {
    return fromJSON(json, this.newObject(), paths, currentPath);
  }

  /**
   * Create a EinnsynObject object from a JSON description
   * 
   * @param einnsynObject
   * @param json
   */
  public OBJECT fromJSON(JSON json, OBJECT einnsynObject, Set<String> paths, String currentPath) {
    if (json.getExternalId() != null) {
      einnsynObject.setExternalId(json.getExternalId());
    }

    // TODO: Fetch journalenhet from authentication
    if (einnsynObject.getId() == null) {
      // Temporarily use Oslo Kommune, since they have lots of subunits for testing
      Enhet journalEnhet = enhetRepository.findById("enhet_01haf8swcbeaxt7s6spy92r7mq");
      einnsynObject.setJournalenhet(journalEnhet);
    }

    return einnsynObject;
  }


  public JSON toJSON(OBJECT einnsynObject) {
    return toJSON(einnsynObject, newJSON());
  }

  public JSON toJSON(OBJECT einnsynObject, Set<String> expandPaths, String currentPath) {
    return toJSON(einnsynObject, newJSON(), expandPaths, currentPath);
  }

  public JSON toJSON(OBJECT einnsynObject, JSON json) {
    return toJSON(einnsynObject, json, new HashSet<String>(), "");
  }

  public JSON toJSON(OBJECT einnsynObject, JSON json, Set<String> expandPaths, String currentPath) {
    json.setId(einnsynObject.getId());
    json.setExternalId(einnsynObject.getExternalId());
    json.setCreated(einnsynObject.getCreated());
    json.setUpdated(einnsynObject.getUpdated());
    return json;
  }


  /**
   * Convert a EinnsynObject to an ES document
   * 
   * @param mappe
   * @return
   */
  public JSON toES(OBJECT object, JSON objectES) {
    this.toJSON(object, objectES);
    // TODO:
    // Add arkivskaperTransitive?
    // Add arkivskaperNavn
    // Add arkivskaperSorteringsnavn

    // TODO:
    // Create child documents for pageviews, innsynskrav, document clicks?
    return objectES;
  }



  /**
   * 
   */
  @Transactional
  public ResponseList<JSON> list(GetListRequestParameters params) {
    ResponseList<JSON> response = new ResponseList<JSON>();
    Page<OBJECT> responsePage;
    IEinnsynRepository<OBJECT, Long> repository = this.getRepository();

    // Fetch the requested list
    if (params.getStartingAfter() != null) {
      responsePage = repository.findByIdGreaterThan(params.getStartingAfter(),
          PageRequest.of(0, params.getLimit() + 1));
    } else if (params.getEndingBefore() != null) {
      responsePage = repository.findByIdLessThan(params.getEndingBefore(),
          PageRequest.of(0, params.getLimit() + 1));
    } else {
      responsePage = repository.findAll(PageRequest.of(0, params.getLimit() + 1));
    }

    List<OBJECT> responseList = new LinkedList<OBJECT>(responsePage.getContent());

    // If there is one more item than requested, set hasMore and remove the last item
    if (responseList.size() > params.getLimit()) {
      response.setHasMore(true);
      responseList.remove(responseList.size() - 1);
    }

    // Convert to JSON
    List<JSON> responseJsonList = new ArrayList<JSON>();
    responseList.forEach(responseObject -> {
      responseJsonList.add(toJSON(responseObject));
    });

    response.setData(responseJsonList);

    return response;
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
  public ExpandableField<JSON> maybeExpand(OBJECT obj, String propertyName, Set<String> expandPaths,
      String currentPath) {
    String updatedPath = currentPath == "" ? propertyName : currentPath + "." + propertyName;
    if (expandPaths.contains(updatedPath)) {
      return new ExpandableField<JSON>(obj.getId(),
          this.toJSON(obj, newJSON(), expandPaths, updatedPath));
    } else {
      return new ExpandableField<JSON>(obj.getId(), null);
    }
  }


}

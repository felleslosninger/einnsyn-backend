package no.einnsyn.apiv3.entities.einnsynobject;

import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import no.einnsyn.apiv3.entities.IEinnsynService;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObject;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;

@Service
public class EinnsynObjectService implements IEinnsynService<EinnsynObject, EinnsynObjectJSON> {

  /**
   * Create a EinnsynObject object from a JSON description
   * 
   * @param einnsynObject
   * @param json
   */
  public EinnsynObject fromJSON(EinnsynObjectJSON json, EinnsynObject einnsynObject,
      Set<String> paths, String currentPath) {
    if (json.getExternalId() != null) {
      einnsynObject.setExternalId(json.getExternalId());
    }

    // TODO: Save "journalenhet", fetch from authentication

    return einnsynObject;
  }


  /**
   * Convert a EinnsynObject to a JSON object
   * 
   * @param einnsynObject
   * @return
   */
  public EinnsynObjectJSON toJSON(EinnsynObject einnsynObject) {
    return toJSON(einnsynObject, new EinnsynObjectJSON());
  }

  public EinnsynObjectJSON toJSON(EinnsynObject einnsynObject, EinnsynObjectJSON json) {
    return toJSON(einnsynObject, json, new HashSet<String>(), "");
  }

  public EinnsynObjectJSON toJSON(EinnsynObject einnsynObject, EinnsynObjectJSON json,
      Set<String> expandPaths, String currentPath) {
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
  public EinnsynObjectJSON toES(EinnsynObject object) {
    return this.toES(object, new EinnsynObjectJSON());
  }

  public EinnsynObjectJSON toES(EinnsynObject object, EinnsynObjectJSON objectES) {
    this.toJSON(object, objectES);
    // TODO:
    // Add arkivskaperTransitive?
    // Add arkivskaperNavn
    // Add arkivskaperSorteringsnavn

    // TODO:
    // Create child documents for pageviews, innsynskrav, document clicks?
    return objectES;
  }

}

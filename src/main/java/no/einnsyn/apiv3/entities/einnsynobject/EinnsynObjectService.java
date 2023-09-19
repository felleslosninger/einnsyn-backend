package no.einnsyn.apiv3.entities.einnsynobject;

import org.springframework.stereotype.Service;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObject;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;

@Service
public class EinnsynObjectService {

  /**
   * Create a EinnsynObject object from a JSON description
   * 
   * @param einnsynObject
   * @param json
   */
  public void fromJSON(EinnsynObject einnsynObject, EinnsynObjectJSON json) {
    if (json.getExternalId() != null) {
      einnsynObject.setExternalId(json.getExternalId());
    }
  }


  /**
   * Convert a EinnsynObject to a JSON object
   * 
   * @param einnsynObject
   * @param depth
   * @return
   */
  public EinnsynObjectJSON toJSON(EinnsynObject einnsynObject, Integer depth) {
    return toJSON(new EinnsynObjectJSON(), einnsynObject, depth);
  }

  public EinnsynObjectJSON toJSON(EinnsynObjectJSON json, EinnsynObject einnsynObject,
      Integer depth) {
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
    return this.toES(new EinnsynObjectJSON(), object);
  }

  public EinnsynObjectJSON toES(EinnsynObjectJSON objectES, EinnsynObject object) {
    this.toJSON(objectES, object, 1);
    // TODO:
    // Add arkivskaperTransitive?
    // Add arkivskaperNavn
    // Add arkivskaperSorteringsnavn

    // TODO:
    // Create child documents for pageviews, innsynskrav, document clicks?
    return objectES;
  }
}

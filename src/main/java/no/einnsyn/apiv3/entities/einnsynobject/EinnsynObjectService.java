package no.einnsyn.apiv3.entities.einnsynobject;

import org.springframework.stereotype.Service;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObject;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;

@Service
public class EinnsynObjectService {
  public void fromJSON(EinnsynObject einnsynObject, EinnsynObjectJSON json) {
    if (json.getExternalId() != null) {
      einnsynObject.setExternalId(json.getExternalId());
    }
  }

  public EinnsynObjectJSON toJSON(EinnsynObject einnsynObject, Integer depth) {
    EinnsynObjectJSON json = new EinnsynObjectJSON();
    return toJSON(einnsynObject, json, depth);
  }

  public EinnsynObjectJSON toJSON(EinnsynObject einnsynObject, EinnsynObjectJSON json,
      Integer depth) {
    json.setId(einnsynObject.getId());
    json.setExternalId(einnsynObject.getExternalId());
    json.setEntity(einnsynObject.getEntity());
    json.setCreated(einnsynObject.getCreated());
    json.setUpdated(einnsynObject.getUpdated());
    return json;
  }
}

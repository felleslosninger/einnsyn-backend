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
}

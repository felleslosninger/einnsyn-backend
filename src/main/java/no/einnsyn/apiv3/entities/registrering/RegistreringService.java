package no.einnsyn.apiv3.entities.registrering;

import org.springframework.stereotype.Service;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.registrering.models.Registrering;
import no.einnsyn.apiv3.entities.registrering.models.RegistreringJSON;

@Service
public class RegistreringService {

  private final EinnsynObjectService einnsynObjectService;

  public RegistreringService(EinnsynObjectService EinnsynObjectService) {
    this.einnsynObjectService = EinnsynObjectService;
  }

  public void fromJSON(Registrering registrering, RegistreringJSON json) {
    einnsynObjectService.fromJSON(registrering, json);

    if (json.getOffentligTittel() != null) {
      registrering.setOffentligTittel(json.getOffentligTittel());
    }

    if (json.getOffentligTittelSensitiv() != null) {
      registrering.setOffentligTittelSensitiv(json.getOffentligTittelSensitiv());
    }

    if (json.getPublisertDato() != null) {
      registrering.setPublisertDato(json.getPublisertDato());
    }

    // TODO: Implement virksomhet
  }


  public RegistreringJSON toJSON(Registrering registrering, Integer depth) {
    RegistreringJSON json = new RegistreringJSON();
    return toJSON(registrering, json, depth);
  }

  public RegistreringJSON toJSON(Registrering registrering, RegistreringJSON json, Integer depth) {
    einnsynObjectService.toJSON(registrering, json, depth);
    json.setOffentligTittel(registrering.getOffentligTittel());
    json.setOffentligTittelSensitiv(registrering.getOffentligTittelSensitiv());
    json.setPublisertDato(registrering.getPublisertDato());
    return json;
  }
}

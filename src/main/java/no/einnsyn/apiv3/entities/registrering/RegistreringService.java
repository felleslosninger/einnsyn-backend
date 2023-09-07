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
}

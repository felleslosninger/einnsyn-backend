package no.einnsyn.apiv3.entities.registrering;

import java.util.HashSet;
import java.util.Set;
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

  public void fromJSON(RegistreringJSON json, Registrering registrering, Set<String> paths,
      String currentPath) {
    einnsynObjectService.fromJSON(json, registrering, paths, currentPath);

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


  public RegistreringJSON toJSON(Registrering registrering, Set<String> expandPaths,
      String currentPath) {
    return toJSON(registrering, new RegistreringJSON(), expandPaths, currentPath);
  }

  public RegistreringJSON toJSON(Registrering registrering, RegistreringJSON json,
      Set<String> expandPaths, String currentPath) {
    einnsynObjectService.toJSON(registrering, json, expandPaths, currentPath);
    json.setOffentligTittel(registrering.getOffentligTittel());
    json.setOffentligTittelSensitiv(registrering.getOffentligTittelSensitiv());
    json.setPublisertDato(registrering.getPublisertDato());
    return json;
  }


  public RegistreringJSON toES(Registrering registrering, RegistreringJSON json) {
    this.toJSON(registrering, json, new HashSet<String>(), "");
    einnsynObjectService.toES(registrering, json);

    // TODO:
    // Create child documents for pageviews, innsynskrav, document clicks?

    return json;
  }
}

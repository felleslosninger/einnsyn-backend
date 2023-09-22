package no.einnsyn.apiv3.entities.registrering;

import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import no.einnsyn.apiv3.entities.IEinnsynService;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.registrering.models.Registrering;
import no.einnsyn.apiv3.entities.registrering.models.RegistreringJSON;

@Service
public class RegistreringService implements IEinnsynService<Registrering, RegistreringJSON> {

  private final EinnsynObjectService einnsynObjectService;

  public RegistreringService(EinnsynObjectService EinnsynObjectService) {
    this.einnsynObjectService = EinnsynObjectService;
  }


  /**
   * Convert a JSON object to a Registrering
   * 
   * @param json
   * @param registrering
   * @param paths A list of paths to expand. Un-expanded objects will be shown as IDs
   * @param currentPath The current path in the object tree
   */
  public Registrering fromJSON(RegistreringJSON json, Registrering registrering, Set<String> paths,
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

    return registrering;
  }


  /**
   * Convert a Registrering to a JSON object
   * 
   * @param registrering
   * @param json
   * @param expandPaths A list of paths to expand. Un-expanded objects will be shown as IDs
   * @param currentPath The current path in the object tree
   * @return
   */
  public RegistreringJSON toJSON(Registrering registrering, RegistreringJSON json,
      Set<String> expandPaths, String currentPath) {
    einnsynObjectService.toJSON(registrering, json, expandPaths, currentPath);
    json.setOffentligTittel(registrering.getOffentligTittel());
    json.setOffentligTittelSensitiv(registrering.getOffentligTittelSensitiv());
    json.setPublisertDato(registrering.getPublisertDato());
    return json;
  }


  /**
   * Convert a Registrering to an Elasticsearch document
   * 
   * @param registrering
   * @param json
   * @return
   */
  public RegistreringJSON toES(Registrering registrering, RegistreringJSON json) {
    this.toJSON(registrering, json, new HashSet<String>(), "");
    einnsynObjectService.toES(registrering, json);

    // TODO:
    // Create child documents for pageviews, innsynskrav, document clicks?

    return json;
  }
}

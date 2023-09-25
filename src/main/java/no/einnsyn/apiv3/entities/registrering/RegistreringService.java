package no.einnsyn.apiv3.entities.registrering;

import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.enhet.EnhetService;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.registrering.models.Registrering;
import no.einnsyn.apiv3.entities.registrering.models.RegistreringJSON;


public abstract class RegistreringService<OBJECT extends Registrering, JSON extends RegistreringJSON>
    extends EinnsynObjectService<OBJECT, JSON> {

  @Autowired
  private EnhetService enhetService;


  /**
   * Convert a JSON object to a Registrering
   * 
   * @param json
   * @param registrering
   * @param paths A list of paths to expand. Un-expanded objects will be shown as IDs
   * @param currentPath The current path in the object tree
   */
  public OBJECT fromJSON(JSON json, OBJECT registrering, Set<String> paths, String currentPath) {
    super.fromJSON(json, registrering, paths, currentPath);

    if (json.getOffentligTittel() != null) {
      registrering.setOffentligTittel(json.getOffentligTittel());
    }

    if (json.getOffentligTittelSensitiv() != null) {
      registrering.setOffentligTittelSensitiv(json.getOffentligTittelSensitiv());
    }

    if (json.getPublisertDato() != null) {
      registrering.setPublisertDato(json.getPublisertDato());
    }

    // Look up administrativEnhet
    String administrativEnhet = json.getAdministrativEnhet();
    if (administrativEnhet != null) {
      registrering.setAdministrativEnhet(administrativEnhet);
      Enhet journalenhet = registrering.getJournalenhet();
      Enhet administrativEnhetObjekt =
          enhetService.findByEnhetskode(json.getAdministrativEnhet(), journalenhet);
      if (administrativEnhetObjekt != null) {
        registrering.setAdministrativEnhetObjekt(administrativEnhetObjekt);
      }
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
  public JSON toJSON(OBJECT registrering, JSON json, Set<String> expandPaths, String currentPath) {

    super.toJSON(registrering, json, expandPaths, currentPath);
    json.setOffentligTittel(registrering.getOffentligTittel());
    json.setOffentligTittelSensitiv(registrering.getOffentligTittelSensitiv());
    json.setPublisertDato(registrering.getPublisertDato());

    Enhet administrativEnhetObjekt = registrering.getAdministrativEnhetObjekt();
    if (administrativEnhetObjekt != null) {
      json.setAdministrativEnhetObjekt(enhetService.maybeExpand(administrativEnhetObjekt,
          "administrativEnhetObjekt", expandPaths, currentPath));
    }

    return json;
  }


  /**
   * Convert a Registrering to an Elasticsearch document
   * 
   * @param registrering
   * @param json
   * @return
   */
  public JSON toES(OBJECT registrering, JSON json) {
    this.toJSON(registrering, json, new HashSet<String>(), "");
    super.toES(registrering, json);

    // TODO:
    // Create child documents for pageviews, innsynskrav, document clicks?

    return json;
  }
}

package no.einnsyn.apiv3.entities.mappe;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.enhet.EnhetService;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.mappe.models.Mappe;
import no.einnsyn.apiv3.entities.mappe.models.MappeJSON;

public abstract class MappeService<OBJECT extends Mappe, JSON extends MappeJSON>
    extends EinnsynObjectService<OBJECT, JSON> {

  @Autowired
  private EnhetService enhetService;


  /**
   * Convert a JSON object to a Mappe
   * 
   * @param json
   * @param mappe
   * @param paths A list of paths containing new objects that will be created from this update
   * @param currentPath The current path in the object tree
   * @return
   */
  public OBJECT fromJSON(JSON json, OBJECT mappe, Set<String> paths, String currentPath) {
    super.fromJSON(json, mappe, paths, currentPath);

    if (json.getOffentligTittel() != null) {
      mappe.setOffentligTittel(json.getOffentligTittel());
    }

    if (json.getOffentligTittelSensitiv() != null) {
      mappe.setOffentligTittelSensitiv(json.getOffentligTittelSensitiv());
    }

    if (json.getBeskrivelse() != null) {
      mappe.setBeskrivelse(json.getBeskrivelse());
    }

    // Set publisertDato to now if not set for new objects
    if (json.getPublisertDato() != null) {
      mappe.setPublisertDato(json.getPublisertDato());
    } else if (mappe.getId() == null) {
      mappe.setPublisertDato(Instant.now());
    }

    // Look up administrativEnhet
    String administrativEnhet = json.getAdministrativEnhet();
    if (administrativEnhet != null) {
      mappe.setAdministrativEnhet(administrativEnhet);
      Enhet journalenhet = mappe.getJournalenhet();
      Enhet administrativEnhetObjekt =
          enhetService.findByEnhetskode(json.getAdministrativEnhet(), journalenhet);
      if (administrativEnhetObjekt != null) {
        mappe.setAdministrativEnhetObjekt(administrativEnhetObjekt);
      }
    }

    return mappe;
  }


  /**
   * Convert a Mappe to a JSON object
   * 
   * @param mappe
   * @param json
   * @param expandPaths A list of "paths" to expand. Un-expanded objects will be shown as IDs
   * @param currentPath The current "path" in the object tree
   * @return
   */
  public JSON toJSON(OBJECT mappe, JSON json, Set<String> expandPaths, String currentPath) {

    super.toJSON(mappe, json, expandPaths, currentPath);
    json.setOffentligTittel(mappe.getOffentligTittel());
    json.setOffentligTittelSensitiv(mappe.getOffentligTittelSensitiv());
    json.setBeskrivelse(mappe.getBeskrivelse());
    json.setPublisertDato(mappe.getPublisertDato());
    json.setAdministrativEnhet(mappe.getAdministrativEnhet());

    Enhet administrativEnhetObjekt = mappe.getAdministrativEnhetObjekt();
    if (administrativEnhetObjekt != null) {
      json.setAdministrativEnhetObjekt(enhetService.maybeExpand(administrativEnhetObjekt,
          "administrativEnhetObjekt", expandPaths, currentPath));
    }

    return json;
  }


  /**
   * Convert a Mappe to an ES document
   * 
   * @param mappe
   * @return
   */
  public JSON toES(JSON mappeES, OBJECT mappe) {
    super.toES(mappe, mappeES);

    // Find list of ancestors
    Enhet administrativEnhet = mappe.getAdministrativEnhetObjekt();
    List<Enhet> administrativEnhetTransitive = enhetService.getTransitiveEnhets(administrativEnhet);

    List<String> administrativEnhetIdTransitive = new ArrayList<String>();
    // Legacy
    List<String> arkivskaperTransitive = new ArrayList<String>();
    // Legacy
    List<String> arkivskaperNavn = new ArrayList<String>();
    for (Enhet ancestor : administrativEnhetTransitive) {
      administrativEnhetIdTransitive.add(ancestor.getId());
      arkivskaperTransitive.add(ancestor.getIri());
      arkivskaperNavn.add(ancestor.getNavn());
    }
    // Legacy fields
    mappeES.setArkivskaperTransitive(arkivskaperTransitive);
    mappeES.setArkivskaperNavn(arkivskaperNavn);
    mappeES.setArkivskaperSorteringNavn(arkivskaperNavn.get(0));
    mappeES.setArkivskaper(mappe.getAdministrativEnhetObjekt().getIri());

    // TODO:
    // Create child documents for pageviews, innsynskrav, document clicks?

    return mappeES;
  }

}

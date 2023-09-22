package no.einnsyn.apiv3.entities.mappe;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import no.einnsyn.apiv3.entities.IEinnsynService;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.enhet.EnhetService;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.mappe.models.Mappe;
import no.einnsyn.apiv3.entities.mappe.models.MappeJSON;
import no.einnsyn.apiv3.utils.AdministrativEnhetFinder;

@Service
public class MappeService implements IEinnsynService<Mappe, MappeJSON> {

  private final EinnsynObjectService einnsynObjectService;
  private final EnhetService enhetService;

  public MappeService(EinnsynObjectService EinnsynObjectService, EnhetService enhetService) {
    this.einnsynObjectService = EinnsynObjectService;
    this.enhetService = enhetService;
  }


  /**
   * Convert a JSON object to a Mappe
   * 
   * @param json
   * @param mappe
   * @param paths A list of paths containing new objects that will be created from this update
   * @param currentPath The current path in the object tree
   * @return
   */
  public Mappe fromJSON(MappeJSON json, Mappe mappe, Set<String> paths, String currentPath) {
    einnsynObjectService.fromJSON(json, mappe, paths, currentPath);

    if (json.getOffentligTittel() != null) {
      mappe.setOffentligTittel(json.getOffentligTittel());
    }

    if (json.getOffentligTittelSensitiv() != null) {
      mappe.setOffentligTittelSensitiv(json.getOffentligTittelSensitiv());
    }

    if (json.getBeskrivelse() != null) {
      mappe.setBeskrivelse(json.getBeskrivelse());
    }

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
          AdministrativEnhetFinder.find(json.getAdministrativEnhet(), journalenhet);
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
  public MappeJSON toJSON(Mappe mappe, MappeJSON json, Set<String> expandPaths,
      String currentPath) {

    einnsynObjectService.toJSON(mappe, json, expandPaths, currentPath);
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
  public MappeJSON toES(MappeJSON mappeES, Mappe mappe) {
    this.toJSON(mappe, mappeES, new HashSet<String>(), "");
    einnsynObjectService.toES(mappe, mappeES);


    // TODO:
    // Add arkivskaperTransitive
    // Add arkivskaperNavn
    // Add arkivskaperSorteringsnavn

    // TODO:
    // Create child documents for pageviews, innsynskrav, document clicks?

    return mappeES;
  }

}

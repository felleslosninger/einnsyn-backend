package no.einnsyn.apiv3.entities.mappe;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.enhet.EnhetRepository;
import no.einnsyn.apiv3.entities.enhet.EnhetService;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.enhet.models.EnhetJSON;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.mappe.models.Mappe;
import no.einnsyn.apiv3.entities.mappe.models.MappeJSON;

@Service
public class MappeService {

  private final EinnsynObjectService einnsynObjectService;
  private final EnhetRepository enhetRepository;
  private final EnhetService enhetService;

  public MappeService(EinnsynObjectService EinnsynObjectService, EnhetRepository enhetRepository,
      EnhetService enhetService) {
    this.einnsynObjectService = EinnsynObjectService;
    this.enhetRepository = enhetRepository;
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

    // TODO: This should possibly be an ExpandableField (Or it should be looked up if it's a code)
    if (json.getArkivskaper() != null) {
      mappe.setArkivskaper(json.getArkivskaper());
    }

    if (json.getPublisertDato() != null) {
      mappe.setPublisertDato(json.getPublisertDato());
    } else if (mappe.getId() == null) {
      mappe.setPublisertDato(Instant.now());
    }

    // Virksomhet
    ExpandableField<EnhetJSON> virksomhetField = json.getVirksomhet();
    if (virksomhetField != null) {
      Enhet enhet = enhetRepository.findById(virksomhetField.getId());
      mappe.setVirksomhet(enhet);
    }

    return mappe;
  }


  /**
   * Convert a Mappe to a JSON object
   * 
   * @param mappe
   * @return
   */
  public MappeJSON toJSON(Mappe mappe) {
    return toJSON(mappe, new MappeJSON(), new HashSet<String>(), "");
  }

  /**
   * Convert a Mappe to a JSON object
   * 
   * @param mappe
   * @param expandPaths A list of "paths" to expand. Un-expanded objects will be shown as IDs
   * @param currentPath The current "path" in the object tree
   * @return
   */
  public MappeJSON toJSON(Mappe mappe, Set<String> expandPaths, String currentPath) {
    return toJSON(mappe, new MappeJSON(), new HashSet<String>(), "");
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
    json.setArkivskaper(mappe.getArkivskaper());
    json.setPublisertDato(mappe.getPublisertDato());

    Enhet virksomhet = mappe.getVirksomhet();
    if (virksomhet != null) {
      json.setVirksomhet(
          enhetService.maybeExpand(virksomhet, "virksomhet", expandPaths, currentPath));
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

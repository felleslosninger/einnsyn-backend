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
   * Create a Mappe object from a JSON description
   * 
   * @param mappe
   * @param json
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
   * @param depth number of steps to recurse into children
   * @return
   */
  public MappeJSON toJSON(Mappe mappe) {
    return toJSON(mappe, new MappeJSON(), new HashSet<String>(), "");
  }

  public MappeJSON toJSON(Mappe mappe, Set<String> expandPaths, String currentPath) {
    return toJSON(mappe, new MappeJSON(), new HashSet<String>(), "");
  }

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


  public ExpandableField<MappeJSON> maybeExpand(Mappe mappe, String propertyName,
      Set<String> expandPaths, String currentPath) {
    if (expandPaths.contains(currentPath)) {
      return new ExpandableField<MappeJSON>(mappe.getId(), this.toJSON(mappe, expandPaths,
          currentPath == "" ? propertyName : currentPath + "." + propertyName));
    } else {
      return new ExpandableField<MappeJSON>(mappe.getId(), null);
    }
  }


}

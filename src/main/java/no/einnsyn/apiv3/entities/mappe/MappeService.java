package no.einnsyn.apiv3.entities.mappe;

import org.springframework.stereotype.Service;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.mappe.models.Mappe;
import no.einnsyn.apiv3.entities.mappe.models.MappeJSON;

@Service
public class MappeService {

  private final EinnsynObjectService einnsynObjectService;

  public MappeService(EinnsynObjectService EinnsynObjectService) {
    this.einnsynObjectService = EinnsynObjectService;
  }


  /**
   * Create a Mappe object from a JSON description
   * 
   * @param mappe
   * @param json
   */
  public void fromJSON(Mappe mappe, MappeJSON json) {
    einnsynObjectService.fromJSON(mappe, json);
    if (json.getOffentligTittel() != null) {
      mappe.setOffentligTittel(json.getOffentligTittel());
    }
    if (json.getOffentligTittelSensitiv() != null) {
      mappe.setOffentligTittelSensitiv(json.getOffentligTittelSensitiv());
    }
    if (json.getBeskrivelse() != null) {
      mappe.setBeskrivelse(json.getBeskrivelse());
    }
    // TODO: This should be an ExpandableField
    if (json.getArkivskaper() != null) {
      mappe.setArkivskaper(json.getArkivskaper());
    }
    if (json.getPublisertDato() != null) {
      mappe.setPublisertDato(json.getPublisertDato());
    }
    if (json.getVirksomhetIri() != null) {
      // Lookup virksomhetsId
      mappe.setVirksomhetIri(json.getVirksomhetIri());
    }
    /*
     * if (json.getVirksomhet() != null) { mappe.setVirksomhet(json.getVirksomhet()); }
     */
  }


  /**
   * Convert a Mappe to a JSON object
   * 
   * @param mappe
   * @param depth number of steps to recurse into children
   * @return
   */
  public MappeJSON toJSON(Mappe mappe, Integer depth) {
    MappeJSON json = new MappeJSON();
    return toJSON(mappe, json, depth);
  }

  public MappeJSON toJSON(Mappe mappe, MappeJSON json, Integer depth) {
    einnsynObjectService.toJSON(mappe, json, depth);
    json.setOffentligTittel(mappe.getOffentligTittel());
    json.setOffentligTittelSensitiv(mappe.getOffentligTittelSensitiv());
    json.setBeskrivelse(mappe.getBeskrivelse());
    json.setArkivskaper(mappe.getArkivskaper());
    json.setPublisertDato(mappe.getPublisertDato());
    json.setVirksomhetIri(mappe.getVirksomhetIri());
    return json;
  }


  /**
   * Convert a Mappe to an ES document
   * 
   * @param mappe
   * @return
   */
  public MappeJSON toES(Mappe mappe) {
    return this.toES(new MappeJSON(), mappe);
  }

  public MappeJSON toES(MappeJSON mappeES, Mappe mappe) {
    this.toJSON(mappe, mappeES, 1);
    einnsynObjectService.toES(mappeES, mappe);

    // TODO:
    // Add arkivskaperTransitive
    // Add arkivskaperNavn
    // Add arkivskaperSorteringsnavn

    // TODO:
    // Create child documents for pageviews, innsynskrav, document clicks?

    return mappeES;
  }


}

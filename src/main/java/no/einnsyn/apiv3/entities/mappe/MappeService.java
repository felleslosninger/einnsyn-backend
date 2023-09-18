package no.einnsyn.apiv3.entities.mappe;

import java.time.Instant;
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
    } else if (mappe.getId() == null) {
      mappe.setPublisertDato(Instant.now());
    }

    // Virksomhet
    ExpandableField<EnhetJSON> virksomhetField = json.getVirksomhet();
    if (virksomhetField != null) {
      Enhet enhet = enhetRepository.findById(virksomhetField.getId());
      mappe.setVirksomhet(enhet);
    }
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

    Enhet virksomhet = mappe.getVirksomhet();
    if (virksomhet != null) {
      json.setVirksomhet(new ExpandableField<EnhetJSON>(virksomhet.getId(),
          enhetService.toJSON(virksomhet, depth - 1)));
    }

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

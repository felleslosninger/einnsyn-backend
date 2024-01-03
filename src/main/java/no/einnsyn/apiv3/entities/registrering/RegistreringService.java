package no.einnsyn.apiv3.entities.registrering;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.enhet.EnhetService;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.registrering.models.Registrering;
import no.einnsyn.apiv3.entities.registrering.models.RegistreringJSON;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class RegistreringService<O extends Registrering, J extends RegistreringJSON>
    extends EinnsynObjectService<O, J> {

  @Autowired private EnhetService enhetService;

  /**
   * Convert a JSON object to a Registrering
   *
   * @param json
   * @param registrering
   * @param paths A list of paths to expand. Un-expanded objects will be shown as IDs
   * @param currentPath The current path in the object tree
   */
  @Override
  public O fromJSON(J json, O registrering, Set<String> paths, String currentPath) {
    super.fromJSON(json, registrering, paths, currentPath);

    if (json.getOffentligTittel() != null) {
      registrering.setOffentligTittel(json.getOffentligTittel());
    }

    if (json.getOffentligTittelSensitiv() != null) {
      registrering.setOffentligTittelSensitiv(json.getOffentligTittelSensitiv());
    }

    // Set publisertDato to now if not set for new objects
    if (json.getPublisertDato() != null) {
      registrering.setPublisertDato(json.getPublisertDato());
    } else if (registrering.getId() == null) {
      registrering.setPublisertDato(Instant.now());
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
  @Override
  public J toJSON(O registrering, J json, Set<String> expandPaths, String currentPath) {

    super.toJSON(registrering, json, expandPaths, currentPath);
    json.setOffentligTittel(registrering.getOffentligTittel());
    json.setOffentligTittelSensitiv(registrering.getOffentligTittelSensitiv());
    json.setPublisertDato(registrering.getPublisertDato());

    Enhet administrativEnhetObjekt = registrering.getAdministrativEnhetObjekt();
    if (administrativEnhetObjekt != null) {
      json.setAdministrativEnhetObjekt(
          enhetService.maybeExpand(
              administrativEnhetObjekt, "administrativEnhetObjekt", expandPaths, currentPath));
    }

    return json;
  }

  /**
   * Convert a Registrering to an Elasticsearch document
   *
   * @param registrering
   * @param registreringES
   * @return
   */
  @Override
  public J toES(O registrering, J registreringES) {
    super.toES(registrering, registreringES);

    // Find list of ancestors
    Enhet administrativEnhet = registrering.getAdministrativEnhetObjekt();
    List<Enhet> administrativEnhetTransitive = enhetService.getTransitiveEnhets(administrativEnhet);

    List<String> administrativEnhetIdTransitive = new ArrayList<>();
    // Legacy
    List<String> arkivskaperTransitive = new ArrayList<>();
    // Legacy
    List<String> arkivskaperNavn = new ArrayList<>();
    for (Enhet ancestor : administrativEnhetTransitive) {
      administrativEnhetIdTransitive.add(ancestor.getId());
      arkivskaperTransitive.add(ancestor.getIri());
      arkivskaperNavn.add(ancestor.getNavn());
    }
    registreringES.setArkivskaperTransitive(arkivskaperTransitive);
    registreringES.setArkivskaperNavn(arkivskaperNavn);
    registreringES.setArkivskaperSorteringNavn(arkivskaperNavn.get(0));
    registreringES.setArkivskaper(registrering.getAdministrativEnhetObjekt().getIri());

    // TODO: Create child documents for pageviews, innsynskrav, document clicks?

    return registreringES;
  }
}

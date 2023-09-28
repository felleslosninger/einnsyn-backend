package no.einnsyn.apiv3.entities.enhet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import lombok.Getter;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.enhet.models.EnhetJSON;

@Service
public class EnhetService extends EinnsynObjectService<Enhet, EnhetJSON> {

  @Getter
  private final EnhetRepository repository;

  EnhetService(EnhetRepository repository) {
    this.repository = repository;
  }

  public Enhet newObject() {
    return new Enhet();
  }

  public EnhetJSON newJSON() {
    return new EnhetJSON();
  }


  public Enhet fromJSON(EnhetJSON json, Enhet enhet, Set<String> paths, String currentPath) {
    super.fromJSON(json, enhet, paths, currentPath);

    if (json.getNavn() != null) {
      enhet.setNavn(json.getNavn());
    }

    if (json.getNavnNynorsk() != null) {
      enhet.setNavnNynorsk(json.getNavnNynorsk());
    }

    if (json.getNavnEngelsk() != null) {
      enhet.setNavnEngelsk(json.getNavnEngelsk());
    }

    if (json.getNavnSami() != null) {
      enhet.setNavnSami(json.getNavnSami());
    }

    if (json.getAvsluttetDato() != null) {
      enhet.setAvsluttetDato(json.getAvsluttetDato());
    }

    if (json.getInnsynskravEpost() != null) {
      enhet.setInnsynskravEpost(json.getInnsynskravEpost());
    }

    if (json.getKontaktpunktAdresse() != null) {
      enhet.setKontaktpunktAdresse(json.getKontaktpunktAdresse());
    }

    if (json.getKontaktpunktEpost() != null) {
      enhet.setKontaktpunktEpost(json.getKontaktpunktEpost());
    }

    if (json.getKontaktpunktTelefon() != null) {
      enhet.setKontaktpunktTelefon(json.getKontaktpunktTelefon());
    }

    if (json.getOrgnummer() != null) {
      enhet.setOrgnummer(json.getOrgnummer());
    }

    if (json.getEnhetskode() != null) {
      enhet.setEnhetskode(json.getEnhetskode());
    }

    if (json.getEnhetstype() != null) {
      enhet.setEnhetstype(json.getEnhetstype());
    }

    if (json.getSkjult() != null) {
      enhet.setSkjult(json.getSkjult());
    }

    if (json.getEFormidling() != null) {
      enhet.setEFormidling(json.getEFormidling());
    }

    if (json.getVisToppnode() != null) {
      enhet.setVisToppnode(json.getVisToppnode());
    }

    if (json.getErTeknisk() != null) {
      enhet.setErTeknisk(json.getErTeknisk());
    }

    if (json.getSkalKonvertereId() != null) {
      enhet.setSkalKonvertereId(json.getSkalKonvertereId());
    }

    if (json.getSkalMottaKvittering() != null) {
      enhet.setSkalMottaKvittering(json.getSkalMottaKvittering());
    }

    if (json.getOrderXmlVersjon() != null) {
      enhet.setOrderXmlVersjon(json.getOrderXmlVersjon());
    }

    if (json.getParent() != null) {
      Enhet parent = repository.findById(json.getParent().getId());
      enhet.setParent(parent);
    }

    return enhet;
  }


  public EnhetJSON toJSON(Enhet enhet, EnhetJSON json, Set<String> expandPaths,
      String currentPath) {
    super.toJSON(enhet, json, expandPaths, currentPath);

    json.setNavn(enhet.getNavn());
    json.setNavnNynorsk(enhet.getNavnNynorsk());
    json.setNavnEngelsk(enhet.getNavnEngelsk());
    json.setNavnSami(enhet.getNavnSami());
    json.setAvsluttetDato(enhet.getAvsluttetDato());
    json.setInnsynskravEpost(enhet.getInnsynskravEpost());
    json.setKontaktpunktAdresse(enhet.getKontaktpunktAdresse());
    json.setKontaktpunktEpost(enhet.getKontaktpunktEpost());
    json.setKontaktpunktTelefon(enhet.getKontaktpunktTelefon());
    json.setOrgnummer(enhet.getOrgnummer());
    json.setEnhetskode(enhet.getEnhetskode());
    json.setEnhetstype(enhet.getEnhetstype());
    json.setSkjult(enhet.isSkjult());
    json.setEFormidling(enhet.getEFormidling());
    json.setVisToppnode(enhet.getVisToppnode());
    json.setErTeknisk(enhet.getErTeknisk());
    json.setSkalKonvertereId(enhet.getSkalKonvertereId());
    json.setSkalMottaKvittering(enhet.getSkalMottaKvittering());
    json.setOrderXmlVersjon(enhet.getOrderXmlVersjon());

    Enhet parent = enhet.getParent();
    if (parent != null) {
      json.setParent(maybeExpand(parent, "parent", expandPaths, currentPath));
    }

    return json;
  }



  /**
   * Search the subtree under `root` for an enhet with matching enhetskode. Searching breadth-first
   * to avoid unnecessary DB queries.
   * 
   * @param enhetskode
   * @param root
   * @return
   */
  public Enhet findByEnhetskode(String enhetskode, Enhet root) {

    // Empty string is not a valid enhetskode
    if (enhetskode == null || root == null || enhetskode.equals("")) {
      return null;
    }

    Integer checkElementCount = 0;
    Integer queryChildrenCount = 0;
    List<Enhet> queue = new ArrayList<Enhet>();
    Set<Enhet> visited = new HashSet<Enhet>();

    // Search for enhet with matching enhetskode, breadth-first to avoid unnecessary DB queries
    queue.add(root);
    while (checkElementCount < queue.size()) {
      Enhet enhet = queue.get(checkElementCount);
      checkElementCount++;

      // Avoid infinite loops
      if (visited.contains(enhet)) {
        continue;
      }
      visited.add(enhet);

      if (enhetskode.equals(enhet.getEnhetskode())) {
        return enhet;
      }

      // Add more children to queue when needed
      while (checkElementCount >= queue.size() && queryChildrenCount < queue.size()) {
        Enhet querier = queue.get(queryChildrenCount);
        queryChildrenCount++;
        List<Enhet> underenheter = querier.getUnderenheter();
        if (underenheter != null) {
          queue.addAll(underenheter);
        }
      }
    }

    return null;
  }


  /**
   * Get a "transitive" list of ancestors for an Enhet object.
   * 
   * @param enhet
   * @return
   */
  public List<Enhet> getTransitiveEnhets(Enhet enhet) {
    List<Enhet> transitiveList = new ArrayList<Enhet>();
    Set<Enhet> visited = new HashSet<Enhet>();
    Enhet parent = enhet;
    while (parent != null && !visited.contains(parent)) {
      transitiveList.add(parent);
      visited.add(parent);
      parent = parent.getParent();
      if (parent != null) {
        String enhetstype = parent.getEnhetstype().toString();
        if (enhetstype.equals("DummyEnhet") || enhetstype.equals("AdministrativEnhet")) {
          break;
        }
      }
    }
    return transitiveList;
  }

}

package no.einnsyn.apiv3.entities.enhet;

import java.util.Set;
import org.springframework.stereotype.Service;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.enhet.models.EnhetJSON;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;

@Service
public class EnhetService {

  private final EinnsynObjectService einnsynObjectService;

  public EnhetService(EinnsynObjectService eInnsynObjectService) {
    this.einnsynObjectService = eInnsynObjectService;
  }


  public Enhet fromJSON(EnhetJSON json, Enhet enhet, Set<String> paths, String currentPath) {
    einnsynObjectService.fromJSON(json, enhet, paths, currentPath);

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

    return enhet;
  }


  public EnhetJSON toJSON(Enhet enhet, Set<String> expandPaths, String currentPath) {
    return toJSON(enhet, new EnhetJSON(), expandPaths, currentPath);
  }

  public EnhetJSON toJSON(Enhet enhet, EnhetJSON json, Set<String> expandPaths,
      String currentPath) {
    einnsynObjectService.toJSON(enhet, json, expandPaths, currentPath);

    // TODO: Parent

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

    return json;
  }


  /**
   * Creates an ExpandableField object. If propertyName is in the expandPaths list, the object will
   * be expanded, if not, it will only contain the ID.
   * 
   * @param enhet
   * @param propertyName Name of the property to expand, appended to currentPath for deeper steps
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return
   */
  public ExpandableField<EnhetJSON> maybeExpand(Enhet enhet, String propertyName,
      Set<String> expandPaths, String currentPath) {
    if (expandPaths.contains(currentPath)) {
      return new ExpandableField<EnhetJSON>(enhet.getId(), this.toJSON(enhet, expandPaths,
          currentPath == "" ? propertyName : currentPath + "." + propertyName));
    } else {
      return new ExpandableField<EnhetJSON>(enhet.getId(), null);
    }
  }

}

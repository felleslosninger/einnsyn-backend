package no.einnsyn.apiv3.entities.enhet;

import org.springframework.stereotype.Service;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.enhet.models.EnhetJSON;

@Service
public class EnhetService {

  private final EinnsynObjectService eInnsynObjectService;

  public EnhetService(EinnsynObjectService eInnsynObjectService) {
    this.eInnsynObjectService = eInnsynObjectService;
  }

  public void fromJSON(Enhet enhet, EnhetJSON json) {
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
  }


  public EnhetJSON toJSON(Enhet enhet, Integer depth) {
    EnhetJSON json = new EnhetJSON();
    return toJSON(json, enhet, depth);
  }

  public EnhetJSON toJSON(EnhetJSON json, Enhet enhet, Integer depth) {
    eInnsynObjectService.toJSON(json, enhet, depth);

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

}

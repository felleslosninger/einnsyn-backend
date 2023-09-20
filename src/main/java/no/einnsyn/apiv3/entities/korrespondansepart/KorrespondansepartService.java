package no.einnsyn.apiv3.entities.korrespondansepart;

import java.util.Set;
import org.springframework.stereotype.Service;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.korrespondansepart.models.Korrespondansepart;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartJSON;

@Service
public class KorrespondansepartService {

  private final EinnsynObjectService einnsynObjectService;

  public KorrespondansepartService(EinnsynObjectService eInnsynObjectService) {
    this.einnsynObjectService = eInnsynObjectService;
  }


  /**
   * 
   * @param json
   * @return
   */
  public Korrespondansepart fromJSON(KorrespondansepartJSON json, Set<String> paths,
      String currentPath) {
    return fromJSON(json, new Korrespondansepart(), paths, currentPath);
  }

  public Korrespondansepart fromJSON(KorrespondansepartJSON json,
      Korrespondansepart korrespondansepart, Set<String> paths, String currentPath) {
    einnsynObjectService.fromJSON(json, korrespondansepart, paths, currentPath);

    if (json.getKorrespondansepartType() != null) {
      korrespondansepart.setKorrespondanseparttype(json.getKorrespondansepartType());
    }

    if (json.getNavn() != null) {
      korrespondansepart.setKorrespondansepartNavn(json.getNavn());
    }

    if (json.getNavnSensitiv() != null) {
      korrespondansepart.setKorrespondansepartNavnSensitiv(json.getNavnSensitiv());
    }

    if (json.getAdministrativEnhet() != null) {
      korrespondansepart.setAdministrativEnhet(json.getAdministrativEnhet());
    }

    if (json.getSaksbehandler() != null) {
      korrespondansepart.setSaksbehandler(json.getSaksbehandler());
    }

    if (json.getEpostadresse() != null) {
      korrespondansepart.setEpostadresse(json.getEpostadresse());
    }

    if (json.getPostnummer() != null) {
      korrespondansepart.setPostnummer(json.getPostnummer());
    }

    return korrespondansepart;
  }


  /**
   * 
   * @param korrespondansepart
   * @param depth
   * @return
   */
  public KorrespondansepartJSON toJSON(Korrespondansepart korrespondansepart,
      Set<String> expandPaths, String currentPath) {
    return toJSON(korrespondansepart, new KorrespondansepartJSON(), expandPaths, currentPath);
  }

  public KorrespondansepartJSON toJSON(Korrespondansepart korrespondansepart,
      KorrespondansepartJSON json, Set<String> expandPaths, String currentPath) {
    einnsynObjectService.toJSON(korrespondansepart, json, expandPaths, currentPath);

    if (korrespondansepart.getKorrespondanseparttype() != null) {
      json.setKorrespondansepartType(korrespondansepart.getKorrespondanseparttype());
    }

    if (korrespondansepart.getKorrespondansepartNavn() != null) {
      json.setNavn(korrespondansepart.getKorrespondansepartNavn());
    }

    if (korrespondansepart.getKorrespondansepartNavnSensitiv() != null) {
      json.setNavnSensitiv(korrespondansepart.getKorrespondansepartNavnSensitiv());
    }

    if (korrespondansepart.getAdministrativEnhet() != null) {
      json.setAdministrativEnhet(korrespondansepart.getAdministrativEnhet());
    }

    if (korrespondansepart.getSaksbehandler() != null) {
      json.setSaksbehandler(korrespondansepart.getSaksbehandler());
    }

    if (korrespondansepart.getEpostadresse() != null) {
      json.setEpostadresse(korrespondansepart.getEpostadresse());
    }

    if (korrespondansepart.getPostnummer() != null) {
      json.setPostnummer(korrespondansepart.getPostnummer());
    }

    return json;
  }


  public ExpandableField<KorrespondansepartJSON> maybeExpand(Korrespondansepart korrpart,
      String propertyName, Set<String> expandPaths, String currentPath) {
    if (expandPaths.contains(currentPath)) {
      return new ExpandableField<KorrespondansepartJSON>(korrpart.getId(), this.toJSON(korrpart,
          expandPaths, currentPath == "" ? propertyName : currentPath + "." + propertyName));
    } else {
      return new ExpandableField<KorrespondansepartJSON>(korrpart.getId(), null);
    }
  }

}

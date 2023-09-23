package no.einnsyn.apiv3.entities.korrespondansepart;

import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.korrespondansepart.models.Korrespondansepart;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartJSON;

@Service
public class KorrespondansepartService
    extends EinnsynObjectService<Korrespondansepart, KorrespondansepartJSON> {

  private final KorrespondansepartRepository korrespondansepartRepository;

  public KorrespondansepartService(KorrespondansepartRepository korrespondansepartRepository) {
    this.korrespondansepartRepository = korrespondansepartRepository;
  }


  /**
   * Convert a JSON object to a Korrespondansepart
   * 
   * @param json
   * @param paths A list of paths containing new objects that will be created from this update
   * @param currentPath The current path in the object tree
   * @return
   */
  public Korrespondansepart fromJSON(KorrespondansepartJSON json, Set<String> paths,
      String currentPath) {
    return fromJSON(json, new Korrespondansepart(), paths, currentPath);
  }


  /**
   * Update a Korrespondansepart from a JSON object, persist/index it to all relevant databases. If
   * no ID is given, a new Korrespondansepart will be created.
   * 
   * @param id
   * @param json
   * @return
   */
  @Transactional
  public KorrespondansepartJSON update(String id, KorrespondansepartJSON json) {
    Korrespondansepart korrpart = null;

    // If ID is given, get the existing saksmappe from DB
    if (id != null) {
      korrpart = korrespondansepartRepository.findById(id);
      if (korrpart == null) {
        throw new Error("Dokumentbeskrivelse not found");
      }
    } else {
      korrpart = new Korrespondansepart();
    }

    // Generate database object from JSON
    Set<String> paths = new HashSet<String>();
    korrpart = fromJSON(json, korrpart, paths, "");
    korrespondansepartRepository.saveAndFlush(korrpart);

    // Generate JSON containing all inserted objects
    KorrespondansepartJSON responseJSON = this.toJSON(korrpart, paths, "");

    return responseJSON;
  }

  /**
   * Convert a JSON object to a Korrespondansepart
   * 
   * @param json
   * @param korrespondansepart
   * @param paths A list of paths containing new objects that will be created from this update
   * @param currentPath The current path in the object tree
   * @return
   */
  public Korrespondansepart fromJSON(KorrespondansepartJSON json,
      Korrespondansepart korrespondansepart, Set<String> paths, String currentPath) {
    super.fromJSON(json, korrespondansepart, paths, currentPath);

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

    if (json.getErBehandlingsansvarlig() != null) {
      korrespondansepart.setErBehandlingsansvarlig(json.getErBehandlingsansvarlig());
    }

    return korrespondansepart;
  }


  /**
   * Convert a Korrespondansepart to a JSON object
   * 
   * @param korrespondansepart
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return
   */
  public KorrespondansepartJSON toJSON(Korrespondansepart korrespondansepart,
      Set<String> expandPaths, String currentPath) {
    return toJSON(korrespondansepart, new KorrespondansepartJSON(), expandPaths, currentPath);
  }

  public KorrespondansepartJSON toJSON(Korrespondansepart korrespondansepart,
      KorrespondansepartJSON json, Set<String> expandPaths, String currentPath) {
    super.toJSON(korrespondansepart, json, expandPaths, currentPath);

    json.setKorrespondansepartType(korrespondansepart.getKorrespondanseparttype());
    json.setNavn(korrespondansepart.getKorrespondansepartNavn());
    json.setNavnSensitiv(korrespondansepart.getKorrespondansepartNavnSensitiv());
    json.setAdministrativEnhet(korrespondansepart.getAdministrativEnhet());
    json.setSaksbehandler(korrespondansepart.getSaksbehandler());
    json.setEpostadresse(korrespondansepart.getEpostadresse());
    json.setPostnummer(korrespondansepart.getPostnummer());
    json.setErBehandlingsansvarlig(korrespondansepart.getErBehandlingsansvarlig());

    return json;
  }


  /**
   * Creates an ExpandableField object. If propertyName is in the expandPaths list, the object will
   * be expanded, if not, it will only contain the ID.
   * 
   * @param korrpart
   * @param propertyName Name of the property to expand, appended to currentPath for deeper steps
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return
   */
  public ExpandableField<KorrespondansepartJSON> maybeExpand(Korrespondansepart korrpart,
      String propertyName, Set<String> expandPaths, String currentPath) {
    String updatedPath = currentPath == "" ? propertyName : currentPath + "." + propertyName;
    if (expandPaths.contains(updatedPath)) {
      return new ExpandableField<KorrespondansepartJSON>(korrpart.getId(),
          this.toJSON(korrpart, expandPaths, updatedPath));
    } else {
      return new ExpandableField<KorrespondansepartJSON>(korrpart.getId(), null);
    }
  }

}

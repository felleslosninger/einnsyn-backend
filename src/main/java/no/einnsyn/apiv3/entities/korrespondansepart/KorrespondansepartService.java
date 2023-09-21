package no.einnsyn.apiv3.entities.korrespondansepart;

import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import no.einnsyn.apiv3.entities.IEinnsynEntityService;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.korrespondansepart.models.Korrespondansepart;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartJSON;

@Service
public class KorrespondansepartService
    implements IEinnsynEntityService<Korrespondansepart, KorrespondansepartJSON> {

  private final EinnsynObjectService einnsynObjectService;
  private final KorrespondansepartRepository korrespondansepartRepository;

  public KorrespondansepartService(EinnsynObjectService eInnsynObjectService,
      KorrespondansepartRepository korrespondansepartRepository) {
    this.einnsynObjectService = eInnsynObjectService;
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
    korrespondansepartRepository.save(korrpart);

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

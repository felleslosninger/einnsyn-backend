package no.einnsyn.apiv3.entities.dokumentobjekt;

import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.DokumentbeskrivelseRepository;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseJSON;
import no.einnsyn.apiv3.entities.dokumentobjekt.models.Dokumentobjekt;
import no.einnsyn.apiv3.entities.dokumentobjekt.models.DokumentobjektJSON;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;

@Service
public class DokumentobjektService
    extends EinnsynObjectService<Dokumentobjekt, DokumentobjektJSON> {

  private final DokumentbeskrivelseRepository dokumentbeskrivelseRepository;
  private final DokumentobjektRepository dokumentobjektRepository;

  public DokumentobjektService(DokumentbeskrivelseRepository dokumentbeskrivelseRepository,
      DokumentobjektRepository dokumentobjektRepository) {
    this.dokumentbeskrivelseRepository = dokumentbeskrivelseRepository;
    this.dokumentobjektRepository = dokumentobjektRepository;
  }


  /**
   * Update a Dokumentbeskrivelse from a JSON object, persist/index it to all relevant databases. If
   * no ID is given, a new Dokumentbeskrivelse will be created.
   * 
   * @param id
   * @param json
   * @return
   */
  @Transactional
  public DokumentobjektJSON update(String id, DokumentobjektJSON json) {
    Dokumentobjekt dokobj = null;

    // If ID is given, get the existing saksmappe from DB
    if (id != null) {
      dokobj = dokumentobjektRepository.findById(id);
      if (dokobj == null) {
        throw new Error("Dokumentobjekt not found");
      }
    } else {
      dokobj = new Dokumentobjekt();
    }

    // Generate database object from JSON
    Set<String> paths = new HashSet<String>();
    dokobj = fromJSON(json, dokobj, paths, "");
    dokumentobjektRepository.saveAndFlush(dokobj);

    // Generate JSON containing all inserted objects
    DokumentobjektJSON responseJSON = this.toJSON(dokobj, paths, "");

    return responseJSON;
  }


  /**
   * Convert a JSON object to a Dokumentobjekt
   * 
   * @param json
   * @param paths A list of paths containing new objects that will be created from this update
   * @param currentPath The current path in the object tree
   * @return
   */
  public Dokumentobjekt fromJSON(DokumentobjektJSON json, Set<String> paths, String currentPath) {
    return fromJSON(json, new Dokumentobjekt(), paths, currentPath);
  }

  /**
   * Convert a JSON object to a Dokumentobjekt
   * 
   * @param json
   * @param dokumentobjekt
   * @param paths A list of paths containing new objects that will be created from this update
   * @param currentPath The current path in the object tree
   * @return
   */
  public Dokumentobjekt fromJSON(DokumentobjektJSON json, Dokumentobjekt dokumentobjekt,
      Set<String> paths, String currentPath) {
    super.fromJSON(json, dokumentobjekt, paths, currentPath);

    if (json.getSystemId() != null) {
      dokumentobjekt.setSystemId(json.getSystemId());
    }

    if (json.getReferanseDokumentfil() != null) {
      dokumentobjekt.setReferanseDokumentfil(json.getReferanseDokumentfil());
    }

    if (json.getDokumentFormat() != null) {
      dokumentobjekt.setDokumentFormat(json.getDokumentFormat());
    }

    if (json.getSjekksum() != null) {
      dokumentobjekt.setSjekksum(json.getSjekksum());
    }

    if (json.getSjekksumalgoritme() != null) {
      dokumentobjekt.setSjekksumalgoritme(json.getSjekksumalgoritme());
    }


    ExpandableField<DokumentbeskrivelseJSON> dokumentbeskrivelseField =
        json.getDokumentbeskrivelse();
    if (dokumentbeskrivelseField != null) {
      Dokumentbeskrivelse dokumentbeskrivelse =
          dokumentbeskrivelseRepository.findById(dokumentbeskrivelseField.getId());
      if (dokumentbeskrivelse != null) {
        dokumentobjekt.setDokumentbeskrivelse(dokumentbeskrivelse);
      }
    }


    return dokumentobjekt;
  }


  /**
   * Convert a Dokumentobjekt to a JSON object
   * 
   * @param dokumentobjekt
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return
   */
  public DokumentobjektJSON toJSON(Dokumentobjekt dokumentobjekt, Set<String> expandPaths,
      String currentPath) {
    return toJSON(dokumentobjekt, new DokumentobjektJSON(), expandPaths, currentPath);
  }

  /**
   * Convert a Dokumentobjekt to a JSON object
   * 
   * @param dokumentobjekt
   * @param json
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return
   */
  public DokumentobjektJSON toJSON(Dokumentobjekt dokumentobjekt, DokumentobjektJSON json,
      Set<String> expandPaths, String currentPath) {
    super.toJSON(dokumentobjekt, json, expandPaths, currentPath);

    json.setSystemId(dokumentobjekt.getSystemId());
    json.setReferanseDokumentfil(dokumentobjekt.getReferanseDokumentfil());
    json.setDokumentFormat(dokumentobjekt.getDokumentFormat());
    json.setSjekksum(dokumentobjekt.getSjekksum());
    json.setSjekksumalgoritme(dokumentobjekt.getSjekksumalgoritme());

    return json;
  }


  /**
   * Creates an ExpandableField object. If propertyName is in the expandPaths list, the object will
   * be expanded, if not, it will only contain the ID.
   * 
   * @param dokumentobjekt
   * @param propertyName Name of the property to expand, appended to currentPath for deeper steps
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return
   */
  public ExpandableField<DokumentobjektJSON> maybeExpand(Dokumentobjekt dokobj, String propertyName,
      Set<String> expandPaths, String currentPath) {
    String updatedPath = currentPath == "" ? propertyName : currentPath + "." + propertyName;
    if (expandPaths.contains(updatedPath)) {
      return new ExpandableField<DokumentobjektJSON>(dokobj.getId(),
          this.toJSON(dokobj, expandPaths, updatedPath));
    } else {
      return new ExpandableField<DokumentobjektJSON>(dokobj.getId(), null);
    }
  }
}

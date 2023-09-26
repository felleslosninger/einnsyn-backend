package no.einnsyn.apiv3.entities.dokumentobjekt;

import java.util.Set;
import org.springframework.stereotype.Service;
import lombok.Getter;
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

  @Getter
  private final DokumentobjektRepository repository;

  public DokumentobjektService(DokumentbeskrivelseRepository dokumentbeskrivelseRepository,
      DokumentobjektRepository dokumentobjektRepository) {
    this.dokumentbeskrivelseRepository = dokumentbeskrivelseRepository;
    this.repository = dokumentobjektRepository;
  }


  public Dokumentobjekt newObject() {
    return new Dokumentobjekt();
  }


  public DokumentobjektJSON newJSON() {
    return new DokumentobjektJSON();
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
   * Delete a Dokumentobjekt
   * 
   * @param id
   * @return
   */
  public DokumentobjektJSON delete(String id) {
    // This ID should be verified in the controller, so it should always exist.
    Dokumentobjekt dokobj = repository.findById(id);
    DokumentobjektJSON dokobjJSON = toJSON(dokobj);
    // dokobjJSON.setDeleted(true);

    // Delete
    repository.deleteById(id);

    return dokobjJSON;
  }

}

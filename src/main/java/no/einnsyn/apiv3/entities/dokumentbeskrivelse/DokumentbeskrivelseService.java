package no.einnsyn.apiv3.entities.dokumentbeskrivelse;

import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import lombok.Getter;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseJSON;
import no.einnsyn.apiv3.entities.dokumentobjekt.DokumentobjektRepository;
import no.einnsyn.apiv3.entities.dokumentobjekt.DokumentobjektService;
import no.einnsyn.apiv3.entities.dokumentobjekt.models.Dokumentobjekt;
import no.einnsyn.apiv3.entities.dokumentobjekt.models.DokumentobjektJSON;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;

@Service
public class DokumentbeskrivelseService
    extends EinnsynObjectService<Dokumentbeskrivelse, DokumentbeskrivelseJSON> {

  private final DokumentobjektRepository dokumentobjektRepository;
  private final DokumentobjektService dokumentobjektService;

  @Getter
  private final DokumentbeskrivelseRepository repository;

  public DokumentbeskrivelseService(DokumentobjektRepository dokumentobjektRepository,
      DokumentobjektService dokumentobjektService,
      DokumentbeskrivelseRepository dokumentbeskrivelseRepository) {
    this.dokumentobjektRepository = dokumentobjektRepository;
    this.dokumentobjektService = dokumentobjektService;
    this.repository = dokumentbeskrivelseRepository;
  }

  public Dokumentbeskrivelse newObject() {
    return new Dokumentbeskrivelse();
  }

  public DokumentbeskrivelseJSON newJSON() {
    return new DokumentbeskrivelseJSON();
  }


  /**
   * Convert a JSON object to a Dokumentbeskrivelse
   * 
   * @param json
   * @param dokbesk
   * @param paths A list of paths containing new objects that will be created from this update
   * @param currentPath The current path in the object tree
   * @return
   */
  public Dokumentbeskrivelse fromJSON(DokumentbeskrivelseJSON json, Dokumentbeskrivelse dokbesk,
      Set<String> paths, String currentPath) {
    super.fromJSON(json, dokbesk, paths, currentPath);

    if (json.getSystemId() != null) {
      dokbesk.setSystemId(json.getSystemId());
    }

    if (json.getDokumentnummer() != null) {
      dokbesk.setDokumentnummer(json.getDokumentnummer());
    }

    if (json.getTilknyttetRegistreringSom() != null) {
      dokbesk.setTilknyttetRegistreringSom(json.getTilknyttetRegistreringSom());
    }

    if (json.getDokumenttype() != null) {
      dokbesk.setDokumenttype(json.getDokumenttype());
    }

    if (json.getTittel() != null) {
      dokbesk.setTittel(json.getTittel());
    }

    if (json.getTittelSensitiv() != null) {
      dokbesk.setTittel_SENSITIV(json.getTittelSensitiv());
    }

    // Dokumentobjekt
    List<ExpandableField<DokumentobjektJSON>> dokobjFieldList = json.getDokumentobjekt();
    if (dokobjFieldList != null) {
      dokobjFieldList.forEach((dokobjField) -> {
        Dokumentobjekt dokobj = null;
        if (dokobjField.getId() != null) {
          dokobj = dokumentobjektRepository.findById(dokobjField.getId());
        } else {
          String dokobjPath =
              currentPath == "" ? "dokumentobjekt" : currentPath + ".dokumentobjekt";
          paths.add(dokobjPath);
          dokobj =
              dokumentobjektService.fromJSON(dokobjField.getExpandedObject(), paths, dokobjPath);
        }
        dokbesk.addDokumentobjekt(dokobj);
      });
    }

    return dokbesk;
  }


  /**
   * Convert a Dokumentbeskrivelse to a JSON object
   * 
   * @param dokbesk
   * @param json
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return
   */
  public DokumentbeskrivelseJSON toJSON(Dokumentbeskrivelse dokbesk, DokumentbeskrivelseJSON json,
      Set<String> expandPaths, String currentPath) {
    super.toJSON(dokbesk, json, expandPaths, currentPath);

    json.setSystemId(dokbesk.getSystemId());
    json.setDokumentnummer(dokbesk.getDokumentnummer());
    json.setTilknyttetRegistreringSom(dokbesk.getTilknyttetRegistreringSom());
    json.setDokumenttype(dokbesk.getDokumenttype());
    json.setTittel(dokbesk.getTittel());
    json.setTittelSensitiv(dokbesk.getTittel_SENSITIV());

    // Dokumentobjekt
    List<Dokumentobjekt> dokobjList = dokbesk.getDokumentobjekt();
    List<ExpandableField<DokumentobjektJSON>> dokobjJSONList = json.getDokumentobjekt();
    for (Dokumentobjekt dokobj : dokobjList) {
      dokobjJSONList.add(
          dokumentobjektService.maybeExpand(dokobj, "dokumentobjekt", expandPaths, currentPath));
    }

    return json;
  }

}

package no.einnsyn.apiv3.entities.dokumentbeskrivelse;

import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseJSON;
import no.einnsyn.apiv3.entities.dokumentobjekt.DokumentobjektRepository;
import no.einnsyn.apiv3.entities.dokumentobjekt.DokumentobjektService;
import no.einnsyn.apiv3.entities.dokumentobjekt.models.Dokumentobjekt;
import no.einnsyn.apiv3.entities.dokumentobjekt.models.DokumentobjektJSON;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.enhet.EnhetRepository;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.enhet.models.EnhetJSON;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;

@Service
public class DokumentbeskrivelseService {

  private final EinnsynObjectService einnsynObjectService;
  private final EnhetRepository enhetRepository;
  private final DokumentobjektRepository dokumentobjektRepository;
  private final DokumentobjektService dokumentobjektService;

  public DokumentbeskrivelseService(EinnsynObjectService eInnsynObjectService,
      EnhetRepository enhetRepository, DokumentobjektRepository dokumentobjektRepository,
      DokumentobjektService dokumentobjektService) {
    this.einnsynObjectService = eInnsynObjectService;
    this.enhetRepository = enhetRepository;
    this.dokumentobjektRepository = dokumentobjektRepository;
    this.dokumentobjektService = dokumentobjektService;
  }


  /**
   * 
   * @param json
   * @return
   */
  public Dokumentbeskrivelse fromJSON(DokumentbeskrivelseJSON json, Set<String> paths,
      String currentPath) {
    return fromJSON(json, new Dokumentbeskrivelse(), paths, currentPath);
  }

  public Dokumentbeskrivelse fromJSON(DokumentbeskrivelseJSON json, Dokumentbeskrivelse dokbesk,
      Set<String> paths, String currentPath) {
    einnsynObjectService.fromJSON(json, dokbesk, paths, currentPath);

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

    // Virksomhet
    ExpandableField<EnhetJSON> virksomhetField = json.getVirksomhet();
    if (virksomhetField != null) {
      Enhet virksomhet = enhetRepository.findById(virksomhetField.getId());
      if (virksomhet != null) {
        dokbesk.setVirksomhet(virksomhet);
      }
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
   * 
   * @param dokbesk
   * @param depth
   * @return
   */
  public DokumentbeskrivelseJSON toJSON(Dokumentbeskrivelse dokbesk, Set<String> expandPaths,
      String currentPath) {
    return toJSON(dokbesk, new DokumentbeskrivelseJSON(), expandPaths, currentPath);
  }

  public DokumentbeskrivelseJSON toJSON(Dokumentbeskrivelse dokbesk, DokumentbeskrivelseJSON json,
      Set<String> expandPaths, String currentPath) {
    einnsynObjectService.toJSON(dokbesk, json, expandPaths, currentPath);

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


  public ExpandableField<DokumentbeskrivelseJSON> maybeExpand(Dokumentbeskrivelse dokbesk,
      String propertyName, Set<String> expandPaths, String currentPath) {
    if (expandPaths.contains(currentPath)) {
      return new ExpandableField<DokumentbeskrivelseJSON>(dokbesk.getId(), this.toJSON(dokbesk,
          expandPaths, currentPath == "" ? propertyName : currentPath + "." + propertyName));
    } else {
      return new ExpandableField<DokumentbeskrivelseJSON>(dokbesk.getId(), null);
    }
  }
}

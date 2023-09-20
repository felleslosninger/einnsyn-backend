package no.einnsyn.apiv3.entities.dokumentobjekt;

import java.util.Set;
import org.springframework.stereotype.Service;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.DokumentbeskrivelseRepository;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseJSON;
import no.einnsyn.apiv3.entities.dokumentobjekt.models.Dokumentobjekt;
import no.einnsyn.apiv3.entities.dokumentobjekt.models.DokumentobjektJSON;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;

@Service
public class DokumentobjektService {

  private final EinnsynObjectService einnsynObjectService;
  private final DokumentbeskrivelseRepository dokumentbeskrivelseRepository;

  public DokumentobjektService(EinnsynObjectService einnsynObjectService,
      DokumentbeskrivelseRepository dokumentbeskrivelseRepository) {
    this.einnsynObjectService = einnsynObjectService;
    this.dokumentbeskrivelseRepository = dokumentbeskrivelseRepository;
  }

  /**
   * 
   * @param json
   * @return
   */
  public Dokumentobjekt fromJSON(DokumentobjektJSON json, Set<String> paths, String currentPath) {
    return fromJSON(json, new Dokumentobjekt(), paths, currentPath);
  }

  public Dokumentobjekt fromJSON(DokumentobjektJSON json, Dokumentobjekt dokumentobjekt,
      Set<String> paths, String currentPath) {
    einnsynObjectService.fromJSON(json, dokumentobjekt, paths, currentPath);

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
   * 
   * @param dokumentobjekt
   * @param expandPaths
   * @param currentPath
   * @return
   */
  public DokumentobjektJSON toJSON(Dokumentobjekt dokumentobjekt, Set<String> expandPaths,
      String currentPath) {
    return toJSON(dokumentobjekt, new DokumentobjektJSON(), expandPaths, currentPath);
  }

  public DokumentobjektJSON toJSON(Dokumentobjekt dokumentobjekt, DokumentobjektJSON json,
      Set<String> expandPaths, String currentPath) {
    einnsynObjectService.toJSON(dokumentobjekt, json, expandPaths, currentPath);

    json.setSystemId(dokumentobjekt.getSystemId());
    json.setReferanseDokumentfil(dokumentobjekt.getReferanseDokumentfil());
    json.setDokumentFormat(dokumentobjekt.getDokumentFormat());
    json.setSjekksum(dokumentobjekt.getSjekksum());
    json.setSjekksumalgoritme(dokumentobjekt.getSjekksumalgoritme());

    return json;
  }


  public ExpandableField<DokumentobjektJSON> maybeExpand(Dokumentobjekt dokobj, String propertyName,
      Set<String> expandPaths, String currentPath) {
    if (expandPaths.contains(currentPath)) {
      return new ExpandableField<DokumentobjektJSON>(dokobj.getId(), this.toJSON(dokobj,
          expandPaths, currentPath == "" ? propertyName : currentPath + "." + propertyName));
    } else {
      return new ExpandableField<DokumentobjektJSON>(dokobj.getId(), null);
    }
  }
}

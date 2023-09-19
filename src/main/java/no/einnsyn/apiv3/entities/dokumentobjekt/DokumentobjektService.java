package no.einnsyn.apiv3.entities.dokumentobjekt;

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
  public Dokumentobjekt fromJSON(DokumentobjektJSON json) {
    return fromJSON(new Dokumentobjekt(), json);
  }

  public Dokumentobjekt fromJSON(Dokumentobjekt dokumentobjekt, DokumentobjektJSON json) {
    einnsynObjectService.fromJSON(dokumentobjekt, json);

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


  public DokumentobjektJSON toJSON(Dokumentobjekt dokumentobjekt, Integer depth) {
    return toJSON(new DokumentobjektJSON(), dokumentobjekt, depth);
  }

  public DokumentobjektJSON toJSON(DokumentobjektJSON json, Dokumentobjekt dokumentobjekt,
      Integer depth) {
    einnsynObjectService.toJSON(json, dokumentobjekt, depth);

    json.setSystemId(dokumentobjekt.getSystemId());
    json.setReferanseDokumentfil(dokumentobjekt.getReferanseDokumentfil());
    json.setDokumentFormat(dokumentobjekt.getDokumentFormat());
    json.setSjekksum(dokumentobjekt.getSjekksum());
    json.setSjekksumalgoritme(dokumentobjekt.getSjekksumalgoritme());

    return json;
  }
}

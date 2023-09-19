package no.einnsyn.apiv3.entities.dokumentbeskrivelse;

import org.springframework.stereotype.Service;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseJSON;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.enhet.EnhetRepository;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.enhet.models.EnhetJSON;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;

@Service
public class DokumentbeskrivelseService {

  private final EinnsynObjectService einnsynObjectService;
  private final EnhetRepository enhetRepository;

  public DokumentbeskrivelseService(EinnsynObjectService eInnsynObjectService,
      EnhetRepository enhetRepository) {
    this.einnsynObjectService = eInnsynObjectService;
    this.enhetRepository = enhetRepository;
  }


  /**
   * 
   * @param json
   * @return
   */
  public Dokumentbeskrivelse fromJSON(DokumentbeskrivelseJSON json) {
    return fromJSON(new Dokumentbeskrivelse(), json);
  }

  public Dokumentbeskrivelse fromJSON(Dokumentbeskrivelse dokbesk, DokumentbeskrivelseJSON json) {
    einnsynObjectService.fromJSON(dokbesk, json);

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

    ExpandableField<EnhetJSON> virksomhetField = json.getVirksomhet();
    if (virksomhetField != null) {
      System.out.println(virksomhetField.getId());
      Enhet virksomhet = enhetRepository.findById(virksomhetField.getId());
      if (virksomhet != null) {
        dokbesk.setVirksomhet(virksomhet);
      }
    }

    return dokbesk;
  }


  /**
   * 
   * @param dokbesk
   * @param depth
   * @return
   */
  public DokumentbeskrivelseJSON toJSON(Dokumentbeskrivelse dokbesk, Integer depth) {
    return toJSON(dokbesk, new DokumentbeskrivelseJSON(), depth);
  }

  public DokumentbeskrivelseJSON toJSON(Dokumentbeskrivelse dokbesk, DokumentbeskrivelseJSON json,
      Integer depth) {
    einnsynObjectService.toJSON(dokbesk, json, depth);

    json.setSystemId(dokbesk.getSystemId());
    json.setDokumentnummer(dokbesk.getDokumentnummer());
    json.setTilknyttetRegistreringSom(dokbesk.getTilknyttetRegistreringSom());
    json.setDokumenttype(dokbesk.getDokumenttype());
    json.setTittel(dokbesk.getTittel());
    json.setTittelSensitiv(dokbesk.getTittel_SENSITIV());

    return json;
  }
}

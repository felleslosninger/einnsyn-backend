package no.einnsyn.apiv3.entities.journalpost;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.DokumentbeskrivelseRepository;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.DokumentbeskrivelseService;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseJSON;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostJSON;
import no.einnsyn.apiv3.entities.korrespondansepart.KorrespondansepartRepository;
import no.einnsyn.apiv3.entities.korrespondansepart.KorrespondansepartService;
import no.einnsyn.apiv3.entities.korrespondansepart.models.Korrespondansepart;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartJSON;
import no.einnsyn.apiv3.entities.registrering.RegistreringService;
import no.einnsyn.apiv3.entities.saksmappe.SaksmappeRepository;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeJSON;
import no.einnsyn.apiv3.entities.skjerming.SkjermingRepository;
import no.einnsyn.apiv3.entities.skjerming.SkjermingService;
import no.einnsyn.apiv3.entities.skjerming.models.Skjerming;
import no.einnsyn.apiv3.entities.skjerming.models.SkjermingJSON;

@Service
public class JournalpostService {

  private final RegistreringService registreringService;
  private final SaksmappeRepository saksmappeRepository;
  private final JournalpostRepository journalpostRepository;
  private final SkjermingRepository skjermingRepository;
  private final SkjermingService skjermingService;
  private final KorrespondansepartRepository korrespondansepartRepository;
  private final KorrespondansepartService korrespondansepartService;
  private final DokumentbeskrivelseRepository dokumentbeskrivelseRepository;
  private final DokumentbeskrivelseService dokumentbeskrivelseService;


  JournalpostService(RegistreringService registreringService,
      SaksmappeRepository saksmappeRepository, JournalpostRepository journalpostRepository,
      SkjermingRepository skjermingRepository, SkjermingService skjermingService,
      KorrespondansepartRepository korrespondansepartRepository,
      KorrespondansepartService korrespondansepartService,
      DokumentbeskrivelseRepository dokumentbeskrivelseRepository,
      DokumentbeskrivelseService dokumentbeskrivelseService) {
    this.registreringService = registreringService;
    this.saksmappeRepository = saksmappeRepository;
    this.journalpostRepository = journalpostRepository;
    this.skjermingRepository = skjermingRepository;
    this.skjermingService = skjermingService;
    this.korrespondansepartRepository = korrespondansepartRepository;
    this.korrespondansepartService = korrespondansepartService;
    this.dokumentbeskrivelseRepository = dokumentbeskrivelseRepository;
    this.dokumentbeskrivelseService = dokumentbeskrivelseService;
  }


  @Transactional
  public Journalpost updateJournalpost(String id, JournalpostJSON journalpostJSON) {
    Journalpost journalpost = null;

    if (id == null && journalpostJSON == null) {
      throw new Error("ID and JSON object missing");
    }

    // If ID is given, check that it matches the ID in the JSON object
    if (id != null && journalpostJSON != null && journalpostJSON.getId() != null
        && !id.equals(journalpostJSON.getId())) {
      throw new Error("ID mismatch");
    }

    // If ID is given, get the existing journalpost from DB
    if (id != null) {
      journalpost = journalpostRepository.findById(id).orElse(null);
      if (journalpost == null) {
        throw new Error("Journalpost not found");
      }
    } else {
      journalpost = new Journalpost();
    }

    fromJSON(journalpost, journalpostJSON);
    journalpostRepository.save(journalpost);

    // Generate and save ES document

    return journalpost;
  }


  /**
   * Create a Journalpost from a JSON object. This will recursively also create children elements,
   * if they are given in the JSON object.
   * 
   * @param json
   * @return
   */
  public Journalpost fromJSON(JournalpostJSON json) {
    return fromJSON(new Journalpost(), json);
  }

  public Journalpost fromJSON(Journalpost journalpost, JournalpostJSON json) {
    registreringService.fromJSON(journalpost, json);

    if (json.getJournalaar() != null) {
      journalpost.setJournalaar(json.getJournalaar());
    }

    if (json.getJournalsekvensnummer() != null) {
      journalpost.setJournalsekvensnummer(json.getJournalsekvensnummer());
    }

    if (json.getJournalpostnummer() != null) {
      journalpost.setJournalpostnummer(json.getJournalpostnummer());
    }

    if (json.getJournalposttype() != null) {
      journalpost.setJournalposttype(json.getJournalposttype());
    }

    if (json.getJournaldato() != null) {
      journalpost.setJournaldato(json.getJournaldato());
    }

    if (json.getDokumentdato() != null) {
      journalpost.setDokumentdato(json.getDokumentdato());
    }

    if (json.getSorteringstype() != null) {
      journalpost.setSorteringstype(json.getSorteringstype());
    }

    // ExpandableField<Virksomhet> journalenhetField = json.getJournalenhet();
    // if (journalenhetField != null) {
    // Virksomhet journalenhet = virksomhetService.updateVirksomhet(virksomhetField.getId(),
    // virksomhetField.getExpandedObject());
    // }


    // Update saksmappe
    ExpandableField<SaksmappeJSON> saksmappeField = json.getSaksmappe();
    if (saksmappeField != null) {
      Saksmappe saksmappe = saksmappeRepository.findById(saksmappeField.getId()).orElse(null);
      if (saksmappe != null) {
        journalpost.setSaksmappe(saksmappe);
      }
    }

    // Update skjerming
    ExpandableField<SkjermingJSON> skjermingField = json.getSkjerming();
    if (skjermingField != null) {
      Skjerming skjerming;
      if (skjermingField.getId() != null) {
        skjerming = skjermingRepository.findById(skjermingField.getId());
      } else {
        skjerming = skjermingService.fromJSON(skjermingField.getExpandedObject());
      }
      journalpost.setSkjerming(skjerming);
    }

    // Update korrespondansepart
    List<ExpandableField<KorrespondansepartJSON>> korrpartFieldList = json.getKorrespondansepart();
    korrpartFieldList.forEach((journalpostField) -> {
      Korrespondansepart korrpart = null;
      if (journalpostField.getId() != null) {
        korrpart = korrespondansepartRepository.findById(journalpostField.getId());
      } else {
        korrpart = korrespondansepartService.fromJSON(journalpostField.getExpandedObject());
      }
      journalpost.addKorrespondansepart(korrpart);
    });

    // Update dokumentbeskrivelse
    List<ExpandableField<DokumentbeskrivelseJSON>> dokbeskFieldList = json.getDokumentbeskrivelse();
    dokbeskFieldList.forEach((dokbeskField) -> {
      Dokumentbeskrivelse dokbesk = null;
      if (dokbeskField.getId() != null) {
        dokbesk = dokumentbeskrivelseRepository.findById(dokbeskField.getId());
      } else {
        dokbesk = dokumentbeskrivelseService.fromJSON(dokbeskField.getExpandedObject());
      }
      journalpost.getDokumentbeskrivelse().add(dokbesk);
    });


    return journalpost;
  }


  /**
   * Convert a Journalpost to a JSON object.
   * 
   * @param journalpost
   * @param depth Number of levels to expand ExpandableFields
   * @return
   */
  public JournalpostJSON toJSON(Journalpost journalpost, Integer depth) {
    return toJSON(new JournalpostJSON(), journalpost, depth);
  }

  public JournalpostJSON toJSON(JournalpostJSON json, Journalpost journalpost, Integer depth) {
    registreringService.toJSON(json, journalpost, depth);
    json.setJournalaar(journalpost.getJournalaar());
    json.setJournalsekvensnummer(journalpost.getJournalsekvensnummer());
    json.setJournalpostnummer(journalpost.getJournalpostnummer());
    json.setJournalposttype(journalpost.getJournalposttype());
    json.setJournaldato(journalpost.getJournaldato());
    json.setDokumentdato(journalpost.getDokumentdato());
    json.setSorteringstype(journalpost.getSorteringstype());

    // Skjerming
    Skjerming skjerming = journalpost.getSkjerming();
    if (skjerming != null) {
      SkjermingJSON skjermingJSON = skjermingService.toJSON(skjerming, depth);
      json.setSkjerming(new ExpandableField<SkjermingJSON>(skjermingJSON));
    }

    // Korrespondansepart
    List<Korrespondansepart> korrpartList = journalpost.getKorrespondansepart();
    List<ExpandableField<KorrespondansepartJSON>> korrpartJSONList = json.getKorrespondansepart();
    for (Korrespondansepart korrpart : korrpartList) {
      KorrespondansepartJSON korrpartJSON = korrespondansepartService.toJSON(korrpart, depth);
      korrpartJSONList.add(new ExpandableField<KorrespondansepartJSON>(korrpartJSON));
    }

    // Dokumentbeskrivelse
    List<Dokumentbeskrivelse> dokbeskList = journalpost.getDokumentbeskrivelse();
    List<ExpandableField<DokumentbeskrivelseJSON>> dokbeskJSONList = json.getDokumentbeskrivelse();
    for (Dokumentbeskrivelse dokbesk : dokbeskList) {
      DokumentbeskrivelseJSON dokbeskJSON = dokumentbeskrivelseService.toJSON(dokbesk, depth);
      dokbeskJSONList.add(new ExpandableField<DokumentbeskrivelseJSON>(dokbeskJSON));
    }

    // ExpandableField<Enhet> journalenhetField = journalpost.getJournalenhet();
    // json.setJournalenhet(journalpost.getJournalenhet());

    // ExpandableField<Saksmappe> saksmappeField = journalpost.getSaksmappe();
    // ...
    return json;
  }
}

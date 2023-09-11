package no.einnsyn.apiv3.entities.journalpost;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostJSON;
import no.einnsyn.apiv3.entities.registrering.RegistreringService;
import no.einnsyn.apiv3.entities.saksmappe.SaksmappeRepository;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeJSON;

@Service
public class JournalpostService {

  private final RegistreringService registreringService;
  private final SaksmappeRepository saksmappeRepository;
  private final JournalpostRepository journalpostRepository;


  JournalpostService(RegistreringService registreringService,
      SaksmappeRepository saksmappeRepository, JournalpostRepository journalpostRepository) {
    this.registreringService = registreringService;
    this.saksmappeRepository = saksmappeRepository;
    this.journalpostRepository = journalpostRepository;
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


    ExpandableField<SaksmappeJSON> saksmappeField = json.getSaksmappe();
    if (saksmappeField != null) {
      Saksmappe saksmappe = saksmappeRepository.findById(saksmappeField.getId()).orElse(null);
      if (saksmappe != null) {
        journalpost.setSaksmappe(saksmappe);
      }
    }

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
    JournalpostJSON json = new JournalpostJSON();
    return toJSON(journalpost, json, depth);
  }

  public JournalpostJSON toJSON(Journalpost journalpost, JournalpostJSON json, Integer depth) {
    registreringService.toJSON(journalpost, json, depth);
    json.setJournalaar(journalpost.getJournalaar());
    json.setJournalsekvensnummer(journalpost.getJournalsekvensnummer());
    json.setJournalpostnummer(journalpost.getJournalpostnummer());
    json.setJournalposttype(journalpost.getJournalposttype());
    json.setJournaldato(journalpost.getJournaldato());
    json.setDokumentdato(journalpost.getDokumentdato());
    json.setSorteringstype(journalpost.getSorteringstype());

    // ExpandableField<Enhet> journalenhetField = journalpost.getJournalenhet();
    // json.setJournalenhet(journalpost.getJournalenhet());

    // ExpandableField<Saksmappe> saksmappeField = journalpost.getSaksmappe();
    // ...
    return json;
  }
}

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

    // If ID is given, get the existing saksmappe from DB
    if (id != null) {
      journalpost = journalpostRepository.findById(id).orElse(null);
      if (journalpost == null) {
        throw new Error("Journalpost not found");
      }
    } else {
      journalpost = new Journalpost();
    }

    fromJSON(journalpost, journalpostJSON);

    // Generate and save ES document

    return journalpost;
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

    // ExpandableField<Virksomhet> journalenhetField = json.getJournalenhet();
    // if (journalenhetField != null) {
    // Virksomhet journalenhet = virksomhetService.updateVirksomhet(virksomhetField.getId(),
    // virksomhetField.getExpandedObject());
    // }

    if (json.getSorteringstype() != null) {
      journalpost.setSorteringstype(json.getSorteringstype());
    }


    ExpandableField<SaksmappeJSON> saksmappeField = json.getSaksmappe();
    if (saksmappeField != null) {
      Saksmappe saksmappe = saksmappeRepository.findById(saksmappeField.getId()).orElse(null);
      if (saksmappe != null) {
        journalpost.setSaksmappe(saksmappe);
      }
    }


    return journalpost;
  }
}

package no.einnsyn.apiv3.entities.saksmappe;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.journalpost.JournalpostService;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostJSON;
import no.einnsyn.apiv3.entities.mappe.MappeService;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeJSON;

@Service
public class SaksmappeService {

  private final SaksmappeRepository saksmappeRepository;
  private final JournalpostService journalpostService;
  private final MappeService mappeService;


  public SaksmappeService(SaksmappeRepository saksmappeRepository, MappeService mappeService,
      JournalpostService journalpostService) {
    this.saksmappeRepository = saksmappeRepository;
    this.mappeService = mappeService;
    this.journalpostService = journalpostService;
  }


  @Transactional
  public Saksmappe updateSaksmappe(String id, SaksmappeJSON saksmappeJSON) {
    Saksmappe saksmappe = null;

    // If ID is given, check that it matches the ID in the JSON object
    if (id != null && saksmappeJSON.getId() != null && !id.equals(saksmappeJSON.getId())) {
      throw new Error("ID mismatch");
    }

    // If ID is given, get the existing saksmappe from DB
    if (id != null) {
      saksmappe = saksmappeRepository.findById(id).orElse(null);
      if (saksmappe == null) {
        throw new Error("Saksmappe not found");
      }
    } else {
      saksmappe = new Saksmappe();
    }

    fromJSON(saksmappe, saksmappeJSON);

    // Generate and save ES document

    return saksmappe;
  }


  public Saksmappe fromJSON(Saksmappe saksmappe, SaksmappeJSON json) {
    mappeService.fromJSON(saksmappe, json);

    if (json.getSaksaar() != null) {
      saksmappe.setSaksaar(json.getSaksaar());
    }

    if (json.getSakssekvensnummer() != null) {
      saksmappe.setSakssekvensnummer(json.getSakssekvensnummer());
    }

    if (json.getSaksdato() != null) {
      saksmappe.setSaksdato(json.getSaksdato());
    }

    // TODO: Implement "virksomhet"
    // if (json.getAdministrativEnhet() != null) {
    // saksmappe.setAdministrativEnhet(json.getAdministrativEnhet());
    // }

    // Journalpost
    List<ExpandableField<JournalpostJSON>> journalposts = json.getJournalpost();
    if (journalposts != null) {
      journalposts.forEach((journalpostField) -> {
        Journalpost journalpost = journalpostService.updateJournalpost(journalpostField.getId(),
            journalpostField.getExpandedObject());
        journalpost.setSaksmappe(saksmappe);
      });
    }

    saksmappe.setEntity("saksmappe");
    saksmappeRepository.save(saksmappe);

    return saksmappe;
  }


  public SaksmappeJSON toJSON(Saksmappe saksmappe) {
    SaksmappeJSON json = new SaksmappeJSON();
    return json;
  }


  @Transactional
  public boolean deleteSaksmappe(String id, String externalId) {
    Saksmappe saksmappe = null;

    if (id != null) {
      saksmappe = saksmappeRepository.findById(id).orElse(null);
    } else if (externalId != null) {
      saksmappe = saksmappeRepository.findByExternalId(externalId).orElse(null);
    }

    if (saksmappe == null) {
      return false;
    }

    // Delete all journalposts

    // Delete saksmappe
    try {
      saksmappeRepository.deleteByExternalId(externalId);
    } catch (Error e) {
      return false;
    }
    return true;
  }
}

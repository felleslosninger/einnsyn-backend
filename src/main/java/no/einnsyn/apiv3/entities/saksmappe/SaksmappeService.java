package no.einnsyn.apiv3.entities.saksmappe;

import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.journalpost.JournalpostRepository;
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
  private final JournalpostRepository journalpostRepository;
  private final MappeService mappeService;


  public SaksmappeService(SaksmappeRepository saksmappeRepository, MappeService mappeService,
      JournalpostService journalpostService, JournalpostRepository journalpostRepository) {
    this.saksmappeRepository = saksmappeRepository;
    this.mappeService = mappeService;
    this.journalpostService = journalpostService;
    this.journalpostRepository = journalpostRepository;
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
    saksmappeRepository.save(saksmappe);

    // Generate and save ES document


    return saksmappe;
  }


  public Saksmappe fromJSON(SaksmappeJSON json) {
    return fromJSON(new Saksmappe(), json);
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

    // Saksmappe needs an ID before adding relations
    if (saksmappe.getInternalId() == null) {
      saksmappeRepository.save(saksmappe);
      System.out.println("After save: " + saksmappe.getInternalId());
    }

    // TODO: Implement "virksomhet"
    // if (json.getAdministrativEnhet() != null) {
    // saksmappe.setAdministrativEnhet(json.getAdministrativEnhet());
    // }

    // Add journalposts
    List<ExpandableField<JournalpostJSON>> journalpostFieldList = json.getJournalpost();
    journalpostFieldList.forEach((journalpostField) -> {
      Journalpost journalpost = null;
      if (journalpostField.getId() != null) {
        journalpost = journalpostRepository.findById(journalpostField.getId()).orElse(null);
      } else {
        journalpost = journalpostService.fromJSON(journalpostField.getExpandedObject());
      }
      saksmappe.addJournalpost(journalpost);
    });

    return saksmappe;
  }


  public SaksmappeJSON toJSON(Saksmappe saksmappe, Integer depth) {
    SaksmappeJSON json = new SaksmappeJSON();
    return toJSON(saksmappe, json, depth);
  }

  public SaksmappeJSON toJSON(Saksmappe saksmappe, SaksmappeJSON json, Integer depth) {
    mappeService.toJSON(saksmappe, json, depth);

    json.setSaksaar(saksmappe.getSaksaar());
    json.setSakssekvensnummer(saksmappe.getSakssekvensnummer());
    json.setSaksdato(saksmappe.getSaksdato());

    List<ExpandableField<JournalpostJSON>> journalpostsJSON = Collections.emptyList();
    List<Journalpost> journalposts = saksmappe.getJournalpost();
    System.out.println("Journalposts: " + journalposts);
    if (journalposts != null) {
      journalposts.forEach((journalpost) -> {
        System.out.println("Add journalpost: " + journalpost.getId());
        journalpostsJSON.add(new ExpandableField<JournalpostJSON>(journalpost.getId(),
            journalpostService.toJSON(journalpost, depth - 1)));
      });
    }
    json.setJournalpost(journalpostsJSON);

    return json;
  }


  @Transactional
  public void deleteSaksmappe(String id, String externalId) {
    Saksmappe saksmappe = null;

    if (id != null) {
      saksmappe = saksmappeRepository.findById(id).orElse(null);
    } else if (externalId != null) {
      saksmappe = saksmappeRepository.findByExternalId(externalId).orElse(null);
    } else {
      throw new Error("ID or external ID not given");
    }

    if (saksmappe == null) {
      throw new Error("Saksmappe not found");
    }

    // Delete all journalposts
    List<Journalpost> journalposts = saksmappe.getJournalpost();
    if (journalposts != null) {
      journalposts.forEach((journalpost) -> {
        // journalpostService.deleteJournalpost(journalpost.getId(), null);
      });
    }

    // Delete saksmappe
    saksmappeRepository.deleteByExternalId(externalId);
  }
}

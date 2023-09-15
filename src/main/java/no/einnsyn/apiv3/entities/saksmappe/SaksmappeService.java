package no.einnsyn.apiv3.entities.saksmappe;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.google.gson.Gson;
import no.einnsyn.apiv3.entities.enhet.EnhetRepository;
import no.einnsyn.apiv3.entities.enhet.EnhetService;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.enhet.models.EnhetJSON;
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
  private final EnhetRepository enhetRepository;
  private final EnhetService enhetService;
  private final MappeService mappeService;
  private final Gson gson;

  @Value("${application.elasticsearchIndex}")
  private String elasticsearchIndex;

  private final ElasticsearchOperations elasticsearchOperations;

  public SaksmappeService(SaksmappeRepository saksmappeRepository, MappeService mappeService,
      JournalpostService journalpostService, JournalpostRepository journalpostRepository,
      EnhetRepository enhetRepository, EnhetService enhetService,
      ElasticsearchOperations elasticsearchOperations, Gson gson) {
    this.saksmappeRepository = saksmappeRepository;
    this.mappeService = mappeService;
    this.journalpostService = journalpostService;
    this.journalpostRepository = journalpostRepository;
    this.enhetRepository = enhetRepository;
    this.enhetService = enhetService;
    this.elasticsearchOperations = elasticsearchOperations;
    this.gson = gson;
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

    // Generate database object from JSON
    saksmappe = fromJSON(saksmappe, saksmappeJSON);
    saksmappeRepository.save(saksmappe);

    // Add / update ElasticSearch document
    this.index(saksmappe);

    return saksmappe;
  }


  /**
   * Index the Saksmappe to ElasticSearch
   * 
   * @param saksmappe
   * @return
   */
  public String index(Saksmappe saksmappe) {
    SaksmappeJSON saksmappeES = toES(saksmappe);
    // Serialize using Gson, to get custom serialization of ExpandedFields
    String sourceString = gson.toJson(saksmappeES);
    IndexQuery indexQuery =
        new IndexQueryBuilder().withId(saksmappe.getId()).withSource(sourceString).build();
    String documentId =
        elasticsearchOperations.index(indexQuery, IndexCoordinates.of(elasticsearchIndex));

    // TODO: Update children / parent?

    return documentId;
  }


  /**
   * Convert a JSON object to Saksmappe
   * 
   * @param json
   * @return
   */
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

    // Administrativ enhet
    ExpandableField<EnhetJSON> administrativEnhetField = json.getAdministrativEnhet();
    if (administrativEnhetField != null) {
      Enhet enhet = enhetRepository.findById(administrativEnhetField.getId());
      saksmappe.setAdministrativEnhet(enhet);
    }

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


  /**
   * Convert a Saksmappe to a JSON object
   * 
   * @param saksmappe
   * @param depth
   * @return
   */
  public SaksmappeJSON toJSON(Saksmappe saksmappe, Integer depth) {
    SaksmappeJSON json = new SaksmappeJSON();
    return toJSON(saksmappe, json, depth);
  }

  public SaksmappeJSON toJSON(Saksmappe saksmappe, SaksmappeJSON json, Integer depth) {
    mappeService.toJSON(saksmappe, json, depth);

    json.setSaksaar(saksmappe.getSaksaar());
    json.setSakssekvensnummer(saksmappe.getSakssekvensnummer());
    json.setSaksdato(saksmappe.getSaksdato());

    // Administrativ enhet
    Enhet administrativEnhet = saksmappe.getAdministrativEnhet();
    if (administrativEnhet != null) {
      json.setAdministrativEnhet(new ExpandableField<EnhetJSON>(administrativEnhet.getId(),
          enhetService.toJSON(administrativEnhet, depth - 1)));
    }

    // Journalposts
    List<ExpandableField<JournalpostJSON>> journalpostsJSON =
        new ArrayList<ExpandableField<JournalpostJSON>>();
    List<Journalpost> journalposts = saksmappe.getJournalpost();
    if (journalposts != null) {
      journalposts.forEach((journalpost) -> {
        journalpostsJSON.add(new ExpandableField<JournalpostJSON>(journalpost.getId(),
            journalpostService.toJSON(journalpost, depth - 1)));
      });
    }
    json.setJournalpost(journalpostsJSON);

    return json;
  }


  /**
   * Convert a Saksmappe to an ES document
   * 
   * @param saksmappe
   * @return
   */
  public SaksmappeJSON toES(Saksmappe saksmappe) {
    return toES(new SaksmappeJSON(), saksmappe);
  }

  public SaksmappeJSON toES(SaksmappeJSON saksmappeES, Saksmappe saksmappe) {
    this.toJSON(saksmappe, saksmappeES, 1);
    mappeService.toES(saksmappeES, saksmappe);

    // Add "type", that for some (legacy) reason is an array
    List<String> type = saksmappeES.getType();
    if (type == null) {
      type = new ArrayList<String>();
      saksmappeES.setType(type);
    }
    type.add("Saksmappe");

    // TODO:
    // Add arkivskaperTransitive
    // Add arkivskaperNavn
    // Add arkivskaperSorteringsnavn

    // TODO:
    // Create child documents for pageviews, innsynskrav, document clicks?

    return saksmappeES;
  }


  /**
   * Delete a Saksmappe, all it's children, and the ES document
   * 
   * @param id
   * @param externalId
   */
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
        // TODO: journalpostService.deleteJournalpost(journalpost.getId(), null);
      });
    }

    // Delete saksmappe
    saksmappeRepository.deleteById(id);

    // Delete ES document
    elasticsearchOperations.delete(id, IndexCoordinates.of(elasticsearchIndex));
  }
}

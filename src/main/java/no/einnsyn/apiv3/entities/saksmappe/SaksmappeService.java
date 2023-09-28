package no.einnsyn.apiv3.entities.saksmappe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.google.gson.Gson;
import lombok.Getter;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.journalpost.JournalpostRepository;
import no.einnsyn.apiv3.entities.journalpost.JournalpostService;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostJSON;
import no.einnsyn.apiv3.entities.mappe.MappeService;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeJSON;

@Service
public class SaksmappeService extends MappeService<Saksmappe, SaksmappeJSON> {

  private final SaksmappeRepository saksmappeRepository;
  private final JournalpostService journalpostService;
  private final JournalpostRepository journalpostRepository;
  private final Gson gson;
  private final ElasticsearchOperations elasticsearchOperations;

  @Getter
  private final SaksmappeRepository repository;

  @Value("${application.elasticsearchIndex}")
  private String elasticsearchIndex;

  public SaksmappeService(SaksmappeRepository saksmappeRepository,
      JournalpostService journalpostService, JournalpostRepository journalpostRepository,
      ElasticsearchOperations elasticsearchOperations, Gson gson, SaksmappeRepository repository) {
    super();
    this.saksmappeRepository = saksmappeRepository;
    this.journalpostService = journalpostService;
    this.journalpostRepository = journalpostRepository;
    this.elasticsearchOperations = elasticsearchOperations;
    this.gson = gson;
    this.repository = repository;
  }

  public Saksmappe newObject() {
    return new Saksmappe();
  }

  public SaksmappeJSON newJSON() {
    return new SaksmappeJSON();
  }


  /**
   * Index the Saksmappe to ElasticSearch
   * 
   * @param saksmappe
   * @return
   */
  public void index(Saksmappe saksmappe, boolean shouldUpdateRelatives) {
    SaksmappeJSON saksmappeES = toES(saksmappe);

    // MappeService may update relatives (parent / children)
    super.index(saksmappe, shouldUpdateRelatives);

    // Serialize using Gson, to get custom serialization of ExpandedFields
    String sourceString = gson.toJson(saksmappeES);
    IndexQuery indexQuery =
        new IndexQueryBuilder().withId(saksmappe.getId()).withSource(sourceString).build();
    elasticsearchOperations.index(indexQuery, IndexCoordinates.of(elasticsearchIndex));

    if (shouldUpdateRelatives) {
      List<Journalpost> journalposts = saksmappe.getJournalpost();
      for (Journalpost journalpost : journalposts) {
        journalpostService.index(journalpost, false);
      }
    }
  }


  /**
   * Convert a JSON object to Saksmappe
   * 
   * @param json
   * @param saksmappe
   * @param paths A list of paths containing new objects that will be created from this update
   * @param currentPath The current path in the object tree
   * @return
   */
  public Saksmappe fromJSON(SaksmappeJSON json, Saksmappe saksmappe, Set<String> paths,
      String currentPath) {
    super.fromJSON(json, saksmappe, paths, currentPath);

    if (json.getSaksaar() != null) {
      saksmappe.setSaksaar(json.getSaksaar());
    }

    if (json.getSakssekvensnummer() != null) {
      saksmappe.setSakssekvensnummer(json.getSakssekvensnummer());
    }

    if (json.getSaksdato() != null) {
      saksmappe.setSaksdato(json.getSaksdato());
    }

    // Add journalposts
    List<ExpandableField<JournalpostJSON>> journalpostFieldList = json.getJournalpost();
    journalpostFieldList.forEach((journalpostField) -> {
      Journalpost journalpost = null;
      if (journalpostField.getId() != null) {
        journalpost = journalpostRepository.findById(journalpostField.getId());
      } else {
        String journalpostPath = currentPath == "" ? "journalpost" : currentPath + ".journalpost";
        paths.add(journalpostPath);
        journalpost = journalpostService.fromJSON(journalpostField.getExpandedObject(), paths,
            journalpostPath);
      }
      // If no administrativEnhet is given for journalpost, set it to the saksmappe's
      if (journalpost.getAdministrativEnhet() == null) {
        journalpost.setAdministrativEnhet(saksmappe.getAdministrativEnhet());
        journalpost.setAdministrativEnhetObjekt(saksmappe.getAdministrativEnhetObjekt());
      }
      saksmappe.addJournalpost(journalpost);
    });

    return saksmappe;
  }


  /**
   * Convert a Saksmappe to a JSON object
   * 
   * @param saksmappe
   * @param json
   * @param expandPaths A list of paths to expand. Un-expanded objects will be shown as IDs
   * @param currentPath The current path in the object tree
   * @return
   */
  public SaksmappeJSON toJSON(Saksmappe saksmappe, SaksmappeJSON json, Set<String> expandPaths,
      String currentPath) {

    super.toJSON(saksmappe, json, expandPaths, currentPath);

    json.setSaksaar(saksmappe.getSaksaar());
    json.setSakssekvensnummer(saksmappe.getSakssekvensnummer());
    json.setSaksdato(saksmappe.getSaksdato());
    json.setSaksnummer(saksmappe.getSaksaar() + "/" + saksmappe.getSakssekvensnummer());

    // Journalposts
    List<ExpandableField<JournalpostJSON>> journalpostsJSON =
        new ArrayList<ExpandableField<JournalpostJSON>>();
    List<Journalpost> journalposts = saksmappe.getJournalpost();
    if (journalposts != null) {
      journalposts.forEach((journalpost) -> {
        journalpostsJSON.add(
            journalpostService.maybeExpand(journalpost, "journalpost", expandPaths, currentPath));
      });
    }
    json.setJournalpost(journalpostsJSON);

    return json;
  }


  /**
   * Convert a Saksmappe to an ES document
   * 
   * @param saksmappe
   * @param saksmappeES
   * @return
   */
  public SaksmappeJSON toES(Saksmappe saksmappe, SaksmappeJSON saksmappeES) {
    super.toES(saksmappeES, saksmappe);

    toJSON(saksmappe, saksmappeES, new HashSet<String>(), "");

    // Add type, that for some (legacy) reason is an array
    saksmappeES.setType(Arrays.asList("Saksmappe"));

    // Legacy, this field name is used in the old front-end.
    saksmappeES.setOffentligTittel_SENSITIV(saksmappe.getOffentligTittelSensitiv());

    // Generate list of saksår / saksnummer in different formats
    // YYYY/N
    // YY/N
    // N/YYYY
    // N/YY
    Integer saksaar = saksmappe.getSaksaar();
    Integer saksaarShort = saksaar % 100;
    Integer sakssekvensnummer = saksmappe.getSakssekvensnummer();
    saksmappeES.setSaksnummerGenerert(
        Arrays.asList(saksaar + "/" + sakssekvensnummer, saksaarShort + "/" + sakssekvensnummer,
            sakssekvensnummer + "/" + saksaar, sakssekvensnummer + "/" + saksaarShort));

    // TODO: Create child documents for pageviews, innsynskrav, document clicks?

    return saksmappeES;
  }


  /**
   * Delete a Saksmappe, all it's children, and the ES document
   * 
   * @param id
   * @param externalId
   */
  @Transactional
  public SaksmappeJSON delete(String id) {
    // This ID should be verified in the controller, so it should always exist.
    Saksmappe saksmappe = saksmappeRepository.findById(id);
    SaksmappeJSON saksmappeJSON = toJSON(saksmappe);
    saksmappeJSON.setDeleted(true);

    // Delete all journalposts
    List<Journalpost> journalposts = saksmappe.getJournalpost();
    if (journalposts != null) {
      journalposts.forEach((journalpost) -> {
        journalpostService.delete(journalpost.getId());
      });
    }

    // Delete saksmappe
    saksmappeRepository.deleteById(id);

    // Delete ES document
    elasticsearchOperations.delete(id, IndexCoordinates.of(elasticsearchIndex));

    return saksmappeJSON;
  }

}

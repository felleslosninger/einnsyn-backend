package no.einnsyn.apiv3.entities.journalpost;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;
import com.google.gson.Gson;
import jakarta.transaction.Transactional;
import lombok.Getter;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.DokumentbeskrivelseRepository;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.DokumentbeskrivelseService;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseJSON;
import no.einnsyn.apiv3.entities.enhet.EnhetService;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
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
public class JournalpostService extends RegistreringService<Journalpost, JournalpostJSON> {

  private final EnhetService enhetService;
  private final SaksmappeRepository saksmappeRepository;
  private final SkjermingRepository skjermingRepository;
  private final SkjermingService skjermingService;
  private final KorrespondansepartRepository korrespondansepartRepository;
  private final KorrespondansepartService korrespondansepartService;
  private final DokumentbeskrivelseRepository dokumentbeskrivelseRepository;
  private final DokumentbeskrivelseService dokumentbeskrivelseService;
  private final Gson gson;
  private final ElasticsearchOperations elasticsearchOperations;

  @Getter
  private final JournalpostRepository repository;

  @Value("${application.elasticsearchIndex}")
  private String elasticsearchIndex;

  JournalpostService(EnhetService enhetService, SaksmappeRepository saksmappeRepository,
      SkjermingRepository skjermingRepository, SkjermingService skjermingService,
      KorrespondansepartRepository korrespondansepartRepository,
      KorrespondansepartService korrespondansepartService,
      DokumentbeskrivelseRepository dokumentbeskrivelseRepository,
      DokumentbeskrivelseService dokumentbeskrivelseService,
      JournalpostRepository journalpostRepository, Gson gson,
      ElasticsearchOperations elasticsearchOperations) {
    super();
    this.enhetService = enhetService;
    this.saksmappeRepository = saksmappeRepository;
    this.skjermingRepository = skjermingRepository;
    this.skjermingService = skjermingService;
    this.korrespondansepartRepository = korrespondansepartRepository;
    this.korrespondansepartService = korrespondansepartService;
    this.dokumentbeskrivelseRepository = dokumentbeskrivelseRepository;
    this.dokumentbeskrivelseService = dokumentbeskrivelseService;
    this.repository = journalpostRepository;
    this.gson = gson;
    this.elasticsearchOperations = elasticsearchOperations;
  }


  public Journalpost newObject() {
    return new Journalpost();
  }


  public JournalpostJSON newJSON() {
    return new JournalpostJSON();
  }


  /**
   * Index the Journalpost to ElasticSearch
   * 
   * @param journalpost
   * @param shouldUpdateRelatives
   * @return
   */
  public void index(Journalpost journalpost, boolean shouldUpdateRelatives) {
    JournalpostJSON journalpostES = toES(journalpost);

    // MappeService may update relatives (parent / children)
    super.index(journalpost, shouldUpdateRelatives);

    // Serialize using Gson, to get custom serialization of ExpandedFields
    String sourceString = gson.toJson(journalpostES);
    IndexQuery indexQuery =
        new IndexQueryBuilder().withId(journalpost.getId()).withSource(sourceString).build();
    elasticsearchOperations.index(indexQuery, IndexCoordinates.of(elasticsearchIndex));

    if (shouldUpdateRelatives) {
      // Re-index parent
    }
  }


  /**
   * Create a Journalpost from a JSON object. This will recursively also create children elements,
   * if they are given in the JSON object.
   * 
   * @param json
   * @param journalpost
   * @param paths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return
   */
  public Journalpost fromJSON(JournalpostJSON json, Journalpost journalpost, Set<String> paths,
      String currentPath) {
    super.fromJSON(json, journalpost, paths, currentPath);

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

    // Update saksmappe
    ExpandableField<SaksmappeJSON> saksmappeField = json.getSaksmappe();
    if (saksmappeField != null) {
      Saksmappe saksmappe = saksmappeRepository.findById(saksmappeField.getId());
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
        String skjermingPath = currentPath == "" ? "skjerming" : currentPath + ".skjerming";
        paths.add(skjermingPath);
        skjerming =
            skjermingService.fromJSON(skjermingField.getExpandedObject(), paths, skjermingPath);
      }
      journalpost.setSkjerming(skjerming);
    }

    // Update korrespondansepart
    List<ExpandableField<KorrespondansepartJSON>> korrpartFieldList = json.getKorrespondansepart();
    korrpartFieldList.forEach((korrpartField) -> {
      Korrespondansepart korrpart = null;
      if (korrpartField.getId() != null) {
        korrpart = korrespondansepartRepository.findById(korrpartField.getId());
      } else {
        KorrespondansepartJSON korrpartJSON = korrpartField.getExpandedObject();
        String korrespondansepartPath =
            currentPath == "" ? "korrespondansepart" : currentPath + ".korrespondansepart";
        paths.add(korrespondansepartPath);
        korrpart = korrespondansepartService.fromJSON(korrpartJSON, paths, korrespondansepartPath);
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
        String dokbeskPath =
            currentPath == "" ? "dokumentbeskrivelse" : currentPath + ".dokumentbeskrivelse";
        paths.add(dokbeskPath);
        dokbesk = dokumentbeskrivelseService.fromJSON(dokbeskField.getExpandedObject(), paths,
            dokbeskPath);
      }
      journalpost.getDokumentbeskrivelse().add(dokbesk);
    });

    // Look for administrativEnhet and saksbehandler from Korrespondansepart
    Boolean updatedAdministrativEnhet = false;
    for (ExpandableField<KorrespondansepartJSON> korrpartField : korrpartFieldList) {
      KorrespondansepartJSON korrpartJSON = korrpartField.getExpandedObject();
      // Add administrativEnhet from Korrespondansepart where `erBehandlingsansvarlig == true`
      if (korrpartJSON.getErBehandlingsansvarlig() == true
          && korrpartJSON.getAdministrativEnhet() != null) {
        journalpost.setAdministrativEnhet(korrpartJSON.getAdministrativEnhet());
        // TODO: journalpost.setSaksbehandler() ?
        updatedAdministrativEnhet = true;
        break;
      }
      // If we haven't found administrativEnhet elsewhere, use the first avsender/mottaker with
      // administrativEnhet set
      else if (journalpost.getAdministrativEnhet() == null
          && korrpartJSON.getAdministrativEnhet() != null
          && (korrpartJSON.getKorrespondanseparttype().equals("avsender")
              || korrpartJSON.getKorrespondanseparttype().equals("mottaker")
              || korrpartJSON.getKorrespondanseparttype().equals("internAvsender")
              || korrpartJSON.getKorrespondanseparttype().equals("internMottaker"))) {
        journalpost.setAdministrativEnhet(korrpartJSON.getAdministrativEnhet());
        // TODO: journalpost.setSaksbehandler() ?
        updatedAdministrativEnhet = true;
      }
    }

    // Look up administrativEnhetObjekt from administrativEnhet
    if (updatedAdministrativEnhet || json.getAdministrativEnhet() != null) {
      String enhetskode = json.getAdministrativEnhet();
      if (enhetskode == null) {
        enhetskode = journalpost.getAdministrativEnhet();
      }
      Enhet journalenhet = journalpost.getJournalenhet();
      Enhet enhet = enhetService.findByEnhetskode(enhetskode, journalenhet);
      if (enhet != null) {
        journalpost.setAdministrativEnhetObjekt(enhet);
      }
    }

    return journalpost;
  }


  /**
   * Convert a Journalpost to a JSON object.
   * 
   * @param journalpost
   * @param json
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return
   */
  public JournalpostJSON toJSON(Journalpost journalpost, JournalpostJSON json,
      Set<String> expandPaths, String currentPath) {

    super.toJSON(journalpost, json, expandPaths, currentPath);

    json.setJournalaar(journalpost.getJournalaar());
    json.setJournalsekvensnummer(journalpost.getJournalsekvensnummer());
    json.setJournalpostnummer(journalpost.getJournalpostnummer());
    json.setJournalposttype(journalpost.getJournalposttype());
    json.setJournaldato(journalpost.getJournaldato());
    json.setDokumentdato(journalpost.getDokumentdato());
    json.setSorteringstype(journalpost.getSorteringstype());
    json.setAdministrativEnhet(journalpost.getAdministrativEnhet());

    // Administrativ enhet
    Enhet administrativEnhetObjekt = journalpost.getAdministrativEnhetObjekt();
    if (administrativEnhetObjekt != null) {
      json.setAdministrativEnhetObjekt(enhetService.maybeExpand(administrativEnhetObjekt,
          "administrativEnhetObjekt", expandPaths, currentPath));
    }

    // Skjerming
    Skjerming skjerming = journalpost.getSkjerming();
    if (skjerming != null) {
      json.setSkjerming(
          skjermingService.maybeExpand(skjerming, "skjerming", expandPaths, currentPath));
    }

    // Korrespondansepart
    List<Korrespondansepart> korrpartList = journalpost.getKorrespondansepart();
    List<ExpandableField<KorrespondansepartJSON>> korrpartJSONList = json.getKorrespondansepart();
    for (Korrespondansepart korrpart : korrpartList) {
      korrpartJSONList.add(korrespondansepartService.maybeExpand(korrpart, "korrespondansepart",
          expandPaths, currentPath));
    }

    // Dokumentbeskrivelse
    List<Dokumentbeskrivelse> dokbeskList = journalpost.getDokumentbeskrivelse();
    List<ExpandableField<DokumentbeskrivelseJSON>> dokbeskJSONList = json.getDokumentbeskrivelse();
    for (Dokumentbeskrivelse dokbesk : dokbeskList) {
      dokbeskJSONList.add(dokumentbeskrivelseService.maybeExpand(dokbesk, "dokumentbeskrivelse",
          expandPaths, currentPath));
    }

    // ExpandableField<Saksmappe> saksmappeField = journalpost.getSaksmappe();
    // ...
    return json;
  }


  /**
   * Create a ElasticSearch document from a Journalpost object.
   * 
   * @param journalpost
   * @param journalpostES
   * @return
   */
  public JournalpostJSON toES(Journalpost journalpost, JournalpostJSON journalpostES) {
    super.toES(journalpost, journalpostES);

    // Get JSON object, and expand required fields
    Set<String> expandPaths = new HashSet<String>();
    expandPaths.add("skjerming");
    expandPaths.add("korrespondansepart");
    expandPaths.add("dokumentbeskrivelse");
    toJSON(journalpost, journalpostES, expandPaths, "");

    // Add type, that for some (legacy) reason is an array
    journalpostES.setType(Arrays.asList("Journalpost"));

    // Legacy, this field name is used in the old front-end.
    journalpostES.setOffentligTittel_SENSITIV(journalpost.getOffentligTittelSensitiv());

    // Populate "avsender" and "mottaker" from Korrespondansepart
    List<Korrespondansepart> korrespondansepartList = journalpost.getKorrespondansepart();
    for (Korrespondansepart korrespondansepart : korrespondansepartList) {

      if (korrespondansepart.getKorrespondanseparttype().equals("avsender")) {
        journalpostES.setAvsender(Arrays.asList(korrespondansepart.getKorrespondansepartNavn()));
        journalpostES.setAvsender_SENSITIV(
            Arrays.asList(korrespondansepart.getKorrespondansepartNavnSensitiv()));
      } else if (korrespondansepart.getKorrespondanseparttype().equals("mottaker")) {
        journalpostES.setMottaker(Arrays.asList(korrespondansepart.getKorrespondansepartNavn()));
        journalpostES
            .setMottaker_SENSITIV(Arrays.asList(korrespondansepart.getKorrespondansepartNavn()));
      }
    }

    return journalpostES;
  }


  /**
   * Delete a Journalpost by ID
   * 
   * @param id
   * @return
   */
  @Transactional
  public JournalpostJSON delete(String id) {
    // This ID should be verified in the controller, so it should always exist.
    Journalpost journalpost = repository.findById(id);
    return delete(journalpost);
  }

  /**
   * Delete a Journalpost
   * 
   * @param journalpost
   * @return
   */
  @Transactional
  public JournalpostJSON delete(Journalpost journalpost) {
    JournalpostJSON journalpostJSON = toJSON(journalpost);
    journalpostJSON.setDeleted(true);

    // Delete all korrespondanseparts
    List<Korrespondansepart> korrespondansepartList = journalpost.getKorrespondansepart();
    if (korrespondansepartList != null) {
      korrespondansepartList.forEach((korrespondansepart) -> {
        korrespondansepartService.delete(korrespondansepart);
      });
    }

    // Delete all dokumentbeskrivelses
    List<Dokumentbeskrivelse> dokbeskList = journalpost.getDokumentbeskrivelse();
    if (dokbeskList != null) {
      dokbeskList.forEach((dokbesk) -> {
        dokumentbeskrivelseService.deleteIfOrphan(dokbesk);
      });
    }

    // Delete journalpost
    repository.delete(journalpost);

    // TODO: Delete skjerming if it doesn't have any remaining references
    Skjerming skjerming = journalpost.getSkjerming();
    if (skjerming != null) {
      // skjermingService.deleteIfOrphan(skjerming.getId());
    }

    // Delete ES document
    elasticsearchOperations.delete(journalpostJSON.getId(),
        IndexCoordinates.of(elasticsearchIndex));

    return journalpostJSON;
  }


  public Page<Journalpost> getPage(JournalpostGetListRequestParameters params) {
    String saksmappeId = params.getSaksmappeId();

    if (saksmappeId != null) {
      if (params.getStartingAfter() != null) {
        return repository.findBySaksmappeIdAndIdGreaterThanOrderByIdDesc(saksmappeId,
            params.getStartingAfter(), PageRequest.of(0, params.getLimit() + 1));
      } else if (params.getEndingBefore() != null) {
        return repository.findBySaksmappeIdAndIdLessThanOrderByIdDesc(saksmappeId,
            params.getEndingBefore(), PageRequest.of(0, params.getLimit() + 1));
      } else {
        return repository.findBySaksmappeIdOrderByIdDesc(saksmappeId,
            PageRequest.of(0, params.getLimit() + 1));
      }
    }

    return super.getPage(params);
  }

}

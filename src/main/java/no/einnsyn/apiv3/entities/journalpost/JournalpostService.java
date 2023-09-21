package no.einnsyn.apiv3.entities.journalpost;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import no.einnsyn.apiv3.entities.IEinnsynEntityService;
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
public class JournalpostService implements IEinnsynEntityService<Journalpost, JournalpostJSON> {

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


  /**
   * Update a Journalpost from a JSON object, persist/index it to all relevant databases. If no ID
   * is given, a new Journalpost will be created.
   * 
   * @param id
   * @param journalpostJSON
   * @return
   */
  @Transactional
  public JournalpostJSON update(String id, JournalpostJSON journalpostJSON) {
    Journalpost journalpost = null;

    if (id == null && journalpostJSON == null) {
      throw new Error("ID and JSON object missing");
    }

    // If ID is given, get the existing journalpost from DB
    if (id != null) {
      journalpost = journalpostRepository.findById(id);
      if (journalpost == null) {
        throw new Error("Journalpost not found");
      }
    } else {
      journalpost = new Journalpost();
    }

    Set<String> paths = new HashSet<String>();
    fromJSON(journalpostJSON, journalpost, paths, "");
    journalpostRepository.save(journalpost);

    // Generate and save ES document

    // Generate JSON containing all inserted objects
    JournalpostJSON responseJSON = this.toJSON(journalpost, paths, "");

    return responseJSON;
  }


  /**
   * Create a Journalpost from a JSON object. This will recursively also create children elements,
   * if they are given in the JSON object.
   * 
   * @param json
   * @param paths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return
   */
  public Journalpost fromJSON(JournalpostJSON json, Set<String> paths, String currentPath) {
    return fromJSON(json, new Journalpost(), paths, currentPath);
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
    registreringService.fromJSON(json, journalpost, paths, currentPath);

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
    korrpartFieldList.forEach((journalpostField) -> {
      Korrespondansepart korrpart = null;
      if (journalpostField.getId() != null) {
        korrpart = korrespondansepartRepository.findById(journalpostField.getId());
      } else {
        String korrespondansepartPath =
            currentPath == "" ? "korrespondansepart" : currentPath + ".korrespondansepart";
        paths.add(korrespondansepartPath);
        korrpart = korrespondansepartService.fromJSON(journalpostField.getExpandedObject(), paths,
            korrespondansepartPath);
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

    return journalpost;
  }


  /**
   * Convert a Journalpost to a JSON object.
   * 
   * @param journalpost
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return
   */
  public JournalpostJSON toJSON(Journalpost journalpost, Set<String> expandPaths,
      String currentPath) {
    return toJSON(journalpost, new JournalpostJSON(), expandPaths, currentPath);
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

    registreringService.toJSON(journalpost, json, expandPaths, currentPath);

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
   * @param json
   * @return
   */
  public JournalpostJSON toES(Journalpost journalpost, JournalpostJSON json) {
    this.toJSON(journalpost, json, new HashSet<String>(), "");
    registreringService.toES(journalpost, json);
    return json;
  }


  /**
   * Creates an ExpandableField object. If propertyName is in the expandPaths list, the object will
   * be expanded, if not, it will only contain the ID.
   * 
   * @param journalpost
   * @param propertyName Name of the property to expand, appended to currentPath for deeper steps
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return
   */
  public ExpandableField<JournalpostJSON> maybeExpand(Journalpost journalpost, String propertyName,
      Set<String> expandPaths, String currentPath) {
    String updatedPath = currentPath == "" ? propertyName : currentPath + "." + propertyName;
    if (expandPaths.contains(updatedPath)) {
      return new ExpandableField<JournalpostJSON>(journalpost.getId(),
          this.toJSON(journalpost, expandPaths, updatedPath));
    } else {
      return new ExpandableField<JournalpostJSON>(journalpost.getId(), null);
    }
  }
}

package no.einnsyn.apiv3.entities.journalpost;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.google.gson.Gson;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.DokumentbeskrivelseRepository;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.innsynskravdel.InnsynskravDelRepository;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostES;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostListQueryDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.KorrespondansepartRepository;
import no.einnsyn.apiv3.entities.korrespondansepart.models.Korrespondansepart;
import no.einnsyn.apiv3.entities.registrering.RegistreringService;
import no.einnsyn.apiv3.entities.saksmappe.SaksmappeRepository;
import no.einnsyn.apiv3.entities.skjerming.SkjermingRepository;
import no.einnsyn.apiv3.entities.skjerming.models.Skjerming;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class JournalpostService extends RegistreringService<Journalpost, JournalpostDTO> {

  @Getter private final JournalpostRepository repository;

  @Getter @Lazy @Autowired private JournalpostService proxy;

  private final SaksmappeRepository saksmappeRepository;
  private final SkjermingRepository skjermingRepository;
  private final KorrespondansepartRepository korrespondansepartRepository;
  private final DokumentbeskrivelseRepository dokumentbeskrivelseRepository;
  private final Gson gson;
  private final ElasticsearchClient esClient;

  private final InnsynskravDelRepository innsynskravDelRepository;

  @Value("${application.elasticsearchIndex}")
  private String elasticsearchIndex;

  JournalpostService(
      SaksmappeRepository saksmappeRepository,
      SkjermingRepository skjermingRepository,
      KorrespondansepartRepository korrespondansepartRepository,
      DokumentbeskrivelseRepository dokumentbeskrivelseRepository,
      JournalpostRepository journalpostRepository,
      Gson gson,
      ElasticsearchClient esClient,
      InnsynskravDelRepository innsynskravDelRepository) {
    super();
    this.saksmappeRepository = saksmappeRepository;
    this.skjermingRepository = skjermingRepository;
    this.korrespondansepartRepository = korrespondansepartRepository;
    this.dokumentbeskrivelseRepository = dokumentbeskrivelseRepository;
    this.repository = journalpostRepository;
    this.gson = gson;
    this.esClient = esClient;
    this.innsynskravDelRepository = innsynskravDelRepository;
  }

  public Journalpost newObject() {
    return new Journalpost();
  }

  public JournalpostDTO newDTO() {
    return new JournalpostDTO();
  }

  /**
   * Index the Journalpost to ElasticSearch
   *
   * @param journalpost
   * @param shouldUpdateRelatives
   * @return
   */
  @Override
  public void index(Journalpost journalpost, boolean shouldUpdateRelatives) {
    JournalpostDTO journalpostES = journalpostService.toES(journalpost);

    // MappeService may update relatives (parent / children)
    super.index(journalpost, shouldUpdateRelatives);

    // Serialize using Gson, to get custom serialization of ExpandedFields
    var source = gson.toJson(journalpostES);
    var jsonObject = gson.fromJson(source, JSONObject.class);
    try {
      esClient.index(i -> i.index(elasticsearchIndex).id(journalpost.getId()).document(jsonObject));
    } catch (Exception e) {
      // TODO: Log error
      System.err.println(e);
      e.printStackTrace();
    }

    if (shouldUpdateRelatives) {
      // Re-index parent
    }
  }

  /**
   * Create a Journalpost from a DTO object. This will recursively also create children elements, if
   * they are given in the DTO object.
   *
   * @param dto
   * @param journalpost
   * @param paths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return
   */
  @Override
  public Journalpost fromDTO(
      JournalpostDTO dto, Journalpost journalpost, Set<String> paths, String currentPath) {
    super.fromDTO(dto, journalpost, paths, currentPath);

    if (dto.getJournalaar() != null) {
      journalpost.setJournalaar(dto.getJournalaar());
    }

    if (dto.getJournalsekvensnummer() != null) {
      journalpost.setJournalsekvensnummer(dto.getJournalsekvensnummer());
    }

    if (dto.getJournalpostnummer() != null) {
      journalpost.setJournalpostnummer(dto.getJournalpostnummer());
    }

    if (dto.getJournalposttype() != null) {
      journalpost.setJournalposttype(dto.getJournalposttype());
    }

    if (dto.getJournaldato() != null) {
      journalpost.setJournaldato(LocalDate.parse(dto.getJournaldato()));
    }

    if (dto.getDokumentetsDato() != null) {
      journalpost.setDokumentdato(LocalDate.parse(dto.getDokumentetsDato()));
    }

    if (dto.getSorteringstype() != null) {
      journalpost.setSorteringstype(dto.getSorteringstype());
    }

    // Update saksmappe
    var saksmappeField = dto.getSaksmappe();
    if (saksmappeField != null) {
      var saksmappe = saksmappeRepository.findById(saksmappeField.getId()).orElse(null);
      if (saksmappe != null) {
        journalpost.setSaksmappe(saksmappe);
      }
    }

    // Update skjerming
    var skjermingField = dto.getSkjerming();
    if (skjermingField != null) {
      Skjerming skjerming;
      if (skjermingField.getId() != null) {
        skjerming = skjermingRepository.findById(skjermingField.getId()).orElse(null);
      } else {
        var skjermingPath = currentPath.isEmpty() ? "skjerming" : currentPath + ".skjerming";
        paths.add(skjermingPath);
        skjerming =
            skjermingService.fromDTO(skjermingField.getExpandedObject(), paths, skjermingPath);
      }
      journalpost.setSkjerming(skjerming);
    }

    // Update korrespondansepart
    var korrpartFieldList = dto.getKorrespondansepart();
    korrpartFieldList.forEach(
        korrpartField -> {
          Korrespondansepart korrpart = null;
          if (korrpartField.getId() != null) {
            korrpart = korrespondansepartRepository.findById(korrpartField.getId()).orElse(null);
          } else {
            var korrpartDTO = korrpartField.getExpandedObject();
            var korrespondansepartPath =
                currentPath.isEmpty() ? "korrespondansepart" : currentPath + ".korrespondansepart";
            paths.add(korrespondansepartPath);
            korrpart =
                korrespondansepartService.fromDTO(korrpartDTO, paths, korrespondansepartPath);
          }
          journalpost.addKorrespondansepart(korrpart);
        });

    // Update dokumentbeskrivelse
    var dokbeskFieldList = dto.getDokumentbeskrivelse();
    dokbeskFieldList.forEach(
        dokbeskField -> {
          Dokumentbeskrivelse dokbesk = null;
          if (dokbeskField.getId() != null) {
            dokbesk = dokumentbeskrivelseRepository.findById(dokbeskField.getId()).orElse(null);
          } else {
            var dokbeskDTO = dokbeskField.getExpandedObject();
            var dokbeskPath =
                currentPath.isEmpty()
                    ? "dokumentbeskrivelse"
                    : currentPath + ".dokumentbeskrivelse";
            paths.add(dokbeskPath);
            dokbesk = dokumentbeskrivelseService.fromDTO(dokbeskDTO, paths, dokbeskPath);
          }
          journalpost.getDokumentbeskrivelse().add(dokbesk);
        });

    // Look for administrativEnhet and saksbehandler from Korrespondansepart
    var updatedAdministrativEnhet = false;
    for (var korrpartField : korrpartFieldList) {
      var korrpartDTO = korrpartField.getExpandedObject();
      if (korrpartDTO == null) {
        var korrpart = korrespondansepartRepository.findById(korrpartField.getId()).orElse(null);
        korrpartDTO = korrespondansepartService.toDTO(korrpart);
      }
      // Add administrativEnhet from Korrespondansepart where `erBehandlingsansvarlig == true`
      if (korrpartDTO.getErBehandlingsansvarlig() != null
          && korrpartDTO.getErBehandlingsansvarlig()
          && korrpartDTO.getAdministrativEnhet() != null) {
        journalpost.setAdministrativEnhet(korrpartDTO.getAdministrativEnhet());
        journalpost.setSaksbehandler(korrpartDTO.getSaksbehandler());
        updatedAdministrativEnhet = true;
        break;
      }
      // If we haven't found administrativEnhet elsewhere, use the first avsender/mottaker with
      // administrativEnhet set
      else if (journalpost.getAdministrativEnhet() == null
          && korrpartDTO.getAdministrativEnhet() != null
          && (korrpartDTO.getKorrespondanseparttype().equals("avsender")
              || korrpartDTO.getKorrespondanseparttype().equals("mottaker")
              || korrpartDTO.getKorrespondanseparttype().equals("internAvsender")
              || korrpartDTO.getKorrespondanseparttype().equals("internMottaker"))) {
        journalpost.setAdministrativEnhet(korrpartDTO.getAdministrativEnhet());
        // TODO: Do we need more logic to find saksbehandler?
        // !StringUtils.containsIgnoreCase(korrespondansepart1.getSaksbehandler(),
        // UFORDELT_LOWER_CASE))
        journalpost.setSaksbehandler(korrpartDTO.getSaksbehandler());
        updatedAdministrativEnhet = true;
      }
    }

    // Look up administrativEnhetObjekt from administrativEnhet
    if (updatedAdministrativEnhet || dto.getAdministrativEnhet() != null) {
      var enhetskode = dto.getAdministrativEnhet();
      if (enhetskode == null) {
        enhetskode = journalpost.getAdministrativEnhet();
      }
      var journalenhet = journalpost.getJournalenhet();
      var enhet = enhetService.findByEnhetskode(enhetskode, journalenhet);
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
   * @param dto
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return
   */
  @Override
  public JournalpostDTO toDTO(
      Journalpost journalpost, JournalpostDTO dto, Set<String> expandPaths, String currentPath) {

    super.toDTO(journalpost, dto, expandPaths, currentPath);

    dto.setJournalaar(journalpost.getJournalaar());
    dto.setJournalsekvensnummer(journalpost.getJournalsekvensnummer());
    dto.setJournalpostnummer(journalpost.getJournalpostnummer());
    dto.setJournalposttype(journalpost.getJournalposttype());
    dto.setJournaldato(journalpost.getJournaldato().toString());
    dto.setDokumentetsDato(journalpost.getDokumentdato().toString());
    dto.setSorteringstype(journalpost.getSorteringstype());
    dto.setAdministrativEnhet(journalpost.getAdministrativEnhet());

    // Administrativ enhet
    var administrativEnhetObjekt = journalpost.getAdministrativEnhetObjekt();
    if (administrativEnhetObjekt != null) {
      dto.setAdministrativEnhetObjekt(
          enhetService.maybeExpand(
              administrativEnhetObjekt, "administrativEnhetObjekt", expandPaths, currentPath));
    }

    // Skjerming
    var skjerming = journalpost.getSkjerming();
    if (skjerming != null) {
      dto.setSkjerming(
          skjermingService.maybeExpand(skjerming, "skjerming", expandPaths, currentPath));
    }

    // Korrespondansepart
    var korrpartList = journalpost.getKorrespondansepart();
    var korrpartJSONList = dto.getKorrespondansepart();
    for (Korrespondansepart korrpart : korrpartList) {
      korrpartJSONList.add(
          korrespondansepartService.maybeExpand(
              korrpart, "korrespondansepart", expandPaths, currentPath));
    }

    // Dokumentbeskrivelse
    var dokbeskList = journalpost.getDokumentbeskrivelse();
    var dokbeskJSONList = dto.getDokumentbeskrivelse();
    for (var dokbesk : dokbeskList) {
      dokbeskJSONList.add(
          dokumentbeskrivelseService.maybeExpand(
              dokbesk, "dokumentbeskrivelse", expandPaths, currentPath));
    }

    return dto;
  }

  /**
   * Create a ElasticSearch document from a Journalpost object.
   *
   * @param journalpost
   * @param journalpostES
   * @return
   */
  public JournalpostES toES(Journalpost journalpost) {
    var journalpostES = new JournalpostES();

    // Get DTO object, and expand required fields
    var expandPaths = new HashSet<String>();
    expandPaths.add("skjerming");
    expandPaths.add("korrespondansepart");
    expandPaths.add("dokumentbeskrivelse");
    journalpostService.toDTO(journalpost, journalpostES, expandPaths, "");

    // Legacy, this field name is used in the old front-end.
    journalpostES.setOffentligTittel_SENSITIV(journalpost.getOffentligTittelSensitiv());

    // Populate "avsender" and "mottaker" from Korrespondansepart
    var korrespondansepartList = journalpost.getKorrespondansepart();
    for (var korrespondansepart : korrespondansepartList) {

      if (korrespondansepart.getKorrespondanseparttype().equals("avsender")) {
        journalpostES.setAvsender(List.of(korrespondansepart.getKorrespondansepartNavn()));
        journalpostES.setAvsender_SENSITIV(
            List.of(korrespondansepart.getKorrespondansepartNavnSensitiv()));
      } else if (korrespondansepart.getKorrespondanseparttype().equals("mottaker")) {
        journalpostES.setMottaker(List.of(korrespondansepart.getKorrespondansepartNavn()));
        journalpostES.setMottaker_SENSITIV(List.of(korrespondansepart.getKorrespondansepartNavn()));
      }
    }

    // Populate "arkivskaperTransitive" and "arkivskaperNavn"
    var administrativEnhet = journalpost.getAdministrativEnhetObjekt();
    var administrativEnhetTransitive = enhetService.getTransitiveEnhets(administrativEnhet);

    var administrativEnhetIdTransitive = new ArrayList<String>();
    // Legacy
    var arkivskaperTransitive = new ArrayList<String>();
    // Legacy
    var arkivskaperNavn = new ArrayList<String>();

    for (var ancestor : administrativEnhetTransitive) {
      administrativEnhetIdTransitive.add(ancestor.getId());
      arkivskaperTransitive.add(ancestor.getIri());
      arkivskaperNavn.add(ancestor.getNavn());
    }

    journalpostES.setArkivskaperTransitive(arkivskaperTransitive);
    journalpostES.setArkivskaperNavn(arkivskaperNavn);
    journalpostES.setArkivskaperSorteringNavn(arkivskaperNavn.get(0));
    journalpostES.setArkivskaper(journalpost.getAdministrativEnhetObjekt().getIri());

    return journalpostES;
  }

  /**
   * Delete a Journalpost
   *
   * @param journalpost
   * @return
   */
  @Transactional
  public JournalpostDTO delete(Journalpost journalpost) {
    var journalpostDTO = getProxy().toDTO(journalpost);
    journalpostDTO.setDeleted(true);

    // Delete all korrespondanseparts
    var korrespondansepartList = journalpost.getKorrespondansepart();
    if (korrespondansepartList != null) {
      journalpost.setKorrespondansepart(List.of());
      korrespondansepartList.forEach(korrespondansepartService::delete);
    }

    // Unrelate all dokumentbeskrivelses
    var dokbeskList = journalpost.getDokumentbeskrivelse();
    if (dokbeskList != null) {
      journalpost.setDokumentbeskrivelse(List.of());
      dokbeskList.forEach(dokumentbeskrivelseService::deleteIfOrphan);
    }

    // Unrelate skjerming, delete if orphan
    var skjerming = journalpost.getSkjerming();
    if (skjerming != null) {
      journalpost.setSkjerming(null);
      skjermingService.deleteIfOrphan(skjerming);
    }

    // Delete all innsynskravDels
    var innsynskravDelList = innsynskravDelRepository.findByJournalpost(journalpost);
    if (innsynskravDelList != null) {
      innsynskravDelList.forEach(innsynskravDelService::delete);
    }

    // Delete journalpost
    repository.delete(journalpost);

    // Delete ES document
    try {
      esClient.delete(d -> d.index(elasticsearchIndex).id(journalpostDTO.getId()));
    } catch (Exception e) {
      // TODO: Log error
    }

    return journalpostDTO;
  }

  public Page<Journalpost> getPage(JournalpostListQueryDTO params) {
    var saksmappeId = params.getSaksmappe();

    if (saksmappeId != null) {
      if (params.getStartingAfter() != null) {
        return repository.findBySaksmappeIdAndIdGreaterThanOrderByIdDesc(
            saksmappeId, params.getStartingAfter(), PageRequest.of(0, params.getLimit() + 1));
      } else if (params.getEndingBefore() != null) {
        return repository.findBySaksmappeIdAndIdLessThanOrderByIdDesc(
            saksmappeId, params.getEndingBefore(), PageRequest.of(0, params.getLimit() + 1));
      } else {
        return repository.findBySaksmappeIdOrderByIdDesc(
            saksmappeId, PageRequest.of(0, params.getLimit() + 1));
      }
    }

    return super.getPage(params);
  }
}

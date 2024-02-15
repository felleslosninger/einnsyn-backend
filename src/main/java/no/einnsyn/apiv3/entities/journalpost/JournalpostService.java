package no.einnsyn.apiv3.entities.journalpost;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.google.gson.Gson;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.paginators.Paginators;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseListQueryDTO;
import no.einnsyn.apiv3.entities.innsynskravdel.InnsynskravDelRepository;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostES;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostListQueryDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.KorrespondansepartRepository;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartListQueryDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartParentDTO;
import no.einnsyn.apiv3.entities.registrering.RegistreringService;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class JournalpostService extends RegistreringService<Journalpost, JournalpostDTO> {

  @Getter private final JournalpostRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private JournalpostService proxy;

  private final KorrespondansepartRepository korrespondansepartRepository;
  private final Gson gson;
  private final ElasticsearchClient esClient;

  private final InnsynskravDelRepository innsynskravDelRepository;

  @Value("${application.elasticsearchIndex}")
  private String elasticsearchIndex;

  JournalpostService(
      KorrespondansepartRepository korrespondansepartRepository,
      JournalpostRepository journalpostRepository,
      Gson gson,
      ElasticsearchClient esClient,
      InnsynskravDelRepository innsynskravDelRepository) {
    super();
    this.korrespondansepartRepository = korrespondansepartRepository;
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
  @Transactional(propagation = Propagation.MANDATORY)
  public void index(Journalpost journalpost, boolean shouldUpdateRelatives)
      throws EInnsynException {
    JournalpostDTO journalpostES = journalpostService.entityToES(journalpost);

    // MappeService may update relatives (parent / children)
    super.index(journalpost, shouldUpdateRelatives);

    // Serialize using Gson, to get custom serialization of ExpandedFields
    var source = gson.toJson(journalpostES);
    var jsonObject = gson.fromJson(source, JSONObject.class);
    try {
      esClient.index(i -> i.index(elasticsearchIndex).id(journalpost.getId()).document(jsonObject));
    } catch (Exception e) {
      log.error("Could not index Journalpost", e);
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
  @Transactional(propagation = Propagation.MANDATORY)
  public Journalpost fromDTO(
      JournalpostDTO dto, Journalpost journalpost, Set<String> paths, String currentPath)
      throws EInnsynException {
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
      var saksmappe = saksmappeService.findById(saksmappeField.getId());
      if (saksmappe != null) {
        journalpost.setSaksmappe(saksmappe);
      }
    }

    // If we don't have an ID, persist the object before adding relations
    if (journalpost.getId() == null) {
      journalpost = repository.saveAndFlush(journalpost);
    }

    // Update skjerming
    var skjermingField = dto.getSkjerming();
    if (skjermingField != null) {
      journalpost.setSkjerming(
          skjermingService.insertOrReturnExisting(skjermingField, "skjerming", paths, currentPath));
    }

    // Update korrespondansepart
    var korrpartFieldList = dto.getKorrespondansepart();
    if (korrpartFieldList != null) {
      for (var korrpartField : korrpartFieldList) {
        journalpost.addKorrespondansepart(
            korrespondansepartService.insertOrReturnExisting(
                korrpartField, "korrespondansepart", paths, currentPath));
      }
    }

    // Update dokumentbeskrivelse
    var dokbeskFieldList = dto.getDokumentbeskrivelse();
    if (dokbeskFieldList != null) {
      for (var dokbeskField : dokbeskFieldList) {
        journalpost.addDokumentbeskrivelse(
            dokumentbeskrivelseService.insertOrReturnExisting(
                dokbeskField, "dokumentbeskrivelse", paths, currentPath));
      }
    }

    // Look for administrativEnhet and saksbehandler from Korrespondansepart
    var updatedAdministrativEnhet = false;
    if (korrpartFieldList != null) {
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
  @Transactional(propagation = Propagation.MANDATORY)
  public JournalpostDTO toDTO(
      Journalpost journalpost, JournalpostDTO dto, Set<String> expandPaths, String currentPath) {

    super.toDTO(journalpost, dto, expandPaths, currentPath);

    dto.setJournalaar(journalpost.getJournalaar());
    dto.setJournalsekvensnummer(journalpost.getJournalsekvensnummer());
    dto.setJournalpostnummer(journalpost.getJournalpostnummer());
    dto.setJournalposttype(journalpost.getJournalposttype());
    dto.setSorteringstype(journalpost.getSorteringstype());
    dto.setAdministrativEnhet(journalpost.getAdministrativEnhet());
    if (journalpost.getJournaldato() != null) {
      dto.setJournaldato(journalpost.getJournaldato().toString());
    }
    if (journalpost.getDokumentdato() != null) {
      dto.setDokumentetsDato(journalpost.getDokumentdato().toString());
    }

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
    var korrpartListDTO = dto.getKorrespondansepart();
    if (korrpartListDTO == null) {
      korrpartListDTO = new ArrayList<>();
      dto.setKorrespondansepart(korrpartListDTO);
    }
    var korrpartList = journalpost.getKorrespondansepart();
    if (korrpartList != null) {
      for (var korrpart : korrpartList) {
        korrpartListDTO.add(
            korrespondansepartService.maybeExpand(
                korrpart, "korrespondansepart", expandPaths, currentPath));
      }
    }

    // Dokumentbeskrivelse
    var dokbeskListDTO = dto.getDokumentbeskrivelse();
    if (dokbeskListDTO == null) {
      dokbeskListDTO = new ArrayList<>();
      dto.setDokumentbeskrivelse(dokbeskListDTO);
    }
    var dokbeskList = journalpost.getDokumentbeskrivelse();
    if (dokbeskList != null) {
      for (var dokbesk : dokbeskList) {
        dokbeskListDTO.add(
            dokumentbeskrivelseService.maybeExpand(
                dokbesk, "dokumentbeskrivelse", expandPaths, currentPath));
      }
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
  @Transactional(propagation = Propagation.MANDATORY)
  public JournalpostES entityToES(Journalpost journalpost) {
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
    if (korrespondansepartList != null) {
      for (var korrespondansepart : korrespondansepartList) {

        if (korrespondansepart.getKorrespondanseparttype().equals("avsender")) {
          journalpostES.setAvsender(List.of(korrespondansepart.getKorrespondansepartNavn()));
          journalpostES.setAvsender_SENSITIV(
              List.of(korrespondansepart.getKorrespondansepartNavnSensitiv()));
        } else if (korrespondansepart.getKorrespondanseparttype().equals("mottaker")) {
          journalpostES.setMottaker(List.of(korrespondansepart.getKorrespondansepartNavn()));
          journalpostES.setMottaker_SENSITIV(
              List.of(korrespondansepart.getKorrespondansepartNavn()));
        }
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

    if (administrativEnhetTransitive != null) {
      for (var ancestor : administrativEnhetTransitive) {
        administrativEnhetIdTransitive.add(ancestor.getId());
        arkivskaperTransitive.add(ancestor.getIri());
        arkivskaperNavn.add(ancestor.getNavn());
      }
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
  @Transactional(propagation = Propagation.MANDATORY)
  public JournalpostDTO delete(Journalpost journalpost) throws EInnsynException {
    var journalpostDTO = proxy.toDTO(journalpost);
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
    var innsynskravDelStream = innsynskravDelRepository.findAllByJournalpost(journalpost);
    var innsynskravDelIterator = innsynskravDelStream.iterator();
    while (innsynskravDelIterator.hasNext()) {
      var innsynskravDel = innsynskravDelIterator.next();
      innsynskravDelService.delete(innsynskravDel);
    }

    // Delete journalpost
    repository.delete(journalpost);

    // Delete ES document
    try {
      esClient.delete(d -> d.index(elasticsearchIndex).id(journalpostDTO.getId()));
    } catch (Exception e) {
      throw new EInnsynException("Could not delete journalpost from ElasticSearch", e);
    }

    return journalpostDTO;
  }

  /**
   * Get custom paginator functions that filters by saksmappeId
   *
   * @param params
   */
  @Override
  public Paginators<Journalpost> getPaginators(BaseListQueryDTO params) {
    if (params instanceof JournalpostListQueryDTO p && p.getSaksmappeId() != null) {
      var saksmappe = saksmappeService.findById(p.getSaksmappeId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(saksmappe, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(saksmappe, pivot, pageRequest));
    }
    return super.getPaginators(params);
  }

  /**
   * @param journalpostId
   * @param query
   * @return
   */
  public ResultList<KorrespondansepartDTO> getKorrespondansepartList(
      String journalpostId, KorrespondansepartListQueryDTO query) {
    query.setJournalpostId(journalpostId);
    return korrespondansepartService.list(query);
  }

  /**
   * @param journalpostId
   * @param dto
   * @return
   */
  public KorrespondansepartDTO addKorrespondansepart(
      String journalpostId, KorrespondansepartDTO dto) throws EInnsynException {
    dto.setParent(new KorrespondansepartParentDTO(journalpostId));
    return korrespondansepartService.add(dto);
  }

  /**
   * @param journalpostId
   * @param query
   * @return
   */
  public ResultList<DokumentbeskrivelseDTO> getDokumentbeskrivelseList(
      String journalpostId, DokumentbeskrivelseListQueryDTO query) {
    query.setJournalpostId(journalpostId);
    return dokumentbeskrivelseService.list(query);
  }

  /**
   * @param journalpostId
   * @param dto
   * @return
   */
  @Transactional
  public DokumentbeskrivelseDTO addDokumentbeskrivelse(
      String journalpostId, DokumentbeskrivelseDTO dto) throws EInnsynException {
    var journalpost = journalpostService.findById(journalpostId);
    var addedDokumentbeskrivelseDTO = dokumentbeskrivelseService.add(dto);
    var journalpostDTO = new JournalpostDTO();
    journalpostDTO.setDokumentbeskrivelse(
        List.of(new ExpandableField<>(addedDokumentbeskrivelseDTO)));
    journalpostService.update(journalpost.getId(), journalpostDTO);
    return addedDokumentbeskrivelseDTO;
  }

  /**
   * Unrelates a Dokumentbeskrivelse from a Journalpost. The Dokumentbeskrivelse is deleted if it is
   * orphaned after the unrelate.
   *
   * @param journalpostId
   * @param dokumentbeskrivelseId
   * @return
   */
  @Transactional
  public JournalpostDTO deleteDokumentbeskrivelse(
      String journalpostId, String dokumentbeskrivelseId) {
    var journalpost = journalpostService.findById(journalpostId);
    var dokumentbeskrivelseList = journalpost.getDokumentbeskrivelse();
    if (dokumentbeskrivelseList != null) {
      var updatedKorrespondansepartList =
          dokumentbeskrivelseList.stream()
              .filter(dokbesk -> !dokbesk.getId().equals(dokumentbeskrivelseId))
              .toList();
      journalpost.setDokumentbeskrivelse(updatedKorrespondansepartList);
    }
    var dokumentbeskrivelse = dokumentbeskrivelseService.findById(dokumentbeskrivelseId);
    dokumentbeskrivelseService.deleteIfOrphan(dokumentbeskrivelse);
    return journalpostService.toDTO(journalpost);
  }
}

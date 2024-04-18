package no.einnsyn.apiv3.entities.journalpost;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.paginators.Paginators;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseListQueryDTO;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.innsynskravdel.InnsynskravDelRepository;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostES;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostListQueryDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartListQueryDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartParentDTO;
import no.einnsyn.apiv3.entities.registrering.RegistreringService;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@SuppressWarnings("java:S1192") // Allow multiple string literals
public class JournalpostService extends RegistreringService<Journalpost, JournalpostDTO> {

  @Getter private final JournalpostRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private JournalpostService proxy;

  private final ElasticsearchClient esClient;

  private final InnsynskravDelRepository innsynskravDelRepository;

  @Value("${application.elasticsearchIndex}")
  private String elasticsearchIndex;

  JournalpostService(
      JournalpostRepository journalpostRepository,
      ElasticsearchClient esClient,
      InnsynskravDelRepository innsynskravDelRepository) {
    super();
    this.repository = journalpostRepository;
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
   * @param journalpost The Journalpost to index
   * @param shouldUpdateRelatives If true, update parent and children
   */
  @Override
  public void index(Journalpost journalpost, boolean shouldUpdateRelatives)
      throws EInnsynException {
    var journalpostES = journalpostService.entityToES(journalpost);

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
   * @param dto The DTO object
   * @param journalpost The Journalpost object
   * @return The Journalpost object
   */
  @Override
  protected Journalpost fromDTO(JournalpostDTO dto, Journalpost journalpost)
      throws EInnsynException {
    super.fromDTO(dto, journalpost);

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
      journalpost.setSkjerming(skjermingService.createOrReturnExisting(skjermingField));
    }

    // Update korrespondansepart
    var korrpartFieldList = dto.getKorrespondansepart();
    if (korrpartFieldList != null) {
      for (var korrpartField : korrpartFieldList) {
        journalpost.addKorrespondansepart(
            korrespondansepartService.createOrReturnExisting(korrpartField));
      }
    }

    // Update dokumentbeskrivelse
    var dokbeskFieldList = dto.getDokumentbeskrivelse();
    if (dokbeskFieldList != null) {
      for (var dokbeskField : dokbeskFieldList) {
        journalpost.addDokumentbeskrivelse(
            dokumentbeskrivelseService.createOrReturnExisting(dokbeskField));
      }
    }

    return journalpost;
  }

  /**
   * Convert a Journalpost to a JSON object.
   *
   * @param journalpost The Journalpost object
   * @param dto The JournalpostDTO object
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return The JournalpostDTO object
   */
  @Override
  protected JournalpostDTO toDTO(
      Journalpost journalpost, JournalpostDTO dto, Set<String> expandPaths, String currentPath) {

    super.toDTO(journalpost, dto, expandPaths, currentPath);

    dto.setJournalaar(journalpost.getJournalaar());
    dto.setJournalsekvensnummer(journalpost.getJournalsekvensnummer());
    dto.setJournalpostnummer(journalpost.getJournalpostnummer());
    dto.setJournalposttype(journalpost.getJournalposttype());
    dto.setSorteringstype(journalpost.getSorteringstype());

    if (journalpost.getJournaldato() != null) {
      dto.setJournaldato(journalpost.getJournaldato().toString());
    }
    if (journalpost.getDokumentdato() != null) {
      dto.setDokumentetsDato(journalpost.getDokumentdato().toString());
    }

    // Get administrativ enhet from korrespondansepart, or parent saksmappe
    dto.setAdministrativEnhet(journalpostService.getAdministrativEnhetKode(journalpost));

    // Administrativ enhet
    var administrativEnhetObjekt = journalpostService.getAdministrativEnhetObjekt(journalpost);
    dto.setAdministrativEnhetObjekt(
        enhetService.maybeExpand(
            administrativEnhetObjekt, "administrativEnhetObjekt", expandPaths, currentPath));

    // Skjerming
    dto.setSkjerming(
        skjermingService.maybeExpand(
            journalpost.getSkjerming(), "skjerming", expandPaths, currentPath));

    // Korrespondansepart
    dto.setKorrespondansepart(
        korrespondansepartService.maybeExpand(
            journalpost.getKorrespondansepart(), "korrespondansepart", expandPaths, currentPath));

    // Dokumentbeskrivelse
    dto.setDokumentbeskrivelse(
        dokumentbeskrivelseService.maybeExpand(
            journalpost.getDokumentbeskrivelse(), "dokumentbeskrivelse", expandPaths, currentPath));

    return dto;
  }

  /**
   * Create a ElasticSearch document from a Journalpost object.
   *
   * @param journalpost The Journalpost object
   * @return The JournalpostES object
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
    var administrativEnhetObjekt = journalpostService.getAdministrativEnhetObjekt(journalpost);
    var administrativEnhetTransitive = enhetService.getTransitiveEnhets(administrativEnhetObjekt);

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
    journalpostES.setArkivskaperSorteringNavn(arkivskaperNavn.getFirst());
    journalpostES.setArkivskaper(administrativEnhetObjekt.getIri());

    return journalpostES;
  }

  /**
   * Delete a Journalpost
   *
   * @param journalpost The Journalpost object
   */
  @Override
  protected void deleteEntity(Journalpost journalpost) throws EInnsynException {
    // Delete all korrespondanseparts
    var korrespondansepartList = journalpost.getKorrespondansepart();
    if (korrespondansepartList != null) {
      journalpost.setKorrespondansepart(null);
      for (var korrespondansepart : korrespondansepartList) {
        korrespondansepartService.delete(korrespondansepart.getId());
      }
    }

    // Unrelate all dokumentbeskrivelses
    var dokbeskList = journalpost.getDokumentbeskrivelse();
    if (dokbeskList != null) {
      journalpost.setDokumentbeskrivelse(List.of());
      for (var dokbesk : dokbeskList) {
        dokumentbeskrivelseService.deleteIfOrphan(dokbesk);
      }
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
      innsynskravDelService.delete(innsynskravDel.getId());
    }

    // Delete ES document
    try {
      esClient.delete(d -> d.index(elasticsearchIndex).id(journalpost.getId()));
    } catch (Exception e) {
      throw new EInnsynException("Could not delete journalpost from ElasticSearch", e);
    }

    super.deleteEntity(journalpost);
  }

  /**
   * Get custom paginator functions that filters by saksmappeId
   *
   * @param params The query parameters
   */
  @Override
  protected Paginators<Journalpost> getPaginators(BaseListQueryDTO params) {
    if (params instanceof JournalpostListQueryDTO p && p.getSaksmappeId() != null) {
      var saksmappe = saksmappeService.findById(p.getSaksmappeId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(saksmappe, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(saksmappe, pivot, pageRequest));
    }
    return super.getPaginators(params);
  }

  /**
   * Get the administrativ enhet kode for a Journalpost. First, look for a korrespondansepart with
   * erBehandlingsansvarlig = true, then fall back to the saksmappe's administrativEnhet.
   *
   * @param journalpostId The journalpost ID
   * @return The administrativ enhet kode
   */
  @Transactional
  public String getAdministrativEnhetKode(String journalpostId) {
    var journalpost = journalpostService.findById(journalpostId);
    return journalpostService.getAdministrativEnhetKode(journalpost);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public String getAdministrativEnhetKode(Journalpost journalpost) {
    var korrespondansepartList = journalpost.getKorrespondansepart();
    if (korrespondansepartList != null) {
      for (var korrespondansepart : korrespondansepartList) {
        if (korrespondansepart.isErBehandlingsansvarlig()) {
          return korrespondansepart.getAdministrativEnhet();
        }
      }
    }
    return journalpost.getSaksmappe().getAdministrativEnhet();
  }

  /**
   * Get the administrativ enhet object for a Journalpost. Get the administrativEnhetKode, and look
   * up the Enhet object.
   *
   * @param journalpost The journalpost ID
   * @return The administrativ enhet object
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public Enhet getAdministrativEnhetObjekt(Journalpost journalpost) {
    var enhetskode = journalpostService.getAdministrativEnhetKode(journalpost);
    var enhetObjekt = enhetService.findByEnhetskode(enhetskode, journalpost.getJournalenhet());
    if (enhetObjekt != null) {
      return enhetObjekt;
    }
    return journalpost.getSaksmappe().getAdministrativEnhetObjekt();
  }

  /**
   * Get the saksbehandler for a Journalpost. Look for a korrespondansepart with
   * erBehandlingsansvarlig = true.
   *
   * @param journalpostId The journalpost ID
   * @return The saksbehandler
   */
  @Transactional
  public String getSaksbehandler(String journalpostId) {
    var journalpost = journalpostService.findById(journalpostId);
    return journalpostService.getSaksbehandler(journalpost);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public String getSaksbehandler(Journalpost journalpost) {
    var korrespondansepartList = journalpost.getKorrespondansepart();
    if (korrespondansepartList != null) {
      for (var korrespondansepart : korrespondansepartList) {
        if (korrespondansepart.isErBehandlingsansvarlig()) {
          return korrespondansepart.getSaksbehandler();
        }
      }
    }
    return null;
  }

  /**
   * @param journalpostId The journalpost ID
   * @param query The query parameters
   * @return The list of Korrespondansepart objects
   */
  public ResultList<KorrespondansepartDTO> getKorrespondansepartList(
      String journalpostId, KorrespondansepartListQueryDTO query) throws EInnsynException {
    query.setJournalpostId(journalpostId);
    return korrespondansepartService.list(query);
  }

  /**
   * @param journalpostId The journalpost ID
   * @param dto The KorrespondansepartDTO object
   * @return The KorrespondansepartDTO object
   */
  public KorrespondansepartDTO addKorrespondansepart(
      String journalpostId, KorrespondansepartDTO dto) throws EInnsynException {
    dto.setParent(new KorrespondansepartParentDTO(journalpostId));
    return korrespondansepartService.add(dto);
  }

  /**
   * @param journalpostId The journalpost ID
   * @param query The query parameters
   * @return The list of Dokumentbeskrivelse objects
   */
  public ResultList<DokumentbeskrivelseDTO> getDokumentbeskrivelseList(
      String journalpostId, DokumentbeskrivelseListQueryDTO query) throws EInnsynException {
    query.setJournalpostId(journalpostId);
    return dokumentbeskrivelseService.list(query);
  }

  /**
   * @param journalpostId The journalpost ID
   * @param dto The DokumentbeskrivelseDTO object
   * @return The DokumentbeskrivelseDTO object
   */
  @Transactional
  public DokumentbeskrivelseDTO addDokumentbeskrivelse(
      String journalpostId, DokumentbeskrivelseDTO dto) throws EInnsynException {
    var addedDokumentbeskrivelseDTO = dokumentbeskrivelseService.add(dto);
    var journalpostDTO = new JournalpostDTO();
    journalpostDTO.setDokumentbeskrivelse(
        List.of(new ExpandableField<>(addedDokumentbeskrivelseDTO)));
    journalpostService.update(journalpostId, journalpostDTO);
    return addedDokumentbeskrivelseDTO;
  }

  /**
   * Unrelates a Dokumentbeskrivelse from a Journalpost. The Dokumentbeskrivelse is deleted if it is
   * orphaned after the unrelate.
   *
   * @param journalpostId The journalpost ID
   * @param dokumentbeskrivelseId The dokumentbeskrivelse ID
   * @return The JournalpostDTO object
   */
  @Transactional
  public JournalpostDTO deleteDokumentbeskrivelse(
      String journalpostId, String dokumentbeskrivelseId) throws EInnsynException {
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

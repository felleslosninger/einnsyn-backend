package no.einnsyn.apiv3.entities.saksmappe;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.paginators.Paginators;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseES;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostES;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostListQueryDTO;
import no.einnsyn.apiv3.entities.mappe.MappeService;
import no.einnsyn.apiv3.entities.registrering.models.RegistreringES;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeES;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeES.SaksmappeWithoutChildrenES;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeListQueryDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SaksmappeService extends MappeService<Saksmappe, SaksmappeDTO> {

  @Getter private final SaksmappeRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private SaksmappeService proxy;

  private final ElasticsearchClient esClient;

  @Value("${application.elasticsearchIndex}")
  private String elasticsearchIndex;

  public SaksmappeService(SaksmappeRepository repository, ElasticsearchClient esClient) {
    super();
    this.repository = repository;
    this.esClient = esClient;
  }

  public Saksmappe newObject() {
    return new Saksmappe();
  }

  public SaksmappeDTO newDTO() {
    return new SaksmappeDTO();
  }

  /**
   * Index the Saksmappe to ElasticSearch
   *
   * @param saksmappe The Saksmappe to index
   */
  @Override
  public boolean index(Saksmappe saksmappe, Instant timestamp) throws EInnsynException {
    // MappeService may update relatives (parent / children)
    if (!super.index(saksmappe, timestamp)) {
      return false;
    }

    // Serialize using Gson, to get custom serialization of ExpandedFields
    var saksmappeES = toLegacyES(saksmappe, new SaksmappeES());

    try {
      esClient.index(i -> i.index(elasticsearchIndex).id(saksmappe.getId()).document(saksmappeES));
    } catch (Exception e) {
      throw new EInnsynException("Could not index Saksmappe to ElasticSearch", e);
    }

    var journalposts = saksmappe.getJournalpost();
    if (journalposts != null) {
      for (var journalpost : journalposts) {
        try {
          journalpostService.index(journalpost, timestamp);
        } catch (Exception e) {
          log.error("Could not index child Journalpost to ElasticSearch", e);
        }
      }
    }

    return true;
  }

  /**
   * Convert a JSON object to Saksmappe
   *
   * @param dto The JSON object to convert from
   * @param saksmappe The Saksmappe to convert to
   * @return The converted Saksmappe
   */
  @Override
  protected Saksmappe fromDTO(SaksmappeDTO dto, Saksmappe saksmappe) throws EInnsynException {
    super.fromDTO(dto, saksmappe);

    if (dto.getSaksaar() != null) {
      saksmappe.setSaksaar(dto.getSaksaar());
    }

    if (dto.getSakssekvensnummer() != null) {
      saksmappe.setSakssekvensnummer(dto.getSakssekvensnummer());
    }

    if (dto.getSaksdato() != null) {
      saksmappe.setSaksdato(LocalDate.parse(dto.getSaksdato()));
    }

    if (dto.getAdministrativEnhet() != null) {
      saksmappe.setAdministrativEnhet(dto.getAdministrativEnhet());
    }

    // Workaround since legacy IDs are used for relations. OneToMany relations fails if the ID is
    // not set.
    if (saksmappe.getId() == null) {
      saksmappe = repository.saveAndFlush(saksmappe);
    }

    // Add journalposts
    var journalpostFieldList = dto.getJournalpost();
    if (journalpostFieldList != null) {
      for (var journalpostField : journalpostFieldList) {
        journalpostField
            .requireExpandedObject()
            .setSaksmappe(new ExpandableField<>(saksmappe.getId()));
        var journalpost = journalpostService.createOrThrow(journalpostField);
        saksmappe.addJournalpost(journalpost);
      }
    }

    // Look up administrativEnhet
    var administrativEnhet = dto.getAdministrativEnhet();
    if (administrativEnhet != null) {
      saksmappe.setAdministrativEnhet(administrativEnhet);
      var journalenhet = saksmappe.getJournalenhet();
      var administrativEnhetObjekt =
          enhetService.findByEnhetskode(dto.getAdministrativEnhet(), journalenhet);
      if (administrativEnhetObjekt != null) {
        saksmappe.setAdministrativEnhetObjekt(administrativEnhetObjekt);
      }
    }

    return saksmappe;
  }

  /**
   * Convert a Saksmappe to a JSON object
   *
   * @param saksmappe The Saksmappe to convert from
   * @param dto The JSON object to convert to
   * @param expandPaths A list of paths to expand. Un-expanded objects will be shown as IDs
   * @param currentPath The current path in the object tree
   * @return The converted JSON object
   */
  @Override
  protected SaksmappeDTO toDTO(
      Saksmappe saksmappe, SaksmappeDTO dto, Set<String> expandPaths, String currentPath) {

    super.toDTO(saksmappe, dto, expandPaths, currentPath);

    dto.setSaksaar(saksmappe.getSaksaar());
    dto.setSakssekvensnummer(saksmappe.getSakssekvensnummer());
    dto.setSaksnummer(saksmappe.getSaksaar() + "/" + saksmappe.getSakssekvensnummer());
    if (saksmappe.getSaksdato() != null) {
      dto.setSaksdato(saksmappe.getSaksdato().toString());
    }
    dto.setAdministrativEnhet(saksmappe.getAdministrativEnhet());

    // AdministrativEnhetObjekt
    dto.setAdministrativEnhetObjekt(
        enhetService.maybeExpand(
            saksmappe.getAdministrativEnhetObjekt(),
            "administrativEnhetObjekt",
            expandPaths,
            currentPath));

    // Journalposts
    dto.setJournalpost(
        journalpostService.maybeExpand(
            saksmappe.getJournalpost(), "journalpost", expandPaths, currentPath));

    return dto;
  }

  // Build a legacy ElasticSearch document, used by the old API / frontend
  @Override
  public BaseES toLegacyES(Saksmappe object, BaseES es) {
    super.toLegacyES(object, es);
    if (es instanceof SaksmappeES saksmappeES) {
      var saksaar = "" + object.getSaksaar();
      var saksaarShort = "" + (object.getSaksaar() % 100);
      var sakssekvensnummer = "" + object.getSakssekvensnummer();

      if (object.getSaksdato() != null) {
        saksmappeES.setSaksdato(object.getSaksdato().toString());
      }
      saksmappeES.setSaksaar(saksaar);
      saksmappeES.setSakssekvensnummer(sakssekvensnummer);
      saksmappeES.setSaksnummer(saksaar + "/" + sakssekvensnummer);
      saksmappeES.setSaksnummerGenerert(
          List.of(
              saksaar + "/" + sakssekvensnummer,
              saksaarShort + "/" + sakssekvensnummer,
              sakssekvensnummer + "/" + saksaar,
              sakssekvensnummer + "/" + saksaarShort));

      // Add children if not a SaksmappeWithoutChildrenES
      if (!(saksmappeES instanceof SaksmappeWithoutChildrenES)) {
        var journalpostList = object.getJournalpost();
        if (journalpostList != null) {
          saksmappeES.setChild(
              journalpostList.stream()
                  .map(j -> (RegistreringES) journalpostService.toLegacyES(j, new JournalpostES()))
                  .toList());
        }
      }

      // Sorteringstype
      saksmappeES.setSorteringstype("sak");
    }
    return es;
  }

  /**
   * Delete a Saksmappe, all it's children, and the ES document
   *
   * @param saksmappe The Saksmappe to delete
   */
  @Override
  protected void deleteEntity(Saksmappe saksmappe) throws EInnsynException {
    // Delete all journalposts
    var journalpostList = saksmappe.getJournalpost();
    if (journalpostList != null) {
      saksmappe.setJournalpost(null);
      for (var journalpost : journalpostList) {
        journalpostService.delete(journalpost.getId());
      }
    }

    // Delete ES document
    try {
      esClient.delete(d -> d.index(elasticsearchIndex).id(saksmappe.getId()));
    } catch (Exception e) {
      throw new EInnsynException("Could not delete Saksmappe from ElasticSearch", e);
    }

    super.deleteEntity(saksmappe);
  }

  /**
   * Get custom paginator functions that filters by saksmappeId
   *
   * @param params The list query parameters
   */
  @Override
  protected Paginators<Saksmappe> getPaginators(BaseListQueryDTO params) {
    if (params instanceof SaksmappeListQueryDTO p) {
      var arkivId = p.getArkivId();
      if (arkivId != null) {
        var arkiv = arkivService.findById(arkivId);
        return new Paginators<>(
            (pivot, pageRequest) -> repository.paginateAsc(arkiv, pivot, pageRequest),
            (pivot, pageRequest) -> repository.paginateDesc(arkiv, pivot, pageRequest));
      }

      var arkivdelId = p.getArkivdelId();
      if (arkivdelId != null) {
        var arkivdel = arkivdelService.findById(arkivdelId);
        return new Paginators<>(
            (pivot, pageRequest) -> repository.paginateAsc(arkivdel, pivot, pageRequest),
            (pivot, pageRequest) -> repository.paginateDesc(arkivdel, pivot, pageRequest));
      }

      var klasseId = p.getKlasseId();
      if (klasseId != null) {
        var klasse = klasseService.findById(klasseId);
        return new Paginators<>(
            (pivot, pageRequest) -> repository.paginateAsc(klasse, pivot, pageRequest),
            (pivot, pageRequest) -> repository.paginateDesc(klasse, pivot, pageRequest));
      }
    }

    return super.getPaginators(params);
  }

  /**
   * @param saksmappeId The ID of the Saksmappe
   * @param query The list query parameters
   * @return The list of Journalposts
   */
  public ResultList<JournalpostDTO> getJournalpostList(
      String saksmappeId, JournalpostListQueryDTO query) throws EInnsynException {
    query.setSaksmappeId(saksmappeId);
    return journalpostService.list(query);
  }

  /**
   * @param saksmappeId The ID of the Saksmappe
   * @param journalpostDTO The Journalpost to add
   * @return The added Journalpost
   */
  public JournalpostDTO addJournalpost(String saksmappeId, JournalpostDTO journalpostDTO)
      throws EInnsynException {
    journalpostDTO.setSaksmappe(new ExpandableField<>(saksmappeId));

    return journalpostService.add(journalpostDTO);
  }
}

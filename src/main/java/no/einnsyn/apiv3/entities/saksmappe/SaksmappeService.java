package no.einnsyn.apiv3.entities.saksmappe;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.google.gson.Gson;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostListQueryDTO;
import no.einnsyn.apiv3.entities.mappe.MappeService;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeES;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaksmappeService extends MappeService<Saksmappe, SaksmappeDTO> {

  @Getter private final SaksmappeRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private SaksmappeService proxy;

  private final Gson gson;
  private final ElasticsearchClient esClient;

  @Value("${application.elasticsearchIndex}")
  private String elasticsearchIndex;

  public SaksmappeService(Gson gson, SaksmappeRepository repository, ElasticsearchClient esClient) {
    super();
    this.gson = gson;
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
   * @param saksmappe
   * @return
   */
  @Override
  public void index(Saksmappe saksmappe, boolean shouldUpdateRelatives) throws EInnsynException {
    var saksmappeES = saksmappeService.entityToES(saksmappe);

    // MappeService may update relatives (parent / children)
    super.index(saksmappe, shouldUpdateRelatives);

    // Serialize using Gson, to get custom serialization of ExpandedFields
    var source = gson.toJson(saksmappeES);
    var jsonObject = gson.fromJson(source, JSONObject.class);
    try {
      // restClient.performRequest(null)
      esClient.index(i -> i.index(elasticsearchIndex).id(saksmappe.getId()).document(jsonObject));
    } catch (Exception e) {
      throw new EInnsynException("Could not index Saksmappe to ElasticSearch", e);
    }

    if (shouldUpdateRelatives) {
      var journalposts = saksmappe.getJournalpost();
      if (journalposts != null) {
        for (var journalpost : journalposts) {
          journalpostService.index(journalpost, false);
        }
      }
    }
  }

  /**
   * Convert a JSON object to Saksmappe
   *
   * @param dto
   * @param saksmappe
   * @param paths A list of paths containing new objects that will be created from this update
   * @param currentPath The current path in the object tree
   * @return
   */
  @Override
  public Saksmappe fromDTO(
      SaksmappeDTO dto, Saksmappe saksmappe, Set<String> paths, String currentPath)
      throws EInnsynException {
    super.fromDTO(dto, saksmappe, paths, currentPath);

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

    // Workaround since legacy IDs are used for relations. OneToMany relations (saksmappe ->
    // journalpost) fails if the ID is not set.
    if (saksmappe.getId() == null) {
      saksmappe = repository.saveAndFlush(saksmappe);
    }

    // Add journalposts
    var journalpostFieldList = dto.getJournalpost();
    if (journalpostFieldList != null) {
      for (var journalpostField : journalpostFieldList) {
        Journalpost journalpost = null;
        if (journalpostField.getId() != null) {
          journalpost = journalpostService.findById(journalpostField.getId());
          if (journalpost == null) {
            throw new EInnsynException("Journalpost " + journalpostField.getId() + " not found");
          }
        } else {
          var journalpostPath =
              currentPath.isEmpty() ? "journalpost" : currentPath + ".journalpost";
          var journalpostDTO = journalpostField.getExpandedObject();
          journalpostDTO.setSaksmappe(new ExpandableField<>(saksmappe.getId()));
          paths.add(journalpostPath);
          journalpost = journalpostService.fromDTO(journalpostDTO, paths, journalpostPath);
          if (journalpost == null) {
            throw new EInnsynException("Could not create Journalpost from DTO");
          }
        }
        // If no administrativEnhet is given for journalpost, set it to the saksmappe's
        if (journalpost.getAdministrativEnhet() == null) {
          journalpost.setAdministrativEnhet(saksmappe.getAdministrativEnhet());
          journalpost.setAdministrativEnhetObjekt(saksmappe.getAdministrativEnhetObjekt());
        }
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
   * @param saksmappe
   * @param dto
   * @param expandPaths A list of paths to expand. Un-expanded objects will be shown as IDs
   * @param currentPath The current path in the object tree
   * @return
   */
  @Override
  public SaksmappeDTO toDTO(
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
    var administrativEnhetObjekt = saksmappe.getAdministrativEnhetObjekt();
    if (administrativEnhetObjekt != null) {
      dto.setAdministrativEnhetObjekt(
          enhetService.maybeExpand(
              administrativEnhetObjekt, "administrativEnhetObjekt", expandPaths, currentPath));
    }

    // Journalposts
    var journalpostListDTO = dto.getJournalpost();
    if (journalpostListDTO == null) {
      journalpostListDTO = new ArrayList<>();
      dto.setJournalpost(journalpostListDTO);
    }
    var journalpostList = saksmappe.getJournalpost();
    if (journalpostList != null) {
      for (var journalpost : journalpostList) {
        journalpostListDTO.add(
            journalpostService.maybeExpand(journalpost, "journalpost", expandPaths, currentPath));
      }
    }

    return dto;
  }

  /**
   * Convert a Saksmappe to an ES document
   *
   * @param saksmappe
   * @param saksmappeES
   * @return
   */
  public SaksmappeES entityToES(Saksmappe saksmappe) {
    var saksmappeES = new SaksmappeES();

    saksmappeService.toDTO(saksmappe, saksmappeES, new HashSet<>(), "");

    // Legacy, this field name is used in the old front-end.
    saksmappeES.setOffentligTittel_SENSITIV(saksmappe.getOffentligTittelSensitiv());

    // Generate list of saksår / saksnummer in different formats
    // YYYY/N
    // YY/N
    // N/YYYY
    // N/YY
    var saksaar = saksmappe.getSaksaar();
    var saksaarShort = saksaar % 100;
    var sakssekvensnummer = saksmappe.getSakssekvensnummer();
    saksmappeES.setSaksnummerGenerert(
        List.of(
            saksaar + "/" + sakssekvensnummer,
            saksaarShort + "/" + sakssekvensnummer,
            sakssekvensnummer + "/" + saksaar,
            sakssekvensnummer + "/" + saksaarShort));

    // Find list of ancestors
    var administrativEnhet = saksmappe.getAdministrativEnhetObjekt();
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
    // Legacy fields
    saksmappeES.setArkivskaperTransitive(arkivskaperTransitive);
    saksmappeES.setArkivskaperNavn(arkivskaperNavn);
    saksmappeES.setArkivskaperSorteringNavn(arkivskaperNavn.get(0));
    saksmappeES.setArkivskaper(saksmappe.getAdministrativEnhetObjekt().getIri());

    // TODO: Create child documents for pageviews, innsynskrav, document clicks?

    return saksmappeES;
  }

  /**
   * Delete a Saksmappe, all it's children, and the ES document
   *
   * @param saksmappe
   */
  @Transactional
  public SaksmappeDTO delete(Saksmappe saksmappe) throws EInnsynException {
    var dto = proxy.toDTO(saksmappe);
    dto.setDeleted(true);

    // Delete all journalposts
    var journalposts = saksmappe.getJournalpost();
    if (journalposts != null) {
      saksmappe.setJournalpost(List.of());
      for (var journalpost : journalposts) {
        journalpostService.delete(journalpost);
      }
    }

    // Delete saksmappe
    repository.delete(saksmappe);

    // Delete ES document
    try {
      esClient.delete(d -> d.index(elasticsearchIndex).id(dto.getId()));
    } catch (Exception e) {
      throw new EInnsynException("Could not delete Saksmappe from ElasticSearch", e);
    }

    return dto;
  }

  /**
   * @param saksmappeId
   * @param query
   * @return
   */
  public ResultList<JournalpostDTO> getJournalpostList(
      String saksmappeId, JournalpostListQueryDTO query) {
    query.setSaksmappe(saksmappeId);
    return journalpostService.list(query);
  }

  /**
   * @param saksmappeId
   * @param journalpostDTO
   * @return
   */
  public JournalpostDTO addJournalpost(String saksmappeId, JournalpostDTO journalpostDTO)
      throws EInnsynException {
    journalpostDTO.setSaksmappe(new ExpandableField<>(saksmappeId));
    return journalpostService.add(journalpostDTO);
  }

  /**
   * Removing a journalpost from a saksmappe will delete the journalpost
   *
   * @param saksmappeId
   * @param journalpostId
   * @return
   */
  @Transactional
  public SaksmappeDTO removeJournalpostFromSaksmappe(String saksmappeId, String journalpostId)
      throws EInnsynException {
    journalpostService.delete(journalpostId);
    var saksmappe = saksmappeService.findById(saksmappeId);
    return saksmappeService.toDTO(saksmappe);
  }
}

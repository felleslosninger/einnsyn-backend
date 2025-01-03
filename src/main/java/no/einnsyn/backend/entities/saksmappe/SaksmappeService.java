package no.einnsyn.backend.entities.saksmappe;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.paginators.Paginators;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.ListResponseBody;
import no.einnsyn.backend.entities.arkiv.models.ListByArkivParameters;
import no.einnsyn.backend.entities.arkivdel.models.ListByArkivdelParameters;
import no.einnsyn.backend.entities.base.models.BaseES;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.klasse.models.ListByKlasseParameters;
import no.einnsyn.backend.entities.lagretsak.LagretSakRepository;
import no.einnsyn.backend.entities.mappe.MappeService;
import no.einnsyn.backend.entities.saksmappe.models.ListBySaksmappeParameters;
import no.einnsyn.backend.entities.saksmappe.models.Saksmappe;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeES;
import no.einnsyn.backend.error.exceptions.EInnsynException;
import no.einnsyn.backend.utils.TimeConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SaksmappeService extends MappeService<Saksmappe, SaksmappeDTO> {

  @Getter private final SaksmappeRepository repository;

  private final LagretSakRepository lagretSakRepository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private SaksmappeService proxy;

  public SaksmappeService(SaksmappeRepository repository, LagretSakRepository lagretSakRepository) {
    super();
    this.repository = repository;
    this.lagretSakRepository = lagretSakRepository;
  }

  public Saksmappe newObject() {
    return new Saksmappe();
  }

  public SaksmappeDTO newDTO() {
    return new SaksmappeDTO();
  }

  /**
   * Override scheduleIndex to reindex the parent Saksmappe.
   *
   * @param saksmappe
   * @param recurseDirection -1 for parents, 1 for children, 0 for both
   */
  @Override
  public void scheduleIndex(Saksmappe saksmappe, int recurseDirection) {
    super.scheduleIndex(saksmappe, recurseDirection);

    if (recurseDirection >= 0 && saksmappe.getJournalpost() != null) {
      for (var journalpost : saksmappe.getJournalpost()) {
        journalpostService.scheduleIndex(journalpost, 1);
      }
    }
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

    // Fallback to journalenhet for administrativEnhetObjekt
    if (saksmappe.getAdministrativEnhetObjekt() == null) {
      saksmappe.setAdministrativEnhetObjekt(saksmappe.getJournalenhet());
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

  @Override
  public BaseES toLegacyES(Saksmappe saksmappe) {
    return toLegacyES(saksmappe, new SaksmappeES());
  }

  @Override
  public BaseES toLegacyES(Saksmappe saksmappe, BaseES es) {
    super.toLegacyES(saksmappe, es);
    if (es instanceof SaksmappeES saksmappeES) {
      var saksaar = "" + saksmappe.getSaksaar();
      var saksaarShort = saksaar.substring(2);
      var sakssekvensnummer = "" + saksmappe.getSakssekvensnummer();

      if (saksmappe.getSaksdato() != null) {
        saksmappeES.setSaksdato(saksmappe.getSaksdato().toString());
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
      saksmappeES.setChild(Collections.emptyList());

      // StandardDato
      saksmappeES.setStandardDato(
          TimeConverter.generateStandardDato(
              saksmappe.getSaksdato(), saksmappe.getPublisertDato()));

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

    // Delete all LagretSak
    var lagretSakStream = lagretSakRepository.findBySaksmappe(saksmappe.getId());
    var lagretSakIterator = lagretSakStream.iterator();
    while (lagretSakIterator.hasNext()) {
      var lagretSak = lagretSakIterator.next();
      lagretSakRepository.delete(lagretSak);
    }

    super.deleteEntity(saksmappe);
  }

  /**
   * Get custom paginator functions that filters by saksmappeId
   *
   * @param params The list query parameters
   */
  @Override
  protected Paginators<Saksmappe> getPaginators(ListParameters params) {
    if (params instanceof ListByArkivParameters p) {
      var arkivId = p.getArkivId();
      if (arkivId != null) {
        var arkiv = arkivService.findById(arkivId);
        return new Paginators<>(
            (pivot, pageRequest) -> repository.paginateAsc(arkiv, pivot, pageRequest),
            (pivot, pageRequest) -> repository.paginateDesc(arkiv, pivot, pageRequest));
      }
    }

    if (params instanceof ListByArkivdelParameters p) {
      var arkivdelId = p.getArkivdelId();
      if (arkivdelId != null) {
        var arkivdel = arkivdelService.findById(arkivdelId);
        return new Paginators<>(
            (pivot, pageRequest) -> repository.paginateAsc(arkivdel, pivot, pageRequest),
            (pivot, pageRequest) -> repository.paginateDesc(arkivdel, pivot, pageRequest));
      }
    }

    if (params instanceof ListByKlasseParameters p) {
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
  public ListResponseBody<JournalpostDTO> listJournalpost(
      String saksmappeId, ListBySaksmappeParameters query) throws EInnsynException {
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

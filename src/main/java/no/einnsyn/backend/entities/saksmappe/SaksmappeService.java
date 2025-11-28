package no.einnsyn.backend.entities.saksmappe;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.paginators.Paginators;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.arkivdel.models.ListByArkivdelParameters;
import no.einnsyn.backend.entities.base.models.BaseES;
import no.einnsyn.backend.entities.journalpost.JournalpostRepository;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.klasse.models.ListByKlasseParameters;
import no.einnsyn.backend.entities.lagretsak.LagretSakRepository;
import no.einnsyn.backend.entities.mappe.MappeService;
import no.einnsyn.backend.entities.saksmappe.models.ListBySaksmappeParameters;
import no.einnsyn.backend.entities.saksmappe.models.Saksmappe;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeES;
import no.einnsyn.backend.utils.TimeConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SaksmappeService extends MappeService<Saksmappe, SaksmappeDTO> {

  @Getter private final SaksmappeRepository repository;

  private final LagretSakRepository lagretSakRepository;
  private final JournalpostRepository journalpostRepository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private SaksmappeService proxy;

  public SaksmappeService(
      SaksmappeRepository repository,
      LagretSakRepository lagretSakRepository,
      JournalpostRepository journalpostRepository) {
    super();
    this.repository = repository;
    this.lagretSakRepository = lagretSakRepository;
    this.journalpostRepository = journalpostRepository;
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
  public boolean scheduleIndex(String saksmappeId, int recurseDirection) {
    var isScheduled = super.scheduleIndex(saksmappeId, recurseDirection);

    if (recurseDirection >= 0 && !isScheduled) {
      try (var journalpostStream = journalpostRepository.streamIdBySaksmappeId(saksmappeId)) {
        journalpostStream.forEach(id -> journalpostService.scheduleIndex(id, 1));
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
        if (journalpostField.getExpandedObject() != null) {
          journalpostField
              .getExpandedObject()
              .setSaksmappe(new ExpandableField<>(saksmappe.getId()));
        }

        var journalpost = journalpostService.createOrReturnExisting(journalpostField);
        journalpost.setSaksmappe(saksmappe);
      }
    }

    var slugBase = getSlugBase(saksmappe);
    saksmappe = setSlug(saksmappe, slugBase);

    return saksmappe;
  }

  @Override
  public String getSlugBase(Saksmappe saksmappe) {
    if (saksmappe.getSaksaar() != null && saksmappe.getSakssekvensnummer() != null) {
      var saksaar = saksmappe.getSaksaar();
      var sakssekvensnummer = saksmappe.getSakssekvensnummer();
      return saksaar + "-" + sakssekvensnummer + "-" + saksmappe.getOffentligTittel();
    } else if (saksmappe.getSaksaar() != null) {
      var saksaar = saksmappe.getSaksaar();
      return saksaar + "-" + saksmappe.getOffentligTittel();
    } else {
      return saksmappe.getOffentligTittel();
    }
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
    try (var journalpostIdStream = journalpostRepository.streamIdBySaksmappeId(saksmappe.getId())) {
      var journalpostIdIterator = journalpostIdStream.iterator();
      while (journalpostIdIterator.hasNext()) {
        journalpostService.delete(journalpostIdIterator.next());
      }
    }

    // Delete all LagretSak
    try (var lagretSakIdStream = lagretSakRepository.streamIdBySaksmappeId(saksmappe.getId())) {
      var lagretSakIdIterator = lagretSakIdStream.iterator();
      while (lagretSakIdIterator.hasNext()) {
        lagretSakService.delete(lagretSakIdIterator.next());
      }
    }

    super.deleteEntity(saksmappe);
  }

  /**
   * Get custom paginator functions that filters by saksmappeId
   *
   * @param params The list query parameters
   */
  @Override
  protected Paginators<Saksmappe> getPaginators(ListParameters params) throws EInnsynException {
    if (params instanceof ListByArkivdelParameters p) {
      var arkivdelId = p.getArkivdelId();
      if (arkivdelId != null) {
        var arkivdel = arkivdelService.findByIdOrThrow(arkivdelId);
        return new Paginators<>(
            (pivot, pageRequest) -> repository.paginateAsc(arkivdel, pivot, pageRequest),
            (pivot, pageRequest) -> repository.paginateDesc(arkivdel, pivot, pageRequest));
      }
    }

    if (params instanceof ListByKlasseParameters p) {
      var klasseId = p.getKlasseId();
      if (klasseId != null) {
        var klasse = klasseService.findByIdOrThrow(klasseId);
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
  public PaginatedList<JournalpostDTO> listJournalpost(
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

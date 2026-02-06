package no.einnsyn.backend.entities.moetemappe;

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
import no.einnsyn.backend.entities.klasse.models.ListByKlasseParameters;
import no.einnsyn.backend.entities.lagretsak.LagretSakRepository;
import no.einnsyn.backend.entities.mappe.MappeService;
import no.einnsyn.backend.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.backend.entities.moetedokument.models.MoetedokumentES;
import no.einnsyn.backend.entities.moetemappe.models.ListByMoetemappeParameters;
import no.einnsyn.backend.entities.moetemappe.models.Moetemappe;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeES;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeES.MoetemappeWithoutChildrenES;
import no.einnsyn.backend.entities.moetesak.MoetesakRepository;
import no.einnsyn.backend.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.backend.entities.registrering.models.RegistreringES;
import no.einnsyn.backend.utils.TimeConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MoetemappeService extends MappeService<Moetemappe, MoetemappeDTO> {

  @Getter(onMethod_ = @Override)
  private final MoetemappeRepository repository;

  private final MoetesakRepository moetesakRepository;
  private final LagretSakRepository lagretSakRepository;

  @SuppressWarnings("java:S6813")
  @Getter(onMethod_ = @Override)
  @Lazy
  @Autowired
  private MoetemappeService proxy;

  public MoetemappeService(
      MoetemappeRepository repository,
      MoetesakRepository moetesakRepository,
      LagretSakRepository lagretSakRepository) {
    this.repository = repository;
    this.moetesakRepository = moetesakRepository;
    this.lagretSakRepository = lagretSakRepository;
  }

  @Override
  public Moetemappe newObject() {
    return new Moetemappe();
  }

  @Override
  public MoetemappeDTO newDTO() {
    return new MoetemappeDTO();
  }

  /**
   * Override scheduleIndex to reindex the parent Moetemappe.
   *
   * @param moetemappeId the ID of the moetemappe
   * @param recurseDirection -1 for parents, 1 for children, 0 for both
   */
  @Override
  public boolean scheduleIndex(String moetemappeId, int recurseDirection) {
    var isScheduled = super.scheduleIndex(moetemappeId, recurseDirection);

    if (recurseDirection >= 0 && !isScheduled) {
      try (var moetesakStream = moetesakRepository.streamIdByMoetemappeId(moetemappeId)) {
        moetesakStream.forEach(id -> moetesakService.scheduleIndex(id, 1));
      }
    }

    return true;
  }

  @Override
  protected Moetemappe fromDTO(MoetemappeDTO dto, Moetemappe moetemappe) throws EInnsynException {
    super.fromDTO(dto, moetemappe);

    if (dto.getMoetenummer() != null) {
      moetemappe.setMoetenummer(dto.getMoetenummer());
    }

    if (dto.getMoetedato() != null) {
      moetemappe.setMoetedato(TimeConverter.timestampToInstant(dto.getMoetedato()));
    }

    if (dto.getMoetested() != null) {
      moetemappe.setMoetested(dto.getMoetested());
    }

    // Look up Enhet for "utvalg", if given
    var utvalgKode = dto.getUtvalg();
    if (utvalgKode != null) {
      moetemappe.setUtvalg(utvalgKode);
      var journalenhet = moetemappe.getJournalenhet();
      var utvalg = enhetService.findByEnhetskode(utvalgKode, journalenhet);
      if (utvalg != null) {
        moetemappe.setUtvalgObjekt(utvalg);
      }
    }

    // Fallback to journalenhet for "utvalg"
    if (moetemappe.getUtvalgObjekt() == null) {
      moetemappe.setUtvalgObjekt(moetemappe.getJournalenhet());
    }

    if (dto.getVideoLink() != null) {
      moetemappe.setVideolink(dto.getVideoLink());
    }

    // Workaround since legacy IDs are used for relations. OneToMany relations fails if the ID is
    // not set.
    if (moetemappe.getId() == null) {
      moetemappe = repository.saveAndFlush(moetemappe);
    }

    // Add Moetesak
    var moetesakFieldList = dto.getMoetesak();
    if (moetesakFieldList != null) {
      for (var moetesakField : moetesakFieldList) {
        var moetesak = moetesakService.createOrReturnExisting(moetesakField);
        moetemappe.addMoetesak(moetesak);
      }
    }

    // Add Moetedokument
    var moetedokumentFieldList = dto.getMoetedokument();
    if (moetedokumentFieldList != null) {
      for (var moetedokumentField : moetedokumentFieldList) {
        var moetedokument = moetedokumentService.createOrReturnExisting(moetedokumentField);
        moetedokument.setMoetemappe(moetemappe);
        moetemappe.addMoetedokument(moetedokument);
      }
    }

    // Add referanseForrigeMoete
    var referanseForrigeMoeteField = dto.getReferanseForrigeMoete();
    if (referanseForrigeMoeteField != null) {
      var forrigeMoete = moetemappeService.findByIdOrThrow(referanseForrigeMoeteField.getId());
      moetemappe.setReferanseForrigeMoete(forrigeMoete);
      forrigeMoete.setReferanseNesteMoete(moetemappe);
    }

    // Add referanseNesteMoete
    var referanseNesteMoeteField = dto.getReferanseNesteMoete();
    if (referanseNesteMoeteField != null) {
      var nesteMoete = moetemappeService.findByIdOrThrow(referanseNesteMoeteField.getId());
      moetemappe.setReferanseNesteMoete(nesteMoete);
      nesteMoete.setReferanseForrigeMoete(moetemappe);
    }

    var slugBase = getSlugBase(moetemappe);
    moetemappe = setSlug(moetemappe, slugBase);

    return moetemappe;
  }

  @Override
  public String getSlugBase(Moetemappe moetemappe) {
    if (moetemappe.getMoetedato() != null && moetemappe.getMoetenummer() != null) {
      var dateTime = TimeConverter.instantToZonedDateTime(moetemappe.getMoetedato());
      var moetenummer = moetemappe.getMoetenummer();
      return dateTime.getYear() + "-" + moetenummer + "-" + moetemappe.getOffentligTittel();
    } else if (moetemappe.getMoetedato() != null) {
      var dateTime = TimeConverter.instantToZonedDateTime(moetemappe.getMoetedato());
      return dateTime.getYear() + "-" + moetemappe.getOffentligTittel();
    }
    return moetemappe.getOffentligTittel();
  }

  @Override
  protected MoetemappeDTO toDTO(
      Moetemappe object, MoetemappeDTO dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(object, dto, expandPaths, currentPath);

    dto.setMoetenummer(object.getMoetenummer());
    dto.setMoetested(object.getMoetested());
    dto.setVideoLink(object.getVideolink());
    dto.setUtvalg(object.getUtvalg());

    if (object.getMoetedato() != null) {
      dto.setMoetedato(object.getMoetedato().toString());
    }

    // Utvalg
    dto.setUtvalgObjekt(
        enhetService.maybeExpand(
            object.getUtvalgObjekt(), "utvalgObjekt", expandPaths, currentPath));

    // Moetesak
    dto.setMoetesak(
        moetesakService.maybeExpand(object.getMoetesak(), "moetesak", expandPaths, currentPath));

    // Moetedokument
    dto.setMoetedokument(
        moetedokumentService.maybeExpand(
            object.getMoetedokument(), "moetedokument", expandPaths, currentPath));

    // ReferanseForrigeMoete
    dto.setReferanseForrigeMoete(
        moetemappeService.maybeExpand(
            object.getReferanseForrigeMoete(), "referanseForrigeMoete", expandPaths, currentPath));

    // ReferanseNesteMoete
    dto.setReferanseNesteMoete(
        moetemappeService.maybeExpand(
            object.getReferanseNesteMoete(), "referanseNesteMoete", expandPaths, currentPath));

    return dto;
  }

  @Override
  public BaseES toLegacyES(Moetemappe object) {
    return toLegacyES(object, new MoetemappeES());
  }

  @Override
  public BaseES toLegacyES(Moetemappe moetemappe, BaseES es) {
    super.toLegacyES(moetemappe, es);
    if (es instanceof MoetemappeES moetemappeES) {
      moetemappeES.setUtvalg(moetemappe.getUtvalg());
      moetemappeES.setMoetested(moetemappe.getMoetested());
      moetemappeES.setSorteringstype("politisk møte");
      if (moetemappe.getMoetedato() != null) {
        moetemappeES.setMoetedato(moetemappe.getMoetedato().toString());
      }

      // Add children if not a MoetemappeWithoutChildrenES
      if (!(moetemappeES instanceof MoetemappeWithoutChildrenES)) {
        var children = moetemappe.getMoetedokument();
        if (children != null) {
          moetemappeES.setChild(
              children.stream()
                  .map(
                      md ->
                          (RegistreringES)
                              moetedokumentService.toLegacyES(md, new MoetedokumentES()))
                  .toList());
        } else {
          moetemappeES.setChild(List.of());
        }

        // Set fulltext to true if any Moetedokument has a fulltext file
        for (var registrering : moetemappeES.getChild()) {
          if (registrering instanceof MoetedokumentES moetedokument && moetedokument.isFulltext()) {
            moetemappeES.setFulltext(true);
            break;
          }
        }
      }

      // StandardDato
      moetemappeES.setStandardDato(
          TimeConverter.generateStandardDato(
              moetemappe.getMoetedato(), moetemappe.getPublisertDato()));
    }
    return es;
  }

  @Override
  protected void deleteEntity(Moetemappe moetemappe) throws EInnsynException {
    // Delete Moetesak
    var moetesakList = moetemappe.getMoetesak();
    if (moetesakList != null) {
      moetemappe.setMoetesak(null);
      for (var moetesak : moetesakList) {
        moetesakService.delete(moetesak.getId());
      }
    }

    // Delete Moetedokument
    var moetedokumentList = moetemappe.getMoetedokument();
    if (moetedokumentList != null) {
      moetemappe.setMoetedokument(null);
      for (var moetedokument : moetedokumentList) {
        moetedokumentService.delete(moetedokument.getId());
      }
    }

    // Remove referanseForrigeMoete
    var referanseForrigeMoete = moetemappe.getReferanseForrigeMoete();
    if (referanseForrigeMoete != null) {
      referanseForrigeMoete.setReferanseNesteMoete(null);
    }

    // Remove referanseNesteMoete
    var referanseNesteMoete = moetemappe.getReferanseNesteMoete();
    if (referanseNesteMoete != null) {
      referanseNesteMoete.setReferanseForrigeMoete(null);
    }

    // Delete all LagretSak
    try (var lagretSakIdStream = lagretSakRepository.streamIdByMoetemappeId(moetemappe.getId())) {
      var lagretSakIdIterator = lagretSakIdStream.iterator();
      while (lagretSakIdIterator.hasNext()) {
        lagretSakService.delete(lagretSakIdIterator.next());
      }
    }

    super.deleteEntity(moetemappe);
  }

  /**
   * Get custom paginator functions that filters by moetemappeId
   *
   * @param params The list query parameters
   */
  @Override
  protected Paginators<Moetemappe> getPaginators(ListParameters params) throws EInnsynException {
    if (params instanceof ListByArkivdelParameters p && p.getArkivdelId() != null) {
      var arkivdel = arkivdelService.findByIdOrThrow(p.getArkivdelId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(arkivdel, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(arkivdel, pivot, pageRequest));
    }

    if (params instanceof ListByKlasseParameters p && p.getKlasseId() != null) {
      var klasse = klasseService.findByIdOrThrow(p.getKlasseId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(klasse, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(klasse, pivot, pageRequest));
    }

    return super.getPaginators(params);
  }

  // Moetedokument
  public PaginatedList<MoetedokumentDTO> listMoetedokument(
      String moetemappeId, ListByMoetemappeParameters query) throws EInnsynException {
    query.setMoetemappeId(moetemappeId);
    return moetedokumentService.list(query);
  }

  public MoetedokumentDTO addMoetedokument(String moetemappeId, MoetedokumentDTO dto)
      throws EInnsynException {
    dto.setMoetemappe(new ExpandableField<>(moetemappeId));
    return moetedokumentService.add(dto);
  }

  // Moetesak
  public PaginatedList<MoetesakDTO> listMoetesak(
      String moetemappeId, ListByMoetemappeParameters query) throws EInnsynException {
    query.setMoetemappeId(moetemappeId);
    return moetesakService.list(query);
  }

  public MoetesakDTO addMoetesak(String moetemappeId, MoetesakDTO dto) throws EInnsynException {
    dto.setMoetemappe(new ExpandableField<>(moetemappeId));
    return moetesakService.add(dto);
  }
}

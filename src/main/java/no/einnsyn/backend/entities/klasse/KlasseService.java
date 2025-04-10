package no.einnsyn.backend.entities.klasse;

import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.paginators.Paginators;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseService;
import no.einnsyn.backend.entities.arkivdel.models.ListByArkivdelParameters;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.klasse.models.Klasse;
import no.einnsyn.backend.entities.klasse.models.KlasseDTO;
import no.einnsyn.backend.entities.klasse.models.ListByKlasseParameters;
import no.einnsyn.backend.entities.moetemappe.MoetemappeRepository;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.saksmappe.SaksmappeRepository;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class KlasseService extends ArkivBaseService<Klasse, KlasseDTO> {

  @Getter private final KlasseRepository repository;

  private final SaksmappeRepository saksmappeRepository;
  private final MoetemappeRepository moetemappeRepository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private KlasseService proxy;

  public KlasseService(
      KlasseRepository repository,
      SaksmappeRepository saksmappeRepository,
      MoetemappeRepository moetemappeRepository) {
    this.repository = repository;
    this.saksmappeRepository = saksmappeRepository;
    this.moetemappeRepository = moetemappeRepository;
  }

  public Klasse newObject() {
    return new Klasse();
  }

  public KlasseDTO newDTO() {
    return new KlasseDTO();
  }

  /** IRI / SystemId are not unique for Klasse. */
  @Transactional(readOnly = true)
  @Override
  public Klasse findById(String id) {
    var object = repository.findById(id).orElse(null);
    log.trace("findById {}:{}, {}", objectClassName, id, object);
    return object;
  }

  /** IRI and SystemID are not unique for Arkivdel. (This should be fixed) */
  @Transactional(readOnly = true)
  @Override
  public Pair<String, Klasse> findPropertyAndObjectByDTO(BaseDTO dto) {
    if (dto.getId() != null) {
      var klasse = repository.findById(dto.getId()).orElse(null);
      if (klasse != null) {
        return Pair.of("id", klasse);
      }
      return null;
    }

    if (dto instanceof KlasseDTO klasseDTO && klasseDTO.getExternalId() != null) {
      var journalenhetId =
          klasseDTO.getJournalenhet() == null
              ? authenticationService.getEnhetId()
              : klasseDTO.getJournalenhet().getId();
      var journalenhet = enhetService.findById(journalenhetId);
      var klasse =
          repository.findByExternalIdAndJournalenhet(klasseDTO.getExternalId(), journalenhet);
      if (klasse != null) {
        return Pair.of("[externalId, journalenhet]", klasse);
      }
    }

    return null;
  }

  @Override
  protected Klasse fromDTO(KlasseDTO dto, Klasse object) throws EInnsynException {
    super.fromDTO(dto, object);

    if (dto.getTittel() != null) {
      object.setTittel(dto.getTittel());
    }

    if (dto.getKlasse() != null) {
      object.setParentKlasse(klasseService.findById(dto.getKlasse().getId()));
    }

    if (dto.getKlassifikasjonssystem() != null) {
      object.setParentKlassifikasjonssystem(
          klassifikasjonssystemService.findById(dto.getKlassifikasjonssystem().getId()));
    }

    if (dto.getArkivdel() != null) {
      object.setParentArkivdel(arkivdelService.findById(dto.getArkivdel().getId()));
    }

    return object;
  }

  @Override
  protected KlasseDTO toDTO(
      Klasse object, KlasseDTO dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(object, dto, expandPaths, currentPath);

    dto.setTittel(object.getTittel());

    dto.setKlasse(
        klasseService.maybeExpand(object.getParentKlasse(), "klasse", expandPaths, currentPath));

    dto.setKlassifikasjonssystem(
        klassifikasjonssystemService.maybeExpand(
            object.getParentKlassifikasjonssystem(),
            "klassifikasjonssystem",
            expandPaths,
            currentPath));

    dto.setArkivdel(
        arkivdelService.maybeExpand(
            object.getParentArkivdel(), "arkivdel", expandPaths, currentPath));

    return dto;
  }

  @Override
  protected void deleteEntity(Klasse object) throws EInnsynException {
    try (var subKlasseIdStream = repository.streamIdByParentKlasse(object)) {
      var subKlasseIdIterator = subKlasseIdStream.iterator();
      while (subKlasseIdIterator.hasNext()) {
        klasseService.delete(subKlasseIdIterator.next());
      }
    }

    try (var saksmappeIdStream = saksmappeRepository.streamIdByParentKlasse(object)) {
      var saksmappeIdIterator = saksmappeIdStream.iterator();
      while (saksmappeIdIterator.hasNext()) {
        saksmappeService.delete(saksmappeIdIterator.next());
      }
    }

    try (var moetemappeIdStream = moetemappeRepository.streamIdByParentKlasse(object)) {
      var moetemappeIdIterator = moetemappeIdStream.iterator();
      while (moetemappeIdIterator.hasNext()) {
        moetemappeService.delete(moetemappeIdIterator.next());
      }
    }

    super.deleteEntity(object);
  }

  // SubKlasse
  public PaginatedList<KlasseDTO> listKlasse(String parentKlasseId, ListByKlasseParameters query)
      throws EInnsynException {
    query.setKlasseId(parentKlasseId);
    return klasseService.list(query);
  }

  public KlasseDTO addKlasse(String parentKlasseId, KlasseDTO klasseDTO) throws EInnsynException {
    klasseDTO.setKlasse(new ExpandableField<KlasseDTO>(parentKlasseId));
    return klasseService.add(klasseDTO);
  }

  // Saksmappe
  public PaginatedList<SaksmappeDTO> listSaksmappe(String klasseId, ListByKlasseParameters query)
      throws EInnsynException {
    query.setKlasseId(klasseId);
    return saksmappeService.list(query);
  }

  // Moetemappe
  public PaginatedList<MoetemappeDTO> listMoetemappe(String klasseId, ListByKlasseParameters query)
      throws EInnsynException {
    query.setKlasseId(klasseId);
    return moetemappeService.list(query);
  }

  /**
   * Override listEntity to filter by journalenhet, since Klasse is not unique by IRI / system_id.
   */
  @Override
  protected List<Klasse> listEntity(ListParameters params, int limit) {
    if (params.getJournalenhet() != null) {
      var journalenhet = enhetService.findById(params.getJournalenhet());
      if (params.getExternalIds() != null) {
        return repository.findByExternalIdInAndJournalenhet(params.getExternalIds(), journalenhet);
      }
      return repository.findByJournalenhet(journalenhet);
    }
    return super.listEntity(params, limit);
  }

  @Override
  protected Paginators<Klasse> getPaginators(ListParameters params) {
    if (params instanceof ListByArkivdelParameters p && p.getArkivdelId() != null) {
      var arkivdel = arkivdelService.findById(p.getArkivdelId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(arkivdel, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(arkivdel, pivot, pageRequest));
    }
    if (params instanceof ListByKlasseParameters p && p.getKlasseId() != null) {
      var klasse = klasseService.findById(p.getKlasseId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(klasse, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(klasse, pivot, pageRequest));
    }
    return super.getPaginators(params);
  }
}

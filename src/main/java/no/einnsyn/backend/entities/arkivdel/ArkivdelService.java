package no.einnsyn.backend.entities.arkivdel;

import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.paginators.Paginators;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.arkiv.models.ListByArkivParameters;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseService;
import no.einnsyn.backend.entities.arkivdel.models.Arkivdel;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.arkivdel.models.ListByArkivdelParameters;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.klasse.KlasseRepository;
import no.einnsyn.backend.entities.klasse.models.KlasseDTO;
import no.einnsyn.backend.entities.klassifikasjonssystem.KlassifikasjonssystemRepository;
import no.einnsyn.backend.entities.klassifikasjonssystem.models.KlassifikasjonssystemDTO;
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
public class ArkivdelService extends ArkivBaseService<Arkivdel, ArkivdelDTO> {

  @Getter protected final ArkivdelRepository repository;

  private final SaksmappeRepository saksmappeRepository;
  private final MoetemappeRepository moetemappeRepository;
  private final KlassifikasjonssystemRepository klassifikasjonssystemRepository;
  private final KlasseRepository klasseRepository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private ArkivdelService proxy;

  public ArkivdelService(
      ArkivdelRepository repository,
      SaksmappeRepository saksmappeRepository,
      MoetemappeRepository moetemappeRepository,
      KlassifikasjonssystemRepository klassifikasjonssystemRepository,
      KlasseRepository klasseRepository) {
    this.repository = repository;
    this.saksmappeRepository = saksmappeRepository;
    this.moetemappeRepository = moetemappeRepository;
    this.klassifikasjonssystemRepository = klassifikasjonssystemRepository;
    this.klasseRepository = klasseRepository;
  }

  public Arkivdel newObject() {
    return new Arkivdel();
  }

  public ArkivdelDTO newDTO() {
    return new ArkivdelDTO();
  }

  /** IRI / SystemId are not unique for Arkiv. */
  @Transactional(readOnly = true)
  @Override
  public Arkivdel findById(String id) {
    var object = repository.findById(id).orElse(null);
    log.trace("findById {}:{}, {}", objectClassName, id, object);
    return object;
  }

  /** IRI and SystemID are not unique for Arkivdel. (This should be fixed) */
  @Transactional(readOnly = true)
  @Override
  public Pair<String, Arkivdel> findPropertyAndObjectByDTO(BaseDTO dto) {
    if (dto.getId() != null) {
      var arkivdel = repository.findById(dto.getId()).orElse(null);
      if (arkivdel != null) {
        return Pair.of("id", arkivdel);
      }
      return null;
    }

    if (dto instanceof ArkivdelDTO arkivdelDTO && arkivdelDTO.getExternalId() != null) {
      var journalenhetId =
          arkivdelDTO.getJournalenhet() == null
              ? authenticationService.getEnhetId()
              : arkivdelDTO.getJournalenhet().getId();
      var journalenhet = enhetService.findById(journalenhetId);
      var arkivdel =
          repository.findByExternalIdAndJournalenhet(arkivdelDTO.getExternalId(), journalenhet);
      if (arkivdel != null) {
        return Pair.of("[externalId, journalenhet]", arkivdel);
      }
    }

    return null;
  }

  @Override
  protected Arkivdel fromDTO(ArkivdelDTO dto, Arkivdel object) throws EInnsynException {
    super.fromDTO(dto, object);

    if (dto.getTittel() != null) {
      object.setTittel(dto.getTittel());
    }

    if (dto.getArkiv() != null) {
      var parentArkiv = arkivService.findByIdOrThrow(dto.getArkiv().getId());
      object.setParent(parentArkiv);
    }

    return object;
  }

  @Override
  protected ArkivdelDTO toDTO(
      Arkivdel object, ArkivdelDTO dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(object, dto, expandPaths, currentPath);

    dto.setTittel(object.getTittel());

    var arkiv = object.getParent();
    if (arkiv != null) {
      dto.setArkiv(arkivService.maybeExpand(arkiv, "arkiv", expandPaths, currentPath));
    }

    return dto;
  }

  @Override
  protected void deleteEntity(Arkivdel arkivdel) throws EInnsynException {
    try (var saksmappeIdStream = saksmappeRepository.streamIdByParentArkivdel(arkivdel)) {
      var saksmappeIdIterator = saksmappeIdStream.iterator();
      while (saksmappeIdIterator.hasNext()) {
        saksmappeService.delete(saksmappeIdIterator.next());
      }
    }

    try (var moetemappeIdStream = moetemappeRepository.streamIdByParentArkivdel(arkivdel)) {
      var moetemappeIdIterator = moetemappeIdStream.iterator();
      while (moetemappeIdIterator.hasNext()) {
        moetemappeService.delete(moetemappeIdIterator.next());
      }
    }

    try (var klassifikasjonssystemIdStream =
        klassifikasjonssystemRepository.streamIdByArkivdel(arkivdel)) {
      var klassifikasjonssystemIdIterator = klassifikasjonssystemIdStream.iterator();
      while (klassifikasjonssystemIdIterator.hasNext()) {
        klassifikasjonssystemService.delete(klassifikasjonssystemIdIterator.next());
      }
    }

    try (var klasseIdStream = klasseRepository.streamIdByParentArkivdel(arkivdel)) {
      var klasseIdIterator = klasseIdStream.iterator();
      while (klasseIdIterator.hasNext()) {
        klasseService.delete(klasseIdIterator.next());
      }
    }

    super.deleteEntity(arkivdel);
  }

  /**
   * Override listEntity to filter by journalenhet, since Arkivdel is not unique by IRI / system_id.
   */
  @Override
  protected List<Arkivdel> listEntity(ListParameters params, int limit) throws EInnsynException {
    if (params.getJournalenhet() != null) {
      var journalenhet = enhetService.findByIdOrThrow(params.getJournalenhet());
      if (params.getExternalIds() != null) {
        return repository.findByExternalIdInAndJournalenhet(params.getExternalIds(), journalenhet);
      }
      return repository.findByJournalenhet(journalenhet);
    }
    return super.listEntity(params, limit);
  }

  @Override
  protected Paginators<Arkivdel> getPaginators(ListParameters params) throws EInnsynException {
    if (params instanceof ListByArkivParameters p && p.getArkivId() != null) {
      var arkiv = arkivService.findByIdOrThrow(p.getArkivId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(arkiv, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(arkiv, pivot, pageRequest));
    }
    return super.getPaginators(params);
  }

  // Klasse
  public PaginatedList<KlasseDTO> listKlasse(String arkivdelId, ListByArkivdelParameters query)
      throws EInnsynException {
    query.setArkivdelId(arkivdelId);
    return klasseService.list(query);
  }

  public KlasseDTO addKlasse(String arkivdelId, KlasseDTO klasseDTO) throws EInnsynException {
    klasseDTO.setArkivdel(new ExpandableField<>(arkivdelId));
    return klasseService.add(klasseDTO);
  }

  // Klassifikasjonssystem
  public PaginatedList<KlassifikasjonssystemDTO> listKlassifikasjonssystem(
      String arkivdelId, ListByArkivdelParameters query) throws EInnsynException {
    query.setArkivdelId(arkivdelId);
    return klassifikasjonssystemService.list(query);
  }

  public KlassifikasjonssystemDTO addKlassifikasjonssystem(
      String arkivdelId, KlassifikasjonssystemDTO klassifikasjonssystemDTO)
      throws EInnsynException {
    klassifikasjonssystemDTO.setArkivdel(new ExpandableField<>(arkivdelId));
    return klassifikasjonssystemService.add(klassifikasjonssystemDTO);
  }

  // Saksmappe
  public PaginatedList<SaksmappeDTO> listSaksmappe(
      String arkivdelId, ListByArkivdelParameters query) throws EInnsynException {
    query.setArkivdelId(arkivdelId);
    return saksmappeService.list(query);
  }

  public SaksmappeDTO addSaksmappe(String arkivdelId, SaksmappeDTO saksmappeDTO)
      throws EInnsynException {
    saksmappeDTO.setArkivdel(new ExpandableField<>(arkivdelId));
    return saksmappeService.add(saksmappeDTO);
  }

  // Moetemappe
  public PaginatedList<MoetemappeDTO> listMoetemappe(
      String arkivdelId, ListByArkivdelParameters query) throws EInnsynException {
    query.setArkivdelId(arkivdelId);
    return moetemappeService.list(query);
  }

  public MoetemappeDTO addMoetemappe(String arkivdelId, MoetemappeDTO moetemappeDTO)
      throws EInnsynException {
    moetemappeDTO.setArkivdel(new ExpandableField<>(arkivdelId));
    return moetemappeService.add(moetemappeDTO);
  }
}

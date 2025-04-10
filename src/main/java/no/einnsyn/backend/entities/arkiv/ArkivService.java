package no.einnsyn.backend.entities.arkiv;

import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.paginators.Paginators;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.arkiv.models.Arkiv;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkiv.models.ListByArkivParameters;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseService;
import no.einnsyn.backend.entities.arkivdel.ArkivdelRepository;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.enhet.models.ListByEnhetParameters;
import no.einnsyn.backend.entities.moetemappe.MoetemappeRepository;
import no.einnsyn.backend.entities.saksmappe.SaksmappeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ArkivService extends ArkivBaseService<Arkiv, ArkivDTO> {

  @Getter private final ArkivRepository repository;
  private final SaksmappeRepository saksmappeRepository;
  private final MoetemappeRepository moetemappeRepository;
  private final ArkivdelRepository arkivdelRepository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private ArkivService proxy;

  public ArkivService(
      ArkivRepository repository,
      SaksmappeRepository saksmappeRepository,
      MoetemappeRepository moetemappeRepository,
      ArkivdelRepository arkivdelRepository) {
    this.repository = repository;
    this.saksmappeRepository = saksmappeRepository;
    this.moetemappeRepository = moetemappeRepository;
    this.arkivdelRepository = arkivdelRepository;
  }

  public Arkiv newObject() {
    return new Arkiv();
  }

  public ArkivDTO newDTO() {
    return new ArkivDTO();
  }

  /** IRI / SystemId are not unique for Arkiv. */
  @Transactional(readOnly = true)
  @Override
  public Arkiv findById(String id) {
    var object = repository.findById(id).orElse(null);
    log.trace("findById {}:{}, {}", objectClassName, id, object);
    return object;
  }

  /** IRI and SystemID are not unique for Arkiv. (This should be fixed) */
  @Transactional(readOnly = true)
  @Override
  public Pair<String, Arkiv> findPropertyAndObjectByDTO(BaseDTO dto) {
    if (dto.getId() != null) {
      var arkiv = repository.findById(dto.getId()).orElse(null);
      if (arkiv != null) {
        return Pair.of("id", arkiv);
      }
      return null;
    }

    if (dto instanceof ArkivDTO arkivDTO && arkivDTO.getExternalId() != null) {
      var journalenhetId =
          arkivDTO.getJournalenhet() == null
              ? authenticationService.getEnhetId()
              : arkivDTO.getJournalenhet().getId();
      var journalenhet = enhetService.findById(journalenhetId);
      var arkiv =
          repository.findByExternalIdAndJournalenhet(arkivDTO.getExternalId(), journalenhet);
      if (arkiv != null) {
        return Pair.of("journalenhet", arkiv);
      }
    }

    return null;
  }

  @Override
  protected Arkiv fromDTO(ArkivDTO dto, Arkiv object) throws EInnsynException {
    super.fromDTO(dto, object);

    if (dto.getTittel() != null) {
      object.setTittel(dto.getTittel());
    }

    if (dto.getArkiv() != null) {
      var parentArkiv = proxy.findById(dto.getArkiv().getId());
      object.setParent(parentArkiv);
    }

    return object;
  }

  @Override
  protected ArkivDTO toDTO(
      Arkiv object, ArkivDTO dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(object, dto, expandPaths, currentPath);

    dto.setTittel(object.getTittel());

    var parent = object.getParent();
    if (object.getParent() != null) {
      dto.setArkiv(arkivService.maybeExpand(parent, "arkiv", expandPaths, currentPath));
    }

    return dto;
  }

  /**
   * Delete an Arkiv-object and all of its children
   *
   * @param arkiv The Arkiv-object to delete
   */
  @Override
  protected void deleteEntity(Arkiv arkiv) throws EInnsynException {
    try (var subArkivIdStream = repository.streamIdByParent(arkiv)) {
      var subArkivIdIterator = subArkivIdStream.iterator();
      while (subArkivIdIterator.hasNext()) {
        arkivService.delete(subArkivIdIterator.next());
      }
    }

    try (var arkivdelIdStream = arkivdelRepository.streamIdByParent(arkiv)) {
      var arkivdelIdIterator = arkivdelIdStream.iterator();
      while (arkivdelIdIterator.hasNext()) {
        arkivdelService.delete(arkivdelIdIterator.next());
      }
    }

    try (var subSaksmappeIdStream = saksmappeRepository.streamIdByParentArkiv(arkiv)) {
      var subSaksmappeIdIterator = subSaksmappeIdStream.iterator();
      while (subSaksmappeIdIterator.hasNext()) {
        saksmappeService.delete(subSaksmappeIdIterator.next());
      }
    }

    try (var subMoetemappeIdStream = moetemappeRepository.streamIdByParentArkiv(arkiv)) {
      var subMoetemappeIdIterator = subMoetemappeIdStream.iterator();
      while (subMoetemappeIdIterator.hasNext()) {
        moetemappeService.delete(subMoetemappeIdIterator.next());
      }
    }

    super.deleteEntity(arkiv);
  }

  // Arkiv
  public PaginatedList<ArkivDTO> listArkiv(String arkivId, ListByArkivParameters query)
      throws EInnsynException {
    query.setArkivId(arkivId);
    return arkivService.list(query);
  }

  public ArkivDTO addArkiv(String arkivId, ArkivDTO body) throws EInnsynException {
    body.setArkiv(new ExpandableField<>(arkivId));
    return arkivService.add(body);
  }

  // Arkivdel
  public PaginatedList<ArkivdelDTO> listArkivdel(String arkivId, ListByArkivParameters query)
      throws EInnsynException {
    query.setArkivId(arkivId);
    return arkivdelService.list(query);
  }

  public ArkivdelDTO addArkivdel(String arkivId, ArkivdelDTO body) throws EInnsynException {
    body.setArkiv(new ExpandableField<>(arkivId));
    return arkivdelService.add(body);
  }

  /**
   * Override listEntity to filter by journalenhet, since Arkiv is not unique by IRI / system_id.
   */
  @Override
  protected List<Arkiv> listEntity(ListParameters params, int limit) {
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
  protected Paginators<Arkiv> getPaginators(ListParameters params) {
    if (params instanceof ListByArkivParameters p && p.getArkivId() != null) {
      var arkiv = arkivService.findById(p.getArkivId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(arkiv, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(arkiv, pivot, pageRequest));
    }

    if (params instanceof ListByEnhetParameters p && p.getEnhetId() != null) {
      var enhet = enhetService.findById(p.getEnhetId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(enhet, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(enhet, pivot, pageRequest));
    }
    return super.getPaginators(params);
  }
}

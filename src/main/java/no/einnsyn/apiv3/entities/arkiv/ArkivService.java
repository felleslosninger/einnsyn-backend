package no.einnsyn.apiv3.entities.arkiv;

import static no.einnsyn.apiv3.entities.base.BaseService.log;

import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.paginators.Paginators;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.arkiv.models.Arkiv;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivListQueryDTO;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.arkivdel.ArkivdelRepository;
import no.einnsyn.apiv3.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.apiv3.entities.arkivdel.models.ArkivdelListQueryDTO;
import no.einnsyn.apiv3.entities.base.models.BaseDTO;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.mappe.models.MappeParentDTO;
import no.einnsyn.apiv3.entities.moetemappe.MoetemappeRepository;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeListQueryDTO;
import no.einnsyn.apiv3.entities.saksmappe.SaksmappeRepository;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeListQueryDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
  public Arkiv findById(String id) {
    var repository = getRepository();
    var object = repository.findById(id).orElse(null);
    log.trace("findById {}:{}, {}", objectClassName, id, object);
    return object;
  }

  /** IRI and SystemID are not unique for Arkiv. (This should be fixed) */
  @Transactional(readOnly = true)
  @Override
  public Arkiv findByDTO(BaseDTO dto) {
    var repository = getRepository();
    if (dto.getId() != null) {
      return repository.findById(dto.getId()).orElse(null);
    }
    return null;
  }

  @Override
  protected Arkiv fromDTO(ArkivDTO dto, Arkiv object) throws EInnsynException {
    super.fromDTO(dto, object);

    if (dto.getTittel() != null) {
      object.setTittel(dto.getTittel());
    }

    if (dto.getParent() != null) {
      var parentArkiv = proxy.findById(dto.getParent().getId());
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
      dto.setParent(arkivService.maybeExpand(parent, "parent", expandPaths, currentPath));
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
    var subArkivStream = repository.findAllByParent(arkiv);
    var subArkivIterator = subArkivStream.iterator();
    while (subArkivIterator.hasNext()) {
      var subArkiv = subArkivIterator.next();
      arkivService.delete(subArkiv.getId());
    }

    var arkivdelStream = arkivdelRepository.findAllByParent(arkiv);
    var arkivdelIterator = arkivdelStream.iterator();
    while (arkivdelIterator.hasNext()) {
      var arkivdel = arkivdelIterator.next();
      arkivdelService.delete(arkivdel.getId());
    }

    var subSaksmappeStream = saksmappeRepository.findAllByParentArkiv(arkiv);
    var subSaksmappeIterator = subSaksmappeStream.iterator();
    while (subSaksmappeIterator.hasNext()) {
      var subSaksmappe = subSaksmappeIterator.next();
      saksmappeService.delete(subSaksmappe.getId());
    }

    var subMoetemappeStream = moetemappeRepository.findAllByParentArkiv(arkiv);
    var subMoetemappeIterator = subMoetemappeStream.iterator();
    while (subMoetemappeIterator.hasNext()) {
      var subMoetemappe = subMoetemappeIterator.next();
      moetemappeService.delete(subMoetemappe.getId());
    }

    super.deleteEntity(arkiv);
  }

  // Arkiv
  public ResultList<ArkivDTO> getArkivList(String arkivId, ArkivListQueryDTO query)
      throws EInnsynException {
    query.setArkivId(arkivId);
    return arkivService.list(query);
  }

  public ArkivDTO addArkiv(String arkivId, ArkivDTO body) throws EInnsynException {
    body.setParent(new ExpandableField<>(arkivId));
    return arkivService.add(body);
  }

  // Arkivdel
  public ResultList<ArkivdelDTO> getArkivdelList(String arkivId, ArkivdelListQueryDTO query)
      throws EInnsynException {
    query.setArkivId(arkivId);
    return arkivdelService.list(query);
  }

  public ArkivdelDTO addArkivdel(String arkivId, ArkivdelDTO body) throws EInnsynException {
    body.setParent(new ExpandableField<>(arkivId));
    return arkivdelService.add(body);
  }

  // Saksmappe
  public ResultList<SaksmappeDTO> getSaksmappeList(String arkivId, SaksmappeListQueryDTO query)
      throws EInnsynException {
    query.setArkivId(arkivId);
    return saksmappeService.list(query);
  }

  public SaksmappeDTO addSaksmappe(String arkivId, SaksmappeDTO body) throws EInnsynException {
    body.setParent(new MappeParentDTO(arkivId));
    return saksmappeService.add(body);
  }

  // Moetemappe
  public ResultList<MoetemappeDTO> getMoetemappeList(String arkivId, MoetemappeListQueryDTO query)
      throws EInnsynException {
    query.setArkivId(arkivId);
    return moetemappeService.list(query);
  }

  public MoetemappeDTO addMoetemappe(String arkivId, MoetemappeDTO body) throws EInnsynException {
    body.setParent(new MappeParentDTO(arkivId));
    return moetemappeService.add(body);
  }

  @Override
  protected Paginators<Arkiv> getPaginators(BaseListQueryDTO params) {
    if (params instanceof ArkivListQueryDTO p) {
      if (p.getArkivId() != null) {
        var arkiv = arkivService.findById(p.getArkivId());
        return new Paginators<>(
            (pivot, pageRequest) -> repository.paginateAsc(arkiv, pivot, pageRequest),
            (pivot, pageRequest) -> repository.paginateDesc(arkiv, pivot, pageRequest));
      }
      if (p.getEnhetId() != null) {
        var enhet = enhetService.findById(p.getEnhetId());
        return new Paginators<>(
            (pivot, pageRequest) -> repository.paginateAsc(enhet, pivot, pageRequest),
            (pivot, pageRequest) -> repository.paginateDesc(enhet, pivot, pageRequest));
      }
    }
    return super.getPaginators(params);
  }
}

package no.einnsyn.apiv3.entities.arkivdel;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.paginators.Paginators;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.arkivdel.models.Arkivdel;
import no.einnsyn.apiv3.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.apiv3.entities.arkivdel.models.ArkivdelListQueryDTO;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.klasse.KlasseRepository;
import no.einnsyn.apiv3.entities.klasse.models.KlasseDTO;
import no.einnsyn.apiv3.entities.klasse.models.KlasseListQueryDTO;
import no.einnsyn.apiv3.entities.klasse.models.KlasseParentDTO;
import no.einnsyn.apiv3.entities.klassifikasjonssystem.KlassifikasjonssystemRepository;
import no.einnsyn.apiv3.entities.klassifikasjonssystem.models.KlassifikasjonssystemDTO;
import no.einnsyn.apiv3.entities.klassifikasjonssystem.models.KlassifikasjonssystemListQueryDTO;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
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

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Arkivdel fromDTO(ArkivdelDTO dto, Arkivdel object, Set<String> paths, String currentPath)
      throws EInnsynException {
    super.fromDTO(dto, object, paths, currentPath);

    if (dto.getTittel() != null) {
      object.setTittel(dto.getTittel());
    }

    if (dto.getParent() != null) {
      var parentArkiv = arkivService.findById(dto.getParent().getId());
      object.setParent(parentArkiv);
    }

    return object;
  }

  @Override
  public ArkivdelDTO toDTO(
      Arkivdel object, ArkivdelDTO dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(object, dto, expandPaths, currentPath);

    dto.setTittel(object.getTittel());

    var arkiv = object.getParent();
    if (arkiv != null) {
      dto.setParent(arkivService.maybeExpand(arkiv, "parent", expandPaths, currentPath));
    }

    return dto;
  }

  @Override
  protected ArkivdelDTO delete(Arkivdel arkivdel) throws EInnsynException {
    var saksmappeStream = saksmappeRepository.findAllByParentArkivdel(arkivdel);
    var saksmappeIterator = saksmappeStream.iterator();
    while (saksmappeIterator.hasNext()) {
      var saksmappe = saksmappeIterator.next();
      saksmappeService.delete(saksmappe.getId());
    }

    var moetemappeStream = moetemappeRepository.findAllByParentArkivdel(arkivdel);
    var moetemappeIterator = moetemappeStream.iterator();
    while (moetemappeIterator.hasNext()) {
      var moetemappe = moetemappeIterator.next();
      moetemappeService.delete(moetemappe.getId());
    }

    var klassifikasjonssystemStream = klassifikasjonssystemRepository.findByArkivdel(arkivdel);
    var klassifikasjonssystemIterator = klassifikasjonssystemStream.iterator();
    while (klassifikasjonssystemIterator.hasNext()) {
      var klassifikasjonssystem = klassifikasjonssystemIterator.next();
      klassifikasjonssystemService.delete(klassifikasjonssystem.getId());
    }

    var klasseStream = klasseRepository.findAllByParentArkivdel(arkivdel);
    var klasseIterator = klasseStream.iterator();
    while (klasseIterator.hasNext()) {
      var klasse = klasseIterator.next();
      klasseService.delete(klasse.getId());
    }

    return super.delete(arkivdel);
  }

  @Override
  public Paginators<Arkivdel> getPaginators(BaseListQueryDTO params) {
    if (params instanceof ArkivdelListQueryDTO p && p.getArkivId() != null) {
      var arkiv = arkivService.findById(p.getArkivId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(arkiv, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(arkiv, pivot, pageRequest));
    }
    return super.getPaginators(params);
  }

  // Klasse
  public ResultList<KlasseDTO> getKlasseList(String arkivdelId, KlasseListQueryDTO query) {
    query.setArkivdelId(arkivdelId);
    return klasseService.list(query);
  }

  public KlasseDTO addKlasse(String arkivdelId, KlasseDTO klasseDTO) throws EInnsynException {
    klasseDTO.setParent(new KlasseParentDTO(arkivdelId));
    return klasseService.add(klasseDTO);
  }

  // Klassifikasjonssystem
  public ResultList<KlassifikasjonssystemDTO> getKlassifikasjonssystemList(
      String arkivdelId, KlassifikasjonssystemListQueryDTO query) {
    query.setArkivdelId(arkivdelId);
    return klassifikasjonssystemService.list(query);
  }

  public KlassifikasjonssystemDTO addKlassifikasjonssystem(
      String arkivdelId, KlassifikasjonssystemDTO klassifikasjonssystemDTO)
      throws EInnsynException {
    klassifikasjonssystemDTO.setParent(new ExpandableField<>(arkivdelId));
    return klassifikasjonssystemService.add(klassifikasjonssystemDTO);
  }

  // Saksmappe
  public ResultList<SaksmappeDTO> getSaksmappeList(String arkivdelId, SaksmappeListQueryDTO query) {
    query.setArkivdelId(arkivdelId);
    return saksmappeService.list(query);
  }

  public SaksmappeDTO addSaksmappe(String arkivdelId, SaksmappeDTO saksmappeDTO)
      throws EInnsynException {
    saksmappeDTO.setParent(new MappeParentDTO(arkivdelId));
    return saksmappeService.add(saksmappeDTO);
  }

  // Moetemappe
  public ResultList<MoetemappeDTO> getMoetemappeList(
      String arkivdelId, MoetemappeListQueryDTO query) {
    query.setArkivdelId(arkivdelId);
    return moetemappeService.list(query);
  }

  public MoetemappeDTO addMoetemappe(String arkivdelId, MoetemappeDTO moetemappeDTO)
      throws EInnsynException {
    moetemappeDTO.setParent(new MappeParentDTO(arkivdelId));
    return moetemappeService.add(moetemappeDTO);
  }
}

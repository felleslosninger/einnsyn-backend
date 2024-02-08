package no.einnsyn.apiv3.entities.klasse;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.common.paginators.Paginators;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.klasse.models.Klasse;
import no.einnsyn.apiv3.entities.klasse.models.KlasseDTO;
import no.einnsyn.apiv3.entities.klasse.models.KlasseListQueryDTO;
import no.einnsyn.apiv3.entities.klasse.models.KlasseParentDTO;
import no.einnsyn.apiv3.entities.mappe.models.MappeParentDTO;
import no.einnsyn.apiv3.entities.moetemappe.MoetemappeRepository;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeListQueryDTO;
import no.einnsyn.apiv3.entities.saksmappe.SaksmappeRepository;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeListQueryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
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

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Klasse fromDTO(KlasseDTO dto, Klasse object, Set<String> paths, String currentPath)
      throws EInnsynException {
    super.fromDTO(dto, object, paths, currentPath);

    if (dto.getTittel() != null) {
      object.setTittel(dto.getTittel());
    }

    var parent = dto.getParent();
    if (parent != null) {
      if (parent.isArkivdel()) {
        var parentArkivdel = arkivdelService.findById(parent.getId());
        object.setArkivdel(parentArkivdel);
      } else if (parent.isKlasse()) {
        var parentKlasse = klasseService.findById(parent.getId());
        object.setParentKlasse(parentKlasse);
      } else if (parent.isKlassifikasjonssystem()) {
        var parentKlassifikasjonssystem = klassifikasjonssystemService.findById(parent.getId());
        object.setKlassifikasjonssystem(parentKlassifikasjonssystem);
      } else {
        throw new EInnsynException("Invalid parent type: " + parent.getId());
      }
    }

    return object;
  }

  @Override
  public KlasseDTO toDTO(
      Klasse object, KlasseDTO dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(object, dto, expandPaths, currentPath);

    dto.setTittel(object.getTittel());

    var parentPath = currentPath.isEmpty() ? "parent" : currentPath + ".parent";
    var shouldExpand = expandPaths != null && expandPaths.contains(parentPath);
    var parentKlasse = object.getParentKlasse();
    if (parentKlasse != null) {
      if (shouldExpand) {
        dto.setParent(
            new KlasseParentDTO(klasseService.toDTO(parentKlasse, expandPaths, parentPath)));
      } else {
        dto.setParent(new KlasseParentDTO(parentKlasse.getId()));
      }
    }

    var parentArkivdel = object.getArkivdel();
    if (parentArkivdel != null) {
      if (shouldExpand) {
        dto.setParent(
            new KlasseParentDTO(arkivdelService.toDTO(parentArkivdel, expandPaths, parentPath)));
      } else {
        dto.setParent(new KlasseParentDTO(parentArkivdel.getId()));
      }
    }

    var parentKlassifikasjonssystem = object.getKlassifikasjonssystem();
    if (parentKlassifikasjonssystem != null) {
      if (shouldExpand) {
        dto.setParent(
            new KlasseParentDTO(
                klassifikasjonssystemService.toDTO(
                    parentKlassifikasjonssystem, expandPaths, parentPath)));
      } else {
        dto.setParent(new KlasseParentDTO(parentKlassifikasjonssystem.getId()));
      }
    }

    return dto;
  }

  @Transactional
  public KlasseDTO delete(Klasse object) throws EInnsynException {
    var dto = proxy.toDTO(object);

    var subklassePage = repository.findByParentKlasse(object, PageRequest.of(0, 100));
    while (subklassePage.hasContent()) {
      for (var subklasse : subklassePage) {
        klasseService.delete(subklasse);
      }
      subklassePage = repository.findByParentKlasse(object, subklassePage.nextPageable());
    }

    var saksmappePage = saksmappeRepository.paginateAsc(object, null, PageRequest.of(0, 100));
    while (saksmappePage.hasContent()) {
      for (var saksmappe : saksmappePage) {
        saksmappeService.delete(saksmappe);
      }
      saksmappePage = saksmappeRepository.paginateAsc(object, null, saksmappePage.nextPageable());
    }

    var moetemappePage = moetemappeRepository.paginateAsc(object, null, PageRequest.of(0, 100));
    while (moetemappePage.hasContent()) {
      for (var moetemappe : moetemappePage) {
        moetemappeService.delete(moetemappe);
      }
      moetemappePage =
          moetemappeRepository.paginateAsc(object, null, moetemappePage.nextPageable());
    }

    dto.setDeleted(true);
    repository.delete(object);
    return dto;
  }

  // SubKlasse
  public ResultList<KlasseDTO> getKlasseList(String parentKlasseId, KlasseListQueryDTO query) {
    query.setKlasseId(parentKlasseId);
    return klasseService.list(query);
  }

  public KlasseDTO addKlasse(String parentKlasseId, KlasseDTO klasseDTO) throws EInnsynException {
    klasseDTO.setParent(new KlasseParentDTO(parentKlasseId));
    return klasseService.add(klasseDTO);
  }

  // Saksmappe
  public ResultList<SaksmappeDTO> getSaksmappeList(String klasseId, SaksmappeListQueryDTO query) {
    query.setKlasseId(klasseId);
    return saksmappeService.list(query);
  }

  public SaksmappeDTO addSaksmappe(String klasseId, SaksmappeDTO saksmappeDTO)
      throws EInnsynException {
    saksmappeDTO.setParent(new MappeParentDTO(klasseId));
    return saksmappeService.add(saksmappeDTO);
  }

  // Moetemappe
  public ResultList<MoetemappeDTO> getMoetemappeList(
      String klasseId, MoetemappeListQueryDTO query) {
    query.setKlasseId(klasseId);
    return moetemappeService.list(query);
  }

  public MoetemappeDTO addMoetemappe(String klasseId, MoetemappeDTO moetemappeDTO)
      throws EInnsynException {
    moetemappeDTO.setParent(new MappeParentDTO(klasseId));
    return moetemappeService.add(moetemappeDTO);
  }

  @Override
  public Paginators<Klasse> getPaginators(BaseListQueryDTO params) {
    if (params instanceof KlasseListQueryDTO p) {
      if (p.getArkivdelId() != null) {
        var arkivdel = arkivdelService.findById(p.getArkivdelId());
        return new Paginators<>(
            (pivot, pageRequest) -> repository.paginateAsc(arkivdel, pivot, pageRequest),
            (pivot, pageRequest) -> repository.paginateDesc(arkivdel, pivot, pageRequest));
      }
      if (p.getKlasseId() != null) {
        var klasse = klasseService.findById(p.getKlasseId());
        return new Paginators<>(
            (pivot, pageRequest) -> repository.paginateAsc(klasse, pivot, pageRequest),
            (pivot, pageRequest) -> repository.paginateDesc(klasse, pivot, pageRequest));
      }
    }
    return super.getPaginators(params);
  }
}

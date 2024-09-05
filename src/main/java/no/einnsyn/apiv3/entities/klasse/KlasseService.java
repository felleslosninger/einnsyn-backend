package no.einnsyn.apiv3.entities.klasse;

import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.common.paginators.Paginators;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.base.models.BaseDTO;
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
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
  public Klasse findById(String id) {
    var repository = getRepository();
    var object = repository.findById(id).orElse(null);
    log.trace("findById {}:{}, {}", objectClassName, id, object);
    return object;
  }

  /** IRI and SystemID are not unique for Arkivdel. (This should be fixed) */
  @Transactional(readOnly = true)
  @Override
  public Klasse findByDTO(BaseDTO dto) {
    var repository = getRepository();
    if (dto.getId() != null) {
      return repository.findById(dto.getId()).orElse(null);
    }

    if (dto instanceof KlasseDTO klasseDTO) {
      if (klasseDTO.getExternalId() != null) {
        var journalenhetId =
            klasseDTO.getJournalenhet() == null
                ? authenticationService.getJournalenhetId()
                : klasseDTO.getJournalenhet().getId();
        var journalenhet = enhetService.findById(journalenhetId);
        return repository.findByExternalIdAndJournalenhet(klasseDTO.getExternalId(), journalenhet);
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

    var parent = dto.getParent();
    if (parent != null) {
      if (parent.isArkivdel()) {
        var parentArkivdel = arkivdelService.findById(parent.getId());
        object.setParentArkivdel(parentArkivdel);
      } else if (parent.isKlasse()) {
        var parentKlasse = klasseService.findById(parent.getId());
        object.setParentKlasse(parentKlasse);
      } else if (parent.isKlassifikasjonssystem()) {
        var parentKlassifikasjonssystem = klassifikasjonssystemService.findById(parent.getId());
        object.setParentKlassifikasjonssystem(parentKlassifikasjonssystem);
      } else {
        throw new EInnsynException("Invalid parent type: " + parent.getId());
      }
    }

    return object;
  }

  @Override
  @SuppressWarnings("java:S1192") // Allow multiple "parent" strings
  protected KlasseDTO toDTO(
      Klasse object, KlasseDTO dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(object, dto, expandPaths, currentPath);

    dto.setTittel(object.getTittel());

    var parentKlasse = object.getParentKlasse();
    if (parentKlasse != null) {
      dto.setParent(
          new KlasseParentDTO(
              klasseService.maybeExpand(parentKlasse, "parent", expandPaths, currentPath)));
    }

    var parentArkivdel = object.getParentArkivdel();
    if (parentArkivdel != null) {
      dto.setParent(
          new KlasseParentDTO(
              arkivdelService.maybeExpand(parentArkivdel, "parent", expandPaths, currentPath)));
    }

    var parentKlassifikasjonssystem = object.getParentKlassifikasjonssystem();
    if (parentKlassifikasjonssystem != null) {
      dto.setParent(
          new KlasseParentDTO(
              klassifikasjonssystemService.maybeExpand(
                  parentKlassifikasjonssystem, "parent", expandPaths, currentPath)));
    }

    return dto;
  }

  @Override
  protected void deleteEntity(Klasse object) throws EInnsynException {
    var subKlasseStream = repository.findAllByParentKlasse(object);
    var subKlasseIterator = subKlasseStream.iterator();
    while (subKlasseIterator.hasNext()) {
      var subKlasse = subKlasseIterator.next();
      klasseService.delete(subKlasse.getId());
    }

    var saksmappeStream = saksmappeRepository.findAllByParentKlasse(object);
    var saksmappeIterator = saksmappeStream.iterator();
    while (saksmappeIterator.hasNext()) {
      var saksmappe = saksmappeIterator.next();
      saksmappeService.delete(saksmappe.getId());
    }

    var moetemappeStream = moetemappeRepository.findAllByParentKlasse(object);
    var moetemappeIterator = moetemappeStream.iterator();
    while (moetemappeIterator.hasNext()) {
      var moetemappe = moetemappeIterator.next();
      moetemappeService.delete(moetemappe.getId());
    }

    super.deleteEntity(object);
  }

  // SubKlasse
  public ResultList<KlasseDTO> getKlasseList(String parentKlasseId, KlasseListQueryDTO query)
      throws EInnsynException {
    query.setKlasseId(parentKlasseId);
    return klasseService.list(query);
  }

  public KlasseDTO addKlasse(String parentKlasseId, KlasseDTO klasseDTO) throws EInnsynException {
    klasseDTO.setParent(new KlasseParentDTO(parentKlasseId));
    return klasseService.add(klasseDTO);
  }

  // Saksmappe
  public ResultList<SaksmappeDTO> getSaksmappeList(String klasseId, SaksmappeListQueryDTO query)
      throws EInnsynException {
    query.setKlasseId(klasseId);
    return saksmappeService.list(query);
  }

  public SaksmappeDTO addSaksmappe(String klasseId, SaksmappeDTO saksmappeDTO)
      throws EInnsynException {
    saksmappeDTO.setParent(new MappeParentDTO(klasseId));
    return saksmappeService.add(saksmappeDTO);
  }

  // Moetemappe
  public ResultList<MoetemappeDTO> getMoetemappeList(String klasseId, MoetemappeListQueryDTO query)
      throws EInnsynException {
    query.setKlasseId(klasseId);
    return moetemappeService.list(query);
  }

  /**
   * Override listEntity to filter by journalenhet, since Klasse is not unique by IRI / system_id.
   */
  @Override
  protected List<Klasse> listEntity(BaseListQueryDTO params, int limit) {
    if (params instanceof KlasseListQueryDTO p && p.getJournalenhet() != null) {
      var journalenhet = enhetService.findById(p.getJournalenhet());
      if (p.getExternalIds() != null) {
        return getRepository().findByExternalIdInAndJournalenhet(p.getExternalIds(), journalenhet);
      }
      return getRepository().findByJournalenhet(journalenhet);
    }
    return super.listEntity(params, limit);
  }

  public MoetemappeDTO addMoetemappe(String klasseId, MoetemappeDTO moetemappeDTO)
      throws EInnsynException {
    moetemappeDTO.setParent(new MappeParentDTO(klasseId));
    return moetemappeService.add(moetemappeDTO);
  }

  @Override
  protected Paginators<Klasse> getPaginators(BaseListQueryDTO params) {
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

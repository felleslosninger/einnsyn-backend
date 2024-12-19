package no.einnsyn.backend.entities.klasse;

import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.paginators.Paginators;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.ListResponseBody;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseService;
import no.einnsyn.backend.entities.arkivdel.models.ListByArkivdelParameters;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.klasse.models.Klasse;
import no.einnsyn.backend.entities.klasse.models.KlasseDTO;
import no.einnsyn.backend.entities.klasse.models.KlasseParentDTO;
import no.einnsyn.backend.entities.klasse.models.ListByKlasseParameters;
import no.einnsyn.backend.entities.mappe.models.MappeParent;
import no.einnsyn.backend.entities.moetemappe.MoetemappeRepository;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.saksmappe.SaksmappeRepository;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.backend.error.exceptions.EInnsynException;
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
              ? authenticationService.getJournalenhetId()
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
  public ListResponseBody<KlasseDTO> listKlasse(String parentKlasseId, ListByKlasseParameters query)
      throws EInnsynException {
    query.setKlasseId(parentKlasseId);
    return klasseService.list(query);
  }

  public KlasseDTO addKlasse(String parentKlasseId, KlasseDTO klasseDTO) throws EInnsynException {
    klasseDTO.setKlasse(new ExpandableField<KlasseDTO>(parentKlasseId));
    return klasseService.add(klasseDTO);
  }

  // Saksmappe
  public ListResponseBody<SaksmappeDTO> listSaksmappe(String klasseId, ListByKlasseParameters query)
      throws EInnsynException {
    query.setKlasseId(klasseId);
    return saksmappeService.list(query);
  }

  public SaksmappeDTO addSaksmappe(String klasseId, SaksmappeDTO saksmappeDTO)
      throws EInnsynException {
    saksmappeDTO.setParent(new MappeParent(klasseId));
    return saksmappeService.add(saksmappeDTO);
  }

  // Moetemappe
  public ListResponseBody<MoetemappeDTO> listMoetemappe(
      String klasseId, ListByKlasseParameters query) throws EInnsynException {
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

  public MoetemappeDTO addMoetemappe(String klasseId, MoetemappeDTO moetemappeDTO)
      throws EInnsynException {
    moetemappeDTO.setParent(new MappeParent(klasseId));
    return moetemappeService.add(moetemappeDTO);
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

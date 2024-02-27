package no.einnsyn.apiv3.entities.klassifikasjonssystem;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.klasse.KlasseRepository;
import no.einnsyn.apiv3.entities.klasse.models.KlasseDTO;
import no.einnsyn.apiv3.entities.klasse.models.KlasseListQueryDTO;
import no.einnsyn.apiv3.entities.klasse.models.KlasseParentDTO;
import no.einnsyn.apiv3.entities.klassifikasjonssystem.models.Klassifikasjonssystem;
import no.einnsyn.apiv3.entities.klassifikasjonssystem.models.KlassifikasjonssystemDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KlassifikasjonssystemService
    extends ArkivBaseService<Klassifikasjonssystem, KlassifikasjonssystemDTO> {

  @Getter private final KlassifikasjonssystemRepository repository;
  private final KlasseRepository klasseRepository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private KlassifikasjonssystemService proxy;

  public KlassifikasjonssystemService(
      KlassifikasjonssystemRepository repository, KlasseRepository klasseRepository) {
    this.repository = repository;
    this.klasseRepository = klasseRepository;
  }

  public Klassifikasjonssystem newObject() {
    return new Klassifikasjonssystem();
  }

  public KlassifikasjonssystemDTO newDTO() {
    return new KlassifikasjonssystemDTO();
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Klassifikasjonssystem fromDTO(
      KlassifikasjonssystemDTO dto,
      Klassifikasjonssystem object,
      Set<String> paths,
      String currentPath)
      throws EInnsynException {
    super.fromDTO(dto, object, paths, currentPath);

    if (dto.getTittel() != null) {
      object.setTittel(dto.getTittel());
    }

    var parent = dto.getParent();
    if (parent != null) {
      var parentArkivdel = arkivdelService.findById(parent.getId());
      object.setArkivdel(parentArkivdel);
    }

    return object;
  }

  @Override
  public KlassifikasjonssystemDTO toDTO(
      Klassifikasjonssystem object,
      KlassifikasjonssystemDTO dto,
      Set<String> expandPaths,
      String currentPath) {
    super.toDTO(object, dto, expandPaths, currentPath);

    dto.setTittel(object.getTittel());

    var parentPath = currentPath.isEmpty() ? "parent" : currentPath + ".parent";
    var parent = object.getArkivdel();
    if (parent != null) {
      dto.setParent(arkivdelService.maybeExpand(parent, "parent", expandPaths, parentPath));
    }

    return dto;
  }

  @Override
  protected KlassifikasjonssystemDTO delete(Klassifikasjonssystem object) throws EInnsynException {
    var klasseStream = klasseRepository.findAllByParentKlassifikasjonssystem(object);
    var klasseIterator = klasseStream.iterator();
    while (klasseIterator.hasNext()) {
      var klasse = klasseIterator.next();
      klasseService.delete(klasse.getId());
    }

    return super.delete(object);
  }

  // Klasse
  public ResultList<KlasseDTO> getKlasseList(String ksysId, KlasseListQueryDTO query) {
    query.setKlassifikasjonssystemId(ksysId);
    return klasseService.list(query);
  }

  public KlasseDTO addKlasse(String ksysId, KlasseDTO klasseDTO) throws EInnsynException {
    klasseDTO.setParent(new KlasseParentDTO(ksysId));
    return klasseService.add(klasseDTO);
  }
}

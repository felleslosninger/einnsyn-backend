package no.einnsyn.backend.entities.klassifikasjonssystem;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.backend.common.responses.models.ListResponseBody;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseService;
import no.einnsyn.backend.entities.klasse.KlasseRepository;
import no.einnsyn.backend.entities.klasse.models.KlasseDTO;
import no.einnsyn.backend.entities.klasse.models.KlasseParentDTO;
import no.einnsyn.backend.entities.klassifikasjonssystem.models.Klassifikasjonssystem;
import no.einnsyn.backend.entities.klassifikasjonssystem.models.KlassifikasjonssystemDTO;
import no.einnsyn.backend.entities.klassifikasjonssystem.models.ListByKlassifikasjonssystemParameters;
import no.einnsyn.backend.error.exceptions.EInnsynException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

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
  protected Klassifikasjonssystem fromDTO(
      KlassifikasjonssystemDTO dto, Klassifikasjonssystem object) throws EInnsynException {
    super.fromDTO(dto, object);

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
  protected KlassifikasjonssystemDTO toDTO(
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
  protected void deleteEntity(Klassifikasjonssystem object) throws EInnsynException {
    var klasseStream = klasseRepository.findAllByParentKlassifikasjonssystem(object);
    var klasseIterator = klasseStream.iterator();
    while (klasseIterator.hasNext()) {
      var klasse = klasseIterator.next();
      klasseService.delete(klasse.getId());
    }

    super.deleteEntity(object);
  }

  // Klasse
  public ListResponseBody<KlasseDTO> listKlasse(
      String ksysId, ListByKlassifikasjonssystemParameters query) throws EInnsynException {
    query.setKlassifikasjonssystemId(ksysId);
    return klasseService.list(query);
  }

  public KlasseDTO addKlasse(String ksysId, KlasseDTO klasseDTO) throws EInnsynException {
    klasseDTO.setParent(new KlasseParentDTO(ksysId));
    return klasseService.add(klasseDTO);
  }
}

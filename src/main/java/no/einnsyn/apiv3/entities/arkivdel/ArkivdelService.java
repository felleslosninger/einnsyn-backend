package no.einnsyn.apiv3.entities.arkivdel;

import jakarta.transaction.Transactional;
import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.arkivdel.models.Arkivdel;
import no.einnsyn.apiv3.entities.arkivdel.models.ArkivdelDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class ArkivdelService extends ArkivBaseService<Arkivdel, ArkivdelDTO> {

  @Getter protected final ArkivdelRepository repository;

  @Getter @Lazy @Autowired private ArkivdelService proxy;

  public ArkivdelService(ArkivdelRepository repository) {
    this.repository = repository;
  }

  public Arkivdel newObject() {
    return new Arkivdel();
  }

  public ArkivdelDTO newDTO() {
    return new ArkivdelDTO();
  }

  @Override
  public Arkivdel fromDTO(ArkivdelDTO dto, Arkivdel object, Set<String> paths, String currentPath) {
    super.fromDTO(dto, object, paths, currentPath);

    if (dto.getTittel() != null) {
      object.setTittel(dto.getTittel());
    }

    return object;
  }

  @Override
  public ArkivdelDTO toDTO(
      Arkivdel object, ArkivdelDTO dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(object, dto, expandPaths, currentPath);

    dto.setTittel(object.getTittel());

    return dto;
  }

  @Transactional
  public ArkivdelDTO delete(Arkivdel object) {
    var dto = getProxy().toDTO(object);
    dto.setDeleted(true);
    repository.delete(object);
    return dto;
  }
}

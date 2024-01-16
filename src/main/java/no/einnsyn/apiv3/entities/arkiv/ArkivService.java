package no.einnsyn.apiv3.entities.arkiv;

import jakarta.transaction.Transactional;
import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.entities.arkiv.models.Arkiv;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.base.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class ArkivService extends BaseService<Arkiv, ArkivDTO> {

  @Getter private final ArkivRepository repository;

  @Getter @Lazy @Autowired private ArkivService proxy;

  public ArkivService(ArkivRepository repository) {
    this.repository = repository;
  }

  public Arkiv newObject() {
    return new Arkiv();
  }

  public ArkivDTO newDTO() {
    return new ArkivDTO();
  }

  @Override
  public Arkiv fromDTO(ArkivDTO dto, Arkiv object, Set<String> paths, String currentPath) {
    super.fromDTO(dto, object, paths, currentPath);

    if (dto.getTittel() != null) {
      object.setTittel(dto.getTittel());
    }

    return object;
  }

  @Override
  public ArkivDTO toDTO(Arkiv object, ArkivDTO dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(object, dto, expandPaths, currentPath);

    dto.setTittel(object.getTittel());

    return dto;
  }

  @Transactional
  public ArkivDTO delete(Arkiv object) {
    var dto = proxy.toDTO(object);
    dto.setDeleted(true);
    repository.delete(object);
    return dto;
  }
}

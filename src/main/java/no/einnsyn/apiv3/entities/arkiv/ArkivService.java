package no.einnsyn.apiv3.entities.arkiv;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.entities.arkiv.models.Arkiv;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.base.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArkivService extends BaseService<Arkiv, ArkivDTO> {

  @Getter private final ArkivRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private ArkivService proxy;

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
  public Arkiv fromDTO(ArkivDTO dto, Arkiv object, Set<String> paths, String currentPath)
      throws EInnsynException {
    super.fromDTO(dto, object, paths, currentPath);

    if (dto.getTittel() != null) {
      object.setTittel(dto.getTittel());
    }

    if (dto.getParent() != null) {
      var expandedParent = dto.getParent().getExpandedObject();
      var parentArkiv = proxy.findById(expandedParent.getId());
      object.setParent(parentArkiv);
    }

    return object;
  }

  @Override
  public ArkivDTO toDTO(Arkiv object, ArkivDTO dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(object, dto, expandPaths, currentPath);

    dto.setTittel(object.getTittel());

    var parent = object.getParent();
    if (object.getParent() != null) {
      dto.setParent(arkivService.maybeExpand(parent, "parent", expandPaths, currentPath));
    }

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

package no.einnsyn.apiv3.entities.klasse;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.klasse.models.Klasse;
import no.einnsyn.apiv3.entities.klasse.models.KlasseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KlasseService extends ArkivBaseService<Klasse, KlasseDTO> {

  @Getter private final KlasseRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private KlasseService proxy;

  public KlasseService(KlasseRepository repository) {
    this.repository = repository;
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

    if (dto.getParent() != null) {
      var expandedParent = dto.getParent().getExpandedObject();
      if (expandedParent.isArkivdel()) {
        var parentArkivdel = arkivdelService.findById(expandedParent.getId());
        object.setParentArkivdel(parentArkivdel);
      } else if (expandedParent.isKlasse()) {
        var parentKlasse = klasseService.findById(expandedParent.getId());
        object.setParentKlasse(parentKlasse);
      } else {
        throw new EInnsynException("Invalid parent type: " + expandedParent.getClass().getName());
      }
    }

    return object;
  }

  @Override
  public KlasseDTO toDTO(
      Klasse object, KlasseDTO dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(object, dto, expandPaths, currentPath);

    dto.setTittel(object.getTittel());

    var parentKlasse = object.getParentKlasse();
    if (parentKlasse != null) {
      // dto.setParent(klasseService.maybeExpand(parentKlasse, "parent", expandPaths, currentPath));
    }

    var parentArkivdel = object.getParentArkivdel();
    if (parentArkivdel != null) {
      // dto.setParent(
      //     arkivdelService.maybeExpand(parentArkivdel, "parent", expandPaths, currentPath));
    }

    return dto;
  }

  @Transactional
  public KlasseDTO delete(Klasse object) {
    var dto = proxy.toDTO(object);
    dto.setDeleted(true);
    repository.delete(object);
    return dto;
  }
}

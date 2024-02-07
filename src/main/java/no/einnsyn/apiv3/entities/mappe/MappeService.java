package no.einnsyn.apiv3.entities.mappe;

import java.time.LocalDate;
import java.util.Set;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.mappe.models.Mappe;
import no.einnsyn.apiv3.entities.mappe.models.MappeDTO;
import no.einnsyn.apiv3.entities.mappe.models.MappeParentDTO;

public abstract class MappeService<O extends Mappe, D extends MappeDTO>
    extends ArkivBaseService<O, D> {

  /**
   * Convert a DTO object to a Mappe
   *
   * @param dto
   * @param mappe
   * @param paths A list of paths containing new objects that will be created from this update
   * @param currentPath The current path in the object tree
   * @return
   */
  @Override
  public O fromDTO(D dto, O mappe, Set<String> paths, String currentPath) throws EInnsynException {
    super.fromDTO(dto, mappe, paths, currentPath);

    if (dto.getOffentligTittel() != null) {
      mappe.setOffentligTittel(dto.getOffentligTittel());
    }

    if (dto.getOffentligTittelSensitiv() != null) {
      mappe.setOffentligTittelSensitiv(dto.getOffentligTittelSensitiv());
    }

    if (dto.getBeskrivelse() != null) {
      mappe.setBeskrivelse(dto.getBeskrivelse());
    }

    var parentField = dto.getParent();
    if (dto.getParent() != null) {
      if (parentField.isArkiv()) {
        var parentArkiv = arkivService.findById(parentField.getId());
        mappe.setArkiv(parentArkiv);
      } else if (parentField.isArkivdel()) {
        var parentArkivdel = arkivdelService.findById(parentField.getId());
        mappe.setArkivdel(parentArkivdel);
      } else if (parentField.isKlasse()) {
        var parentKlasse = klasseService.findById(parentField.getId());
        mappe.setKlasse(parentKlasse);
      } else {
        throw new EInnsynException("Invalid parent type: " + parentField.getClass().getName());
      }
    }

    // Set publisertDato to now if not set for new objects
    if (dto.getPublisertDato() != null) {
      mappe.setPublisertDato(LocalDate.parse(dto.getPublisertDato()));
    } else if (mappe.getId() == null) {
      mappe.setPublisertDato(LocalDate.now());
    }

    return mappe;
  }

  /**
   * Convert a Mappe to a DTO object
   *
   * @param mappe
   * @param dto
   * @param expandPaths A list of "paths" to expand. Un-expanded objects will be shown as IDs
   * @param currentPath The current "path" in the object tree
   * @return
   */
  @Override
  public D toDTO(O mappe, D dto, Set<String> expandPaths, String currentPath) {

    super.toDTO(mappe, dto, expandPaths, currentPath);
    dto.setOffentligTittel(mappe.getOffentligTittel());
    dto.setOffentligTittelSensitiv(mappe.getOffentligTittelSensitiv());
    dto.setBeskrivelse(mappe.getBeskrivelse());
    if (mappe.getPublisertDato() != null) {
      dto.setPublisertDato(mappe.getPublisertDato().toString());
    }

    var parentPath = currentPath.isEmpty() ? "parent" : currentPath + ".parent";
    var shouldExpand = expandPaths != null && expandPaths.contains(parentPath);
    if (mappe.getArkiv() != null) {
      if (shouldExpand) {
        dto.setParent(
            new MappeParentDTO(arkivService.toDTO(mappe.getArkiv(), expandPaths, parentPath)));
      } else {
        dto.setParent(new MappeParentDTO(mappe.getArkiv().getId()));
      }
    } else if (mappe.getArkivdel() != null) {
      if (shouldExpand) {
        dto.setParent(
            new MappeParentDTO(
                arkivdelService.toDTO(mappe.getArkivdel(), expandPaths, parentPath)));
      } else {
        dto.setParent(new MappeParentDTO(mappe.getArkivdel().getId()));
      }
    } else if (mappe.getKlasse() != null) {
      if (shouldExpand) {
        dto.setParent(
            new MappeParentDTO(klasseService.toDTO(mappe.getKlasse(), expandPaths, parentPath)));
      } else {
        dto.setParent(new MappeParentDTO(mappe.getKlasse().getId()));
      }
    }

    return dto;
  }
}

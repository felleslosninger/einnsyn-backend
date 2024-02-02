package no.einnsyn.apiv3.entities.mappe;

import java.time.LocalDate;
import java.util.Set;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.mappe.models.Mappe;
import no.einnsyn.apiv3.entities.mappe.models.MappeDTO;

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
      var expandedParent = parentField.getExpandedObject();
      if (expandedParent.isArkiv()) {
        var parentArkiv = arkivService.findById(expandedParent.getId());
        mappe.setArkiv(parentArkiv);
      } else if (expandedParent.isArkivdel()) {
        var parentArkivdel = arkivdelService.findById(expandedParent.getId());
        mappe.setArkivdel(parentArkivdel);
      } else if (expandedParent.isKlasse()) {
        var parentKlasse = klasseService.findById(expandedParent.getId());
        mappe.setKlasse(parentKlasse);
      } else {
        throw new EInnsynException("Invalid parent type: " + expandedParent.getClass().getName());
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

    return dto;
  }
}

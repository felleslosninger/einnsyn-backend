package no.einnsyn.apiv3.entities.mappe;

import java.time.LocalDate;
import java.util.Set;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.mappe.models.Mappe;
import no.einnsyn.apiv3.entities.mappe.models.MappeDTO;
import no.einnsyn.apiv3.entities.mappe.models.MappeParentDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;

public abstract class MappeService<O extends Mappe, D extends MappeDTO>
    extends ArkivBaseService<O, D> {

  /**
   * Convert a DTO object to a Mappe
   *
   * @param dto The DTO object
   * @param mappe The Mappe object
   * @return The Mappe object
   */
  @Override
  protected O fromDTO(D dto, O mappe) throws EInnsynException {
    super.fromDTO(dto, mappe);

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
        mappe.setParentArkiv(parentArkiv);
      } else if (parentField.isArkivdel()) {
        var parentArkivdel = arkivdelService.findById(parentField.getId());
        mappe.setParentArkivdel(parentArkivdel);
      } else if (parentField.isKlasse()) {
        var parentKlasse = klasseService.findById(parentField.getId());
        mappe.setParentKlasse(parentKlasse);
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
   * @param mappe The Mappe object
   * @param dto The DTO object
   * @param expandPaths A list of "paths" to expand. Un-expanded objects will be shown as IDs
   * @param currentPath The current "path" in the object tree
   * @return The DTO object
   */
  @Override
  @SuppressWarnings("java:S1192") // Allow multiple "parent" strings
  protected D toDTO(O mappe, D dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(mappe, dto, expandPaths, currentPath);

    dto.setOffentligTittel(mappe.getOffentligTittel());
    dto.setOffentligTittelSensitiv(mappe.getOffentligTittelSensitiv());
    dto.setBeskrivelse(mappe.getBeskrivelse());

    if (mappe.getPublisertDato() != null) {
      dto.setPublisertDato(mappe.getPublisertDato().toString());
    }

    if (mappe.getParentArkiv() != null) {
      dto.setParent(
          new MappeParentDTO(
              arkivService.maybeExpand(
                  mappe.getParentArkiv(), "parent", expandPaths, currentPath)));
    } else if (mappe.getParentArkivdel() != null) {
      dto.setParent(
          new MappeParentDTO(
              arkivdelService.maybeExpand(
                  mappe.getParentArkivdel(), "parent", expandPaths, currentPath)));
    } else if (mappe.getParentKlasse() != null) {
      dto.setParent(
          new MappeParentDTO(
              klasseService.maybeExpand(
                  mappe.getParentKlasse(), "parent", expandPaths, currentPath)));
    }

    return dto;
  }
}

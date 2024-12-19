package no.einnsyn.backend.entities.mappe;

import java.time.Instant;
import java.util.Set;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseService;
import no.einnsyn.backend.entities.base.models.BaseES;
import no.einnsyn.backend.entities.mappe.models.Mappe;
import no.einnsyn.backend.entities.mappe.models.MappeDTO;
import no.einnsyn.backend.entities.mappe.models.MappeES;
import no.einnsyn.backend.entities.mappe.models.MappeParent;
import no.einnsyn.backend.error.exceptions.EInnsynException;
import no.einnsyn.backend.error.exceptions.ForbiddenException;
import no.einnsyn.backend.utils.TimeConverter;
import org.springframework.transaction.annotation.Transactional;

public abstract class MappeService<O extends Mappe, D extends MappeDTO>
    extends ArkivBaseService<O, D> {

  /**
   * TODO: This should be in ArkivBase when Arkiv / Arkivdel is fixed.
   *
   * @param id The ID of the object to find
   * @return The object with the given ID, or null if not found
   */
  @Override
  @Transactional(readOnly = true)
  public O findById(String id) {
    if (!id.startsWith(idPrefix)) {
      var object = getRepository().findBySystemId(id);
      if (object != null) {
        return object;
      }
    }

    return super.findById(id);
  }

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
      if (!authenticationService.isAdmin()) {
        throw new ForbiddenException("publisertDato will be set automatically");
      }
      mappe.setPublisertDato(TimeConverter.timestampToInstant(dto.getPublisertDato()));
    } else if (mappe.getId() == null) {
      mappe.setPublisertDato(Instant.now());
    }

    // Set oppdatertDato to now if not set for new objects
    if (dto.getOppdatertDato() != null) {
      if (!authenticationService.isAdmin()) {
        throw new ForbiddenException("oppdatertDato will be set automatically");
      }
      mappe.setOppdatertDato(TimeConverter.timestampToInstant(dto.getOppdatertDato()));
    } else {
      mappe.setOppdatertDato(Instant.now());
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

    if (mappe.getOppdatertDato() != null) {
      dto.setOppdatertDato(mappe.getOppdatertDato().toString());
    }

    if (mappe.getParentArkiv() != null) {
      dto.setParent(
          new MappeParent(
              arkivService.maybeExpand(
                  mappe.getParentArkiv(), "parent", expandPaths, currentPath)));
    } else if (mappe.getParentArkivdel() != null) {
      dto.setParent(
          new MappeParent(
              arkivdelService.maybeExpand(
                  mappe.getParentArkivdel(), "parent", expandPaths, currentPath)));
    } else if (mappe.getParentKlasse() != null) {
      dto.setParent(
          new MappeParent(
              klasseService.maybeExpand(
                  mappe.getParentKlasse(), "parent", expandPaths, currentPath)));
    }

    return dto;
  }

  // Build a legacy ElasticSearch document, used by the old API / frontend
  @Override
  protected BaseES toLegacyES(O registrering, BaseES es) {
    super.toLegacyES(registrering, es);
    if (es instanceof MappeES mappeES) {
      mappeES.setOffentligTittel(registrering.getOffentligTittel());
      mappeES.setOffentligTittel_SENSITIV(registrering.getOffentligTittelSensitiv());
      if (registrering.getPublisertDato() != null) {
        mappeES.setPublisertDato(registrering.getPublisertDato().toString());
      }
      if (registrering.getOppdatertDato() != null) {
        mappeES.setOppdatertDato(registrering.getOppdatertDato().toString());
      }
    }
    return es;
  }
}

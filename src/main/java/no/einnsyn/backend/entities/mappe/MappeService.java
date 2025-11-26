package no.einnsyn.backend.entities.mappe;

import java.time.Instant;
import java.util.Set;
import no.einnsyn.backend.common.exceptions.models.AuthorizationException;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.hasslug.HasSlugService;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseService;
import no.einnsyn.backend.entities.base.models.BaseES;
import no.einnsyn.backend.entities.mappe.models.Mappe;
import no.einnsyn.backend.entities.mappe.models.MappeDTO;
import no.einnsyn.backend.entities.mappe.models.MappeES;
import no.einnsyn.backend.utils.TimeConverter;
import org.springframework.transaction.annotation.Transactional;

public abstract class MappeService<O extends Mappe, D extends MappeDTO>
    extends ArkivBaseService<O, D> implements HasSlugService<O, MappeService<O, D>> {

  @Override
  public abstract MappeRepository<O> getRepository();

  /**
   * @param id The ID of the object to find
   * @return The object with the given ID, or null if not found
   */
  @Override
  @Transactional(readOnly = true)
  public O findById(String id) {
    if (!id.startsWith(idPrefix)) {
      var repository = getRepository();
      // TODO: This should be in ArkivBase when Arkiv / Arkivdel is fixed.
      var object = repository.findBySystemId(id);
      if (object != null) {
        return object;
      }
      object = repository.findBySlug(id);
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

    if (dto.getArkivdel() != null) {
      var arkivdel = arkivdelService.findByIdOrThrow(dto.getArkivdel().getId());
      mappe.setParentArkivdel(arkivdel);
    }

    if (dto.getKlasse() != null) {
      var klasse = klasseService.findByIdOrThrow(dto.getKlasse().getId());
      mappe.setParentKlasse(klasse);
    }

    // TODO: Add support for parent Moetemappe
    if (dto.getMoetemappe() != null) {}

    // TODO: Add support for parent Saksmappe
    if (dto.getSaksmappe() != null) {}

    // Set publisertDato to now if not set for new objects
    if (dto.getPublisertDato() != null) {
      if (!authenticationService.isAdmin()) {
        throw new AuthorizationException("publisertDato will be set automatically");
      }
      mappe.setPublisertDato(TimeConverter.timestampToInstant(dto.getPublisertDato()));
    } else if (mappe.getId() == null) {
      mappe.setPublisertDato(Instant.now());
    }

    // Set oppdatertDato to now if not set for new objects
    if (dto.getOppdatertDato() != null) {
      if (!authenticationService.isAdmin()) {
        throw new AuthorizationException("oppdatertDato will be set automatically");
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
      dto.setPublisertDato(TimeConverter.instantToTimestamp(mappe.getPublisertDato()));
    }

    if (mappe.getOppdatertDato() != null) {
      dto.setOppdatertDato(TimeConverter.instantToTimestamp(mappe.getOppdatertDato()));
    }

    if (mappe.getParentArkivdel() != null) {
      dto.setArkivdel(
          arkivdelService.maybeExpand(
              mappe.getParentArkivdel(), "arkivdel", expandPaths, currentPath));
    }

    if (mappe.getParentKlasse() != null) {
      dto.setKlasse(
          klasseService.maybeExpand(mappe.getParentKlasse(), "klasse", expandPaths, currentPath));
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

  public abstract String getSlugBase(O mappe);
}

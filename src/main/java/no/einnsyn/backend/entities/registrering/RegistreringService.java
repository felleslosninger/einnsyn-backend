package no.einnsyn.backend.entities.registrering;

import java.time.Instant;
import java.util.Set;
import no.einnsyn.backend.common.exceptions.models.AuthorizationException;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.hasslug.HasSlugService;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseService;
import no.einnsyn.backend.entities.base.models.BaseES;
import no.einnsyn.backend.entities.registrering.models.Registrering;
import no.einnsyn.backend.entities.registrering.models.RegistreringDTO;
import no.einnsyn.backend.entities.registrering.models.RegistreringES;
import no.einnsyn.backend.utils.TimeConverter;
import org.springframework.transaction.annotation.Transactional;

public abstract class RegistreringService<O extends Registrering, D extends RegistreringDTO>
    extends ArkivBaseService<O, D> implements HasSlugService<O, RegistreringService<O, D>> {

  @Override
  public abstract RegistreringRepository<O> getRepository();

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
      var object = getRepository().findBySystemId(id);
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
   * Convert a DTO object to a Registrering
   *
   * @param dto The DTO object to convert from
   * @param registrering The Registrering object to convert to
   */
  @Override
  protected O fromDTO(D dto, O registrering) throws EInnsynException {
    super.fromDTO(dto, registrering);

    if (dto.getOffentligTittel() != null) {
      registrering.setOffentligTittel(dto.getOffentligTittel());
    }

    if (dto.getOffentligTittelSensitiv() != null) {
      registrering.setOffentligTittelSensitiv(dto.getOffentligTittelSensitiv());
    }

    if (dto.getBeskrivelse() != null) {
      registrering.setBeskrivelse(dto.getBeskrivelse());
    }

    // Set publisertDato to now if not set for new objects
    if (dto.getPublisertDato() != null) {
      if (!authenticationService.isAdmin()) {
        throw new AuthorizationException("publisertDato will be set automatically");
      }
      registrering.setPublisertDato(TimeConverter.timestampToInstant(dto.getPublisertDato()));
    } else if (registrering.getId() == null) {
      registrering.setPublisertDato(Instant.now());
    }

    // Set oppdatertDato to now
    if (dto.getOppdatertDato() != null) {
      if (!authenticationService.isAdmin()) {
        throw new AuthorizationException("oppdatertDato will be set automatically");
      }
      registrering.setOppdatertDato(TimeConverter.timestampToInstant(dto.getOppdatertDato()));
    } else {
      registrering.setOppdatertDato(Instant.now());
    }

    // Set avhendetTil
    // TODO: The "recipient" should also have to accept this.
    if (dto.getAvhendetTil() != null) {
      registrering.setAvhendetTil(enhetService.findByIdOrThrow(dto.getAvhendetTil().getId()));
    }

    return registrering;
  }

  /**
   * Convert a Registrering to a DTO object
   *
   * @param registrering The Registrering object to convert from
   * @param dto The DTO object to convert to
   * @param expandPaths A list of paths to expand. Un-expanded objects will be shown as IDs
   * @param currentPath The current path in the object tree
   * @return The converted DTO object
   */
  @Override
  protected D toDTO(O registrering, D dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(registrering, dto, expandPaths, currentPath);

    dto.setOffentligTittel(registrering.getOffentligTittel());
    dto.setOffentligTittelSensitiv(registrering.getOffentligTittelSensitiv());
    dto.setBeskrivelse(registrering.getBeskrivelse());
    if (registrering.getPublisertDato() != null) {
      dto.setPublisertDato(registrering.getPublisertDato().toString());
    }
    if (registrering.getOppdatertDato() != null) {
      dto.setOppdatertDato(registrering.getOppdatertDato().toString());
    }
    dto.setAvhendetTil(
        enhetService.maybeExpand(
            registrering.getAvhendetTil(), "avhendetTil", expandPaths, currentPath));

    return dto;
  }

  @Override
  public BaseES toLegacyES(O registrering, BaseES es) {
    super.toLegacyES(registrering, es);
    if (es instanceof RegistreringES registreringES) {
      registreringES.setOffentligTittel(registrering.getOffentligTittel());
      registreringES.setOffentligTittel_SENSITIV(registrering.getOffentligTittelSensitiv());
      if (registrering.getPublisertDato() != null) {
        registreringES.setPublisertDato(
            TimeConverter.instantToTimestamp(registrering.getPublisertDato()));
      }
      if (registrering.getOppdatertDato() != null) {
        registreringES.setOppdatertDato(
            TimeConverter.instantToTimestamp(registrering.getOppdatertDato()));
      }
    }
    return es;
  }

  public abstract String getSlugBase(O registrering);
}

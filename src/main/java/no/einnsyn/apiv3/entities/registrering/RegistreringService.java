package no.einnsyn.apiv3.entities.registrering;

import java.time.Instant;
import java.util.Set;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.base.models.BaseES;
import no.einnsyn.apiv3.entities.registrering.models.Registrering;
import no.einnsyn.apiv3.entities.registrering.models.RegistreringDTO;
import no.einnsyn.apiv3.entities.registrering.models.RegistreringES;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import no.einnsyn.apiv3.error.exceptions.ForbiddenException;
import no.einnsyn.apiv3.utils.TimestampConverter;

public abstract class RegistreringService<O extends Registrering, D extends RegistreringDTO>
    extends ArkivBaseService<O, D> {

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
        throw new ForbiddenException("publisertDato will be set automatically");
      }
      registrering.setPublisertDato(TimestampConverter.timestampToInstant(dto.getPublisertDato()));
    } else if (registrering.getId() == null) {
      registrering.setPublisertDato(Instant.now());
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

    return dto;
  }

  @Override
  public BaseES toLegacyES(O registrering, BaseES es) {
    super.toLegacyES(registrering, es);
    if (es instanceof RegistreringES registreringES) {
      registreringES.setOffentligTittel(registrering.getOffentligTittel());
      registreringES.setOffentligTittel_SENSITIV(registrering.getOffentligTittelSensitiv());
      registreringES.setPublisertDato(registrering.getPublisertDato().toString());
    }
    return es;
  }
}

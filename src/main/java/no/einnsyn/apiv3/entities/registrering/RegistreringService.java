package no.einnsyn.apiv3.entities.registrering;

import java.time.Instant;
import java.util.Set;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.registrering.models.Registrering;
import no.einnsyn.apiv3.entities.registrering.models.RegistreringDTO;

public abstract class RegistreringService<O extends Registrering, D extends RegistreringDTO>
    extends ArkivBaseService<O, D> {

  /**
   * Convert a DTO object to a Registrering
   *
   * @param dto
   * @param registrering
   * @param paths A list of paths to expand. Un-expanded objects will be shown as IDs
   * @param currentPath The current path in the object tree
   */
  @Override
  public O fromDTO(D dto, O registrering, Set<String> paths, String currentPath)
      throws EInnsynException {
    super.fromDTO(dto, registrering, paths, currentPath);

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
      registrering.setPublisertDato(Instant.parse(dto.getPublisertDato()));
    } else if (registrering.getId() == null) {
      registrering.setPublisertDato(Instant.now());
    }

    return registrering;
  }

  /**
   * Convert a Registrering to a JSON object
   *
   * @param registrering
   * @param dto
   * @param expandPaths A list of paths to expand. Un-expanded objects will be shown as IDs
   * @param currentPath The current path in the object tree
   * @return
   */
  @Override
  public D toDTO(O registrering, D dto, Set<String> expandPaths, String currentPath) {

    super.toDTO(registrering, dto, expandPaths, currentPath);
    dto.setOffentligTittel(registrering.getOffentligTittel());
    dto.setOffentligTittelSensitiv(registrering.getOffentligTittelSensitiv());
    dto.setBeskrivelse(registrering.getBeskrivelse());
    if (registrering.getPublisertDato() != null) {
      dto.setPublisertDato(registrering.getPublisertDato().toString());
    }

    return dto;
  }
}

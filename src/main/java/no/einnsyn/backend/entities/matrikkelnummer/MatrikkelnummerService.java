package no.einnsyn.backend.entities.matrikkelnummer;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.backend.common.exceptions.models.BadRequestException;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.base.BaseService;
import no.einnsyn.backend.entities.mappe.models.Mappe;
import no.einnsyn.backend.entities.matrikkelnummer.models.Matrikkelnummer;
import no.einnsyn.backend.entities.matrikkelnummer.models.MatrikkelnummerDTO;
import no.einnsyn.backend.entities.registrering.models.Registrering;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatrikkelnummerService extends BaseService<Matrikkelnummer, MatrikkelnummerDTO> {

  @Getter(onMethod_ = @Override)
  private final MatrikkelnummerRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter(onMethod_ = @Override)
  @Lazy
  @Autowired
  private MatrikkelnummerService proxy;

  public MatrikkelnummerService(MatrikkelnummerRepository repository) {
    this.repository = repository;
  }

  @Override
  public Matrikkelnummer newObject() {
    return new Matrikkelnummer();
  }

  @Override
  public MatrikkelnummerDTO newDTO() {
    return new MatrikkelnummerDTO();
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public Matrikkelnummer createForMappe(ExpandableField<MatrikkelnummerDTO> dtoField, Mappe mappe)
      throws EInnsynException {
    if (mappe == null || mappe.getId() == null) {
      throw new BadRequestException("Matrikkelnummer must be attached to an existing mappe");
    }
    var matrikkelnummer = createChild(dtoField);
    matrikkelnummer.setMappeId(mappe.getId());
    return repository.save(matrikkelnummer);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public Matrikkelnummer createForRegistrering(
      ExpandableField<MatrikkelnummerDTO> dtoField, Registrering registrering)
      throws EInnsynException {
    if (registrering == null || registrering.getId() == null) {
      throw new BadRequestException("Matrikkelnummer must be attached to an existing registrering");
    }
    var matrikkelnummer = createChild(dtoField);
    matrikkelnummer.setRegistreringId(registrering.getId());
    return repository.save(matrikkelnummer);
  }

  private Matrikkelnummer createChild(ExpandableField<MatrikkelnummerDTO> dtoField)
      throws EInnsynException {
    if (dtoField == null) {
      throw new BadRequestException("Cannot create a null matrikkelnummer");
    }

    var dto = dtoField.requireExpandedObject();
    if (dto.getId() != null) {
      throw new BadRequestException("Matrikkelnummer must be provided as a new object");
    }
    if (dto.getKommunenummer() == null
        || dto.getGaardsnummer() == null
        || dto.getBruksnummer() == null) {
      throw new BadRequestException(
          "Matrikkelnummer must include kommunenummer, gaardsnummer and bruksnummer");
    }

    return fromDTO(dto, newObject());
  }

  @Override
  protected Matrikkelnummer fromDTO(MatrikkelnummerDTO dto, Matrikkelnummer matrikkelnummer)
      throws EInnsynException {
    super.fromDTO(dto, matrikkelnummer);

    if (dto.getKommunenummer() != null) {
      matrikkelnummer.setKommunenummer(dto.getKommunenummer());
    }

    if (dto.getGaardsnummer() != null) {
      matrikkelnummer.setGaardsnummer(dto.getGaardsnummer());
    }

    if (dto.getBruksnummer() != null) {
      matrikkelnummer.setBruksnummer(dto.getBruksnummer());
    }

    if (dto.getFestenummer() != null) {
      matrikkelnummer.setFestenummer(dto.getFestenummer());
    }

    if (dto.getSeksjonsnummer() != null) {
      matrikkelnummer.setSeksjonsnummer(dto.getSeksjonsnummer());
    }

    return matrikkelnummer;
  }

  @Override
  protected MatrikkelnummerDTO toDTO(
      Matrikkelnummer matrikkelnummer,
      MatrikkelnummerDTO dto,
      Set<String> expandPaths,
      String currentPath) {
    super.toDTO(matrikkelnummer, dto, expandPaths, currentPath);
    dto.setKommunenummer(matrikkelnummer.getKommunenummer());
    dto.setGaardsnummer(matrikkelnummer.getGaardsnummer());
    dto.setBruksnummer(matrikkelnummer.getBruksnummer());
    dto.setFestenummer(matrikkelnummer.getFestenummer());
    dto.setSeksjonsnummer(matrikkelnummer.getSeksjonsnummer());
    return dto;
  }
}

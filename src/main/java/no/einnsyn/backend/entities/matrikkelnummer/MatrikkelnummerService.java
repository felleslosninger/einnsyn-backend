package no.einnsyn.backend.entities.matrikkelnummer;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.backend.common.exceptions.models.AuthorizationException;
import no.einnsyn.backend.common.exceptions.models.BadRequestException;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseService;
import no.einnsyn.backend.entities.base.UniqueFieldMatch;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import no.einnsyn.backend.entities.matrikkelnummer.models.Matrikkelnummer;
import no.einnsyn.backend.entities.matrikkelnummer.models.MatrikkelnummerDTO;
import no.einnsyn.backend.utils.TimeConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatrikkelnummerService extends ArkivBaseService<Matrikkelnummer, MatrikkelnummerDTO> {

  private static final String ROOT_ENHET_EXTERNAL_ID = "root";

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

  private Enhet getRootEnhet() throws BadRequestException {
    return enhetService.findOrThrow(ROOT_ENHET_EXTERNAL_ID);
  }

  @Transactional(readOnly = true)
  @Override
  public UniqueFieldMatch<Matrikkelnummer> findUniqueFieldMatch(BaseDTO dto) {
    var matchById = super.findUniqueFieldMatch(dto);
    if (matchById != null) {
      return matchById;
    }

    if (dto instanceof MatrikkelnummerDTO matrikkelnummerDTO) {
      var matrikkelnummer =
          repository
              .findByKommunenummerAndGaardsnummerAndBruksnummerAndFestenummerAndSeksjonsnummer(
                  matrikkelnummerDTO.getKommunenummer(),
                  matrikkelnummerDTO.getGaardsnummer(),
                  matrikkelnummerDTO.getBruksnummer(),
                  matrikkelnummerDTO.getFestenummer(),
                  matrikkelnummerDTO.getSeksjonsnummer());
      if (matrikkelnummer != null) {
        return new UniqueFieldMatch<>(
            "[kommunenummer, gaardsnummer, bruksnummer, festenummer, seksjonsnummer]",
            matrikkelnummer);
      }
    }

    return null;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  @Override
  public Matrikkelnummer findOrCreate(ExpandableField<MatrikkelnummerDTO> dtoField)
      throws EInnsynException {
    if (dtoField == null) {
      throw new BadRequestException("Cannot lookup a null value");
    }

    var id = dtoField.getId();
    var dto = dtoField.getExpandedObject();

    if (id != null) {
      var matrikkelnummer = getProxy().findOrThrow(id);
      if (dto != null) {
        authorizeUpdate(matrikkelnummer.getId(), dto);
        matrikkelnummer = updateEntity(matrikkelnummer, dto);
      }
      return matrikkelnummer;
    }

    if (dto == null) {
      throw new BadRequestException("Cannot lookup Matrikkelnummer without id or expanded object");
    }

    var match = getProxy().findUniqueFieldMatch(dto);
    if (match != null) {
      return match.object();
    }

    return addEntity(dto);
  }

  @Override
  protected void authorizeUpdate(String id, MatrikkelnummerDTO dto) throws EInnsynException {
    if (!authenticationService.isAdmin()) {
      throw new AuthorizationException(
          "Not authorized to update " + objectClassName + " with id " + id);
    }
  }

  @Override
  public void authorizeDelete(String id) throws EInnsynException {
    if (!authenticationService.isAdmin()) {
      throw new AuthorizationException(
          "Not authorized to delete " + objectClassName + " with id " + id);
    }
  }

  @Override
  public boolean scheduleIndex(String matrikkelnummerId, int recurseDirection) {
    var isScheduled = super.scheduleIndex(matrikkelnummerId, recurseDirection);

    if (recurseDirection <= 0 && !isScheduled) {
      try (var journalpostStream =
          repository.streamJournalpostIdByMatrikkelnummerId(matrikkelnummerId)) {
        journalpostStream.forEach(id -> journalpostService.scheduleIndex(id, -1));
      }
      try (var saksmappeStream =
          repository.streamSaksmappeIdByMatrikkelnummerId(matrikkelnummerId)) {
        saksmappeStream.forEach(id -> saksmappeService.scheduleIndex(id, -1));
      }
    }

    return true;
  }

  @Override
  protected Matrikkelnummer fromDTO(MatrikkelnummerDTO dto, Matrikkelnummer matrikkelnummer)
      throws EInnsynException {
    if (dto.getExternalId() != null) {
      matrikkelnummer.setExternalId(dto.getExternalId());
    }

    if (dto.getAccessibleAfter() != null) {
      matrikkelnummer.setAccessibleAfter(
          TimeConverter.timestampToInstant(dto.getAccessibleAfter()));
    }

    if (dto.getSystemId() != null) {
      matrikkelnummer.setSystemId(dto.getSystemId());
    }

    matrikkelnummer.setJournalenhet(getRootEnhet());

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

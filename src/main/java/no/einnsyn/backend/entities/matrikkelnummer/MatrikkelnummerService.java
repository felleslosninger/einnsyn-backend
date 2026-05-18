package no.einnsyn.backend.entities.matrikkelnummer;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.backend.common.exceptions.models.BadRequestException;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseService;
import no.einnsyn.backend.entities.base.UniqueFieldMatch;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import no.einnsyn.backend.entities.matrikkelnummer.models.Matrikkelnummer;
import no.einnsyn.backend.entities.matrikkelnummer.models.MatrikkelnummerDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatrikkelnummerService extends ArkivBaseService<Matrikkelnummer, MatrikkelnummerDTO> {

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

  @Transactional(readOnly = true)
  @Override
  public UniqueFieldMatch<Matrikkelnummer> findUniqueFieldMatch(BaseDTO dto) {
    var matchById = super.findUniqueFieldMatch(dto);
    if (matchById != null) {
      return matchById;
    }

    if (dto instanceof MatrikkelnummerDTO matrikkelnummerDTO) {
      var journalenhet = getJournalenhetForUniqueMatch(matrikkelnummerDTO);
      if (journalenhet != null) {
        var matrikkelnummer =
            repository
                .findByJournalenhetAndKommunenummerAndGaardsnummerAndBruksnummerAndFestenummerAndSeksjonsnummer(
                    journalenhet,
                    matrikkelnummerDTO.getKommunenummer(),
                    matrikkelnummerDTO.getGaardsnummer(),
                    matrikkelnummerDTO.getBruksnummer(),
                    matrikkelnummerDTO.getFestenummer(),
                    matrikkelnummerDTO.getSeksjonsnummer());
        if (matrikkelnummer != null) {
          return new UniqueFieldMatch<>(
              "[journalenhet, kommunenummer, gaardsnummer, bruksnummer, festenummer, seksjonsnummer]",
              matrikkelnummer);
        }
      }
    }

    return null;
  }

  private Enhet getJournalenhetForUniqueMatch(MatrikkelnummerDTO dto) {
    var journalenhetField = dto.getJournalenhet();
    if (journalenhetField != null && journalenhetField.getId() != null) {
      return enhetService.find(journalenhetField.getId());
    }

    var authenticatedEnhetId = authenticationService.getEnhetId();
    if (authenticatedEnhetId != null) {
      return enhetService.find(authenticatedEnhetId);
    }

    return null;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public Matrikkelnummer findOrCreate(
      ExpandableField<MatrikkelnummerDTO> dtoField, Enhet journalenhet) throws EInnsynException {
    if (dtoField == null) {
      throw new BadRequestException("Cannot lookup a null value");
    }

    var dto = dtoField.getExpandedObject();
    if (dto != null && dto.getJournalenhet() == null && journalenhet != null) {
      dto.setJournalenhet(new ExpandableField<>(journalenhet.getId()));
    }

    var matrikkelnummer = getProxy().findOrCreate(dtoField);
    if (journalenhet != null
        && matrikkelnummer.getJournalenhet() != null
        && !journalenhet.getId().equals(matrikkelnummer.getJournalenhet().getId())) {
      throw new BadRequestException("Matrikkelnummer belongs to another journalenhet");
    }

    return matrikkelnummer;
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

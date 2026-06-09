package no.einnsyn.backend.entities.matrikkelnummer;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.backend.common.exceptions.models.AuthorizationException;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.entities.base.BaseService;
import no.einnsyn.backend.entities.matrikkelnummer.models.Matrikkelnummer;
import no.einnsyn.backend.entities.matrikkelnummer.models.MatrikkelnummerDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

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

  /**
   * Override scheduleIndex to reindex the parent entity in Elasticsearch.
   *
   * @param matrikkelnummerId the ID of the matrikkelnummer
   * @param recurseDirection -1 for parents, 1 for children, 0 for both
   */
  @Override
  public boolean scheduleIndex(String matrikkelnummerId, int recurseDirection) {
    var wasAlreadyScheduled = super.scheduleIndex(matrikkelnummerId, recurseDirection);

    if (recurseDirection <= 0 && !wasAlreadyScheduled) {
      var saksmappeId = repository.findSaksmappeIdById(matrikkelnummerId);
      if (saksmappeId != null) {
        saksmappeService.scheduleIndex(saksmappeId, -1);
        return wasAlreadyScheduled;
      }

      var moetemappeId = repository.findMoetemappeIdById(matrikkelnummerId);
      if (moetemappeId != null) {
        moetemappeService.scheduleIndex(moetemappeId, -1);
        return wasAlreadyScheduled;
      }

      var journalpostId = repository.findJournalpostIdById(matrikkelnummerId);
      if (journalpostId != null) {
        journalpostService.scheduleIndex(journalpostId, -1);
        return wasAlreadyScheduled;
      }

      var moetesakId = repository.findMoetesakIdById(matrikkelnummerId);
      if (moetesakId != null) {
        moetesakService.scheduleIndex(moetesakId, -1);
        return wasAlreadyScheduled;
      }

      var moetedokumentId = repository.findMoetedokumentIdById(matrikkelnummerId);
      if (moetedokumentId != null) {
        moetedokumentService.scheduleIndex(moetedokumentId, -1);
        return wasAlreadyScheduled;
      }
    }

    return wasAlreadyScheduled;
  }

  @Override
  protected void authorizeDelete(String id) throws EInnsynException {
    var matrikkelnummer = proxy.findOrThrow(id);

    if (matrikkelnummer.getSaksmappe() != null) {
      saksmappeService.authorizeDelete(matrikkelnummer.getSaksmappe().getId());
    } else if (matrikkelnummer.getMoetemappe() != null) {
      moetemappeService.authorizeDelete(matrikkelnummer.getMoetemappe().getId());
    } else if (matrikkelnummer.getJournalpost() != null) {
      journalpostService.authorizeDelete(matrikkelnummer.getJournalpost().getId());
    } else if (matrikkelnummer.getMoetesak() != null) {
      moetesakService.authorizeDelete(matrikkelnummer.getMoetesak().getId());
    } else if (matrikkelnummer.getMoetedokument() != null) {
      moetedokumentService.authorizeDelete(matrikkelnummer.getMoetedokument().getId());
    } else {
      throw new AuthorizationException("Not authorized to delete " + id);
    }
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

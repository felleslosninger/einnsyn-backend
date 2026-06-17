package no.einnsyn.backend.entities.matrikkelnummer;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.exceptions.models.InternalServerErrorException;
import no.einnsyn.backend.common.paginators.Paginators;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseService;
import no.einnsyn.backend.entities.journalpost.models.Journalpost;
import no.einnsyn.backend.entities.journalpost.models.ListByJournalpostParameters;
import no.einnsyn.backend.entities.matrikkelnummer.models.Matrikkelnummer;
import no.einnsyn.backend.entities.matrikkelnummer.models.MatrikkelnummerDTO;
import no.einnsyn.backend.entities.moetedokument.models.ListByMoetedokumentParameters;
import no.einnsyn.backend.entities.moetedokument.models.Moetedokument;
import no.einnsyn.backend.entities.moetemappe.models.ListByMoetemappeParameters;
import no.einnsyn.backend.entities.moetemappe.models.Moetemappe;
import no.einnsyn.backend.entities.moetesak.models.ListByMoetesakParameters;
import no.einnsyn.backend.entities.moetesak.models.Moetesak;
import no.einnsyn.backend.entities.saksmappe.models.ListBySaksmappeParameters;
import no.einnsyn.backend.entities.saksmappe.models.Saksmappe;
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

  @Transactional(propagation = Propagation.MANDATORY)
  public Matrikkelnummer findOrCreateAndAddToParent(MatrikkelnummerDTO dto, Object parent)
      throws EInnsynException {
    return switch (parent) {
      case Saksmappe saksmappe -> {
        var existing =
            repository
                .findBySaksmappeAndKommunenummerAndGaardsnummerAndBruksnummerAndFestenummerAndSeksjonsnummer(
                    saksmappe,
                    dto.getKommunenummer(),
                    dto.getGaardsnummer(),
                    dto.getBruksnummer(),
                    dto.getFestenummer(),
                    dto.getSeksjonsnummer());
        if (existing.isPresent()) {
          saksmappe.addMatrikkelnummer(existing.get());
          yield existing.get();
        } else {
          var m = newObject();
          fromDTO(dto, m);
          m.setSaksmappe(saksmappe);
          repository.saveAndFlush(m);
          saksmappe.addMatrikkelnummer(m);
          yield m;
        }
      }
      case Moetemappe moetemappe -> {
        var existing =
            repository
                .findByMoetemappeAndKommunenummerAndGaardsnummerAndBruksnummerAndFestenummerAndSeksjonsnummer(
                    moetemappe,
                    dto.getKommunenummer(),
                    dto.getGaardsnummer(),
                    dto.getBruksnummer(),
                    dto.getFestenummer(),
                    dto.getSeksjonsnummer());
        if (existing.isPresent()) {
          moetemappe.addMatrikkelnummer(existing.get());
          yield existing.get();
        } else {
          var m = newObject();
          fromDTO(dto, m);
          m.setMoetemappe(moetemappe);
          repository.saveAndFlush(m);
          moetemappe.addMatrikkelnummer(m);
          yield m;
        }
      }
      case Journalpost journalpost -> {
        var existing =
            repository
                .findByJournalpostAndKommunenummerAndGaardsnummerAndBruksnummerAndFestenummerAndSeksjonsnummer(
                    journalpost,
                    dto.getKommunenummer(),
                    dto.getGaardsnummer(),
                    dto.getBruksnummer(),
                    dto.getFestenummer(),
                    dto.getSeksjonsnummer());
        if (existing.isPresent()) {
          journalpost.addMatrikkelnummer(existing.get());
          yield existing.get();
        } else {
          var m = newObject();
          fromDTO(dto, m);
          m.setJournalpost(journalpost);
          repository.saveAndFlush(m);
          journalpost.addMatrikkelnummer(m);
          yield m;
        }
      }
      case Moetesak moetesak -> {
        var existing =
            repository
                .findByMoetesakAndKommunenummerAndGaardsnummerAndBruksnummerAndFestenummerAndSeksjonsnummer(
                    moetesak,
                    dto.getKommunenummer(),
                    dto.getGaardsnummer(),
                    dto.getBruksnummer(),
                    dto.getFestenummer(),
                    dto.getSeksjonsnummer());
        if (existing.isPresent()) {
          moetesak.addMatrikkelnummer(existing.get());
          yield existing.get();
        } else {
          var m = newObject();
          fromDTO(dto, m);
          m.setMoetesak(moetesak);
          repository.saveAndFlush(m);
          moetesak.addMatrikkelnummer(m);
          yield m;
        }
      }
      case Moetedokument moetedokument -> {
        var existing =
            repository
                .findByMoetedokumentAndKommunenummerAndGaardsnummerAndBruksnummerAndFestenummerAndSeksjonsnummer(
                    moetedokument,
                    dto.getKommunenummer(),
                    dto.getGaardsnummer(),
                    dto.getBruksnummer(),
                    dto.getFestenummer(),
                    dto.getSeksjonsnummer());
        if (existing.isPresent()) {
          moetedokument.addMatrikkelnummer(existing.get());
          yield existing.get();
        } else {
          var m = newObject();
          fromDTO(dto, m);
          m.setMoetedokument(moetedokument);
          repository.saveAndFlush(m);
          moetedokument.addMatrikkelnummer(m);
          yield m;
        }
      }
      default ->
          throw new InternalServerErrorException(
              "Unsupported parent type for Matrikkelnummer: " + parent.getClass().getSimpleName());
    };
  }

  @Override
  protected Paginators<Matrikkelnummer> getPaginators(ListParameters params)
      throws EInnsynException {
    if (params instanceof ListBySaksmappeParameters p && p.getSaksmappeId() != null) {
      var saksmappe = saksmappeService.findOrThrow(p.getSaksmappeId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(saksmappe, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(saksmappe, pivot, pageRequest));
    }
    if (params instanceof ListByMoetemappeParameters p && p.getMoetemappeId() != null) {
      var moetemappe = moetemappeService.findOrThrow(p.getMoetemappeId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(moetemappe, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(moetemappe, pivot, pageRequest));
    }
    if (params instanceof ListByJournalpostParameters p && p.getJournalpostId() != null) {
      var journalpost = journalpostService.findOrThrow(p.getJournalpostId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(journalpost, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(journalpost, pivot, pageRequest));
    }
    if (params instanceof ListByMoetesakParameters p && p.getMoetesakId() != null) {
      var moetesak = moetesakService.findOrThrow(p.getMoetesakId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(moetesak, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(moetesak, pivot, pageRequest));
    }
    if (params instanceof ListByMoetedokumentParameters p && p.getMoetedokumentId() != null) {
      var moetedokument = moetedokumentService.findOrThrow(p.getMoetedokumentId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(moetedokument, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(moetedokument, pivot, pageRequest));
    }
    return super.getPaginators(params);
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

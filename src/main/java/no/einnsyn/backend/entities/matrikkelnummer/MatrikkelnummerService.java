package no.einnsyn.backend.entities.matrikkelnummer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import no.einnsyn.backend.common.exceptions.models.BadRequestException;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.paginators.Paginators;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseService;
import no.einnsyn.backend.entities.base.UniqueFieldMatch;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.base.models.BaseES;
import no.einnsyn.backend.entities.journalpost.models.ListByJournalpostParameters;
import no.einnsyn.backend.entities.matrikkelnummer.models.Matrikkelnummer;
import no.einnsyn.backend.entities.matrikkelnummer.models.MatrikkelnummerDTO;
import no.einnsyn.backend.entities.matrikkelnummer.models.MatrikkelnummerES;
import no.einnsyn.backend.entities.moetedokument.models.ListByMoetedokumentParameters;
import no.einnsyn.backend.entities.moetemappe.models.ListByMoetemappeParameters;
import no.einnsyn.backend.entities.moetesak.models.ListByMoetesakParameters;
import no.einnsyn.backend.entities.saksmappe.models.ListBySaksmappeParameters;
import no.einnsyn.backend.utils.ExpandPathResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
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

  @Override
  public MatrikkelnummerDTO add(MatrikkelnummerDTO dto) throws EInnsynException {
    if (dto.getId() != null) {
      throw new BadRequestException(
          "Cannot create a Matrikkelnummer with an ID set: " + dto.getId());
    }
    authorizeAdd(dto);
    var existingMatch = getProxy().findUniqueFieldMatch(dto);
    if (existingMatch != null) {
      return getProxy().toDTO(existingMatch.object(), ExpandPathResolver.resolve(dto));
    }
    var paths = ExpandPathResolver.resolve(dto);
    var added = addEntity(dto);
    scheduleIndex(added.getId());
    return getProxy().toDTO(added, paths);
  }

  @Override
  @Transactional(readOnly = true)
  public UniqueFieldMatch<Matrikkelnummer> findUniqueFieldMatch(BaseDTO baseDTO) {
    if (baseDTO instanceof MatrikkelnummerDTO dto) {
      if (dto.getSaksmappe() != null && dto.getKommunenummer() != null) {
        var saksmappe = saksmappeService.find(dto.getSaksmappe().getId());
        if (saksmappe != null) {
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
            return new UniqueFieldMatch<>("matrikkelnummer", existing.get());
          }
        }
      }
      if (dto.getMoetemappe() != null && dto.getKommunenummer() != null) {
        var moetemappe = moetemappeService.find(dto.getMoetemappe().getId());
        if (moetemappe != null) {
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
            return new UniqueFieldMatch<>("matrikkelnummer", existing.get());
          }
        }
      }
      if (dto.getJournalpost() != null && dto.getKommunenummer() != null) {
        var journalpost = journalpostService.find(dto.getJournalpost().getId());
        if (journalpost != null) {
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
            return new UniqueFieldMatch<>("matrikkelnummer", existing.get());
          }
        }
      }
      if (dto.getMoetesak() != null && dto.getKommunenummer() != null) {
        var moetesak = moetesakService.find(dto.getMoetesak().getId());
        if (moetesak != null) {
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
            return new UniqueFieldMatch<>("matrikkelnummer", existing.get());
          }
        }
      }
      if (dto.getMoetedokument() != null && dto.getKommunenummer() != null) {
        var moetedokument = moetedokumentService.find(dto.getMoetedokument().getId());
        if (moetedokument != null) {
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
            return new UniqueFieldMatch<>("matrikkelnummer", existing.get());
          }
        }
      }
    }
    return super.findUniqueFieldMatch(baseDTO);
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

    // Set parent relationship — same pattern as KorrespondansepartService
    if (dto.getSaksmappe() != null) {
      var saksmappe = saksmappeService.findForUpdateOrThrow(dto.getSaksmappe());
      saksmappe.addMatrikkelnummer(matrikkelnummer);
    } else if (dto.getMoetemappe() != null) {
      var moetemappe = moetemappeService.findForUpdateOrThrow(dto.getMoetemappe());
      moetemappe.addMatrikkelnummer(matrikkelnummer);
    } else if (dto.getJournalpost() != null) {
      var journalpost = journalpostService.findForUpdateOrThrow(dto.getJournalpost());
      journalpost.addMatrikkelnummer(matrikkelnummer);
    } else if (dto.getMoetesak() != null) {
      var moetesak = moetesakService.findForUpdateOrThrow(dto.getMoetesak());
      moetesak.addMatrikkelnummer(matrikkelnummer);
    } else if (dto.getMoetedokument() != null) {
      var moetedokument = moetedokumentService.findForUpdateOrThrow(dto.getMoetedokument());
      moetedokument.addMatrikkelnummer(matrikkelnummer);
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

    if (matrikkelnummer.getSaksmappe() != null) {
      dto.setSaksmappe(
          saksmappeService.maybeExpand(
              matrikkelnummer.getSaksmappe(), "saksmappe", expandPaths, currentPath));
    } else if (matrikkelnummer.getMoetemappe() != null) {
      dto.setMoetemappe(
          moetemappeService.maybeExpand(
              matrikkelnummer.getMoetemappe(), "moetemappe", expandPaths, currentPath));
    } else if (matrikkelnummer.getJournalpost() != null) {
      dto.setJournalpost(
          journalpostService.maybeExpand(
              matrikkelnummer.getJournalpost(), "journalpost", expandPaths, currentPath));
    } else if (matrikkelnummer.getMoetesak() != null) {
      dto.setMoetesak(
          moetesakService.maybeExpand(
              matrikkelnummer.getMoetesak(), "moetesak", expandPaths, currentPath));
    } else if (matrikkelnummer.getMoetedokument() != null) {
      dto.setMoetedokument(
          moetedokumentService.maybeExpand(
              matrikkelnummer.getMoetedokument(), "moetedokument", expandPaths, currentPath));
    }

    return dto;
  }

  @Override
  public BaseES toLegacyES(Matrikkelnummer matrikkelnummer, BaseES es) {
    super.toLegacyES(matrikkelnummer, es);
    if (es instanceof MatrikkelnummerES mnES) {
      mnES.setKommunenummer(matrikkelnummer.getKommunenummer());
      mnES.setGaardsnummer(matrikkelnummer.getGaardsnummer());
      mnES.setBruksnummer(matrikkelnummer.getBruksnummer());
      mnES.setFestenummer(matrikkelnummer.getFestenummer());
      mnES.setSeksjonsnummer(matrikkelnummer.getSeksjonsnummer());
      mnES.setMatrikkelId(buildMatrikkelIds(matrikkelnummer));
    }
    return es;
  }

  private static List<String> buildMatrikkelIds(Matrikkelnummer m) {
    var k = m.getKommunenummer();
    var g = m.getGaardsnummer();
    var b = m.getBruksnummer();
    int f = m.getFestenummer() != null ? m.getFestenummer() : 0;
    int s = m.getSeksjonsnummer() != null ? m.getSeksjonsnummer() : 0;

    if (k == null || g == null || b == null) {
      return List.of();
    }

    var ids = new ArrayList<String>();
    ids.add(g + "/" + b);
    ids.add(k + "-" + g + "/" + b);
    ids.add(k + "/" + g + "/" + b);
    ids.add(k + "-" + g + "/" + b + "/" + f + "/" + s);
    ids.add(k + "/" + g + "/" + b + "/" + f + "/" + s);
    return ids;
  }
}

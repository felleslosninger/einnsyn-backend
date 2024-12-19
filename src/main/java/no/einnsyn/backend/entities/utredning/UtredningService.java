package no.einnsyn.backend.entities.utredning;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.backend.common.responses.models.ListResponseBody;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseService;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.backend.entities.moetesak.MoetesakRepository;
import no.einnsyn.backend.entities.utredning.models.ListByUtredningParameters;
import no.einnsyn.backend.entities.utredning.models.Utredning;
import no.einnsyn.backend.entities.utredning.models.UtredningDTO;
import no.einnsyn.backend.error.exceptions.EInnsynException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UtredningService extends ArkivBaseService<Utredning, UtredningDTO> {

  @Getter private final UtredningRepository repository;

  private final MoetesakRepository moetesakRepository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private UtredningService proxy;

  public UtredningService(UtredningRepository repository, MoetesakRepository moetesakRepository) {
    this.repository = repository;
    this.moetesakRepository = moetesakRepository;
  }

  public Utredning newObject() {
    return new Utredning();
  }

  public UtredningDTO newDTO() {
    return new UtredningDTO();
  }

  /**
   * Override scheduleIndex to also reindex the parent moetesak.
   *
   * @param utredning
   * @param recurseDirection -1 for parents, 1 for children, 0 for both
   */
  @Override
  public void scheduleIndex(Utredning utredning, int recurseDirection) {
    super.scheduleIndex(utredning, recurseDirection);

    // Index moetesak
    if (recurseDirection <= 0) {
      var moetesak = moetesakRepository.findByUtredning(utredning);
      if (moetesak != null) {
        moetesakService.scheduleIndex(moetesak, -1);
      }
    }
  }

  @Override
  protected Utredning fromDTO(UtredningDTO dto, Utredning utredning) throws EInnsynException {
    super.fromDTO(dto, utredning);

    // Workaround since legacy IDs are used for relations. OneToMany relations fails if the ID is
    // not set.
    if (utredning.getId() == null) {
      utredning = repository.saveAndFlush(utredning);
    }

    // Saksbeskrivelse
    if (dto.getSaksbeskrivelse() != null) {
      // Replace?
      var oldSaksbeskrivelse = utredning.getSaksbeskrivelse();
      if (oldSaksbeskrivelse != null) {
        utredning.setSaksbeskrivelse(null);
        moetesaksbeskrivelseService.delete(oldSaksbeskrivelse.getId());
      }
      var saksbeskrivelse =
          moetesaksbeskrivelseService.createOrReturnExisting(dto.getSaksbeskrivelse());
      utredning.setSaksbeskrivelse(saksbeskrivelse);
    }

    // Innstilling
    if (dto.getInnstilling() != null) {
      // Replace?
      var oldInnstilling = utredning.getInnstilling();
      if (oldInnstilling != null) {
        utredning.setInnstilling(null);
        moetesaksbeskrivelseService.delete(oldInnstilling.getId());
      }
      var innstilling = moetesaksbeskrivelseService.createOrReturnExisting(dto.getInnstilling());
      utredning.setInnstilling(innstilling);
    }

    // Utredningsdokument
    if (dto.getUtredningsdokument() != null) {
      for (var dokument : dto.getUtredningsdokument()) {
        utredning.addUtredningsdokument(
            dokumentbeskrivelseService.createOrReturnExisting(dokument));
      }
    }

    return utredning;
  }

  @Override
  protected UtredningDTO toDTO(
      Utredning utredning, UtredningDTO dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(utredning, dto, expandPaths, currentPath);

    // Saksbeskrivelse
    var saksbeskrivelse = utredning.getSaksbeskrivelse();
    if (saksbeskrivelse != null) {
      dto.setSaksbeskrivelse(
          moetesaksbeskrivelseService.maybeExpand(
              saksbeskrivelse, "saksbeskrivelse", expandPaths, currentPath));
    }

    // Innstilling
    var innstilling = utredning.getInnstilling();
    if (innstilling != null) {
      dto.setInnstilling(
          moetesaksbeskrivelseService.maybeExpand(
              innstilling, "innstilling", expandPaths, currentPath));
    }

    // Utredningsdokument
    var utredningsdokumentList = utredning.getUtredningsdokument();
    if (utredningsdokumentList != null) {
      dto.setUtredningsdokument(
          utredningsdokumentList.stream()
              .map(
                  dokument ->
                      dokumentbeskrivelseService.maybeExpand(
                          dokument, "utredningsdokument", expandPaths, currentPath))
              .toList());
    }

    return dto;
  }

  public ListResponseBody<DokumentbeskrivelseDTO> listUtredningsdokument(
      String utredningId, ListByUtredningParameters query) throws EInnsynException {
    query.setUtredningId(utredningId);
    return dokumentbeskrivelseService.list(query);
  }

  @Transactional(rollbackFor = Exception.class)
  @Retryable
  public DokumentbeskrivelseDTO addUtredningsdokument(
      String utredningId, DokumentbeskrivelseDTO dokumentbeskrivelseDTO) throws EInnsynException {
    dokumentbeskrivelseDTO = dokumentbeskrivelseService.add(dokumentbeskrivelseDTO);
    var dokumentbeskrivelse = dokumentbeskrivelseService.findById(dokumentbeskrivelseDTO.getId());
    var utredning = utredningService.findById(utredningId);
    utredning.addUtredningsdokument(dokumentbeskrivelse);
    utredningService.scheduleIndex(utredning, -1);

    return dokumentbeskrivelseDTO;
  }

  @Transactional(rollbackFor = Exception.class)
  @Retryable
  public DokumentbeskrivelseDTO deleteUtredningsdokument(
      String utredningId, String utredningsdokumentId) throws EInnsynException {
    var utredning = utredningService.findById(utredningId);
    var dokumentbeskrivelse = dokumentbeskrivelseService.findById(utredningsdokumentId);
    var utredningsdokumentList = utredning.getUtredningsdokument();
    if (utredningsdokumentList != null) {
      utredning.setUtredningsdokument(
          utredningsdokumentList.stream()
              .filter(dokument -> !dokument.getId().equals(utredningsdokumentId))
              .toList());
    }
    return dokumentbeskrivelseService.deleteIfOrphan(dokumentbeskrivelse);
  }

  @Override
  protected void deleteEntity(Utredning utredning) throws EInnsynException {
    // Delete saksbeskrivelse
    var saksbeskrivelse = utredning.getSaksbeskrivelse();
    if (saksbeskrivelse != null) {
      utredning.setSaksbeskrivelse(null);
      moetesaksbeskrivelseService.delete(saksbeskrivelse.getId());
    }

    // Delete innstilling
    var innstilling = utredning.getInnstilling();
    if (innstilling != null) {
      utredning.setInnstilling(null);
      moetesaksbeskrivelseService.delete(innstilling.getId());
    }

    // Delete all utredningsdokument
    var utredningsdokumentList = utredning.getUtredningsdokument();
    if (utredningsdokumentList != null) {
      utredning.setUtredningsdokument(null);
      for (var dokument : utredningsdokumentList) {
        dokumentbeskrivelseService.deleteIfOrphan(dokument);
      }
    }

    // Remove link from moetesak
    var moetesak = moetesakRepository.findByUtredning(utredning);
    if (moetesak != null) {
      moetesak.setUtredning(null);
    }

    super.deleteEntity(utredning);
  }
}

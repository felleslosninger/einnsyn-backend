package no.einnsyn.apiv3.entities.utredning;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.utredning.models.Utredning;
import no.einnsyn.apiv3.entities.utredning.models.UtredningDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UtredningService extends ArkivBaseService<Utredning, UtredningDTO> {

  @Getter private final UtredningRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private UtredningService proxy;

  public UtredningService(UtredningRepository repository) {
    this.repository = repository;
  }

  public Utredning newObject() {
    return new Utredning();
  }

  public UtredningDTO newDTO() {
    return new UtredningDTO();
  }

  @Override
  public Utredning fromDTO(
      UtredningDTO dto, Utredning utredning, Set<String> paths, String currentPath)
      throws EInnsynException {
    super.fromDTO(dto, utredning, paths, currentPath);

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
        moetesaksbeskrivelseService.delete(oldSaksbeskrivelse);
      }
      var saksbeskrivelse =
          moetesaksbeskrivelseService.insertOrReturnExisting(
              dto.getSaksbeskrivelse(), "saksbeskrivelse", paths, currentPath);
      utredning.setSaksbeskrivelse(saksbeskrivelse);
    }

    // Innstilling
    if (dto.getInnstilling() != null) {
      // Replace?
      var oldInnstilling = utredning.getInnstilling();
      if (oldInnstilling != null) {
        utredning.setInnstilling(null);
        moetesaksbeskrivelseService.delete(oldInnstilling);
      }
      var innstilling =
          moetesaksbeskrivelseService.insertOrReturnExisting(
              dto.getInnstilling(), "innstilling", paths, currentPath);
      utredning.setInnstilling(innstilling);
    }

    // Utredningsdokument
    if (dto.getUtredningsdokument() != null) {
      for (var dokument : dto.getUtredningsdokument()) {
        utredning.addUtredningsdokument(
            dokumentbeskrivelseService.insertOrReturnExisting(
                dokument, "utredningsdokument", paths, currentPath));
      }
    }

    return utredning;
  }

  @Override
  public UtredningDTO toDTO(
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

  @Transactional
  public UtredningDTO delete(Utredning utredning) {
    var dto = proxy.toDTO(utredning);

    // Delete saksbeskrivelse
    var saksbeskrivelse = utredning.getSaksbeskrivelse();
    if (saksbeskrivelse != null) {
      utredning.setSaksbeskrivelse(null);
      moetesaksbeskrivelseService.delete(saksbeskrivelse);
    }

    // Delete innstilling
    var innstilling = utredning.getInnstilling();
    if (innstilling != null) {
      utredning.setInnstilling(null);
      moetesaksbeskrivelseService.delete(innstilling);
    }

    // Delete all utredningsdokument
    var utredningsdokumentList = utredning.getUtredningsdokument();
    if (utredningsdokumentList != null) {
      utredning.setUtredningsdokument(null);
      utredningsdokumentList.forEach(dokumentbeskrivelseService::deleteIfOrphan);
    }

    dto.setDeleted(true);
    repository.delete(utredning);
    return dto;
  }
}

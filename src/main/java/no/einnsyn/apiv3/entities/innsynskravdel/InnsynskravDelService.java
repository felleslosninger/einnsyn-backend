package no.einnsyn.apiv3.entities.innsynskravdel;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.entities.base.BaseService;
import no.einnsyn.apiv3.entities.innsynskrav.InnsynskravRepository;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDel;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelDTO;
import no.einnsyn.apiv3.entities.journalpost.JournalpostRepository;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InnsynskravDelService extends BaseService<InnsynskravDel, InnsynskravDelDTO> {

  @Getter private final InnsynskravDelRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private InnsynskravDelService proxy;

  private final InnsynskravRepository innsynskravRepository;

  private final JournalpostRepository journalpostRepository;

  public InnsynskravDelService(
      InnsynskravDelRepository repository,
      InnsynskravRepository innsynskravRepository,
      JournalpostRepository journalpostRepository) {
    super();
    this.repository = repository;
    this.innsynskravRepository = innsynskravRepository;
    this.journalpostRepository = journalpostRepository;
  }

  public InnsynskravDel newObject() {
    return new InnsynskravDel();
  }

  public InnsynskravDelDTO newDTO() {
    return new InnsynskravDelDTO();
  }

  /**
   * Convert JSON to InnsynskravDel.
   *
   * @param dto
   * @param innsynskravDel
   * @param paths
   * @param currentPath
   * @return
   */
  @Override
  public InnsynskravDel fromDTO(
      InnsynskravDelDTO dto, InnsynskravDel innsynskravDel, Set<String> paths, String currentPath)
      throws EInnsynException {
    super.fromDTO(dto, innsynskravDel, paths, currentPath);

    // Set reference to innsynskrav
    if (dto.getInnsynskrav() != null) {
      var innsynskrav = innsynskravRepository.findById(dto.getInnsynskrav().getId()).orElse(null);
      innsynskravDel.setInnsynskrav(innsynskrav);
    }

    // Set reference to journalpost
    if (dto.getJournalpost() != null) {
      var journalpost = journalpostRepository.findById(dto.getJournalpost().getId()).orElse(null);
      innsynskravDel.setJournalpost(journalpost);
    }

    // If the object doesn't exist, set the Enhet from the Journalpost
    if (innsynskravDel.getId() == null && innsynskravDel.getEnhet() == null) {
      Journalpost journalpost = innsynskravDel.getJournalpost();
      innsynskravDel.setEnhet(journalpost.getJournalenhet());
    }

    return innsynskravDel;
  }

  /**
   * Convert InnsynskravDel to JSON.
   *
   * @param innsynskravDel
   * @param dto
   * @param expandPaths
   * @param currentPath
   * @return
   */
  @Override
  public InnsynskravDelDTO toDTO(
      InnsynskravDel innsynskravDel,
      InnsynskravDelDTO dto,
      Set<String> expandPaths,
      String currentPath) {
    dto = super.toDTO(innsynskravDel, dto, expandPaths, currentPath);

    // Journalpost
    var journalpost = innsynskravDel.getJournalpost();
    dto.setJournalpost(
        journalpostService.maybeExpand(journalpost, "journalpost", expandPaths, currentPath));

    // Enhet
    var enhet = innsynskravDel.getEnhet();
    dto.setEnhet(enhetService.maybeExpand(enhet, "enhet", expandPaths, currentPath));

    if (innsynskravDel.getSent() != null) {
      dto.setSent(innsynskravDel.getSent().toString());
    }

    return dto;
  }

  @Transactional
  public InnsynskravDelDTO delete(InnsynskravDel innsynskravDel) {
    var dto = newDTO();
    var innsynskrav = innsynskravDel.getInnsynskrav();

    // Remove reference to this innsynskravDel from innsynskrav
    innsynskrav.getInnsynskravDel().remove(innsynskravDel);

    repository.delete(innsynskravDel);
    dto.setDeleted(true);
    return dto;
  }
}

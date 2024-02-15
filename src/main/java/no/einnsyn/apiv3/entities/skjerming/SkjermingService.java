package no.einnsyn.apiv3.entities.skjerming;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.journalpost.JournalpostRepository;
import no.einnsyn.apiv3.entities.skjerming.models.Skjerming;
import no.einnsyn.apiv3.entities.skjerming.models.SkjermingDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SkjermingService extends ArkivBaseService<Skjerming, SkjermingDTO> {

  @Getter private final SkjermingRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private SkjermingService proxy;

  private final JournalpostRepository journalpostRepository;

  public SkjermingService(
      SkjermingRepository repository, JournalpostRepository journalpostRepository) {
    this.repository = repository;
    this.journalpostRepository = journalpostRepository;
  }

  public Skjerming newObject() {
    return new Skjerming();
  }

  public SkjermingDTO newDTO() {
    return new SkjermingDTO();
  }

  /**
   * Update a Skjerming object from a JSON object
   *
   * @param dto
   * @param skjerming
   * @param paths A list of paths containing new objects that will be created from this update
   * @param currentPath The current path in the object tree
   * @return
   */
  @Override
  public Skjerming fromDTO(
      SkjermingDTO dto, Skjerming skjerming, Set<String> paths, String currentPath)
      throws EInnsynException {
    super.fromDTO(dto, skjerming, paths, currentPath);

    if (dto.getTilgangsrestriksjon() != null) {
      skjerming.setTilgangsrestriksjon(dto.getTilgangsrestriksjon());
    }

    if (dto.getSkjermingshjemmel() != null) {
      skjerming.setSkjermingshjemmel(dto.getSkjermingshjemmel());
    }

    return skjerming;
  }

  /**
   * Convert a Skjerming object to a JSON object
   *
   * @param skjerming
   * @param dto
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return
   */
  @Override
  public SkjermingDTO toDTO(
      Skjerming skjerming, SkjermingDTO dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(skjerming, dto, expandPaths, currentPath);

    if (skjerming.getTilgangsrestriksjon() != null) {
      dto.setTilgangsrestriksjon(skjerming.getTilgangsrestriksjon());
    }

    if (skjerming.getSkjermingshjemmel() != null) {
      dto.setSkjermingshjemmel(skjerming.getSkjermingshjemmel());
    }

    return dto;
  }

  /**
   * Delete a Skjerming if no journalposts refer to it
   *
   * @param skjerming
   * @return
   */
  @Transactional
  public SkjermingDTO deleteIfOrphan(Skjerming skjerming) throws EInnsynException {
    var hasJournalpostRelations = journalpostRepository.existsBySkjerming(skjerming);
    if (hasJournalpostRelations) {
      return proxy.toDTO(skjerming);
    } else {
      return skjermingService.delete(skjerming);
    }
  }
}

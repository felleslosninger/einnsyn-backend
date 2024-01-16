package no.einnsyn.apiv3.entities.skjerming;

import jakarta.transaction.Transactional;
import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.entities.base.BaseService;
import no.einnsyn.apiv3.entities.journalpost.JournalpostRepository;
import no.einnsyn.apiv3.entities.skjerming.models.Skjerming;
import no.einnsyn.apiv3.entities.skjerming.models.SkjermingDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class SkjermingService extends BaseService<Skjerming, SkjermingDTO> {

  @Getter private final SkjermingRepository repository;

  @Getter @Lazy @Autowired private SkjermingService proxy;

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
      SkjermingDTO dto, Skjerming skjerming, Set<String> paths, String currentPath) {
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
   * Delete a Skjerming
   *
   * @param skjerming
   * @return
   */
  @Transactional
  public SkjermingDTO delete(Skjerming object) {
    var dto = getProxy().toDTO(object);
    dto.setDeleted(true);
    repository.delete(object);
    return dto;
  }

  /**
   * Delete a Skjerming if no journalposts refer to it
   *
   * @param skjerming
   * @return
   */
  @Transactional
  public SkjermingDTO deleteIfOrphan(Skjerming skjerming) {
    int journalpostRelations = journalpostRepository.countBySkjerming(skjerming);
    if (journalpostRelations > 0) {
      var dto = getProxy().toDTO(skjerming);
      dto.setDeleted(false);
      return dto;
    } else {
      return delete(skjerming);
    }
  }
}

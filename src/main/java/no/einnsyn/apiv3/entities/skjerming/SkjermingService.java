package no.einnsyn.apiv3.entities.skjerming;

import java.util.Set;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import lombok.Getter;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.journalpost.JournalpostRepository;
import no.einnsyn.apiv3.entities.skjerming.models.Skjerming;
import no.einnsyn.apiv3.entities.skjerming.models.SkjermingJSON;

@Service
public class SkjermingService extends EinnsynObjectService<Skjerming, SkjermingJSON> {

  @Getter
  private final SkjermingRepository repository;

  @Getter
  private SkjermingService service = this;

  private final JournalpostRepository journalpostRepository;

  public SkjermingService(SkjermingRepository repository,
      JournalpostRepository journalpostRepository) {
    this.repository = repository;
    this.journalpostRepository = journalpostRepository;
  }

  public Skjerming newObject() {
    return new Skjerming();
  }

  public SkjermingJSON newJSON() {
    return new SkjermingJSON();
  }


  /**
   * Update a Skjerming object from a JSON object
   * 
   * @param json
   * @param skjerming
   * @param paths A list of paths containing new objects that will be created from this update
   * @param currentPath The current path in the object tree
   * @return
   */
  @Override
  public Skjerming fromJSON(SkjermingJSON json, Skjerming skjerming, Set<String> paths,
      String currentPath) {
    super.fromJSON(json, skjerming, paths, currentPath);

    if (json.getTilgangsrestriksjon() != null) {
      skjerming.setTilgangsrestriksjon(json.getTilgangsrestriksjon());
    }

    if (json.getSkjermingshjemmel() != null) {
      skjerming.setSkjermingshjemmel(json.getSkjermingshjemmel());
    }

    return skjerming;
  }


  /**
   * Convert a Skjerming object to a JSON object
   * 
   * @param skjerming
   * @param json
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return
   */
  @Override
  public SkjermingJSON toJSON(Skjerming skjerming, SkjermingJSON json, Set<String> expandPaths,
      String currentPath) {
    super.toJSON(skjerming, json, expandPaths, currentPath);

    if (skjerming.getTilgangsrestriksjon() != null) {
      json.setTilgangsrestriksjon(skjerming.getTilgangsrestriksjon());
    }

    if (skjerming.getSkjermingshjemmel() != null) {
      json.setSkjermingshjemmel(skjerming.getSkjermingshjemmel());
    }

    return json;
  }


  /**
   * Delete a Skjerming
   * 
   * @param id
   * @return
   */
  @Transactional
  public SkjermingJSON delete(String id) {
    // This ID should be verified in the controller, so it should always exist.
    Skjerming skjerming = repository.findById(id);
    return delete(skjerming);
  }

  /**
   * Delete a Skjerming
   * 
   * @param skjerming
   * @return
   */
  @Transactional
  public SkjermingJSON delete(Skjerming skjerming) {
    SkjermingJSON skjermingJSON = toJSON(skjerming);
    skjermingJSON.setDeleted(true);

    // Delete
    repository.delete(skjerming);

    return skjermingJSON;
  }


  /**
   * Delete a Skjerming if no journalposts refer to it
   * 
   * @param skjerming
   * @return
   */
  @Transactional
  public SkjermingJSON deleteIfOrphan(Skjerming skjerming) {
    int journalpostRelations = journalpostRepository.countBySkjerming(skjerming);
    if (journalpostRelations > 0) {
      SkjermingJSON skjermingJSON = toJSON(skjerming);
      skjermingJSON.setDeleted(false);
      return skjermingJSON;
    } else {
      return delete(skjerming);
    }
  }

}

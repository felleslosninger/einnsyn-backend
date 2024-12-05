package no.einnsyn.backend.entities.skjerming;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseService;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.base.models.BaseES;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import no.einnsyn.backend.entities.journalpost.JournalpostRepository;
import no.einnsyn.backend.entities.skjerming.models.Skjerming;
import no.einnsyn.backend.entities.skjerming.models.SkjermingDTO;
import no.einnsyn.backend.entities.skjerming.models.SkjermingES;
import no.einnsyn.backend.error.exceptions.EInnsynException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.util.Pair;
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
   * @param dto
   */
  @Transactional(readOnly = true)
  @Override
  public Pair<String, Skjerming> findPropertyAndObjectByDTO(BaseDTO dto) {

    // Lookup by ID first
    var skjermingById = super.findPropertyAndObjectByDTO(dto);
    if (skjermingById != null) {
      return skjermingById;
    }

    // Lookup by unique fields
    if (dto instanceof SkjermingDTO skjermingDTO) {
      var skjermingshjemmel = skjermingDTO.getSkjermingshjemmel();
      var tilgangsrestriksjon = skjermingDTO.getTilgangsrestriksjon();

      String journalenhetId = null;
      Enhet journalenhet = null;
      var journalenhetDTO = skjermingDTO.getJournalenhet();
      if (journalenhetDTO != null) {
        journalenhet = enhetService.findById(journalenhetDTO.getId());
        journalenhetId = journalenhet.getId();
        if (!enhetService.isAncestorOf(authenticationService.getJournalenhetId(), journalenhetId)) {
          return null;
        }
      }

      if (journalenhetId == null) {
        journalenhetId = authenticationService.getJournalenhetId();
        journalenhet = enhetService.findById(journalenhetId);
      }

      var skjerming =
          repository.findBySkjermingshjemmelAndTilgangsrestriksjonAndJournalenhet(
              skjermingshjemmel, tilgangsrestriksjon, journalenhet);
      if (skjerming != null) {
        return Pair.of("[skjermingshjemmel, tilgangsrestriksjon, journalenhet]", skjerming);
      }
    }

    return null;
  }

  /**
   * Override scheduleIndex to reindex the parent Skjerming.
   *
   * @param skjerming
   * @param recurseDirection -1 for parents, 1 for children, 0 for both
   */
  @Override
  public void scheduleIndex(Skjerming skjerming, int recurseDirection) {
    super.scheduleIndex(skjerming, recurseDirection);

    // Reindex parents
    if (recurseDirection <= 0) {
      var journalpostList = journalpostRepository.findBySkjerming(skjerming);
      for (var journalpost : journalpostList) {
        journalpostService.scheduleIndex(journalpost, -1);
      }
    }
  }

  /**
   * Update a Skjerming object from a JSON object
   *
   * @param dto The SkjermingDTO object to update from
   * @param skjerming The Skjerming object to update
   * @return The updated Skjerming object
   */
  @Override
  protected Skjerming fromDTO(SkjermingDTO dto, Skjerming skjerming) throws EInnsynException {
    super.fromDTO(dto, skjerming);

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
   * @param skjerming The Skjerming object to convert from
   * @param dto The SkjermingDTO object to convert to
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return The converted SkjermingDTO object
   */
  @Override
  protected SkjermingDTO toDTO(
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

  @Override
  public BaseES toLegacyES(Skjerming skjerming, BaseES es) {
    super.toLegacyES(skjerming, es);
    if (es instanceof SkjermingES skjermingES) {
      skjermingES.setTilgangsrestriksjon(skjerming.getTilgangsrestriksjon());
      skjermingES.setSkjermingshjemmel(skjerming.getSkjermingshjemmel());
    }
    return es;
  }

  /**
   * Delete a Skjerming if no journalposts refer to it
   *
   * @param skjerming The Skjerming object to delete
   * @return The deleted Skjerming object
   */
  public SkjermingDTO deleteIfOrphan(Skjerming skjerming) throws EInnsynException {
    var hasJournalpostRelations = journalpostRepository.existsBySkjerming(skjerming);
    if (hasJournalpostRelations) {
      return proxy.toDTO(skjerming);
    } else {
      return skjermingService.delete(skjerming.getId());
    }
  }
}

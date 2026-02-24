package no.einnsyn.backend.entities.votering;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseService;
import no.einnsyn.backend.entities.votering.models.Votering;
import no.einnsyn.backend.entities.votering.models.VoteringDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class VoteringService extends ArkivBaseService<Votering, VoteringDTO> {

  @Getter(onMethod_ = @Override)
  private final VoteringRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter(onMethod_ = @Override)
  @Lazy
  @Autowired
  private VoteringService proxy;

  public VoteringService(VoteringRepository repository) {
    this.repository = repository;
  }

  @Override
  public Votering newObject() {
    return new Votering();
  }

  @Override
  public VoteringDTO newDTO() {
    return new VoteringDTO();
  }

  @Override
  protected Votering fromDTO(VoteringDTO dto, Votering votering) throws EInnsynException {
    super.fromDTO(dto, votering);

    // Workaround since legacy IDs are used for relations. OneToMany relations fails if the ID is
    // not set.
    if (votering.getId() == null) {
      votering = repository.saveAndFlush(votering);
    }

    if (dto.getStemme() != null) {
      votering.setStemme(VoteringDTO.StemmeEnum.fromValue(dto.getStemme()));
    }

    // Moetedeltaker
    var moetedeltakerField = dto.getMoetedeltaker();
    if (moetedeltakerField != null) {
      var oldMoetedeltakerId = votering.getMoetedeltaker();
      votering.setMoetedeltaker(moetedeltakerService.createOrReturnExisting(moetedeltakerField));

      // Delete orphaned Moetedeltaker
      if (oldMoetedeltakerId != null) {
        moetedeltakerService.deleteIfOrphan(oldMoetedeltakerId);
      }
    }

    // Representerer
    var representererField = dto.getRepresenterer();
    if (representererField != null) {
      var oldRepresenterer = votering.getRepresenterer();
      votering.setRepresenterer(identifikatorService.createOrReturnExisting(representererField));

      // Delete orphaned Representerer
      if (oldRepresenterer != null) {
        identifikatorService.deleteIfOrphan(oldRepresenterer);
      }
    }

    return votering;
  }

  @Override
  protected VoteringDTO toDTO(
      Votering votering, VoteringDTO dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(votering, dto, expandPaths, currentPath);

    if (votering.getStemme() != null) {
      dto.setStemme(votering.getStemme().toJson());
    }

    var moetedeltaker = votering.getMoetedeltaker();
    if (moetedeltaker != null) {
      dto.setMoetedeltaker(
          moetedeltakerService.maybeExpand(
              moetedeltaker, "moetedeltaker", expandPaths, currentPath));
    }

    var representerer = votering.getRepresenterer();
    if (representerer != null) {
      dto.setRepresenterer(
          identifikatorService.maybeExpand(
              representerer, "representerer", expandPaths, currentPath));
    }

    return dto;
  }

  @Override
  protected void deleteEntity(Votering votering) throws EInnsynException {
    var moetedeltaker = votering.getMoetedeltaker();
    if (moetedeltaker != null) {
      votering.setMoetedeltaker(null);
      moetedeltakerService.deleteIfOrphan(moetedeltaker);
    }
    var representerer = votering.getRepresenterer();
    if (representerer != null) {
      votering.setRepresenterer(null);
      identifikatorService.deleteIfOrphan(representerer);
    }

    super.deleteEntity(votering);
  }
}

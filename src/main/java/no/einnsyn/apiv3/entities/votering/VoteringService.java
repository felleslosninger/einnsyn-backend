package no.einnsyn.apiv3.entities.votering;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.votering.models.Votering;
import no.einnsyn.apiv3.entities.votering.models.VoteringDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoteringService extends ArkivBaseService<Votering, VoteringDTO> {

  @Getter private final VoteringRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private VoteringService proxy;

  public VoteringService(VoteringRepository repository) {
    this.repository = repository;
  }

  public Votering newObject() {
    return new Votering();
  }

  public VoteringDTO newDTO() {
    return new VoteringDTO();
  }

  @Override
  public Votering fromDTO(VoteringDTO dto, Votering votering, Set<String> paths, String currentPath)
      throws EInnsynException {
    super.fromDTO(dto, votering, paths, currentPath);

    // Workaround since legacy IDs are used for relations. OneToMany relations fails if the ID is
    // not set.
    if (votering.getId() == null) {
      votering = repository.saveAndFlush(votering);
    }

    // Moetedeltaker
    var moetedeltakerField = dto.getMoetedeltaker();
    if (moetedeltakerField != null) {
      votering.setMoetedeltaker(
          moetedeltakerService.insertOrReturnExisting(
              moetedeltakerField, "moetedeltaker", paths, currentPath));
    }

    // Representerer
    var representererField = dto.getRepresenterer();
    if (representererField != null) {
      votering.setRepresenterer(
          identifikatorService.insertOrReturnExisting(
              representererField, "representerer", paths, currentPath));
    }

    return votering;
  }

  @Override
  public VoteringDTO toDTO(
      Votering votering, VoteringDTO dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(votering, dto, expandPaths, currentPath);

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

  @Transactional
  public VoteringDTO delete(Votering votering) {
    var dto = proxy.toDTO(votering);

    if (votering.getMoetedeltaker() != null) {
      moetedeltakerService.delete(votering.getMoetedeltaker());
    }

    if (votering.getRepresenterer() != null) {
      identifikatorService.delete(votering.getRepresenterer());
    }

    dto.setDeleted(true);
    repository.delete(votering);
    return dto;
  }
}

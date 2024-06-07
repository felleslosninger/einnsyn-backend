package no.einnsyn.apiv3.entities.moetedeltaker;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.moetedeltaker.models.Moetedeltaker;
import no.einnsyn.apiv3.entities.moetedeltaker.models.MoetedeltakerDTO;
import no.einnsyn.apiv3.entities.votering.VoteringRepository;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MoetedeltakerService extends ArkivBaseService<Moetedeltaker, MoetedeltakerDTO> {

  @Getter private final MoetedeltakerRepository repository;
  private final VoteringRepository voteringRepository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private MoetedeltakerService proxy;

  public MoetedeltakerService(
      MoetedeltakerRepository repository, VoteringRepository voteringRepository) {
    this.repository = repository;
    this.voteringRepository = voteringRepository;
  }

  public Moetedeltaker newObject() {
    return new Moetedeltaker();
  }

  public MoetedeltakerDTO newDTO() {
    return new MoetedeltakerDTO();
  }

  @Override
  protected Moetedeltaker fromDTO(MoetedeltakerDTO dto, Moetedeltaker moetedeltaker)
      throws EInnsynException {
    super.fromDTO(dto, moetedeltaker);

    if (dto.getMoetedeltakerNavn() != null) {
      moetedeltaker.setMoetedeltakerNavn(dto.getMoetedeltakerNavn());
    }

    if (dto.getMoetedeltakerFunksjon() != null) {
      moetedeltaker.setMoetedeltakerFunksjon(dto.getMoetedeltakerFunksjon());
    }

    return moetedeltaker;
  }

  @Override
  protected MoetedeltakerDTO toDTO(
      Moetedeltaker moetedeltaker,
      MoetedeltakerDTO dto,
      Set<String> expandPaths,
      String currentPath) {
    super.toDTO(moetedeltaker, dto, expandPaths, currentPath);
    dto.setMoetedeltakerNavn(moetedeltaker.getMoetedeltakerNavn());
    dto.setMoetedeltakerFunksjon(moetedeltaker.getMoetedeltakerFunksjon());
    return dto;
  }

  @Transactional(rollbackFor = EInnsynException.class)
  public MoetedeltakerDTO deleteIfOrphan(Moetedeltaker moetedeltaker) throws EInnsynException {
    var hasVoteringRelations = voteringRepository.existsByMoetedeltaker(moetedeltaker);
    if (hasVoteringRelations) {
      return proxy.toDTO(moetedeltaker);
    } else {
      return moetedeltakerService.delete(moetedeltaker.getId());
    }
  }
}

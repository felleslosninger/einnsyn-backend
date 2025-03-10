package no.einnsyn.backend.entities.identifikator;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseService;
import no.einnsyn.backend.entities.identifikator.models.Identifikator;
import no.einnsyn.backend.entities.identifikator.models.IdentifikatorDTO;
import no.einnsyn.backend.entities.votering.VoteringRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentifikatorService extends ArkivBaseService<Identifikator, IdentifikatorDTO> {

  @Getter private final IdentifikatorRepository repository;
  private final VoteringRepository voteringRepository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private IdentifikatorService proxy;

  public IdentifikatorService(
      IdentifikatorRepository dokumentobjektRepository, VoteringRepository voteringRepository) {
    this.repository = dokumentobjektRepository;
    this.voteringRepository = voteringRepository;
  }

  public Identifikator newObject() {
    return new Identifikator();
  }

  public IdentifikatorDTO newDTO() {
    return new IdentifikatorDTO();
  }

  @Override
  protected Identifikator fromDTO(IdentifikatorDTO dto, Identifikator identifikator)
      throws EInnsynException {
    super.fromDTO(dto, identifikator);

    if (dto.getNavn() != null) {
      identifikator.setNavn(dto.getNavn());
    }

    if (dto.getIdentifikator() != null) {
      identifikator.setIdentifikator(dto.getIdentifikator());
    }

    if (dto.getInitialer() != null) {
      identifikator.setInitialer(dto.getInitialer());
    }

    if (dto.getEpostadresse() != null) {
      identifikator.setEpostadresse(dto.getEpostadresse());
    }

    return identifikator;
  }

  @Override
  protected IdentifikatorDTO toDTO(
      Identifikator identifikator,
      IdentifikatorDTO dto,
      Set<String> expandPaths,
      String currentPath) {
    super.toDTO(identifikator, dto, expandPaths, currentPath);
    dto.setNavn(identifikator.getNavn());
    dto.setIdentifikator(identifikator.getIdentifikator());
    dto.setInitialer(identifikator.getInitialer());
    dto.setEpostadresse(identifikator.getEpostadresse());
    return dto;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public IdentifikatorDTO deleteIfOrphan(Identifikator identifikator) throws EInnsynException {
    var hasVoteringRelations = voteringRepository.existsByRepresenterer(identifikator);
    if (hasVoteringRelations) {
      return proxy.toDTO(identifikator);
    } else {
      return identifikatorService.delete(identifikator.getId());
    }
  }
}

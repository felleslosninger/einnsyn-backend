package no.einnsyn.apiv3.entities.votering;

import lombok.Getter;
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

  // TODO: Implement fromDTO, toDTO

  @Transactional
  public VoteringDTO delete(Votering object) {
    var dto = proxy.toDTO(object);
    dto.setDeleted(true);
    repository.delete(object);
    return dto;
  }
}

package no.einnsyn.apiv3.entities.moetedeltaker;

import lombok.Getter;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.moetedeltaker.models.Moetedeltaker;
import no.einnsyn.apiv3.entities.moetedeltaker.models.MoetedeltakerDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MoetedeltakerService extends ArkivBaseService<Moetedeltaker, MoetedeltakerDTO> {

  @Getter private final MoetedeltakerRepository repository;

  @Getter @Lazy @Autowired private MoetedeltakerService proxy;

  public MoetedeltakerService(MoetedeltakerRepository repository) {
    this.repository = repository;
  }

  public Moetedeltaker newObject() {
    return new Moetedeltaker();
  }

  public MoetedeltakerDTO newDTO() {
    return new MoetedeltakerDTO();
  }

  @Transactional
  public MoetedeltakerDTO delete(Moetedeltaker object) {
    var dto = proxy.toDTO(object);
    dto.setDeleted(true);
    repository.delete(object);
    return dto;
  }
}

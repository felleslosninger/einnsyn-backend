package no.einnsyn.apiv3.entities.utredning;

import jakarta.transaction.Transactional;
import lombok.Getter;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.utredning.models.Utredning;
import no.einnsyn.apiv3.entities.utredning.models.UtredningDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class UtredningService extends ArkivBaseService<Utredning, UtredningDTO> {

  @Getter private final UtredningRepository repository;

  @Getter @Lazy @Autowired private UtredningService proxy;

  public UtredningService(UtredningRepository repository) {
    this.repository = repository;
  }

  public Utredning newObject() {
    return new Utredning();
  }

  public UtredningDTO newDTO() {
    return new UtredningDTO();
  }

  @Transactional
  public UtredningDTO delete(Utredning object) {
    var dto = proxy.toDTO(object);
    dto.setDeleted(true);
    repository.delete(object);
    return dto;
  }
}

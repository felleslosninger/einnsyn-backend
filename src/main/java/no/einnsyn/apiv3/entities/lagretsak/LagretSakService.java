package no.einnsyn.apiv3.entities.lagretsak;

import jakarta.transaction.Transactional;
import lombok.Getter;
import no.einnsyn.apiv3.entities.base.BaseService;
import no.einnsyn.apiv3.entities.lagretsak.models.LagretSak;
import no.einnsyn.apiv3.entities.lagretsak.models.LagretSakDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class LagretSakService extends BaseService<LagretSak, LagretSakDTO> {

  @Getter private final LagretSakRepository repository;

  @Getter @Lazy @Autowired LagretSakService proxy;

  public LagretSakService(LagretSakRepository repository) {
    this.repository = repository;
  }

  public LagretSak newObject() {
    return new LagretSak();
  }

  public LagretSakDTO newDTO() {
    return new LagretSakDTO();
  }

  @Transactional
  public LagretSakDTO delete(LagretSak object) {
    var dto = getProxy().toDTO(object);
    dto.setDeleted(true);
    repository.delete(object);
    return dto;
  }
}

package no.einnsyn.apiv3.entities.lagretsoek;

import jakarta.transaction.Transactional;
import lombok.Getter;
import no.einnsyn.apiv3.entities.base.BaseService;
import no.einnsyn.apiv3.entities.lagretsoek.models.LagretSoek;
import no.einnsyn.apiv3.entities.lagretsoek.models.LagretSoekDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class LagretSoekService extends BaseService<LagretSoek, LagretSoekDTO> {

  @Getter private final LagretSoekRepository repository;

  @Getter @Lazy @Autowired LagretSoekService proxy;

  public LagretSoekService(LagretSoekRepository repository) {
    this.repository = repository;
  }

  public LagretSoek newObject() {
    return new LagretSoek();
  }

  public LagretSoekDTO newDTO() {
    return new LagretSoekDTO();
  }

  @Transactional
  public LagretSoekDTO delete(LagretSoek object) {
    var dto = getProxy().toDTO(object);
    dto.setDeleted(true);
    repository.delete(object);
    return dto;
  }
}

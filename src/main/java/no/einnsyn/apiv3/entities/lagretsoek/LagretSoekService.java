package no.einnsyn.apiv3.entities.lagretsoek;

import lombok.Getter;
import no.einnsyn.apiv3.entities.base.BaseService;
import no.einnsyn.apiv3.entities.lagretsoek.models.LagretSoek;
import no.einnsyn.apiv3.entities.lagretsoek.models.LagretSoekDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class LagretSoekService extends BaseService<LagretSoek, LagretSoekDTO> {

  @Getter private final LagretSoekRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  LagretSoekService proxy;

  public LagretSoekService(LagretSoekRepository repository) {
    this.repository = repository;
  }

  public LagretSoek newObject() {
    return new LagretSoek();
  }

  public LagretSoekDTO newDTO() {
    return new LagretSoekDTO();
  }

  // TODO: Implement fromDTO, toDTO

  @Override
  protected LagretSoekDTO delete(LagretSoek object) throws EInnsynException {
    // TODO: Handle subscriptions
    return super.delete(object);
  }
}

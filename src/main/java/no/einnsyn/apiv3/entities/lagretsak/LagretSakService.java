package no.einnsyn.apiv3.entities.lagretsak;

import lombok.Getter;
import no.einnsyn.apiv3.entities.base.BaseService;
import no.einnsyn.apiv3.entities.lagretsak.models.LagretSak;
import no.einnsyn.apiv3.entities.lagretsak.models.LagretSakDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class LagretSakService extends BaseService<LagretSak, LagretSakDTO> {

  @Getter private final LagretSakRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  LagretSakService proxy;

  public LagretSakService(LagretSakRepository repository) {
    this.repository = repository;
  }

  public LagretSak newObject() {
    return new LagretSak();
  }

  public LagretSakDTO newDTO() {
    return new LagretSakDTO();
  }

  // TODO: Implement fromDTO, toDTO

  @Override
  protected void deleteEntity(LagretSak object) throws EInnsynException {
    // TODO: Handle subscriptions
    super.deleteEntity(object);
  }
}

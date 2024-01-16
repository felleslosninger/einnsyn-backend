package no.einnsyn.apiv3.entities.moetesak;

import jakarta.transaction.Transactional;
import lombok.Getter;
import no.einnsyn.apiv3.entities.moetesak.models.Moetesak;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.apiv3.entities.registrering.RegistreringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class MoetesakService extends RegistreringService<Moetesak, MoetesakDTO> {

  @Getter private final MoetesakRepository repository;

  @Getter @Lazy @Autowired private MoetesakService proxy;

  public MoetesakService(MoetesakRepository repository) {
    this.repository = repository;
  }

  public Moetesak newObject() {
    return new Moetesak();
  }

  public MoetesakDTO newDTO() {
    return new MoetesakDTO();
  }

  @Transactional
  public MoetesakDTO delete(Moetesak object) {
    var dto = getProxy().toDTO(object);
    dto.setDeleted(true);
    repository.delete(object);
    return dto;
  }
}

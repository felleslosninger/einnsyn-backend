package no.einnsyn.apiv3.entities.moetesaksbeskrivelse;

import jakarta.transaction.Transactional;
import lombok.Getter;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.moetesaksbeskrivelse.models.Moetesaksbeskrivelse;
import no.einnsyn.apiv3.entities.moetesaksbeskrivelse.models.MoetesaksbeskrivelseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class MoetesaksbeskrivelseService
    extends ArkivBaseService<Moetesaksbeskrivelse, MoetesaksbeskrivelseDTO> {

  @Getter private final MoetesaksbeskrivelseRepository repository;

  @Getter @Lazy @Autowired private MoetesaksbeskrivelseService proxy;

  public MoetesaksbeskrivelseService(MoetesaksbeskrivelseRepository repository) {
    this.repository = repository;
  }

  public Moetesaksbeskrivelse newObject() {
    return new Moetesaksbeskrivelse();
  }

  public MoetesaksbeskrivelseDTO newDTO() {
    return new MoetesaksbeskrivelseDTO();
  }

  @Transactional
  public MoetesaksbeskrivelseDTO delete(Moetesaksbeskrivelse object) {
    var dto = proxy.toDTO(object);
    dto.setDeleted(true);
    repository.delete(object);
    return dto;
  }
}

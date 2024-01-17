package no.einnsyn.apiv3.entities.identifikator;

import lombok.Getter;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.identifikator.models.Identifikator;
import no.einnsyn.apiv3.entities.identifikator.models.IdentifikatorDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentifikatorService extends ArkivBaseService<Identifikator, IdentifikatorDTO> {

  @Getter private final IdentifikatorRepository repository;

  @Getter @Lazy @Autowired private IdentifikatorService proxy;

  public IdentifikatorService(IdentifikatorRepository dokumentobjektRepository) {
    this.repository = dokumentobjektRepository;
  }

  public Identifikator newObject() {
    return new Identifikator();
  }

  public IdentifikatorDTO newDTO() {
    return new IdentifikatorDTO();
  }

  @Transactional
  public IdentifikatorDTO delete(Identifikator object) {
    var dto = proxy.toDTO(object);
    dto.setDeleted(true);
    repository.delete(object);
    return dto;
  }
}

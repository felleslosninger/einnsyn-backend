package no.einnsyn.apiv3.entities.vedtak;

import jakarta.transaction.Transactional;
import lombok.Getter;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.vedtak.models.Vedtak;
import no.einnsyn.apiv3.entities.vedtak.models.VedtakDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class VedtakService extends ArkivBaseService<Vedtak, VedtakDTO> {

  @Getter private final VedtakRepository repository;

  @Getter @Lazy @Autowired private VedtakService proxy;

  public VedtakService(VedtakRepository repository) {
    this.repository = repository;
  }

  public Vedtak newObject() {
    return new Vedtak();
  }

  public VedtakDTO newDTO() {
    return new VedtakDTO();
  }

  @Transactional
  public VedtakDTO delete(Vedtak object) {
    var dto = proxy.toDTO(object);
    dto.setDeleted(true);
    repository.delete(object);
    return dto;
  }
}

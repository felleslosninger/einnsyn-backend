package no.einnsyn.apiv3.entities.moetedokument;

import jakarta.transaction.Transactional;
import lombok.Getter;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.moetedokument.models.Moetedokument;
import no.einnsyn.apiv3.entities.moetedokument.models.MoetedokumentDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class MoetedokumentService extends ArkivBaseService<Moetedokument, MoetedokumentDTO> {

  @Getter private final MoetedokumentRepository repository;

  @Getter @Lazy @Autowired private MoetedokumentService proxy;

  public MoetedokumentService(MoetedokumentRepository repository) {
    this.repository = repository;
  }

  public Moetedokument newObject() {
    return new Moetedokument();
  }

  public MoetedokumentDTO newDTO() {
    return new MoetedokumentDTO();
  }

  @Transactional
  public MoetedokumentDTO delete(Moetedokument object) {
    var dto = getProxy().toDTO(object);
    dto.setDeleted(true);
    repository.delete(object);
    return dto;
  }
}

package no.einnsyn.apiv3.entities.moetedokument;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.moetedokument.models.Moetedokument;
import no.einnsyn.apiv3.entities.moetedokument.models.MoetedokumentDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MoetedokumentService extends ArkivBaseService<Moetedokument, MoetedokumentDTO> {

  @Getter private final MoetedokumentRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private MoetedokumentService proxy;

  public MoetedokumentService(MoetedokumentRepository repository) {
    this.repository = repository;
  }

  public Moetedokument newObject() {
    return new Moetedokument();
  }

  public MoetedokumentDTO newDTO() {
    return new MoetedokumentDTO();
  }

  @Override
  public Moetedokument fromDTO(
      MoetedokumentDTO dto, Moetedokument moetedokument, Set<String> paths, String currentPath)
      throws EInnsynException {
    super.fromDTO(dto, moetedokument, paths, currentPath);

    if (dto.getMoetedokumenttype() != null) {
      moetedokument.setMoetedokumentregistreringstype(dto.getMoetedokumenttype());
    }

    return moetedokument;
  }

  @Override
  public MoetedokumentDTO toDTO(
      Moetedokument moetedokument,
      MoetedokumentDTO dto,
      Set<String> expandPaths,
      String currentPath) {
    super.toDTO(moetedokument, dto, expandPaths, currentPath);

    dto.setMoetedokumenttype(moetedokument.getMoetedokumentregistreringstype());

    return dto;
  }

  @Transactional
  public MoetedokumentDTO delete(Moetedokument object) {
    var dto = proxy.toDTO(object);
    dto.setDeleted(true);
    repository.delete(object);
    return dto;
  }
}

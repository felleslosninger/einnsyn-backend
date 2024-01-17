package no.einnsyn.apiv3.entities.behandlingsprotokoll;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.behandlingsprotokoll.models.Behandlingsprotokoll;
import no.einnsyn.apiv3.entities.behandlingsprotokoll.models.BehandlingsprotokollDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BehandlingsprotokollService
    extends ArkivBaseService<Behandlingsprotokoll, BehandlingsprotokollDTO> {

  @Getter private final BehandlingsprotokollRepository repository;

  @Getter @Lazy @Autowired private BehandlingsprotokollService proxy;

  public BehandlingsprotokollService(BehandlingsprotokollRepository repository) {
    this.repository = repository;
  }

  public Behandlingsprotokoll newObject() {
    return new Behandlingsprotokoll();
  }

  public BehandlingsprotokollDTO newDTO() {
    return new BehandlingsprotokollDTO();
  }

  @Override
  public Behandlingsprotokoll fromDTO(
      BehandlingsprotokollDTO dto,
      Behandlingsprotokoll object,
      Set<String> paths,
      String currentPath) {
    super.fromDTO(dto, object, paths, currentPath);

    if (dto.getTekstInnhold() != null) {
      object.setTekstInnhold(dto.getTekstInnhold());
    }

    if (dto.getTekstFormat() != null) {
      object.setTekstFormat(dto.getTekstFormat());
    }

    return object;
  }

  @Override
  public BehandlingsprotokollDTO toDTO(
      Behandlingsprotokoll object,
      BehandlingsprotokollDTO dto,
      Set<String> expandPaths,
      String currentPath) {
    super.toDTO(object, dto, expandPaths, currentPath);

    dto.setTekstInnhold(object.getTekstInnhold());
    dto.setTekstFormat(object.getTekstFormat());

    return dto;
  }

  @Transactional
  public BehandlingsprotokollDTO delete(Behandlingsprotokoll object) {
    var dto = proxy.toDTO(object);
    dto.setDeleted(true);
    repository.delete(object);
    return dto;
  }
}

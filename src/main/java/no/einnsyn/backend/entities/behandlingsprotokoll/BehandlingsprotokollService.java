package no.einnsyn.backend.entities.behandlingsprotokoll;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseService;
import no.einnsyn.backend.entities.behandlingsprotokoll.models.Behandlingsprotokoll;
import no.einnsyn.backend.entities.behandlingsprotokoll.models.BehandlingsprotokollDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class BehandlingsprotokollService
    extends ArkivBaseService<Behandlingsprotokoll, BehandlingsprotokollDTO> {

  @Getter private final BehandlingsprotokollRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private BehandlingsprotokollService proxy;

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
  protected Behandlingsprotokoll fromDTO(
      BehandlingsprotokollDTO dto, Behandlingsprotokoll behandlingsprotokoll)
      throws EInnsynException {
    super.fromDTO(dto, behandlingsprotokoll);

    if (dto.getTekstInnhold() != null) {
      behandlingsprotokoll.setTekstInnhold(dto.getTekstInnhold());
    }

    if (dto.getTekstFormat() != null) {
      behandlingsprotokoll.setTekstFormat(dto.getTekstFormat());
    }

    if (behandlingsprotokoll.getId() == null) {
      repository.saveAndFlush(behandlingsprotokoll);
    }

    return behandlingsprotokoll;
  }

  @Override
  protected BehandlingsprotokollDTO toDTO(
      Behandlingsprotokoll object,
      BehandlingsprotokollDTO dto,
      Set<String> expandPaths,
      String currentPath) {
    super.toDTO(object, dto, expandPaths, currentPath);

    dto.setTekstInnhold(object.getTekstInnhold());
    dto.setTekstFormat(object.getTekstFormat());

    return dto;
  }
}

package no.einnsyn.backend.entities.moetesaksbeskrivelse;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseService;
import no.einnsyn.backend.entities.moetesaksbeskrivelse.models.Moetesaksbeskrivelse;
import no.einnsyn.backend.entities.moetesaksbeskrivelse.models.MoetesaksbeskrivelseDTO;
import no.einnsyn.backend.error.exceptions.EInnsynException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class MoetesaksbeskrivelseService
    extends ArkivBaseService<Moetesaksbeskrivelse, MoetesaksbeskrivelseDTO> {

  @Getter private final MoetesaksbeskrivelseRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private MoetesaksbeskrivelseService proxy;

  public MoetesaksbeskrivelseService(MoetesaksbeskrivelseRepository repository) {
    this.repository = repository;
  }

  public Moetesaksbeskrivelse newObject() {
    return new Moetesaksbeskrivelse();
  }

  public MoetesaksbeskrivelseDTO newDTO() {
    return new MoetesaksbeskrivelseDTO();
  }

  @Override
  protected Moetesaksbeskrivelse fromDTO(MoetesaksbeskrivelseDTO dto, Moetesaksbeskrivelse object)
      throws EInnsynException {
    super.fromDTO(dto, object);

    if (dto.getTekstInnhold() != null) {
      object.setTekstInnhold(dto.getTekstInnhold());
    }

    if (dto.getTekstFormat() != null) {
      object.setTekstFormat(dto.getTekstFormat());
    }

    return object;
  }

  @Override
  protected MoetesaksbeskrivelseDTO toDTO(
      Moetesaksbeskrivelse object,
      MoetesaksbeskrivelseDTO dto,
      Set<String> expandPaths,
      String currentPath) {
    super.toDTO(object, dto, expandPaths, currentPath);

    dto.setTekstInnhold(object.getTekstInnhold());
    dto.setTekstFormat(object.getTekstFormat());

    return dto;
  }
}

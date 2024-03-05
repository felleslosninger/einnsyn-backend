package no.einnsyn.apiv3.entities.moetesaksbeskrivelse;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
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
  public Moetesaksbeskrivelse fromDTO(
      MoetesaksbeskrivelseDTO dto,
      Moetesaksbeskrivelse object,
      Set<String> paths,
      String currentPath)
      throws EInnsynException {
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
  public MoetesaksbeskrivelseDTO toDTO(
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

package no.einnsyn.apiv3.entities.lagretsak;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.entities.base.BaseService;
import no.einnsyn.apiv3.entities.lagretsak.models.LagretSak;
import no.einnsyn.apiv3.entities.lagretsak.models.LagretSakDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class LagretSakService extends BaseService<LagretSak, LagretSakDTO> {

  @Getter private final LagretSakRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  LagretSakService proxy;

  public LagretSakService(LagretSakRepository repository) {
    this.repository = repository;
  }

  public LagretSak newObject() {
    return new LagretSak();
  }

  public LagretSakDTO newDTO() {
    return new LagretSakDTO();
  }

  @Override
  protected LagretSak fromDTO(LagretSakDTO dto, LagretSak lagretSak) throws EInnsynException {
    super.fromDTO(dto, lagretSak);

    if (dto.getBruker() != null) {
      var bruker = brukerService.returnExistingOrThrow(dto.getBruker());
      lagretSak.setBruker(bruker);
    }

    if (dto.getSaksmappe() != null) {
      var saksmappe = saksmappeService.returnExistingOrThrow(dto.getSaksmappe());
      lagretSak.setSaksmappe(saksmappe);
    }

    if (dto.getMoetemappe() != null) {
      var moetemappe = moetemappeService.returnExistingOrThrow(dto.getMoetemappe());
      lagretSak.setMoetemappe(moetemappe);
    }

    if (dto.getAbonnere() != null) {
      lagretSak.setAbonnere(dto.getAbonnere());
    }

    return lagretSak;
  }

  @Override
  protected LagretSakDTO toDTO(
      LagretSak lagretSak, LagretSakDTO dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(lagretSak, dto, expandPaths, currentPath);

    dto.setBruker(
        brukerService.maybeExpand(lagretSak.getBruker(), "bruker", expandPaths, currentPath));
    dto.setSaksmappe(
        saksmappeService.maybeExpand(
            lagretSak.getSaksmappe(), "saksmappe", expandPaths, currentPath));
    dto.setMoetemappe(
        moetemappeService.maybeExpand(
            lagretSak.getMoetemappe(), "moetemappe", expandPaths, currentPath));
    dto.setAbonnere(lagretSak.isAbonnere());

    return dto;
  }

  @Override
  protected void deleteEntity(LagretSak object) throws EInnsynException {
    super.deleteEntity(object);

    // Delete lagret sak treff
  }
}

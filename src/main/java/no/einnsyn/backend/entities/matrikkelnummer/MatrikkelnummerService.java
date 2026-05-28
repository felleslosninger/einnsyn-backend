package no.einnsyn.backend.entities.matrikkelnummer;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.backend.common.exceptions.models.AuthorizationException;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.entities.base.BaseService;
import no.einnsyn.backend.entities.matrikkelnummer.models.Matrikkelnummer;
import no.einnsyn.backend.entities.matrikkelnummer.models.MatrikkelnummerDTO;
import no.einnsyn.backend.utils.id.IdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class MatrikkelnummerService extends BaseService<Matrikkelnummer, MatrikkelnummerDTO> {

  @Getter(onMethod_ = @Override)
  private final MatrikkelnummerRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter(onMethod_ = @Override)
  @Lazy
  @Autowired
  private MatrikkelnummerService proxy;

  public MatrikkelnummerService(MatrikkelnummerRepository repository) {
    this.repository = repository;
  }

  @Override
  public Matrikkelnummer newObject() {
    return new Matrikkelnummer();
  }

  @Override
  public MatrikkelnummerDTO newDTO() {
    return new MatrikkelnummerDTO();
  }

  @Override
  protected void authorizeDelete(String id) throws EInnsynException {
    var matrikkelnummer = proxy.findOrThrow(id);

    var mappeId = matrikkelnummer.getMappeId();
    if (mappeId != null) {
      var mappeEntity = IdUtils.resolveEntity(mappeId);
      if ("Saksmappe".equals(mappeEntity)) {
        saksmappeService.authorizeDelete(mappeId);
        return;
      }
      if ("Moetemappe".equals(mappeEntity)) {
        moetemappeService.authorizeDelete(mappeId);
        return;
      }
      throw new AuthorizationException("Not authorized to delete " + id);
    }

    var registreringId = matrikkelnummer.getRegistreringId();
    if (registreringId != null) {
      var registreringEntity = IdUtils.resolveEntity(registreringId);
      if ("Journalpost".equals(registreringEntity)) {
        journalpostService.authorizeDelete(registreringId);
        return;
      }
      if ("Moetesak".equals(registreringEntity)) {
        moetesakService.authorizeDelete(registreringId);
        return;
      }
      if ("Moetedokument".equals(registreringEntity)) {
        moetedokumentService.authorizeDelete(registreringId);
        return;
      }
      throw new AuthorizationException("Not authorized to delete " + id);
    }

    throw new AuthorizationException("Not authorized to delete " + id);
  }

  @Override
  protected Matrikkelnummer fromDTO(MatrikkelnummerDTO dto, Matrikkelnummer matrikkelnummer)
      throws EInnsynException {
    super.fromDTO(dto, matrikkelnummer);

    if (dto.getKommunenummer() != null) {
      matrikkelnummer.setKommunenummer(dto.getKommunenummer());
    }

    if (dto.getGaardsnummer() != null) {
      matrikkelnummer.setGaardsnummer(dto.getGaardsnummer());
    }

    if (dto.getBruksnummer() != null) {
      matrikkelnummer.setBruksnummer(dto.getBruksnummer());
    }

    if (dto.getFestenummer() != null) {
      matrikkelnummer.setFestenummer(dto.getFestenummer());
    }

    if (dto.getSeksjonsnummer() != null) {
      matrikkelnummer.setSeksjonsnummer(dto.getSeksjonsnummer());
    }

    return matrikkelnummer;
  }

  @Override
  protected MatrikkelnummerDTO toDTO(
      Matrikkelnummer matrikkelnummer,
      MatrikkelnummerDTO dto,
      Set<String> expandPaths,
      String currentPath) {
    super.toDTO(matrikkelnummer, dto, expandPaths, currentPath);
    dto.setKommunenummer(matrikkelnummer.getKommunenummer());
    dto.setGaardsnummer(matrikkelnummer.getGaardsnummer());
    dto.setBruksnummer(matrikkelnummer.getBruksnummer());
    dto.setFestenummer(matrikkelnummer.getFestenummer());
    dto.setSeksjonsnummer(matrikkelnummer.getSeksjonsnummer());
    return dto;
  }
}

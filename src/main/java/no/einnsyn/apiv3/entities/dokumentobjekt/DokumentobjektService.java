package no.einnsyn.apiv3.entities.dokumentobjekt;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.dokumentobjekt.models.Dokumentobjekt;
import no.einnsyn.apiv3.entities.dokumentobjekt.models.DokumentobjektDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class DokumentobjektService extends ArkivBaseService<Dokumentobjekt, DokumentobjektDTO> {

  @Getter private final DokumentobjektRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private DokumentobjektService proxy;

  public DokumentobjektService(DokumentobjektRepository dokumentobjektRepository) {
    this.repository = dokumentobjektRepository;
  }

  public Dokumentobjekt newObject() {
    return new Dokumentobjekt();
  }

  public DokumentobjektDTO newDTO() {
    return new DokumentobjektDTO();
  }

  /**
   * Convert a DTO object to a Dokumentobjekt
   *
   * @param dto
   * @param dokumentobjekt
   * @param paths A list of paths containing new objects that will be created from this update
   * @param currentPath The current path in the object tree
   * @return
   */
  @Override
  public Dokumentobjekt fromDTO(
      DokumentobjektDTO dto, Dokumentobjekt dokumentobjekt, Set<String> paths, String currentPath)
      throws EInnsynException {
    super.fromDTO(dto, dokumentobjekt, paths, currentPath);

    if (dto.getSystemId() != null) {
      dokumentobjekt.setSystemId(dto.getSystemId());
    }

    if (dto.getReferanseDokumentfil() != null) {
      dokumentobjekt.setReferanseDokumentfil(dto.getReferanseDokumentfil());
    }

    if (dto.getFormat() != null) {
      dokumentobjekt.setDokumentFormat(dto.getFormat());
    }

    if (dto.getSjekksum() != null) {
      dokumentobjekt.setSjekksum(dto.getSjekksum());
    }

    if (dto.getSjekksumAlgoritme() != null) {
      dokumentobjekt.setSjekksumalgoritme(dto.getSjekksumAlgoritme());
    }

    return dokumentobjekt;
  }

  /**
   * Convert a Dokumentobjekt to a DTO object
   *
   * @param dokumentobjekt
   * @param dto
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return
   */
  @Override
  public DokumentobjektDTO toDTO(
      Dokumentobjekt dokumentobjekt,
      DokumentobjektDTO dto,
      Set<String> expandPaths,
      String currentPath) {
    super.toDTO(dokumentobjekt, dto, expandPaths, currentPath);

    dto.setSystemId(dokumentobjekt.getSystemId());
    dto.setReferanseDokumentfil(dokumentobjekt.getReferanseDokumentfil());
    dto.setFormat(dokumentobjekt.getDokumentFormat());
    dto.setSjekksum(dokumentobjekt.getSjekksum());
    dto.setSjekksumAlgoritme(dokumentobjekt.getSjekksumalgoritme());

    return dto;
  }
}

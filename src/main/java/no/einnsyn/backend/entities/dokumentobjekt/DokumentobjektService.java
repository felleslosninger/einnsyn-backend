package no.einnsyn.backend.entities.dokumentobjekt;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseService;
import no.einnsyn.backend.entities.base.models.BaseES;
import no.einnsyn.backend.entities.dokumentobjekt.models.Dokumentobjekt;
import no.einnsyn.backend.entities.dokumentobjekt.models.DokumentobjektDTO;
import no.einnsyn.backend.entities.dokumentobjekt.models.DokumentobjektES;
import no.einnsyn.backend.error.exceptions.EInnsynException;
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
   * Override the scheduleIndex method to reindex the parent Dokumentbeskrivelse.
   *
   * @param dokumentobjekt
   * @param recurseDirection -1 for parents, 1 for children, 0 for both
   */
  @Override
  public void scheduleIndex(Dokumentobjekt dokumentobjekt, int recurseDirection) {
    super.scheduleIndex(dokumentobjekt, recurseDirection);

    // Reindex parents
    if (recurseDirection <= 0 && dokumentobjekt.getDokumentbeskrivelse() != null) {
      dokumentbeskrivelseService.scheduleIndex(dokumentobjekt.getDokumentbeskrivelse(), -1);
    }
  }

  /**
   * Convert a DTO object to a Dokumentobjekt
   *
   * @param dto The DTO object
   * @param dokumentobjekt The entity object
   * @return The entity object
   */
  @Override
  protected Dokumentobjekt fromDTO(DokumentobjektDTO dto, Dokumentobjekt dokumentobjekt)
      throws EInnsynException {
    super.fromDTO(dto, dokumentobjekt);

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

    if (dto.getDokumentbeskrivelse() != null) {
      var dokumentbeskrivelse =
          dokumentbeskrivelseService.findById(dto.getDokumentbeskrivelse().getId());
      dokumentbeskrivelse.addDokumentobjekt(dokumentobjekt);
    }

    return dokumentobjekt;
  }

  /**
   * Convert a Dokumentobjekt to a DTO object
   *
   * @param dokumentobjekt The entity object
   * @param dto The DTO object
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return The DTO object
   */
  @Override
  protected DokumentobjektDTO toDTO(
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

    var dokumentbeskrivelse = dokumentobjekt.getDokumentbeskrivelse();
    if (dokumentbeskrivelse != null) {
      dto.setDokumentbeskrivelse(
          dokumentbeskrivelseService.maybeExpand(
              dokumentbeskrivelse, "dokumentbeskrivelse", expandPaths, currentPath));
    }

    return dto;
  }

  @Override
  public BaseES toLegacyES(Dokumentobjekt dokumentobjekt, BaseES es) {
    super.toLegacyES(dokumentobjekt, es);
    if (es instanceof DokumentobjektES dokumentobjektES) {
      dokumentobjektES.setFormat(dokumentobjekt.getDokumentFormat());
      dokumentobjektES.setReferanseDokumentfil(dokumentobjekt.getReferanseDokumentfil());
    }
    return es;
  }

  @Override
  protected void deleteEntity(Dokumentobjekt dokobj) throws EInnsynException {
    if (dokobj.getDokumentbeskrivelse() != null) {
      dokobj.getDokumentbeskrivelse().removeDokumentobjekt(dokobj);
    }
    super.deleteEntity(dokobj);
  }
}

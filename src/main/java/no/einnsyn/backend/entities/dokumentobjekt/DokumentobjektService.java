package no.einnsyn.backend.entities.dokumentobjekt;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseService;
import no.einnsyn.backend.entities.base.models.BaseES;
import no.einnsyn.backend.entities.dokumentbeskrivelse.DokumentbeskrivelseRepository;
import no.einnsyn.backend.entities.dokumentobjekt.models.Dokumentobjekt;
import no.einnsyn.backend.entities.dokumentobjekt.models.DokumentobjektDTO;
import no.einnsyn.backend.entities.dokumentobjekt.models.DokumentobjektES;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class DokumentobjektService extends ArkivBaseService<Dokumentobjekt, DokumentobjektDTO> {

  @Getter private final DokumentobjektRepository repository;

  private final DokumentbeskrivelseRepository dokumentbeskrivelseRepository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private DokumentobjektService proxy;

  public DokumentobjektService(
      DokumentobjektRepository dokumentobjektRepository,
      DokumentbeskrivelseRepository dokumentbeskrivelseRepository) {
    this.repository = dokumentobjektRepository;
    this.dokumentbeskrivelseRepository = dokumentbeskrivelseRepository;
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
  public boolean scheduleIndex(String dokumentobjektId, int recurseDirection) {
    var isScheduled = super.scheduleIndex(dokumentobjektId, recurseDirection);

    // Reindex parents
    if (recurseDirection <= 0 && !isScheduled) {
      var dokumentbeskrivelseId =
          dokumentbeskrivelseRepository.findIdByDokumentobjektId(dokumentobjektId);
      if (dokumentbeskrivelseId != null) {
        dokumentbeskrivelseService.scheduleIndex(dokumentbeskrivelseId, -1);
      }
    }

    return true;
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
    dto.setFormat(dokumentobjekt.getDokumentFormat());
    dto.setSjekksum(dokumentobjekt.getSjekksum());
    dto.setSjekksumAlgoritme(dokumentobjekt.getSjekksumalgoritme());

    // Don't expose source URLs
    if (getProxy().isOwnerOf(korrespondansepart)) {
      dto.setReferanseDokumentfil(dokumentobjekt.getReferanseDokumentfil());
    }

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

package no.einnsyn.apiv3.entities.dokumentbeskrivelse;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.apiv3.entities.dokumentobjekt.models.Dokumentobjekt;
import no.einnsyn.apiv3.entities.dokumentobjekt.models.DokumentobjektDTO;
import no.einnsyn.apiv3.entities.journalpost.JournalpostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class DokumentbeskrivelseService
    extends ArkivBaseService<Dokumentbeskrivelse, DokumentbeskrivelseDTO> {

  private final JournalpostRepository journalpostRepository;

  @Getter private final DokumentbeskrivelseRepository repository;

  @Getter @Lazy @Autowired private DokumentbeskrivelseService proxy;

  public DokumentbeskrivelseService(
      DokumentbeskrivelseRepository dokumentbeskrivelseRepository,
      JournalpostRepository journalpostRepository) {
    this.repository = dokumentbeskrivelseRepository;
    this.journalpostRepository = journalpostRepository;
  }

  public Dokumentbeskrivelse newObject() {
    return new Dokumentbeskrivelse();
  }

  public DokumentbeskrivelseDTO newDTO() {
    return new DokumentbeskrivelseDTO();
  }

  /**
   * Convert a DTO object to a Dokumentbeskrivelse
   *
   * @param dto
   * @param dokbesk
   * @param paths A list of paths containing new objects that will be created from this update
   * @param currentPath The current path in the object tree
   * @return
   */
  @Override
  public Dokumentbeskrivelse fromDTO(
      DokumentbeskrivelseDTO dto,
      Dokumentbeskrivelse dokbesk,
      Set<String> paths,
      String currentPath) {
    super.fromDTO(dto, dokbesk, paths, currentPath);

    if (dto.getSystemId() != null) {
      dokbesk.setSystemId(dto.getSystemId());
    }

    if (dto.getDokumentnummer() != null) {
      dokbesk.setDokumentnummer(dto.getDokumentnummer());
    }

    if (dto.getTilknyttetRegistreringSom() != null) {
      dokbesk.setTilknyttetRegistreringSom(dto.getTilknyttetRegistreringSom());
    }

    if (dto.getDokumenttype() != null) {
      dokbesk.setDokumenttype(dto.getDokumenttype());
    }

    if (dto.getTittel() != null) {
      dokbesk.setTittel(dto.getTittel());
    }

    if (dto.getTittelSensitiv() != null) {
      dokbesk.setTittel_SENSITIV(dto.getTittelSensitiv());
    }

    // Dokumentobjekt
    List<ExpandableField<DokumentobjektDTO>> dokobjFieldList = dto.getDokumentobjekt();
    if (dokobjFieldList != null) {
      dokobjFieldList.forEach(
          dokobjField -> {
            Dokumentobjekt dokobj = null;
            if (dokobjField.getId() != null) {
              dokobj = dokumentobjektService.findById(dokobjField.getId());
            } else {
              String dokobjPath =
                  currentPath.isEmpty() ? "dokumentobjekt" : currentPath + ".dokumentobjekt";
              paths.add(dokobjPath);
              dokobj =
                  dokumentobjektService.fromDTO(dokobjField.getExpandedObject(), paths, dokobjPath);
            }
            dokbesk.addDokumentobjekt(dokobj);
          });
    }

    return dokbesk;
  }

  /**
   * Convert a Dokumentbeskrivelse to a DTO object
   *
   * @param dokbesk
   * @param dto
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return
   */
  @Override
  public DokumentbeskrivelseDTO toDTO(
      Dokumentbeskrivelse dokbesk,
      DokumentbeskrivelseDTO dto,
      Set<String> expandPaths,
      String currentPath) {
    super.toDTO(dokbesk, dto, expandPaths, currentPath);

    dto.setSystemId(dokbesk.getSystemId());
    dto.setDokumentnummer(dokbesk.getDokumentnummer());
    dto.setTilknyttetRegistreringSom(dokbesk.getTilknyttetRegistreringSom());
    dto.setDokumenttype(dokbesk.getDokumenttype());
    dto.setTittel(dokbesk.getTittel());
    dto.setTittelSensitiv(dokbesk.getTittel_SENSITIV());

    // Dokumentobjekt
    List<Dokumentobjekt> dokobjList = dokbesk.getDokumentobjekt();
    List<ExpandableField<DokumentobjektDTO>> dokobjJSONList = dto.getDokumentobjekt();
    for (Dokumentobjekt dokobj : dokobjList) {
      dokobjJSONList.add(
          dokumentobjektService.maybeExpand(dokobj, "dokumentobjekt", expandPaths, currentPath));
    }

    return dto;
  }

  /**
   * Delete a Dokumentbeskrivelse
   *
   * @param dokbesk
   * @return
   */
  @Transactional
  public DokumentbeskrivelseDTO delete(Dokumentbeskrivelse dokbesk) {
    var dto = getProxy().toDTO(dokbesk);
    dto.setDeleted(true);

    // Delete all dokumentobjekts
    var dokobjList = dokbesk.getDokumentobjekt();
    if (dokobjList != null) {
      dokobjList.forEach(dokumentobjektService::delete);
    }

    // Delete
    repository.delete(dokbesk);

    return dto;
  }

  @Transactional
  public DokumentbeskrivelseDTO deleteIfOrphan(Dokumentbeskrivelse dokbesk) {
    int journalpostRelations = journalpostRepository.countByDokumentbeskrivelse(dokbesk);
    if (journalpostRelations > 0) {
      var dto = getProxy().toDTO(dokbesk);
      dto.setDeleted(false);
      return dto;
    } else {
      return getProxy().delete(dokbesk);
    }
  }

  // TODO: Download dokumentbeskrivelse
  public byte[] downloadDokumentbeskrivelse(
      String dokumentbeskrivelseId, String dokumentobjektId, String extension) {
    // var dokumentbeskrivelse = repository.findById(dokumentbeskrivelseId).orElse(null);
    return new byte[0];
  }
}

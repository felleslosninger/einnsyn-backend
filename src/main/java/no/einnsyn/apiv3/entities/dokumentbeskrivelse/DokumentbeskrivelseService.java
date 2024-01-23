package no.einnsyn.apiv3.entities.dokumentbeskrivelse;

import java.util.ArrayList;
import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseListQueryDTO;
import no.einnsyn.apiv3.entities.dokumentobjekt.models.Dokumentobjekt;
import no.einnsyn.apiv3.entities.journalpost.JournalpostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DokumentbeskrivelseService
    extends ArkivBaseService<Dokumentbeskrivelse, DokumentbeskrivelseDTO> {

  private final JournalpostRepository journalpostRepository;

  @Getter private final DokumentbeskrivelseRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private DokumentbeskrivelseService proxy;

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
      String currentPath)
      throws EInnsynException {
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

    if (dto.getTittel() != null) {
      dokbesk.setTittel(dto.getTittel());
    }

    if (dto.getTittelSensitiv() != null) {
      dokbesk.setTittel_SENSITIV(dto.getTittelSensitiv());
    }

    // Persist before adding relations
    if (dokbesk.getId() == null) {
      dokbesk = repository.saveAndFlush(dokbesk);
    }

    // Dokumentobjekt
    var dokobjFieldList = dto.getDokumentobjekt();
    if (dokobjFieldList != null) {
      for (var dokobjField : dokobjFieldList) {
        Dokumentobjekt dokobj = null;
        if (dokobjField.getId() != null) {
          dokobj = dokumentobjektService.findById(dokobjField.getId());
          if (dokobj == null) {
            throw new EInnsynException(
                "Dokumentobjekt with id " + dokobjField.getId() + " not found");
          }
        } else {
          var dokobjPath =
              currentPath.isEmpty() ? "dokumentobjekt" : currentPath + ".dokumentobjekt";
          paths.add(dokobjPath);
          var dokobjDto = dokobjField.getExpandedObject();
          dokobj = dokumentobjektService.fromDTO(dokobjDto, paths, dokobjPath);
          if (dokobj == null) {
            throw new EInnsynException("Could not create Dokumentobjekt from DTO");
          }
        }
        dokbesk.addDokumentobjekt(dokobj);
      }
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
    dto.setTittel(dokbesk.getTittel());
    dto.setTittelSensitiv(dokbesk.getTittel_SENSITIV());

    // Dokumentobjekt
    var dokobjListDTO = dto.getDokumentobjekt();
    if (dokobjListDTO == null) {
      dokobjListDTO = new ArrayList<>();
      dto.setDokumentobjekt(dokobjListDTO);
    }
    var dokobjList = dokbesk.getDokumentobjekt();
    if (dokobjList != null) {
      for (var dokobj : dokobjList) {
        dokobjListDTO.add(
            dokumentobjektService.maybeExpand(dokobj, "dokumentobjekt", expandPaths, currentPath));
      }
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
    var dto = proxy.toDTO(dokbesk);
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
      var dto = proxy.toDTO(dokbesk);
      dto.setDeleted(false);
      return dto;
    } else {
      return proxy.delete(dokbesk);
    }
  }

  // TODO: Download dokumentbeskrivelse
  public byte[] downloadDokumentbeskrivelse(
      String dokumentbeskrivelseId, String dokumentobjektId, String extension) {
    // var dokumentbeskrivelse = repository.findById(dokumentbeskrivelseId).orElse(null);
    return new byte[0];
  }

  @Override
  public Page<Dokumentbeskrivelse> getPage(BaseListQueryDTO params, PageRequest pageRequest) {
    var journalpostId =
        (params instanceof DokumentbeskrivelseListQueryDTO p) ? p.getJournalpost() : null;
    if (journalpostId == null) {
      return super.getPage(params, pageRequest);
    }

    var journalpost = journalpostService.findById(journalpostId);
    var startingAfter = params.getStartingAfter();
    var endingBefore = params.getEndingBefore();
    var hasStartingAfter = startingAfter != null;
    var hasEndingBefore = endingBefore != null;
    var ascending = "asc".equals(params.getSortOrder());
    var descending = !ascending;
    var pivot = hasStartingAfter ? startingAfter : endingBefore;

    if ((ascending && hasStartingAfter) || (descending && hasEndingBefore)) {
      return repository.findByJournalpostAndIdGreaterThanEqualOrderByIdAsc(
          journalpost, pivot, pageRequest);
    }
    if ((descending && hasStartingAfter) || (ascending && hasEndingBefore)) {
      return repository.findByJournalpostAndIdLessThanEqualOrderByIdDesc(
          journalpost, pivot, pageRequest);
    }

    if (ascending) {
      return repository.findByJournalpostOrderByIdAsc(journalpost, pageRequest);
    } else {
      return repository.findByJournalpostOrderByIdDesc(journalpost, pageRequest);
    }
  }
}

package no.einnsyn.apiv3.entities.korrespondansepart;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.common.paginators.Paginators;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.models.Korrespondansepart;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartListQueryDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartParentDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KorrespondansepartService
    extends ArkivBaseService<Korrespondansepart, KorrespondansepartDTO> {

  @Getter private final KorrespondansepartRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private KorrespondansepartService proxy;

  public KorrespondansepartService(KorrespondansepartRepository repository) {
    this.repository = repository;
  }

  public Korrespondansepart newObject() {
    return new Korrespondansepart();
  }

  public KorrespondansepartDTO newDTO() {
    return new KorrespondansepartDTO();
  }

  /**
   * Convert a JSON object to a Korrespondansepart
   *
   * @param dto
   * @param korrespondansepart
   * @param paths A list of paths containing new objects that will be created from this update
   * @param currentPath The current path in the object tree
   * @return
   */
  @Override
  public Korrespondansepart fromDTO(
      KorrespondansepartDTO dto,
      Korrespondansepart korrespondansepart,
      Set<String> paths,
      String currentPath)
      throws EInnsynException {
    super.fromDTO(dto, korrespondansepart, paths, currentPath);

    if (dto.getKorrespondanseparttype() != null) {
      korrespondansepart.setKorrespondanseparttype(dto.getKorrespondanseparttype());
    }

    if (dto.getKorrespondansepartNavn() != null) {
      korrespondansepart.setKorrespondansepartNavn(dto.getKorrespondansepartNavn());
    }

    if (dto.getKorrespondansepartNavnSensitiv() != null) {
      korrespondansepart.setKorrespondansepartNavnSensitiv(dto.getKorrespondansepartNavnSensitiv());
    }

    if (dto.getAdministrativEnhet() != null) {
      korrespondansepart.setAdministrativEnhet(dto.getAdministrativEnhet());
    }

    if (dto.getSaksbehandler() != null) {
      korrespondansepart.setSaksbehandler(dto.getSaksbehandler());
    }

    if (dto.getEpostadresse() != null) {
      korrespondansepart.setEpostadresse(dto.getEpostadresse());
    }

    if (dto.getPostnummer() != null) {
      korrespondansepart.setPostnummer(dto.getPostnummer());
    }

    if (dto.getErBehandlingsansvarlig() != null) {
      korrespondansepart.setErBehandlingsansvarlig(dto.getErBehandlingsansvarlig());
    }

    var parentField = dto.getParent();
    if (parentField != null) {
      var parentId = parentField.getId();
      if (parentId == null) {
        throw new EInnsynException("Parent id is required");
      }
      if (parentField.isJournalpost()) {
        korrespondansepart.setParentJournalpost(journalpostService.findById(parentId));
      } else if (parentField.isMoetedokument()) {
        korrespondansepart.setParentMoetedokument(moetedokumentService.findById(parentId));
      } else if (parentField.isMoetesak()) {
        korrespondansepart.setParentMoetesak(moetesakService.findById(parentId));
      } else {
        throw new EInnsynException("Invalid parent type: " + parentField.getClass().getName());
      }
    }

    return korrespondansepart;
  }

  /**
   * Convert a Korrespondansepart to a JSON object
   *
   * @param korrespondansepart
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return
   */
  @Override
  public KorrespondansepartDTO toDTO(
      Korrespondansepart korrespondansepart,
      KorrespondansepartDTO dto,
      Set<String> expandPaths,
      String currentPath) {
    super.toDTO(korrespondansepart, dto, expandPaths, currentPath);

    dto.setKorrespondanseparttype(korrespondansepart.getKorrespondanseparttype());
    dto.setKorrespondansepartNavn(korrespondansepart.getKorrespondansepartNavn());
    dto.setKorrespondansepartNavnSensitiv(korrespondansepart.getKorrespondansepartNavnSensitiv());
    dto.setAdministrativEnhet(korrespondansepart.getAdministrativEnhet());
    dto.setSaksbehandler(korrespondansepart.getSaksbehandler());
    dto.setEpostadresse(korrespondansepart.getEpostadresse());
    dto.setPostnummer(korrespondansepart.getPostnummer());
    dto.setErBehandlingsansvarlig(korrespondansepart.isErBehandlingsansvarlig());

    // Parent is journalpost
    if (korrespondansepart.getParentJournalpost() != null) {
      dto.setParent(
          new KorrespondansepartParentDTO(
              journalpostService.maybeExpand(
                  korrespondansepart.getParentJournalpost(),
                  "journalpost",
                  expandPaths,
                  currentPath)));
    }
    // Parent is Moetedokument
    else if (korrespondansepart.getParentMoetedokument() != null) {
      dto.setParent(
          new KorrespondansepartParentDTO(
              moetedokumentService.maybeExpand(
                  korrespondansepart.getParentMoetedokument(),
                  "moetedokument",
                  expandPaths,
                  currentPath)));
    }
    // Parent is Moetesak
    else if (korrespondansepart.getParentMoetesak() != null) {
      dto.setParent(
          new KorrespondansepartParentDTO(
              moetesakService.maybeExpand(
                  korrespondansepart.getParentMoetesak(), "moetesak", expandPaths, currentPath)));
    }

    return dto;
  }

  /**
   * Delete a Korrespondansepart
   *
   * @param korrpart
   * @return
   */
  @Transactional
  public KorrespondansepartDTO delete(Korrespondansepart obj) {
    var dto = proxy.toDTO(obj);
    dto.setDeleted(true);
    repository.delete(obj);
    return dto;
  }

  @Override
  public Paginators<Korrespondansepart> getPaginators(BaseListQueryDTO params) {
    if (params instanceof KorrespondansepartListQueryDTO p && p.getJournalpostId() != null) {
      var journalpost = journalpostService.findById(p.getJournalpostId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(journalpost, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(journalpost, pivot, pageRequest));
    }
    return super.getPaginators(params);
  }
}

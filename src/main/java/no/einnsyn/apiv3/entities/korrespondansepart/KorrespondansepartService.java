package no.einnsyn.apiv3.entities.korrespondansepart;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.models.Korrespondansepart;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartListQueryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    var journalpostField = dto.getJournalpost();
    if (journalpostField != null) {
      var journalpost = journalpostService.findById(journalpostField.getId());
      if (journalpost == null) {
        throw new EInnsynException(
            "Journalpost with id " + journalpostField.getId() + " not found");
      }
      korrespondansepart.setJournalpost(journalpost);
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

    var journalpost = korrespondansepart.getJournalpost();
    if (journalpost != null) {
      dto.setJournalpost(
          journalpostService.maybeExpand(journalpost, "journalpost", expandPaths, currentPath));
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
  public Page<Korrespondansepart> getPage(BaseListQueryDTO params, PageRequest pageRequest) {
    var journalpostId =
        (params instanceof KorrespondansepartListQueryDTO p) ? p.getJournalpost() : null;
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

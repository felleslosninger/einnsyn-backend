package no.einnsyn.apiv3.entities.korrespondansepart;

import jakarta.transaction.Transactional;
import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.journalpost.JournalpostRepository;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.models.Korrespondansepart;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class KorrespondansepartService
    extends ArkivBaseService<Korrespondansepart, KorrespondansepartDTO> {

  @Getter private final KorrespondansepartRepository repository;

  @Getter @Lazy @Autowired private KorrespondansepartService proxy;

  private final JournalpostRepository journalpostRepository;

  public KorrespondansepartService(
      KorrespondansepartRepository repository, JournalpostRepository journalpostRepository) {
    this.repository = repository;
    this.journalpostRepository = journalpostRepository;
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
      String currentPath) {
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

    ExpandableField<JournalpostDTO> journalpostField = dto.getJournalpost();
    if (journalpostField != null) {
      var journalpost = journalpostRepository.findById(journalpostField.getId()).orElse(null);
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
    var dto = getProxy().toDTO(obj);
    dto.setDeleted(true);
    repository.delete(obj);
    return dto;
  }
}

package no.einnsyn.apiv3.entities.korrespondansepart;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.paginators.Paginators;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.base.models.BaseES;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.models.Korrespondansepart;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartES;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartListQueryDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartParentDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

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
   * Override scheduleReindex to reindex the parent journalpost, moetedokument or moetesak.
   *
   * @param korrespondansepart
   * @param recurseDirection -1 for parents, 1 for children, 0 for both
   */
  @Override
  public void scheduleReindex(Korrespondansepart korrespondansepart, int recurseDirection) {
    super.scheduleReindex(korrespondansepart, recurseDirection);

    // Reindex parents
    if (recurseDirection <= 0) {
      if (korrespondansepart.getParentJournalpost() != null) {
        journalpostService.scheduleReindex(korrespondansepart.getParentJournalpost(), -1);
      }
      if (korrespondansepart.getParentMoetesak() != null) {
        moetesakService.scheduleReindex(korrespondansepart.getParentMoetesak(), -1);
      }
      if (korrespondansepart.getParentMoetedokument() != null) {
        moetedokumentService.scheduleReindex(korrespondansepart.getParentMoetedokument(), -1);
      }
    }
  }

  /**
   * Convert a DTO object to a Korrespondansepart entity object
   *
   * @param dto The DTO object
   * @param korrespondansepart The Korrespondansepart entity object
   * @return The Korrespondansepart entity object
   */
  @Override
  protected Korrespondansepart fromDTO(
      KorrespondansepartDTO dto, Korrespondansepart korrespondansepart) throws EInnsynException {
    super.fromDTO(dto, korrespondansepart);

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

    if (dto.getErBehandlingsansvarlig() != null) {
      korrespondansepart.setErBehandlingsansvarlig(dto.getErBehandlingsansvarlig());
      // Update parent journalpost
      if (dto.getErBehandlingsansvarlig()
          && korrespondansepart.getParentJournalpost() != null
          && korrespondansepart.getAdministrativEnhet() != null) {
        var journalpost = korrespondansepart.getParentJournalpost();
        var administrativEnhetKode = korrespondansepart.getAdministrativEnhet();
        var administrativEnhetObjekt =
            journalpostService.getAdministrativEnhetObjekt(journalpost, administrativEnhetKode);
        journalpost.setAdministrativEnhet(administrativEnhetKode);
        journalpost.setAdministrativEnhetObjekt(administrativEnhetObjekt);
        // TODO: This can be removed when the old API is no longer in use:
        journalpost.setArkivskaper(administrativEnhetObjekt.getIri());
      }
    }

    return korrespondansepart;
  }

  /**
   * Convert a Korrespondansepart to a DTO object
   *
   * @param korrespondansepart The Korrespondansepart entity object
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return The DTO object
   */
  @Override
  protected KorrespondansepartDTO toDTO(
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

  @Override
  public BaseES toLegacyES(Korrespondansepart korrespondansepart, BaseES es) {
    super.toLegacyES(korrespondansepart, es);
    if (es instanceof KorrespondansepartES korrespondansepartES) {
      korrespondansepartES.setKorrespondansepartNavn(
          korrespondansepart.getKorrespondansepartNavn());
      korrespondansepartES.setKorrespondansepartNavn_SENSITIV(
          korrespondansepart.getKorrespondansepartNavnSensitiv());
      korrespondansepartES.setKorrespondanseparttype(
          korrespondansepart.getKorrespondanseparttype());
      korrespondansepartES.setAdministrativEnhet(korrespondansepart.getAdministrativEnhet());
      korrespondansepartES.setSaksbehandler(korrespondansepart.getSaksbehandler());
      korrespondansepartES.setErBehandlingsansvarlig(korrespondansepart.isErBehandlingsansvarlig());
    }
    return es;
  }

  @Override
  protected Paginators<Korrespondansepart> getPaginators(BaseListQueryDTO params) {
    if (params instanceof KorrespondansepartListQueryDTO p && p.getJournalpostId() != null) {
      var journalpost = journalpostService.findById(p.getJournalpostId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(journalpost, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(journalpost, pivot, pageRequest));
    }
    return super.getPaginators(params);
  }
}

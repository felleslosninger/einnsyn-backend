package no.einnsyn.backend.entities.korrespondansepart;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.paginators.Paginators;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseService;
import no.einnsyn.backend.entities.base.models.BaseES;
import no.einnsyn.backend.entities.journalpost.JournalpostRepository;
import no.einnsyn.backend.entities.journalpost.models.ListByJournalpostParameters;
import no.einnsyn.backend.entities.korrespondansepart.models.Korrespondansepart;
import no.einnsyn.backend.entities.korrespondansepart.models.KorrespondansepartDTO;
import no.einnsyn.backend.entities.korrespondansepart.models.KorrespondansepartES;
import no.einnsyn.backend.entities.korrespondansepart.models.KorrespondanseparttypeResolver;
import no.einnsyn.backend.entities.moetedokument.MoetedokumentRepository;
import no.einnsyn.backend.entities.moetesak.MoetesakRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class KorrespondansepartService
    extends ArkivBaseService<Korrespondansepart, KorrespondansepartDTO> {

  @Getter private final KorrespondansepartRepository repository;

  private final JournalpostRepository journalpostRepository;
  private final MoetesakRepository moetesakRepository;
  private final MoetedokumentRepository moetedokumentRepository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private KorrespondansepartService proxy;

  public KorrespondansepartService(
      KorrespondansepartRepository repository,
      JournalpostRepository journalpostRepository,
      MoetesakRepository moetesakRepository,
      MoetedokumentRepository moetedokumentRepository) {
    this.repository = repository;
    this.journalpostRepository = journalpostRepository;
    this.moetesakRepository = moetesakRepository;
    this.moetedokumentRepository = moetedokumentRepository;
  }

  public Korrespondansepart newObject() {
    return new Korrespondansepart();
  }

  public KorrespondansepartDTO newDTO() {
    return new KorrespondansepartDTO();
  }

  /**
   * Override scheduleIndex to reindex the parent journalpost, moetedokument or moetesak.
   *
   * @param korrespondansepartId the ID of the korrespondansepart
   * @param recurseDirection -1 for parents, 1 for children, 0 for both
   */
  @Override
  public boolean scheduleIndex(String korrespondansepartId, int recurseDirection) {
    var isScheduled = super.scheduleIndex(korrespondansepartId, recurseDirection);

    // Reindex parents
    if (recurseDirection <= 0 && !isScheduled) {
      var journalpostId = journalpostRepository.findIdByKorrespondansepartId(korrespondansepartId);
      if (journalpostId != null) {
        journalpostService.scheduleIndex(journalpostId, -1);
        return true;
      }

      var moetesakId = moetesakRepository.findIdByKorrespondansepartId(korrespondansepartId);
      if (moetesakId != null) {
        moetesakService.scheduleIndex(moetesakId, -1);
        return true;
      }

      var moetedokumentId =
          moetedokumentRepository.findByKorrespondansepartId(korrespondansepartId);
      if (moetedokumentId != null) {
        moetedokumentService.scheduleIndex(moetedokumentId, -1);
        return true;
      }
    }

    return true;
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

    if (dto.getErBehandlingsansvarlig() != null) {
      korrespondansepart.setErBehandlingsansvarlig(dto.getErBehandlingsansvarlig());
    }

    if (dto.getJournalpost() != null) {
      korrespondansepart.setParentJournalpost(
          journalpostService.findByIdOrThrow(dto.getJournalpost().getId()));
    } else if (dto.getMoetedokument() != null) {
      korrespondansepart.setParentMoetedokument(
          moetedokumentService.findByIdOrThrow(dto.getMoetedokument().getId()));
    } else if (dto.getMoetesak() != null) {
      korrespondansepart.setParentMoetesak(
          moetesakService.findByIdOrThrow(dto.getMoetesak().getId()));
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
    dto.setErBehandlingsansvarlig(korrespondansepart.isErBehandlingsansvarlig());

    // Only document owners can see Saksbehandler
    if (getProxy().isOwnerOf(korrespondansepart)) {
      dto.setSaksbehandler(korrespondansepart.getSaksbehandler());
      dto.setEpostadresse(korrespondansepart.getEpostadresse());
      dto.setPostnummer(korrespondansepart.getPostnummer());
    }

    // Parent is journalpost
    if (korrespondansepart.getParentJournalpost() != null) {
      dto.setJournalpost(
          journalpostService.maybeExpand(
              korrespondansepart.getParentJournalpost(), "journalpost", expandPaths, currentPath));
    }
    // Parent is Moetedokument
    else if (korrespondansepart.getParentMoetedokument() != null) {
      dto.setMoetedokument(
          moetedokumentService.maybeExpand(
              korrespondansepart.getParentMoetedokument(),
              "moetedokument",
              expandPaths,
              currentPath));
    }
    // Parent is Moetesak
    else if (korrespondansepart.getParentMoetesak() != null) {
      dto.setMoetesak(
          moetesakService.maybeExpand(
              korrespondansepart.getParentMoetesak(), "moetesak", expandPaths, currentPath));
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
          KorrespondanseparttypeResolver.toIRI(korrespondansepart.getKorrespondanseparttype()));
      korrespondansepartES.setErBehandlingsansvarlig(korrespondansepart.isErBehandlingsansvarlig());
      korrespondansepartES.setAdministrativEnhet(korrespondansepart.getAdministrativEnhet());
    }
    return es;
  }

  @Override
  protected Paginators<Korrespondansepart> getPaginators(ListParameters params)
      throws EInnsynException {
    if (params instanceof ListByJournalpostParameters p && p.getJournalpostId() != null) {
      var journalpost = journalpostService.findByIdOrThrow(p.getJournalpostId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(journalpost, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(journalpost, pivot, pageRequest));
    }
    return super.getPaginators(params);
  }
}

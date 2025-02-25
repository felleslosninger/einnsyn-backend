package no.einnsyn.backend.entities.dokumentbeskrivelse;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.paginators.Paginators;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseService;
import no.einnsyn.backend.entities.base.models.BaseES;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseES;
import no.einnsyn.backend.entities.dokumentobjekt.models.DokumentobjektDTO;
import no.einnsyn.backend.entities.dokumentobjekt.models.DokumentobjektES;
import no.einnsyn.backend.entities.journalpost.JournalpostRepository;
import no.einnsyn.backend.entities.journalpost.models.ListByJournalpostParameters;
import no.einnsyn.backend.entities.moetedokument.MoetedokumentRepository;
import no.einnsyn.backend.entities.moetedokument.models.ListByMoetedokumentParameters;
import no.einnsyn.backend.entities.moetesak.MoetesakRepository;
import no.einnsyn.backend.entities.moetesak.models.ListByMoetesakParameters;
import no.einnsyn.backend.entities.utredning.UtredningRepository;
import no.einnsyn.backend.entities.utredning.models.ListByUtredningParameters;
import no.einnsyn.backend.entities.vedtak.VedtakRepository;
import no.einnsyn.backend.entities.vedtak.models.ListByVedtakParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DokumentbeskrivelseService
    extends ArkivBaseService<Dokumentbeskrivelse, DokumentbeskrivelseDTO> {

  @Getter private final DokumentbeskrivelseRepository repository;
  private final JournalpostRepository journalpostRepository;
  private final MoetesakRepository moetesakRepository;
  private final MoetedokumentRepository moetedokumentRepository;
  private final UtredningRepository utredningRepository;
  private final VedtakRepository vedtakRepository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private DokumentbeskrivelseService proxy;

  public DokumentbeskrivelseService(
      DokumentbeskrivelseRepository dokumentbeskrivelseRepository,
      JournalpostRepository journalpostRepository,
      MoetesakRepository moetesakRepository,
      MoetedokumentRepository moetedokumentRepository,
      UtredningRepository utredningRepository,
      VedtakRepository vedtakRepository) {
    this.repository = dokumentbeskrivelseRepository;
    this.journalpostRepository = journalpostRepository;
    this.moetesakRepository = moetesakRepository;
    this.moetedokumentRepository = moetedokumentRepository;
    this.utredningRepository = utredningRepository;
    this.vedtakRepository = vedtakRepository;
  }

  public Dokumentbeskrivelse newObject() {
    return new Dokumentbeskrivelse();
  }

  public DokumentbeskrivelseDTO newDTO() {
    return new DokumentbeskrivelseDTO();
  }

  /**
   * Override scheduleIndex to also trigger reindexing of parents.
   *
   * @param dokumentbeskrivelse
   * @param recurseDirection -1 for parents, 1 for children, 0 for both
   */
  @Override
  public void scheduleIndex(Dokumentbeskrivelse dokumentbeskrivelse, int recurseDirection) {
    super.scheduleIndex(dokumentbeskrivelse, recurseDirection);

    // Reindex parents
    if (recurseDirection <= 0) {
      for (var journalpost : journalpostRepository.findByDokumentbeskrivelse(dokumentbeskrivelse)) {
        journalpostService.scheduleIndex(journalpost, -1);
      }
      for (var moetesak : moetesakRepository.findByDokumentbeskrivelse(dokumentbeskrivelse)) {
        moetesakService.scheduleIndex(moetesak, -1);
      }
      for (var moetedokument :
          moetedokumentRepository.findByDokumentbeskrivelse(dokumentbeskrivelse)) {
        moetedokumentService.scheduleIndex(moetedokument, -1);
      }
    }
  }

  /**
   * Convert a DTO object to a Dokumentbeskrivelse
   *
   * @param dto The DTO object
   * @param dokbesk The entity object
   * @return The entity object
   */
  @Override
  protected Dokumentbeskrivelse fromDTO(DokumentbeskrivelseDTO dto, Dokumentbeskrivelse dokbesk)
      throws EInnsynException {
    super.fromDTO(dto, dokbesk);

    if (dto.getSystemId() != null) {
      dokbesk.setSystemId(dto.getSystemId());
    }

    if (dto.getDokumentnummer() != null) {
      dokbesk.setDokumentnummer(dto.getDokumentnummer());
    }

    if (dto.getDokumenttype() != null) {
      dokbesk.setDokumenttype(dto.getDokumenttype());
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
        var dokobjDTO = dokumentobjektService.getDTO(dokobjField);
        dokumentbeskrivelseService.addDokumentobjekt(dokbesk.getId(), dokobjDTO);
      }
    }

    return dokbesk;
  }

  /**
   * Convert a Dokumentbeskrivelse to a DTO object
   *
   * @param dokbesk The entity object
   * @param dto The DTO object
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return The DTO object
   */
  @Override
  protected DokumentbeskrivelseDTO toDTO(
      Dokumentbeskrivelse dokbesk,
      DokumentbeskrivelseDTO dto,
      Set<String> expandPaths,
      String currentPath) {
    super.toDTO(dokbesk, dto, expandPaths, currentPath);

    dto.setSystemId(dokbesk.getSystemId());
    dto.setDokumentnummer(dokbesk.getDokumentnummer());
    dto.setDokumenttype(dokbesk.getDokumenttype());
    dto.setTilknyttetRegistreringSom(dokbesk.getTilknyttetRegistreringSom());
    dto.setTittel(dokbesk.getTittel());
    dto.setTittelSensitiv(dokbesk.getTittel_SENSITIV());

    // Dokumentobjekt
    dto.setDokumentobjekt(
        dokumentobjektService.maybeExpand(
            dokbesk.getDokumentobjekt(), "dokumentobjekt", expandPaths, currentPath));

    return dto;
  }

  @Override
  public BaseES toLegacyES(Dokumentbeskrivelse dokumentbeskrivelse, BaseES es) {
    super.toLegacyES(dokumentbeskrivelse, es);
    if (es instanceof DokumentbeskrivelseES dokumentbeskrivelseES) {
      dokumentbeskrivelseES.setTittel(dokumentbeskrivelse.getTittel());
      dokumentbeskrivelseES.setTittel_SENSITIV(dokumentbeskrivelse.getTittel_SENSITIV());
      dokumentbeskrivelseES.setTilknyttetRegistreringSom(
          dokumentbeskrivelse.getTilknyttetRegistreringSom());
      dokumentbeskrivelseES.setDokumenttype(dokumentbeskrivelse.getDokumenttype());
      var dokumentobjekt = dokumentbeskrivelse.getDokumentobjekt();
      if (dokumentobjekt != null) {
        var dokumentobjektES =
            dokumentobjekt.stream()
                .map(
                    d ->
                        (DokumentobjektES)
                            dokumentobjektService.toLegacyES(d, new DokumentobjektES()))
                .toList();
        dokumentbeskrivelseES.setDokumentobjekt(dokumentobjektES);
      }
    }
    return es;
  }

  /**
   * Delete a Dokumentbeskrivelse
   *
   * @param dokbesk The entity object
   */
  @Override
  protected void deleteEntity(Dokumentbeskrivelse dokbesk) throws EInnsynException {
    // Delete all dokumentobjekts
    var dokobjList = dokbesk.getDokumentobjekt();
    if (dokobjList != null) {
      dokbesk.setDokumentobjekt(null);
      for (var dokobj : dokobjList) {
        dokumentobjektService.delete(dokobj.getId());
      }
    }

    super.deleteEntity(dokbesk);
  }

  @Transactional(rollbackFor = Exception.class)
  public DokumentbeskrivelseDTO deleteIfOrphan(Dokumentbeskrivelse dokbesk)
      throws EInnsynException {
    // Check if there are objects related to this
    if (journalpostRepository.countByDokumentbeskrivelse(dokbesk) > 0
        || moetesakRepository.countByDokumentbeskrivelse(dokbesk) > 0
        || moetedokumentRepository.countByDokumentbeskrivelse(dokbesk) > 0
        || utredningRepository.countByUtredningsdokument(dokbesk) > 0
        || vedtakRepository.countByVedtaksdokument(dokbesk) > 0) {
      return proxy.toDTO(dokbesk);
    }

    return dokumentbeskrivelseService.delete(dokbesk.getId());
  }

  public DokumentobjektDTO addDokumentobjekt(String dokbeskId, DokumentobjektDTO dto)
      throws EInnsynException {
    dto.setDokumentbeskrivelse(new ExpandableField<>(dokbeskId));
    return dokumentobjektService.add(dto);
  }

  // TODO: Download dokumentbeskrivelse
  public byte[] downloadDokumentbeskrivelse(
      String dokumentbeskrivelseId, String dokumentobjektId, String extension) {
    // var dokumentbeskrivelse = repository.findById(dokumentbeskrivelseId).orElse(null);
    return new byte[0];
  }

  @Override
  protected Paginators<Dokumentbeskrivelse> getPaginators(ListParameters params) {
    if (params instanceof ListByJournalpostParameters p && p.getJournalpostId() != null) {
      var journalpost = journalpostService.findById(p.getJournalpostId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(journalpost, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(journalpost, pivot, pageRequest));
    }
    if (params instanceof ListByMoetesakParameters p && p.getMoetesakId() != null) {
      var moetesak = moetesakService.findById(p.getMoetesakId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(moetesak, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(moetesak, pivot, pageRequest));
    }
    if (params instanceof ListByMoetedokumentParameters p && p.getMoetedokumentId() != null) {
      var moetedokument = moetedokumentService.findById(p.getMoetedokumentId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(moetedokument, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(moetedokument, pivot, pageRequest));
    }
    if (params instanceof ListByUtredningParameters p && p.getUtredningId() != null) {
      var utredning = utredningService.findById(p.getUtredningId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(utredning, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(utredning, pivot, pageRequest));
    }
    if (params instanceof ListByVedtakParameters p && p.getVedtakId() != null) {
      var vedtak = vedtakService.findById(p.getVedtakId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(vedtak, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(vedtak, pivot, pageRequest));
    }
    return super.getPaginators(params);
  }
}

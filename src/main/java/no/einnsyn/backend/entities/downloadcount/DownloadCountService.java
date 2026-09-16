package no.einnsyn.backend.entities.downloadcount;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseService;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBase;
import no.einnsyn.backend.entities.base.BaseService;
import no.einnsyn.backend.entities.base.models.BaseES;
import no.einnsyn.backend.entities.dokumentbeskrivelse.DokumentbeskrivelseRepository;
import no.einnsyn.backend.entities.downloadcount.models.DownloadCount;
import no.einnsyn.backend.entities.downloadcount.models.DownloadCountDTO;
import no.einnsyn.backend.entities.downloadcount.models.DownloadCountES;
import no.einnsyn.backend.entities.journalpost.JournalpostRepository;
import no.einnsyn.backend.entities.moetedokument.MoetedokumentRepository;
import no.einnsyn.backend.entities.moetemappe.MoetemappeRepository;
import no.einnsyn.backend.entities.moetesak.MoetesakRepository;
import no.einnsyn.backend.utils.id.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hourly download buckets per Dokumentobjekt.
 *
 * <p>Buckets are standalone statistics: they are never deleted with the Dokumentobjekt, and there
 * is no API for deleting them. The Journalpost/Moetesak/Moetemappe and the Enhet a bucket is
 * attributed to are therefore resolved once, when the bucket is created and everything still
 * exists, and stored on the row.
 */
@Service
@Slf4j
public class DownloadCountService extends BaseService<DownloadCount, DownloadCountDTO> {

  @Getter(onMethod_ = @Override)
  private final DownloadCountRepository repository;

  private final DokumentbeskrivelseRepository dokumentbeskrivelseRepository;
  private final JournalpostRepository journalpostRepository;
  private final MoetedokumentRepository moetedokumentRepository;
  private final MoetemappeRepository moetemappeRepository;
  private final MoetesakRepository moetesakRepository;

  @SuppressWarnings("java:S6813")
  @Getter(onMethod_ = @Override)
  @Lazy
  @Autowired
  private DownloadCountService proxy;

  public DownloadCountService(
      DownloadCountRepository repository,
      DokumentbeskrivelseRepository dokumentbeskrivelseRepository,
      JournalpostRepository journalpostRepository,
      MoetedokumentRepository moetedokumentRepository,
      MoetemappeRepository moetemappeRepository,
      MoetesakRepository moetesakRepository) {
    this.repository = repository;
    this.dokumentbeskrivelseRepository = dokumentbeskrivelseRepository;
    this.journalpostRepository = journalpostRepository;
    this.moetedokumentRepository = moetedokumentRepository;
    this.moetemappeRepository = moetemappeRepository;
    this.moetesakRepository = moetesakRepository;
  }

  @Override
  public DownloadCount newObject() {
    return new DownloadCount();
  }

  @Override
  public DownloadCountDTO newDTO() {
    return new DownloadCountDTO();
  }

  /**
   * Record a download for a Dokumentobjekt. Atomically creates or increments an hourly bucket in
   * the database.
   *
   * <p>Incrementing an existing bucket is a single atomic update, since concurrent downloads of the
   * same Dokumentobjekt within the same hour are expected on this path. Only when no bucket exists
   * yet is the parent and Enhet resolved, so the resolution cost is paid once per bucket rather
   * than once per download. See {@link DownloadCountRepository#insertOrIncrementCount}.
   *
   * <p>The bucket is deliberately not scheduled for indexing here. Each download would otherwise
   * trigger a full index cycle of the same row, and concurrent cycles race with each other, so the
   * last writer, not the latest count, decides what Elasticsearch holds. The update bumps {@code
   * _updated}, which makes the hourly reindex scheduler pick the bucket up once and index its final
   * count.
   */
  @Transactional
  public void recordDownload(String dokumentobjektId) {
    // Hour boundaries are the same in every whole-hour time zone, so no zone is needed here.
    var bucketStart = Instant.now().truncatedTo(ChronoUnit.HOURS);
    if (repository.incrementCount(dokumentobjektId, bucketStart) > 0) {
      return;
    }

    var parent = findParent(dokumentobjektId);
    var enhet = parent != null ? ArkivBaseService.getAdministrativEnhet(parent) : null;
    if (parent == null) {
      log.warn(
          "No parent found for Dokumentobjekt {}, download is not attributed", dokumentobjektId);
    }
    repository.insertOrIncrementCount(
        IdGenerator.generateId(DownloadCount.class),
        dokumentobjektId,
        bucketStart,
        parent != null ? parent.getId() : null,
        enhet != null ? enhet.getId() : null);
  }

  @Override
  @Transactional(readOnly = true)
  public String getESParent(DownloadCount downloadCount, String id) {
    return downloadCount != null ? downloadCount.getParentId() : null;
  }

  /**
   * Find the Journalpost, Moetesak or Moetemappe that download statistics for a Dokumentobjekt are
   * attributed to. Files attached to a Moetedokument have no Registrering above them, so they are
   * attributed to the Moetemappe.
   *
   * @param dokumentobjektId the Dokumentobjekt
   * @return the parent, or null if the Dokumentobjekt is not attached to any
   */
  private ArkivBase findParent(String dokumentobjektId) {
    var dokumentbeskrivelseId =
        dokumentbeskrivelseRepository.findIdByDokumentobjektId(dokumentobjektId);
    if (dokumentbeskrivelseId == null) {
      return null;
    }

    var journalpostId =
        findFirst(journalpostRepository.streamIdByDokumentbeskrivelseId(dokumentbeskrivelseId));
    if (journalpostId != null) {
      return journalpostRepository.findById(journalpostId).orElse(null);
    }

    var moetesakId =
        findFirst(moetesakRepository.streamIdByDokumentbeskrivelseId(dokumentbeskrivelseId));
    if (moetesakId == null) {
      moetesakId =
          findFirst(moetesakRepository.streamIdByUtredningsdokumentId(dokumentbeskrivelseId));
    }
    if (moetesakId == null) {
      moetesakId = findFirst(moetesakRepository.streamIdByVedtaksdokumentId(dokumentbeskrivelseId));
    }
    if (moetesakId != null) {
      return moetesakRepository.findById(moetesakId).orElse(null);
    }

    var moetedokumentId =
        findFirst(moetedokumentRepository.streamIdByDokumentbeskrivelseId(dokumentbeskrivelseId));
    if (moetedokumentId != null) {
      var moetemappeId = moetemappeRepository.findIdByMoetedokumentId(moetedokumentId);
      return moetemappeId != null ? moetemappeRepository.findById(moetemappeId).orElse(null) : null;
    }

    return null;
  }

  private static String findFirst(Stream<String> stream) {
    try (stream) {
      return stream.findFirst().orElse(null);
    }
  }

  @Override
  public BaseES toLegacyES(DownloadCount downloadCount) {
    return toLegacyES(downloadCount, downloadCount.getParentId());
  }

  @Override
  public BaseES toLegacyES(DownloadCount downloadCount, String esParent) {
    var downloadCountES = new DownloadCountES();
    toLegacyES(downloadCount, downloadCountES);
    if (esParent != null) {
      var relation = new DownloadCountES.DownloadCountRelation();
      relation.setParent(esParent);
      downloadCountES.setStatRelation(relation);
    }
    return downloadCountES;
  }

  @Override
  public BaseES toLegacyES(DownloadCount downloadCount, BaseES es) {
    super.toLegacyES(downloadCount, es);
    if (es instanceof DownloadCountES downloadCountES) {
      downloadCountES.setCount(downloadCount.getCount());

      // Attribute the bucket the same way ArkivBaseService attributes its parent
      var enhet = downloadCount.getEnhet();
      if (enhet != null) {
        var administrativEnhetTransitive = new ArrayList<String>();
        for (var transitiveEnhet : enhetService.getTransitiveEnhets(enhet)) {
          administrativEnhetTransitive.add(transitiveEnhet.getId());
        }
        downloadCountES.setAdministrativEnhet(enhet.getId());
        downloadCountES.setAdministrativEnhetTransitive(administrativEnhetTransitive);
      }
    }
    return es;
  }
}

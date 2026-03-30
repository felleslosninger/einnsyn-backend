package no.einnsyn.backend.entities.downloadcount;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class DownloadCountService extends BaseService<DownloadCount, DownloadCountDTO> {

  private static final ZoneId NORWEGIAN_ZONE = ZoneId.of("Europe/Oslo");

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

  @Override
  protected void authorizeDelete(String id) throws EInnsynException {
    var downloadCount = proxy.findOrThrow(id);
    dokumentobjektService.authorizeDelete(downloadCount.getDokumentobjektId());
  }

  /**
   * Record a download for a Dokumentobjekt. Finds or creates an hourly bucket in the database and
   * schedules it for ES indexing. The parent Journalpost/Moetesak/Moetemappe is resolved later at
   * indexing time.
   */
  @Transactional
  @Retryable(
      includes = {
        ObjectOptimisticLockingFailureException.class,
        DataIntegrityViolationException.class
      })
  public void recordDownload(String dokumentobjektId) {
    try {
      var bucketStart = ZonedDateTime.now(NORWEGIAN_ZONE).truncatedTo(ChronoUnit.HOURS).toInstant();
      var existing = repository.findByDokumentobjektIdAndBucketStart(dokumentobjektId, bucketStart);
      if (existing != null) {
        existing.setCount(existing.getCount() + 1);
        repository.saveAndFlush(existing);
        scheduleIndex(existing.getId());
      } else {
        var stat = newObject();
        stat.setDokumentobjektId(dokumentobjektId);
        stat.setBucketStart(bucketStart);
        stat.setCount(1);
        repository.saveAndFlush(stat);
        scheduleIndex(stat.getId());
      }
    } catch (Exception e) {
      log.warn(
          "Failed to record download statistics for Dokumentobjekt {}: {}",
          dokumentobjektId,
          e.getMessage(),
          e);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public String getESParent(DownloadCount downloadCount, String id) {
    if (downloadCount != null) {
      var parentId = findParentId(downloadCount.getDokumentobjektId());
      if (parentId != null) {
        return parentId;
      }
    }
    // Try to get the parent from the ES index (needed when parent is deleted before child)
    try {
      esClient.indices().refresh(r -> r.index(elasticsearchIndex));
      var esResponse =
          esClient.search(
              sr -> sr.index(elasticsearchIndex).query(q -> q.ids(ids -> ids.values(List.of(id)))),
              Void.class);
      return esResponse.hits().hits().getFirst().routing();
    } catch (Exception e) {
      log.error("Failed to get parent for DownloadCount {}", id, e);
    }
    return null;
  }

  private String findParentId(String dokumentobjektId) {
    var dokumentbeskrivelseId =
        dokumentbeskrivelseRepository.findIdByDokumentobjektId(dokumentobjektId);
    if (dokumentbeskrivelseId == null) {
      return null;
    }

    var journalpostId =
        findFirst(journalpostRepository.streamIdByDokumentbeskrivelseId(dokumentbeskrivelseId));
    if (journalpostId != null) {
      return journalpostId;
    }

    var moetesakId =
        findFirst(moetesakRepository.streamIdByDokumentbeskrivelseId(dokumentbeskrivelseId));
    if (moetesakId != null) {
      return moetesakId;
    }

    var utredningMoetesakId =
        findFirst(moetesakRepository.streamIdByUtredningsdokumentId(dokumentbeskrivelseId));
    if (utredningMoetesakId != null) {
      return utredningMoetesakId;
    }

    var vedtakMoetesakId =
        findFirst(moetesakRepository.streamIdByVedtaksdokumentId(dokumentbeskrivelseId));
    if (vedtakMoetesakId != null) {
      return vedtakMoetesakId;
    }

    var moetedokumentId =
        findFirst(moetedokumentRepository.streamIdByDokumentbeskrivelseId(dokumentbeskrivelseId));
    if (moetedokumentId != null) {
      return moetemappeRepository.findIdByMoetedokumentId(moetedokumentId);
    }

    return null;
  }

  private static String findFirst(Stream<String> stream) {
    try (stream) {
      var iterator = stream.iterator();
      if (iterator.hasNext()) {
        return iterator.next();
      }
      return null;
    }
  }

  @Override
  public BaseES toLegacyES(DownloadCount downloadCount) {
    return toLegacyES(downloadCount, new DownloadCountES());
  }

  @Override
  public BaseES toLegacyES(DownloadCount downloadCount, BaseES es) {
    super.toLegacyES(downloadCount, es);
    if (es instanceof DownloadCountES downloadCountES) {
      downloadCountES.setCount(downloadCount.getCount());

      var parentId = getProxy().getESParent(downloadCount, downloadCount.getId());
      if (parentId != null) {
        var relation = new DownloadCountES.DownloadCountRelation();
        relation.setParent(parentId);
        downloadCountES.setStatRelation(relation);
      }
    }
    return es;
  }
}

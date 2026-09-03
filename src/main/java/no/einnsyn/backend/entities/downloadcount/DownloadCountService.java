package no.einnsyn.backend.entities.downloadcount;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
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
   * Record a download for a Dokumentobjekt. Atomically creates or increments an hourly bucket in
   * the database and schedules it for ES indexing. The parent Journalpost/Moetesak/Moetemappe is
   * resolved later at indexing time.
   *
   * <p>The increment is a single atomic upsert rather than a read-modify-write, since concurrent
   * downloads of the same Dokumentobjekt within the same hour are expected on this path. See {@link
   * DownloadCountRepository#incrementCount}.
   */
  @Transactional
  public void recordDownload(String dokumentobjektId) {
    var bucketStart = ZonedDateTime.now(NORWEGIAN_ZONE).truncatedTo(ChronoUnit.HOURS).toInstant();
    var id =
        repository.incrementCount(
            IdGenerator.generateId(DownloadCount.class), dokumentobjektId, bucketStart);
    scheduleIndex(id);
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
    // Read the routing off the existing ES document. This is needed when the Dokumentobjekt is
    // deleted before its download counts, leaving the parent unresolvable from the database.
    try {
      var routing = findRoutingInIndex(id);
      if (routing != null) {
        return routing;
      }

      // Only a document indexed within this same request is not searchable yet. Download counts
      // are indexed by earlier download requests, so this refresh is the rare path rather than the
      // normal one — refreshing first would mean a full index refresh per document.
      esClient.indices().refresh(r -> r.index(elasticsearchIndex));
      routing = findRoutingInIndex(id);
      if (routing == null) {
        log.debug("No ES document found for DownloadCount {}, it has no parent to resolve", id);
      }
      return routing;
    } catch (Exception e) {
      log.error("Failed to get parent for DownloadCount {}", id, e);
    }
    return null;
  }

  /**
   * Look up the routing (parent id) of an indexed DownloadCount.
   *
   * @param id the DownloadCount id
   * @return the routing, or null if the document is not searchable
   */
  private String findRoutingInIndex(String id) throws IOException {
    var esResponse =
        esClient.search(
            sr -> sr.index(elasticsearchIndex).query(q -> q.ids(ids -> ids.values(List.of(id)))),
            Void.class);
    var hits = esResponse.hits().hits();
    return hits.isEmpty() ? null : hits.getFirst().routing();
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
    // No parent has been resolved for us, so resolve it before building the document. Indexing goes
    // through the overload below instead, reusing the parent it already resolved.
    return toLegacyES(downloadCount, getProxy().getESParent(downloadCount, downloadCount.getId()));
  }

  /**
   * Build the ES document using an already resolved parent. Resolving the parent walks up to six
   * repositories, so indexing must not trigger it a second time.
   */
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
    }
    return es;
  }
}

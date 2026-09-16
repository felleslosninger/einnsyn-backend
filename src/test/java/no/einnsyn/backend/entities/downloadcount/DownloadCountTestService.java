package no.einnsyn.backend.entities.downloadcount;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import no.einnsyn.backend.entities.downloadcount.models.DownloadCount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lookups and transaction choreography for the hourly download buckets, for use in tests. */
@Service
@Lazy
public class DownloadCountTestService {

  private static final long AWAIT_TIMEOUT_SECONDS = 60;

  @Autowired private DownloadCountRepository downloadCountRepository;
  @Autowired private DownloadCountService downloadCountService;
  @Autowired private ElasticsearchClient esClient;

  @Value("${application.elasticsearch.index}")
  private String elasticsearchIndex;

  /**
   * Remove every download bucket from the database and the index.
   *
   * <p>Buckets are standalone statistics and deliberately survive the deletion of the fixtures a
   * test creates, so tests that download something must clean them up themselves to satisfy the row
   * and document count checks in EinnsynTestBase.
   */
  @Transactional
  public void deleteAll() throws IOException {
    esClient.deleteByQuery(
        d ->
            d.index(elasticsearchIndex)
                .query(q -> q.term(t -> t.field("type").value("DownloadCount")))
                .refresh(true));
    downloadCountRepository.deleteAll();
  }

  /**
   * All hourly buckets recorded for a Dokumentobjekt. Production code never looks buckets up by
   * Dokumentobjekt, so rather than keep a query for tests alone, filter the (small) test table.
   */
  @Transactional(readOnly = true)
  public List<DownloadCount> findBuckets(String dokumentobjektId) {
    var buckets = new ArrayList<DownloadCount>();
    for (var bucket : downloadCountRepository.findAll()) {
      if (dokumentobjektId.equals(bucket.getDokumentobjektId())) {
        buckets.add(bucket);
      }
    }
    return buckets;
  }

  /**
   * The id of the Enhet a Dokumentobjekt's single bucket is attributed to, or null if none. The
   * relation is lazy, so it is read here, inside the transaction, rather than by the test.
   */
  @Transactional(readOnly = true)
  public String getEnhetId(String dokumentobjektId) {
    var buckets = findBuckets(dokumentobjektId);
    if (buckets.size() != 1) {
      throw new IllegalStateException("Expected one bucket, found " + buckets.size());
    }
    var enhet = buckets.getFirst().getEnhet();
    return enhet != null ? enhet.getId() : null;
  }

  /** Total number of downloads recorded for a Dokumentobjekt, across all hourly buckets. */
  @Transactional(readOnly = true)
  public int getDownloadCount(String dokumentobjektId) {
    return findBuckets(dokumentobjektId).stream().mapToInt(DownloadCount::getCount).sum();
  }

  /** The most recent update timestamp across all hourly buckets for a Dokumentobjekt. */
  @Transactional(readOnly = true)
  public Instant getUpdated(String dokumentobjektId) {
    return findBuckets(dokumentobjektId).stream()
        .map(DownloadCount::getUpdated)
        .max(Comparator.naturalOrder())
        .orElseThrow();
  }

  /**
   * Record a download from a transaction that opens before {@code transactionStarted} fires, but
   * that only reaches the download bucket after {@code proceed} is released. This lets a test run
   * an "early" transaction behind a later one, which is what separates PostgreSQL's
   * transaction-scoped {@code now()} from {@code clock_timestamp()}.
   */
  @Transactional
  public void recordDownloadInPinnedTransaction(
      String dokumentobjektId, CountDownLatch transactionStarted, CountDownLatch proceed)
      throws InterruptedException {
    // Issue a statement so the transaction really starts here, pinning now() to this point in time.
    downloadCountRepository.count();
    transactionStarted.countDown();
    if (!proceed.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
      throw new IllegalStateException("Timed out waiting for the other transaction");
    }
    downloadCountService.recordDownload(dokumentobjektId);
  }
}

package no.einnsyn.backend.entities.downloadcount;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import no.einnsyn.backend.entities.downloadcount.models.DownloadCount;
import org.springframework.beans.factory.annotation.Autowired;
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

  /** All hourly buckets recorded for a Dokumentobjekt. */
  @Transactional(readOnly = true)
  public List<DownloadCount> findBuckets(String dokumentobjektId) {
    List<String> ids;
    try (var idStream = downloadCountRepository.streamIdByDokumentobjektId(dokumentobjektId)) {
      ids = idStream.toList();
    }
    return downloadCountRepository.findByIdIn(ids);
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

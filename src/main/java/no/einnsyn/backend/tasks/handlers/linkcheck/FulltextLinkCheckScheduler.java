package no.einnsyn.backend.tasks.handlers.linkcheck;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.einnsyn.backend.entities.dokumentobjekt.DokumentobjektRepository;
import no.einnsyn.backend.entities.dokumentobjekt.DokumentobjektService;
import no.einnsyn.backend.utils.ApplicationShutdownListenerService;
import no.einnsyn.backend.utils.ParallelRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Continuously checks the links to fulltext files (Dokumentobjekt). Each run checks the batch of
 * links that were checked longest ago, whether they failed or not, so a large number of failed
 * links can't keep the others from being checked. Since the oldest checks are always picked next,
 * the checker starts over from the beginning once every link has been checked, without keeping
 * track of its position.
 *
 * <p>The checks are started at a steady rate, so the sources get an even load instead of bursts.
 * The concurrency only limits how many checks can run at once when sources are slow to respond.
 */
@Service
@Slf4j
public class FulltextLinkCheckScheduler {

  private final DokumentobjektRepository dokumentobjektRepository;
  private final DokumentobjektService dokumentobjektService;
  private final ApplicationShutdownListenerService applicationShutdownListenerService;
  private final ParallelRunner parallelRunner;

  @Value("${application.linkCheck.batchSize:1000}")
  private int batchSize;

  // Links checked per second. Zero or less turns the link check off.
  @Value("${application.linkCheck.rate:1}")
  private double rate;

  public FulltextLinkCheckScheduler(
      DokumentobjektRepository dokumentobjektRepository,
      DokumentobjektService dokumentobjektService,
      ApplicationShutdownListenerService applicationShutdownListenerService,
      @Value("${application.linkCheck.concurrency:10}") int concurrency) {
    this.parallelRunner = new ParallelRunner(concurrency);
    this.dokumentobjektRepository = dokumentobjektRepository;
    this.dokumentobjektService = dokumentobjektService;
    this.applicationShutdownListenerService = applicationShutdownListenerService;
  }

  /**
   * While the proxy is down, check every minute whether it works again. Not locked, since every
   * instance keeps its own proxy status.
   */
  @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
  public void checkProxyWhileDown() {
    if (!dokumentobjektService.isProxyWorking()) {
      dokumentobjektService.checkReferenceUrls();
    }
  }

  // The rate decides the speed, so the delay only matters when there is nothing to check, e.g.
  // while the proxy is down
  @Scheduled(
      fixedDelay = 1,
      timeUnit = TimeUnit.SECONDS,
      initialDelayString = "${application.linkCheck.initialDelay:PT5M}")
  @SchedulerLock(name = "FulltextLinkCheck", lockAtLeastFor = "PT10S")
  public void checkLinks() {
    if (rate <= 0) {
      log.debug("Fulltext link check is turned off.");
      return;
    }

    // Without a proxy every check would fail on our side, so there is nothing to check
    if (!dokumentobjektService.isDownloadProxyConfigured()) {
      log.debug("Download proxy is not configured, skipping fulltext link check.");
      return;
    }

    // When a link failed on our side, the job is paused until the proxy works again
    if (!dokumentobjektService.isProxyWorking()) {
      log.warn("Reference URLs can't be reached, skipping fulltext link check.");
      return;
    }

    var ids = dokumentobjektRepository.findIdsByOldestLinkCheck(PageRequest.of(0, batchSize));

    // On shutdown, or when the proxy goes down, no new checks are started. The remaining links are
    // picked first by the next run.
    var futures = new ArrayList<CompletableFuture<Void>>();
    var nanosPerCheck = (long) (1_000_000_000L / rate);
    var nextStart = System.nanoTime();
    for (var id : ids) {
      if (!waitUntil(nextStart)) {
        log.warn("Interrupted. Aborting fulltext link check.");
        break;
      }
      if (applicationShutdownListenerService.isShuttingDown()) {
        log.warn("Application is shutting down. Aborting fulltext link check.");
        break;
      }
      if (!dokumentobjektService.isProxyWorking()) {
        log.warn("Reference URLs can't be reached. Aborting fulltext link check.");
        break;
      }
      futures.add(
          parallelRunner.run(
              () -> {
                // ParallelRunner may wait for a free slot after the check above, so check again
                if (!applicationShutdownListenerService.isShuttingDown()) {
                  dokumentobjektService.checkLink(id);
                }
              }));
      // After waiting for a free slot, the lost time isn't made up, so the rate is never exceeded
      nextStart = Math.max(nextStart, System.nanoTime()) + nanosPerCheck;
    }

    // ParallelRunner logs any failure, so failed futures are ignored here
    CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
        .exceptionally(_ -> null)
        .join();
    log.info("Finished checking {} of {} fulltext links.", futures.size(), ids.size());
  }

  /** Sleep until the given System.nanoTime(). Returns false if interrupted. */
  private static boolean waitUntil(long nanoTime) {
    var wait = nanoTime - System.nanoTime();
    if (wait <= 0) {
      return true;
    }
    try {
      Thread.sleep(Duration.ofNanos(wait));
      return true;
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt();
      return false;
    }
  }
}

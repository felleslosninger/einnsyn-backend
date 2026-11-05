package no.einnsyn.backend.tasks.handlers.linkcheck;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.einnsyn.backend.entities.dokumentobjekt.DokumentobjektRepository;
import no.einnsyn.backend.entities.dokumentobjekt.DokumentobjektService;
import no.einnsyn.backend.utils.ParallelRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Continuously checks the links to fulltext files (Dokumentobjekt). Each run checks a batch of
 * links: first failed links that haven't been rechecked recently, then the links that were checked
 * longest ago. Since the oldest checks are always picked next, the checker starts over from the
 * beginning once every link has been checked, without keeping track of its position.
 */
@Service
@Slf4j
public class FulltextLinkCheckScheduler {

  private final DokumentobjektRepository dokumentobjektRepository;
  private final DokumentobjektService dokumentobjektService;
  private final ParallelRunner parallelRunner;

  @Value("${application.linkCheck.batchSize:1000}")
  private int batchSize;

  @Value("${application.linkCheck.recheckFailedAfter:P1D}")
  private Duration recheckFailedAfter;

  public FulltextLinkCheckScheduler(
      DokumentobjektRepository dokumentobjektRepository,
      DokumentobjektService dokumentobjektService,
      @Value("${application.linkCheck.concurrency:10}") int concurrency) {
    this.parallelRunner = new ParallelRunner(concurrency);
    this.dokumentobjektRepository = dokumentobjektRepository;
    this.dokumentobjektService = dokumentobjektService;
  }

  @Scheduled(
      fixedDelayString = "${application.linkCheck.interval:PT1M}",
      initialDelayString = "${application.linkCheck.initialDelay:PT5M}")
  @SchedulerLock(name = "FulltextLinkCheck", lockAtLeastFor = "PT10S")
  public void checkLinks() {
    // Without a proxy every check would fail on our side, so there is nothing to check
    if (!dokumentobjektService.isDownloadProxyConfigured()) {
      log.debug("Download proxy is not configured, skipping fulltext link check.");
      return;
    }

    var ids = new LinkedHashSet<String>();
    ids.addAll(
        dokumentobjektRepository.findIdsWithFailedLinkCheckedBefore(
            Instant.now().minus(recheckFailedAfter), PageRequest.of(0, batchSize)));
    if (ids.size() < batchSize) {
      ids.addAll(
          dokumentobjektRepository.findIdsByOldestLinkCheck(
              PageRequest.of(0, batchSize - ids.size())));
    }

    // ParallelRunner logs any failure, so failed futures are ignored here
    var futures =
        ids.stream()
            .map(id -> parallelRunner.run(() -> dokumentobjektService.checkLink(id)))
            .toArray(CompletableFuture[]::new);
    CompletableFuture.allOf(futures).exceptionally(_ -> null).join();
    log.info("Finished checking {} fulltext links.", ids.size());
  }
}

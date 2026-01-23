package no.einnsyn.backend.tasks.handlers.lagretsoek;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.einnsyn.backend.entities.lagretsoek.LagretSoekRepository;
import no.einnsyn.backend.entities.lagretsoek.LagretSoekService;
import no.einnsyn.backend.utils.ParallelRunner;
import no.einnsyn.backend.utils.ShedlockExtenderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class LegacyLagretSoekConversionScheduler {

  private static final int LOCK_EXTEND_INTERVAL = 60 * 1000; // 1 minute

  private final LagretSoekRepository lagretSoekRepository;
  private final LagretSoekService lagretSoekService;
  private final ShedlockExtenderService shedlockExtenderService;
  private final ParallelRunner parallelRunner;

  @Value("${application.legacyLagretSoekConversion.dryRun:true}")
  private boolean dryRun;

  public LegacyLagretSoekConversionScheduler(
      LagretSoekRepository lagretSoekRepository,
      LagretSoekService lagretSoekService,
      ShedlockExtenderService shedlockExtenderService,
      @Value("${application.elasticsearch.concurrency:10}") int concurrency) {
    this.parallelRunner = new ParallelRunner(concurrency);
    this.lagretSoekRepository = lagretSoekRepository;
    this.lagretSoekService = lagretSoekService;
    this.shedlockExtenderService = shedlockExtenderService;
  }

  @Scheduled(
      fixedDelayString = "${application.legacyLagretSoekConversion.interval:PT1H}",
      initialDelayString = "${application.legacyLagretSoekConversion.initialDelay:PT1M}")
  @SchedulerLock(
      name = "LegacyLagretSoekConversionScheduler",
      lockAtLeastFor = "PT1M",
      lockAtMostFor = "PT10M")
  @Transactional(readOnly = true)
  public void convertLegacyLagretSoek() {
    var startTime = Instant.now();
    log.info("Starting conversion of legacy LagretSoek.");

    try (var idStream = lagretSoekRepository.streamLegacyLagretSoek()) {
      processLegacyRecords(idStream, startTime);
    } catch (Exception e) {
      log.error("Error during conversion of legacy LagretSoek: {}", e.getMessage(), e);
    }
  }

  private void processLegacyRecords(Stream<String> idStream, Instant startTime) {
    var lastExtended = System.currentTimeMillis();
    var futures = ConcurrentHashMap.<CompletableFuture<Void>>newKeySet();
    var found = 0;
    var idIterator = idStream.iterator();

    while (idIterator.hasNext()) {
      var id = idIterator.next();
      found++;
      log.debug("Converting {}, startTime: {}, currently converted: {}", id, startTime, found);

      submitConversionTask(id, futures);
      lastExtended = shedlockExtenderService.maybeExtendLock(lastExtended, LOCK_EXTEND_INTERVAL);
    }

    awaitAllConversions(futures, found);
  }

  private void submitConversionTask(String id, Set<CompletableFuture<Void>> futures) {
    try {
      var future = parallelRunner.run(() -> convertSingleRecord(id));
      futures.add(future);
      future.whenComplete(
          (_, exception) -> {
            futures.remove(future);
            if (exception != null) {
              log.error(
                  "Failed to index document {} in Elasticsearch: {}",
                  id,
                  exception.getMessage(),
                  exception);
            }
          });
    } catch (Exception e) {
      log.error("Failed to convert LagretSoek {}", id, e);
    }
  }

  private void convertSingleRecord(String id) {
    try {
      lagretSoekService.convertLegacyLagretSoek(id, dryRun);
    } catch (Exception e) {
      log.error("Failed to convert LagretSoek {}", id, e);
    }
  }

  private void awaitAllConversions(Set<CompletableFuture<Void>> futures, int found) {
    try {
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
      log.info("Finished converting {} legacy LagretSoek objects.", found);
    } catch (CompletionException e) {
      log.error(
          "One or more conversion tasks failed for LagretSoek. Error: {}",
          e.getCause().getMessage(),
          e.getCause());
    }
  }
}

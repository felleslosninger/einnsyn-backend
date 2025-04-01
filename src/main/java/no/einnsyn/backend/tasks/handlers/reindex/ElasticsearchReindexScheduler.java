package no.einnsyn.backend.tasks.handlers.reindex;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockExtender;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.einnsyn.backend.common.indexable.IndexableRepository;
import no.einnsyn.backend.entities.base.BaseService;
import no.einnsyn.backend.entities.innsynskrav.InnsynskravRepository;
import no.einnsyn.backend.entities.innsynskrav.InnsynskravService;
import no.einnsyn.backend.entities.journalpost.JournalpostRepository;
import no.einnsyn.backend.entities.journalpost.JournalpostService;
import no.einnsyn.backend.entities.moetemappe.MoetemappeRepository;
import no.einnsyn.backend.entities.moetemappe.MoetemappeService;
import no.einnsyn.backend.entities.moetesak.MoetesakRepository;
import no.einnsyn.backend.entities.moetesak.MoetesakService;
import no.einnsyn.backend.entities.saksmappe.SaksmappeRepository;
import no.einnsyn.backend.entities.saksmappe.SaksmappeService;
import no.einnsyn.backend.utils.ParallelRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ElasticsearchReindexScheduler {

  private static final int LOCK_EXTEND_INTERVAL = 60 * 1000; // 1 minute

  @Value("${application.elasticsearch.reindexer.getBatchSize:1000}")
  private int elasticsearchReindexGetBatchSize;

  @Value("${application.elasticsearch.reindexer.indexBatchSize:1000}")
  private int elasticsearchReindexIndexBatchSize;

  @Value("${application.elasticsearch.index}")
  private String elasticsearchIndex;

  @Lazy @Autowired private ElasticsearchReindexScheduler proxy;

  private final ParallelRunner parallelRunner;
  private final JournalpostService journalpostService;
  private final JournalpostRepository journalpostRepository;
  private final SaksmappeService saksmappeService;
  private final SaksmappeRepository saksmappeRepository;
  private final MoetemappeService moetemappeService;
  private final MoetemappeRepository moetemappeRepository;
  private final MoetesakService moetesakService;
  private final MoetesakRepository moetesakRepository;
  private final InnsynskravService innsynskravService;
  private final InnsynskravRepository innsynskravRepository;

  private Instant saksmappeSchemaTimestamp;
  private Instant journalpostSchemaTimestamp;
  private Instant moetemappeSchemaTimestamp;
  private Instant moetesakSchemaTimestamp;
  private Instant innsynskravSchemaTimestamp;

  public ElasticsearchReindexScheduler(
      JournalpostService journalpostService,
      JournalpostRepository journalpostRepository,
      SaksmappeService saksmappeService,
      SaksmappeRepository saksmappeRepository,
      MoetemappeService moetemappeService,
      MoetemappeRepository moetemappeRepository,
      MoetesakService moetesakService,
      MoetesakRepository moetesakRepository,
      InnsynskravService innsynskravService,
      InnsynskravRepository innsynskravRepository,
      @Value("${application.elasticsearch.concurrency:10}") int concurrency,
      @Value("${application.elasticsearch.reindexer.saksmappeSchemaTimestamp}")
          String saksmappeSchemaTimestampString,
      @Value("${application.elasticsearch.reindexer.journalpostSchemaTimestamp}")
          String journalpostSchemaTimestampString,
      @Value("${application.elasticsearch.reindexer.moetemappeSchemaTimestamp}")
          String moetemappeSchemaTimestampString,
      @Value("${application.elasticsearch.reindexer.moetesakSchemaTimestamp}")
          String moetesakSchemaTimestampString,
      @Value("${application.elasticsearch.reindexer.innsynskravSchemaTimestamp}")
          String innsynskravSchemaTimestampString) {
    this.journalpostService = journalpostService;
    this.journalpostRepository = journalpostRepository;
    this.saksmappeService = saksmappeService;
    this.saksmappeRepository = saksmappeRepository;
    this.moetemappeService = moetemappeService;
    this.moetemappeRepository = moetemappeRepository;
    this.moetesakService = moetesakService;
    this.moetesakRepository = moetesakRepository;
    this.innsynskravService = innsynskravService;
    this.innsynskravRepository = innsynskravRepository;
    parallelRunner = new ParallelRunner(concurrency);
    saksmappeSchemaTimestamp = Instant.parse(saksmappeSchemaTimestampString);
    journalpostSchemaTimestamp = Instant.parse(journalpostSchemaTimestampString);
    moetemappeSchemaTimestamp = Instant.parse(moetemappeSchemaTimestampString);
    moetesakSchemaTimestamp = Instant.parse(moetesakSchemaTimestampString);
    innsynskravSchemaTimestamp = Instant.parse(innsynskravSchemaTimestampString);
  }

  // Extend lock every LOCK_EXTEND_INTERVAL
  // Unless we create a new transaction, Shedlock will use the already opened "readOnly" transaction
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public long maybeExtendLock(long lastExtended) {
    var now = System.currentTimeMillis();
    if (now - lastExtended > LOCK_EXTEND_INTERVAL / 2) {
      LockExtender.extendActiveLock(
          Duration.of(LOCK_EXTEND_INTERVAL, ChronoUnit.MILLIS),
          Duration.of(LOCK_EXTEND_INTERVAL, ChronoUnit.MILLIS));
      return now;
    }
    return lastExtended;
  }

  @Transactional(readOnly = true)
  public void reindexForEntity(
      String entityName,
      IndexableRepository<?> repository,
      BaseService<?, ?> service,
      Instant schemaVersion) {
    var lastExtended = System.currentTimeMillis();
    var futures = ConcurrentHashMap.<CompletableFuture<Void>>newKeySet();
    log.info("Starting reindexing of {}.", entityName);

    try (var idStream = repository.findUnIndexed(schemaVersion)) {
      var found = 0;
      var idIterator = idStream.iterator();
      while (idIterator.hasNext()) {
        var id = idIterator.next();
        found++;
        var future = parallelRunner.run(() -> service.index(id));
        lastExtended = proxy.maybeExtendLock(lastExtended);

        futures.add(future);
        future.whenComplete(
            (result, exception) -> {
              futures.remove(future);
              if (exception != null) {
                log.error(
                    "Failed to index document {} in Elasticsearch: {}",
                    id,
                    exception.getMessage(),
                    exception);
              }
            });
      }

      try {
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("Finished indexing {} {} documents.", found, entityName);
      } catch (CompletionException e) {
        log.error(
            "One or more indexing tasks failed for {}. Error:" + " {}",
            entityName,
            e.getCause().getMessage(),
            e.getCause());
        log.info("Attempted indexing {} {} documents before failure.", found, entityName);
      }
    }
  }

  /**
   * Update outdated documents in Elasticsearch. This will loop through all items in Journalpost,
   * Saksmappe, Moetemappe and Moetesak where `lastIndexed` is older than `schemaVersion`, or
   * `lastIndexed` is older than `_updated` and reindex them.
   */
  @Scheduled(cron = "${application.elasticsearch.reindexer.cron.updateOutdated:0 0 * * * *}")
  @SchedulerLock(name = "UpdateOutdatedEs", lockAtLeastFor = "1m")
  public void reindexOutdatedDocuments() {

    proxy.reindexForEntity(
        "Journalpost", journalpostRepository, journalpostService, journalpostSchemaTimestamp);

    proxy.reindexForEntity(
        "Saksmappe", saksmappeRepository, saksmappeService, saksmappeSchemaTimestamp);

    proxy.reindexForEntity(
        "Moetemappe", moetemappeRepository, moetemappeService, moetemappeSchemaTimestamp);

    proxy.reindexForEntity(
        "Moetesak", moetesakRepository, moetesakService, moetesakSchemaTimestamp);

    proxy.reindexForEntity(
        "Innsynskrav", innsynskravRepository, innsynskravService, innsynskravSchemaTimestamp);

    log.info("Finished reindexing of outdated documents");
  }
}

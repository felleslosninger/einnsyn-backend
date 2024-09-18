package no.einnsyn.apiv3.tasks.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Semaphore;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockExtender;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.einnsyn.apiv3.entities.journalpost.JournalpostRepository;
import no.einnsyn.apiv3.entities.journalpost.JournalpostService;
import no.einnsyn.apiv3.entities.moetemappe.MoetemappeRepository;
import no.einnsyn.apiv3.entities.moetemappe.MoetemappeService;
import no.einnsyn.apiv3.entities.moetesak.MoetesakRepository;
import no.einnsyn.apiv3.entities.moetesak.MoetesakService;
import no.einnsyn.apiv3.entities.saksmappe.SaksmappeRepository;
import no.einnsyn.apiv3.entities.saksmappe.SaksmappeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ElasticsearchReindexScheduler {

  private static final int LOCK_EXTEND_INTERVAL = 5 * 60 * 1000; // 5 minutes

  @Value("${application.elasticsearch.reindexer.batchSize:1000}")
  private int elasticsearchReindexBatchSize;

  @Value("${application.elasticsearch.index}")
  private String elasticsearchIndex;

  private final Semaphore semaphore;

  private final ElasticsearchClient esClient;

  private static final Instant schemaVersion = Instant.parse("2024-09-18T00:00:00Z");

  private final JournalpostService journalpostService;
  private final JournalpostRepository journalpostRepository;
  private final SaksmappeService saksmappeService;
  private final SaksmappeRepository saksmappeRepository;
  private final MoetemappeService moetemappeService;
  private final MoetemappeRepository moetemappeRepository;
  private final MoetesakService moetesakService;
  private final MoetesakRepository moetesakRepository;

  public ElasticsearchReindexScheduler(
      ElasticsearchClient esClient,
      JournalpostService journalpostService,
      JournalpostRepository journalpostRepository,
      SaksmappeService saksmappeService,
      SaksmappeRepository saksmappeRepository,
      MoetemappeService moetemappeService,
      MoetemappeRepository moetemappeRepository,
      MoetesakService moetesakService,
      MoetesakRepository moetesakRepository,
      @Value("${application.elasticsearch.reindexer.concurrency:5}") int concurrency) {
    this.esClient = esClient;
    this.journalpostService = journalpostService;
    this.journalpostRepository = journalpostRepository;
    this.saksmappeService = saksmappeService;
    this.saksmappeRepository = saksmappeRepository;
    this.moetemappeService = moetemappeService;
    this.moetemappeRepository = moetemappeRepository;
    this.moetesakService = moetesakService;
    this.moetesakRepository = moetesakRepository;
    semaphore = new Semaphore(concurrency);
  }

  // Extend lock every 5 minutes
  long maybeExtendLock(long lastExtended) {
    var now = System.currentTimeMillis();
    if (now - lastExtended > LOCK_EXTEND_INTERVAL) {
      LockExtender.extendActiveLock(
          Duration.of(5, ChronoUnit.MINUTES), Duration.of(10, ChronoUnit.MINUTES));
      return now;
    }
    return lastExtended;
  }

  /** Update outdated documents in Elasticsearch. */
  @Scheduled(cron = "${application.elasticsearch.reindexer.cron.updateOutdated:0 0 * * * *}")
  @SchedulerLock(name = "UpdateOutdatedEs", lockAtLeastFor = "5m", lockAtMostFor = "10m")
  @Transactional(readOnly = true)
  public void updateOutdatedDocuments() {
    var lastExtended = System.currentTimeMillis();

    try (var journalpostStream = journalpostRepository.findUnIndexed(schemaVersion)) {
      var journalpostIterator = journalpostStream.iterator();
      while (journalpostIterator.hasNext()) {
        var obj = journalpostIterator.next();
        acquire(semaphore);
        journalpostService.index(obj.getId());
        release(semaphore);
        lastExtended = maybeExtendLock(lastExtended);
      }
    }

    try (var saksmappeStream = saksmappeRepository.findUnIndexed(schemaVersion)) {
      var saksmappeIterator = saksmappeStream.iterator();
      while (saksmappeIterator.hasNext()) {
        var obj = saksmappeIterator.next();
        acquire(semaphore);
        saksmappeService.index(obj.getId());
        release(semaphore);
        lastExtended = maybeExtendLock(lastExtended);
      }
    }

    try (var moetemappeStream = moetemappeRepository.findUnIndexed(schemaVersion)) {
      var moetemappeIterator = moetemappeStream.iterator();
      while (moetemappeIterator.hasNext()) {
        var obj = moetemappeIterator.next();
        acquire(semaphore);
        moetemappeService.index(obj.getId());
        release(semaphore);
        lastExtended = maybeExtendLock(lastExtended);
      }
    }

    try (var moetesakStream = moetesakRepository.findUnIndexed(schemaVersion)) {
      var moetesakIterator = moetesakStream.iterator();
      while (moetesakIterator.hasNext()) {
        var obj = moetesakIterator.next();
        acquire(semaphore);
        moetesakService.index(obj.getId());
        release(semaphore);
        lastExtended = maybeExtendLock(lastExtended);
      }
    }
  }

  /** Remove documents from ES that does not exist in the database */
  @Scheduled(cron = "${application.elasticsearch.reindexer.cron.removeStale:0 0 0 * * 6}")
  @SchedulerLock(name = "RemoveStaleEs", lockAtLeastFor = "10m", lockAtMostFor = "1h")
  public void removeStaleDocuments() {
    var lastExtended = System.currentTimeMillis();

    var journalpostEsListIterator =
        new ElasticsearchIdListIterator(
            esClient, elasticsearchIndex, "journalpost", elasticsearchReindexBatchSize);
    while (journalpostEsListIterator.hasNext()) {
      var ids = journalpostEsListIterator.next();
      var removeList = journalpostRepository.findNonExistingIds(ids.toArray(new String[0]));
      deleteDocumentList(removeList);
      lastExtended = maybeExtendLock(lastExtended);
    }

    var saksmappeEsListIterator =
        new ElasticsearchIdListIterator(
            esClient, elasticsearchIndex, "saksmappe", elasticsearchReindexBatchSize);
    while (saksmappeEsListIterator.hasNext()) {
      var ids = saksmappeEsListIterator.next();
      var removeList = saksmappeRepository.findNonExistingIds(ids.toArray(new String[0]));
      deleteDocumentList(removeList);
      lastExtended = maybeExtendLock(lastExtended);
    }

    var moetemappeEsListIterator =
        new ElasticsearchIdListIterator(
            esClient, elasticsearchIndex, "moetemappe", elasticsearchReindexBatchSize);
    while (moetemappeEsListIterator.hasNext()) {
      var ids = moetemappeEsListIterator.next();
      var removeList = moetemappeRepository.findNonExistingIds(ids.toArray(new String[0]));
      deleteDocumentList(removeList);
      lastExtended = maybeExtendLock(lastExtended);
    }

    var moetesakEsListIterator =
        new ElasticsearchIdListIterator(
            esClient, elasticsearchIndex, "moetesak", elasticsearchReindexBatchSize);
    while (moetesakEsListIterator.hasNext()) {
      var ids = moetesakEsListIterator.next();
      var removeList = moetesakRepository.findNonExistingIds(ids.toArray(new String[0]));
      deleteDocumentList(removeList);
      lastExtended = maybeExtendLock(lastExtended);
    }
  }

  /** try / catch wrapper for semaphore.acquire() */
  void acquire(Semaphore semaphore) {
    try {
      semaphore.acquire();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /** wrapper for semaphore.release(), for consistency */
  void release(Semaphore semaphore) {
    semaphore.release();
  }

  /**
   * Helper method to delete a list of documents from Elasticsearch.
   *
   * @param obj
   */
  void deleteDocumentList(List<String> idList) {
    var br = new BulkRequest.Builder();

    if (idList.isEmpty()) {
      return;
    }

    for (String id : idList) {
      br.operations(op -> op.delete(del -> del.index(elasticsearchIndex).id(id)));
    }

    try {
      var bulkRequest = br.build();
      var response = esClient.bulk(bulkRequest);
      if (response.errors()) {
        log.error("Bulk delete had errors. Details: {}", response);
      }
    } catch (Exception e) {
      log.error("Failed to delete documents from Elasticsearch: {}", idList, e);
    }
  }
}

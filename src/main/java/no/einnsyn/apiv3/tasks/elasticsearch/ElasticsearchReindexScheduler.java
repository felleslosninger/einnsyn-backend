package no.einnsyn.apiv3.tasks.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockExtender;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import net.logstash.logback.argument.StructuredArguments;
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
      MoetesakRepository moetesakRepository) {
    this.esClient = esClient;
    this.journalpostService = journalpostService;
    this.journalpostRepository = journalpostRepository;
    this.saksmappeService = saksmappeService;
    this.saksmappeRepository = saksmappeRepository;
    this.moetemappeService = moetemappeService;
    this.moetemappeRepository = moetemappeRepository;
    this.moetesakService = moetesakService;
    this.moetesakRepository = moetesakRepository;
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

  /**
   * Update outdated documents in Elasticsearch. This will loop through all items in Journalpost,
   * Saksmappe, Moetemappe and Moetesak where `lastIndexed` is older than `schemaVersion`, or
   * `lastIndexed` is older than `_updated` and reindex them.
   */
  @Scheduled(cron = "${application.elasticsearch.reindexer.cron.updateOutdated:0 0 * * * *}")
  @SchedulerLock(name = "UpdateOutdatedEs", lockAtLeastFor = "9m", lockAtMostFor = "10m")
  @Transactional(readOnly = true)
  public void updateOutdatedDocuments() {
    var lastExtended = System.currentTimeMillis();
    log.info("Starting reindexing of outdated documents");

    try (var journalpostStream = journalpostRepository.findUnIndexed(schemaVersion)) {
      var journalpostIterator = journalpostStream.iterator();
      while (journalpostIterator.hasNext()) {
        var obj = journalpostIterator.next();
        log.debug("Reindex journalpost {}", obj.getId());
        journalpostService.index(obj.getId());
        lastExtended = maybeExtendLock(lastExtended);
      }
    }

    try (var saksmappeStream = saksmappeRepository.findUnIndexed(schemaVersion)) {
      var saksmappeIterator = saksmappeStream.iterator();
      while (saksmappeIterator.hasNext()) {
        var obj = saksmappeIterator.next();
        log.debug("Reindex saksmappe {}", obj.getId());
        saksmappeService.index(obj.getId());
        lastExtended = maybeExtendLock(lastExtended);
      }
    }

    try (var moetemappeStream = moetemappeRepository.findUnIndexed(schemaVersion)) {
      var moetemappeIterator = moetemappeStream.iterator();
      while (moetemappeIterator.hasNext()) {
        var obj = moetemappeIterator.next();
        log.debug("Reindex moetemappe {}", obj.getId());
        moetemappeService.index(obj.getId());
        lastExtended = maybeExtendLock(lastExtended);
      }
    }

    try (var moetesakStream = moetesakRepository.findUnIndexed(schemaVersion)) {
      var moetesakIterator = moetesakStream.iterator();
      while (moetesakIterator.hasNext()) {
        var obj = moetesakIterator.next();
        log.debug("Reindex moetesak {}", obj.getId());
        moetesakService.index(obj.getId());
        lastExtended = maybeExtendLock(lastExtended);
      }
    }

    log.info("Finished reindexing of outdated documents");
  }

  /**
   * Remove documents from ES that does not exist in the database. This will loop through all items
   * in Elastic in batches, and query Postgres for the ids in each batch that are *not* in the
   * database. These will then be deleted from Elastic.
   */
  @Scheduled(cron = "${application.elasticsearch.reindexer.cron.removeStale:0 0 0 * * 6}")
  @SchedulerLock(name = "RemoveStaleEs", lockAtLeastFor = "9m", lockAtMostFor = "10m")
  public void removeStaleDocuments() {
    var lastExtended = System.currentTimeMillis();
    log.info("Starting removal of stale documents");

    var journalpostEsListIterator =
        new ElasticsearchIdListIterator(
            esClient, elasticsearchIndex, "Journalpost", elasticsearchReindexBatchSize);
    while (journalpostEsListIterator.hasNext()) {
      var ids = journalpostEsListIterator.next();
      var removeList = journalpostRepository.findNonExistingIds(ids.toArray(new String[0]));
      deleteDocumentList(removeList);
      lastExtended = maybeExtendLock(lastExtended);
    }

    var saksmappeEsListIterator =
        new ElasticsearchIdListIterator(
            esClient, elasticsearchIndex, "Saksmappe", elasticsearchReindexBatchSize);
    while (saksmappeEsListIterator.hasNext()) {
      var ids = saksmappeEsListIterator.next();
      var removeList = saksmappeRepository.findNonExistingIds(ids.toArray(new String[0]));
      deleteDocumentList(removeList);
      lastExtended = maybeExtendLock(lastExtended);
    }

    var moetemappeEsListIterator =
        new ElasticsearchIdListIterator(
            esClient, elasticsearchIndex, "Moetemappe", elasticsearchReindexBatchSize);
    while (moetemappeEsListIterator.hasNext()) {
      var ids = moetemappeEsListIterator.next();
      var removeList = moetemappeRepository.findNonExistingIds(ids.toArray(new String[0]));
      deleteDocumentList(removeList);
      lastExtended = maybeExtendLock(lastExtended);
    }

    var moetesakEsListIterator =
        new ElasticsearchIdListIterator(
            esClient, elasticsearchIndex, "Moetesak", elasticsearchReindexBatchSize);
    while (moetesakEsListIterator.hasNext()) {
      var ids = moetesakEsListIterator.next();
      var removeList = moetesakRepository.findNonExistingIds(ids.toArray(new String[0]));
      deleteDocumentList(removeList);
      lastExtended = maybeExtendLock(lastExtended);
    }

    log.info("Finished removal of stale documents");
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

    log.info(
        "Removing {} documents",
        idList.size(),
        StructuredArguments.raw("documents", String.join(", ", idList)));

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

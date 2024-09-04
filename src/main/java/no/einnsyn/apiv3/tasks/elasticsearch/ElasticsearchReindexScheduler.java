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

@Slf4j
public class ElasticsearchReindexScheduler {

  private static final int LOCK_EXTEND_INTERVAL = 5 * 60 * 1000;

  @Value("${application.elasticsearchReindexBatchSize:1000}")
  private int elasticsearchReindexBatchSize;

  @Value("${application.elasticsearchIndex}")
  private String elasticsearchIndex;

  private final ElasticsearchClient esClient;

  private static final Instant schemaVersion = Instant.parse("2024-08-01T00:00:00Z");

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

  /** Update outdated documents in Elasticsearch. */
  @Scheduled(cron = "0 0 * * * *")
  @SchedulerLock(name = "UpdateOutdatedEs", lockAtLeastFor = "5m", lockAtMostFor = "10m")
  void updateOutdatedDocuments() {
    var lastExtended = System.currentTimeMillis();

    try (var journalpostStream =
        journalpostRepository.findAllByLastIndexedLessThanUpdatedOrLastIndexedLessThanOrderByIdAsc(
            schemaVersion)) {
      var journalpostIterator = journalpostStream.iterator();
      while (journalpostIterator.hasNext()) {
        var obj = journalpostIterator.next();
        journalpostService.index(obj.getId());
        lastExtended = maybeExtendLock(lastExtended);
      }
    }

    try (var saksmappeStream =
        saksmappeRepository.findAllByLastIndexedLessThanUpdatedOrLastIndexedLessThanOrderByIdAsc(
            schemaVersion)) {
      var saksmappeIterator = saksmappeStream.iterator();
      while (saksmappeIterator.hasNext()) {
        var obj = saksmappeIterator.next();
        saksmappeService.index(obj.getId());
        lastExtended = maybeExtendLock(lastExtended);
      }
    }

    try (var moetemappeStream =
        moetemappeRepository.findAllByLastIndexedLessThanUpdatedOrLastIndexedLessThanOrderByIdAsc(
            schemaVersion)) {
      var moetemappeIterator = moetemappeStream.iterator();
      while (moetemappeIterator.hasNext()) {
        var obj = moetemappeIterator.next();
        moetemappeService.index(obj.getId());
        lastExtended = maybeExtendLock(lastExtended);
      }
    }

    try (var moetesakStream =
        moetesakRepository.findAllByLastIndexedLessThanUpdatedOrLastIndexedLessThanOrderByIdAsc(
            schemaVersion)) {
      var moetesakIterator = moetesakStream.iterator();
      while (moetesakIterator.hasNext()) {
        var obj = moetesakIterator.next();
        moetesakService.index(obj.getId());
        lastExtended = maybeExtendLock(lastExtended);
      }
    }
  }

  /** */
  @Scheduled(cron = "0 0 0 * * 6")
  @SchedulerLock(name = "RemoveStaleEs", lockAtLeastFor = "10m", lockAtMostFor = "1h")
  void removeStaleDocuments() {
    var lastExtended = System.currentTimeMillis();

    var journalpostEsListIterator =
        new ElasticsearchIdListIterator(
            esClient, elasticsearchIndex, "journalpost", elasticsearchReindexBatchSize);
    while (journalpostEsListIterator.hasNext()) {
      var ids = journalpostEsListIterator.next();
      var removeList = journalpostRepository.findNonExistingIds(ids);
      deleteDocumentList(removeList);
      lastExtended = maybeExtendLock(lastExtended);
    }

    var saksmappeEsListIterator =
        new ElasticsearchIdListIterator(
            esClient, elasticsearchIndex, "saksmappe", elasticsearchReindexBatchSize);
    while (saksmappeEsListIterator.hasNext()) {
      var ids = saksmappeEsListIterator.next();
      var removeList = saksmappeRepository.findNonExistingIds(ids);
      deleteDocumentList(removeList);
      lastExtended = maybeExtendLock(lastExtended);
    }

    var moetemappeEsListIterator =
        new ElasticsearchIdListIterator(
            esClient, elasticsearchIndex, "moetemappe", elasticsearchReindexBatchSize);
    while (moetemappeEsListIterator.hasNext()) {
      var ids = moetemappeEsListIterator.next();
      var removeList = moetemappeRepository.findNonExistingIds(ids);
      deleteDocumentList(removeList);
      lastExtended = maybeExtendLock(lastExtended);
    }

    var moetesakEsListIterator =
        new ElasticsearchIdListIterator(
            esClient, elasticsearchIndex, "moetesak", elasticsearchReindexBatchSize);
    while (moetesakEsListIterator.hasNext()) {
      var ids = moetesakEsListIterator.next();
      var removeList = moetesakRepository.findNonExistingIds(ids);
      deleteDocumentList(removeList);
      lastExtended = maybeExtendLock(lastExtended);
    }
  }

  /**
   * Helper method to delete a list of documents from Elasticsearch.
   *
   * @param obj
   */
  void deleteDocumentList(List<String> idList) {
    var br = new BulkRequest.Builder();

    for (String id : idList) {
      br.operations(op -> op.delete(del -> del.index(elasticsearchIndex).id(id)));
    }

    try {
      var response = esClient.bulk(br.build());
      if (response.errors()) {
        log.error("Bulk delete had errors. Details: {}", response);
      }
    } catch (Exception e) {
      log.error("Failed to delete documents from Elasticsearch: {}", idList, e);
    }
  }
}

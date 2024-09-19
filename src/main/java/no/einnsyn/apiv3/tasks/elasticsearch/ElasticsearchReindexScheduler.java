package no.einnsyn.apiv3.tasks.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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
import no.einnsyn.apiv3.utils.ParallelRunner;
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

  private static final int LOCK_EXTEND_INTERVAL = 5 * 60 * 1000; // 5 minutes

  @Value("${application.elasticsearch.reindexer.batchSize:1000}")
  private int elasticsearchReindexBatchSize;

  @Value("${application.elasticsearch.index}")
  private String elasticsearchIndex;

  @Lazy @Autowired ElasticsearchReindexScheduler proxy;

  private final ParallelRunner parallelRunner;

  private final ElasticsearchClient esClient;

  private static final Instant schemaVersion = Instant.parse("2024-09-19T18:03:00Z");

  private final JournalpostService journalpostService;
  private final JournalpostRepository journalpostRepository;
  private final SaksmappeService saksmappeService;
  private final SaksmappeRepository saksmappeRepository;
  private final MoetemappeService moetemappeService;
  private final MoetemappeRepository moetemappeRepository;
  private final MoetesakService moetesakService;
  private final MoetesakRepository moetesakRepository;

  private final EntityManager entityManager;

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
      EntityManager entityManager,
      @Value("${application.elasticsearch.concurrency:10}") int concurrency) {
    this.esClient = esClient;
    this.journalpostService = journalpostService;
    this.journalpostRepository = journalpostRepository;
    this.saksmappeService = saksmappeService;
    this.saksmappeRepository = saksmappeRepository;
    this.moetemappeService = moetemappeService;
    this.moetemappeRepository = moetemappeRepository;
    this.moetesakService = moetesakService;
    this.moetesakRepository = moetesakRepository;
    this.entityManager = entityManager;
    parallelRunner = new ParallelRunner(concurrency);
  }

  // Extend lock every 5 minutes
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public long maybeExtendLock(long lastExtended) {
    var now = System.currentTimeMillis();
    if (now - lastExtended > LOCK_EXTEND_INTERVAL) {
      LockExtender.extendActiveLock(
          Duration.of(LOCK_EXTEND_INTERVAL * 2, ChronoUnit.MILLIS),
          Duration.of(LOCK_EXTEND_INTERVAL * 2, ChronoUnit.MILLIS));
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
  @SchedulerLock(name = "UpdateOutdatedEs", lockAtLeastFor = "10m", lockAtMostFor = "10m")
  @Transactional(readOnly = true)
  public void updateOutdatedDocuments() {
    var lastExtended = System.currentTimeMillis();
    log.info("Starting reindexing of outdated documents");

    try (var journalpostStream = journalpostRepository.findUnIndexed(schemaVersion)) {
      var foundJournalpost = new AtomicInteger(0);
      var journalpostIterator = journalpostStream.iterator();
      while (journalpostIterator.hasNext()) {
        var id = journalpostIterator.next().getId();
        parallelRunner.run(
            () -> {
              log.info("Reindex journalpost {}", id);
              journalpostService.index(id);
              foundJournalpost.incrementAndGet();
            });
        if (foundJournalpost.get() % 1000 == 0) {
          entityManager.clear();
        }
        lastExtended = proxy.maybeExtendLock(lastExtended);
      }
      log.info("Finished reindexing of {} outdated Journalposts", foundJournalpost);
    } catch (Exception e) {
      log.error("Failed to reindex journalpost", e);
    }

    try (var saksmappeStream = saksmappeRepository.findUnIndexed(schemaVersion)) {
      var foundSaksmappe = new AtomicInteger(0);
      var saksmappeIterator = saksmappeStream.iterator();
      while (saksmappeIterator.hasNext()) {
        var id = saksmappeIterator.next().getId();
        parallelRunner.run(
            () -> {
              log.info("Reindex saksmappe {}", id);
              saksmappeService.index(id);
              foundSaksmappe.incrementAndGet();
            });
        lastExtended = proxy.maybeExtendLock(lastExtended);
        if (foundSaksmappe.get() % 1000 == 0) {
          entityManager.clear();
        }
      }
      log.info("Finished reindexing of {} outdated Saksmappe", foundSaksmappe);
    } catch (Exception e) {
      log.error("Failed to reindex saksmappe", e);
    }

    try (var moetemappeStream = moetemappeRepository.findUnIndexed(schemaVersion)) {
      var foundMoetemappe = new AtomicInteger(0);
      var moetemappeIterator = moetemappeStream.iterator();
      while (moetemappeIterator.hasNext()) {
        var id = moetemappeIterator.next().getId();
        parallelRunner.run(
            () -> {
              log.info("Reindex moetemappe {}", id);
              moetemappeService.index(id);
              foundMoetemappe.incrementAndGet();
            });
        lastExtended = proxy.maybeExtendLock(lastExtended);
        if (foundMoetemappe.get() % 1000 == 0) {
          entityManager.clear();
        }
      }
      log.info("Finished reindexing of {} outdated Moetemappe", foundMoetemappe);
    } catch (Exception e) {
      log.error("Failed to reindex moetemappe", e);
    }

    try (var moetesakStream = moetesakRepository.findUnIndexed(schemaVersion)) {
      var foundMoetesak = new AtomicInteger(0);
      var moetesakIterator = moetesakStream.iterator();
      while (moetesakIterator.hasNext()) {
        var id = moetesakIterator.next().getId();
        parallelRunner.run(
            () -> {
              log.info("Reindex moetesak {}", id);
              moetesakService.index(id);
              foundMoetesak.incrementAndGet();
            });
        lastExtended = proxy.maybeExtendLock(lastExtended);
        if (foundMoetesak.get() % 1000 == 0) {
          entityManager.clear();
      }
      log.info("Finished reindexing of {} outdated Moetesak", foundMoetesak);
    } catch (Exception e) {
      log.error("Failed to reindex moetesak", e);
    }

    log.info("Finished reindexing of outdated documents");
  }

  /**
   * Remove documents from ES that does not exist in the database. This will loop through all items
   * in Elastic in batches, and query Postgres for the ids in each batch that are *not* in the
   * database. These will then be deleted from Elastic.
   */
  @Scheduled(cron = "${application.elasticsearch.reindexer.cron.removeStale:0 0 0 * * 6}")
  @SchedulerLock(name = "RemoveStaleEs", lockAtLeastFor = "10m", lockAtMostFor = "10m")
  public void removeStaleDocuments() {
    var lastExtended = System.currentTimeMillis();
    log.info("Starting removal of stale documents");

    var foundJournalpost = 0;
    var removedJournalpost = 0;
    var journalpostEsListIterator =
        new ElasticsearchIdListIterator(
            esClient, elasticsearchIndex, "Journalpost", elasticsearchReindexBatchSize);
    while (journalpostEsListIterator.hasNext()) {
      var ids = journalpostEsListIterator.next();
      foundJournalpost += ids.size();
      var removeList = journalpostRepository.findNonExistingIds(ids.toArray(new String[0]));
      removedJournalpost += removeList.size();
      deleteDocumentList(removeList);
      lastExtended = proxy.maybeExtendLock(lastExtended);
    }
    log.info(
        "Finished removal of stale Journalposts. Found {}, removed {}.",
        foundJournalpost,
        removedJournalpost);

    var foundSaksmappe = 0;
    var removedSaksmappe = 0;
    var saksmappeEsListIterator =
        new ElasticsearchIdListIterator(
            esClient, elasticsearchIndex, "Saksmappe", elasticsearchReindexBatchSize);
    while (saksmappeEsListIterator.hasNext()) {
      var ids = saksmappeEsListIterator.next();
      foundSaksmappe += ids.size();
      var removeList = saksmappeRepository.findNonExistingIds(ids.toArray(new String[0]));
      removedSaksmappe += removeList.size();
      deleteDocumentList(removeList);
      lastExtended = proxy.maybeExtendLock(lastExtended);
    }
    log.info(
        "Finished removal of stale Saksmappes. Found {}, removed {}.",
        foundSaksmappe,
        removedSaksmappe);

    var foundMoetemappe = 0;
    var removedMoetemappe = 0;
    var moetemappeEsListIterator =
        new ElasticsearchIdListIterator(
            esClient, elasticsearchIndex, "Moetemappe", elasticsearchReindexBatchSize);
    while (moetemappeEsListIterator.hasNext()) {
      var ids = moetemappeEsListIterator.next();
      foundMoetemappe += ids.size();
      var removeList = moetemappeRepository.findNonExistingIds(ids.toArray(new String[0]));
      removedMoetemappe += removeList.size();
      deleteDocumentList(removeList);
      lastExtended = proxy.maybeExtendLock(lastExtended);
    }
    log.info(
        "Finished removal of stale Moetemappes. Found {}, removed {}.",
        foundMoetemappe,
        removedMoetemappe);

    var foundMoetesak = 0;
    var removedMoetesak = 0;
    var moetesakEsListIterator =
        new ElasticsearchIdListIterator(
            esClient, elasticsearchIndex, "Moetesak", elasticsearchReindexBatchSize);
    while (moetesakEsListIterator.hasNext()) {
      var ids = moetesakEsListIterator.next();
      foundMoetesak += ids.size();
      var removeList = moetesakRepository.findNonExistingIds(ids.toArray(new String[0]));
      removedMoetesak += removeList.size();
      deleteDocumentList(removeList);
      lastExtended = proxy.maybeExtendLock(lastExtended);
    }
    log.info(
        "Finished removal of stale Moetesaks. Found {}, removed {}.",
        foundMoetesak,
        removedMoetesak);
  }

  /**
   * Helper method to delete a list of documents from Elasticsearch.
   *
   * @param obj
   */
  void deleteDocumentList(List<String> idList) {
    var br = new BulkRequest.Builder();

    log.debug("Removing {} documents", idList.size());

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

package no.einnsyn.apiv3.tasks.handlers.reindex;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.google.gson.Gson;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockExtender;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import net.logstash.logback.argument.StructuredArguments;
import no.einnsyn.apiv3.entities.base.models.Base;
import no.einnsyn.apiv3.entities.innsynskrav.InnsynskravRepository;
import no.einnsyn.apiv3.entities.innsynskrav.InnsynskravService;
import no.einnsyn.apiv3.entities.journalpost.JournalpostRepository;
import no.einnsyn.apiv3.entities.journalpost.JournalpostService;
import no.einnsyn.apiv3.entities.moetemappe.MoetemappeRepository;
import no.einnsyn.apiv3.entities.moetemappe.MoetemappeService;
import no.einnsyn.apiv3.entities.moetesak.MoetesakRepository;
import no.einnsyn.apiv3.entities.moetesak.MoetesakService;
import no.einnsyn.apiv3.entities.saksmappe.SaksmappeRepository;
import no.einnsyn.apiv3.entities.saksmappe.SaksmappeService;
import no.einnsyn.apiv3.utils.ElasticsearchIterator;
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

  @Value("${application.elasticsearch.reindexer.getBatchSize:1000}")
  private int elasticsearchReindexGetBatchSize;

  @Value("${application.elasticsearch.reindexer.indexBatchSize:1000}")
  private int elasticsearchReindexIndexBatchSize;

  @Value("${application.elasticsearch.index}")
  private String elasticsearchIndex;

  @Lazy @Autowired ElasticsearchReindexScheduler proxy;

  private final ParallelRunner parallelRunner;
  private final ElasticsearchClient esClient;
  private final EntityManager entityManager;
  private final Gson gson;

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
      ElasticsearchClient esClient,
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
      EntityManager entityManager,
      Gson gson,
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
    this.esClient = esClient;
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
    this.entityManager = entityManager;
    this.gson = gson;
    parallelRunner = new ParallelRunner(concurrency);
    saksmappeSchemaTimestamp = Instant.parse(saksmappeSchemaTimestampString);
    journalpostSchemaTimestamp = Instant.parse(journalpostSchemaTimestampString);
    moetemappeSchemaTimestamp = Instant.parse(moetemappeSchemaTimestampString);
    moetesakSchemaTimestamp = Instant.parse(moetesakSchemaTimestampString);
    innsynskravSchemaTimestamp = Instant.parse(innsynskravSchemaTimestampString);
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

  private void maybeClearEntityManager(int count) {
    if (count % 10000 == 0) {
      entityManager.clear();
    }
  }

  private List<String> getNextBatch(Iterator<? extends Base> iterator) {
    var list = new ArrayList<String>();
    for (int i = 0; i < elasticsearchReindexIndexBatchSize && iterator.hasNext(); i++) {
      list.add(iterator.next().getId());
    }
    return list;
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

    try (var journalpostStream = journalpostRepository.findUnIndexed(journalpostSchemaTimestamp)) {
      var foundJournalpost = new AtomicInteger(0);
      var journalpostIterator = journalpostStream.iterator();
      while (journalpostIterator.hasNext()) {
        var batch = getNextBatch(journalpostIterator);
        foundJournalpost.addAndGet(batch.size());
        parallelRunner.run(() -> journalpostService.reIndex(batch));
        lastExtended = proxy.maybeExtendLock(lastExtended);
        maybeClearEntityManager(foundJournalpost.get());
      }
      log.info("Finished reindexing of {} outdated Journalposts", foundJournalpost);
    } catch (Exception e) {
      log.error("Failed to reindex journalpost", e);
    }

    try (var saksmappeStream = saksmappeRepository.findUnIndexed(saksmappeSchemaTimestamp)) {
      var foundSaksmappe = new AtomicInteger(0);
      var saksmappeIterator = saksmappeStream.iterator();
      while (saksmappeIterator.hasNext()) {
        var batch = getNextBatch(saksmappeIterator);
        foundSaksmappe.addAndGet(batch.size());
        parallelRunner.run(() -> saksmappeService.reIndex(batch));
        lastExtended = proxy.maybeExtendLock(lastExtended);
        maybeClearEntityManager(foundSaksmappe.get());
      }
      log.info("Finished reindexing of {} outdated Saksmappe", foundSaksmappe);
    } catch (Exception e) {
      log.error("Failed to reindex saksmappe", e);
    }

    try (var moetemappeStream = moetemappeRepository.findUnIndexed(moetemappeSchemaTimestamp)) {
      var foundMoetemappe = new AtomicInteger(0);
      var moetemappeIterator = moetemappeStream.iterator();
      while (moetemappeIterator.hasNext()) {
        var batch = getNextBatch(moetemappeIterator);
        foundMoetemappe.addAndGet(batch.size());
        parallelRunner.run(() -> moetemappeService.reIndex(batch));
        lastExtended = proxy.maybeExtendLock(lastExtended);
        maybeClearEntityManager(foundMoetemappe.get());
      }
      log.info("Finished reindexing of {} outdated Moetemappe", foundMoetemappe);
    } catch (Exception e) {
      log.error("Failed to reindex moetemappe", e);
    }

    try (var moetesakStream = moetesakRepository.findUnIndexed(moetesakSchemaTimestamp)) {
      var foundMoetesak = new AtomicInteger(0);
      var moetesakIterator = moetesakStream.iterator();
      while (moetesakIterator.hasNext()) {
        var batch = getNextBatch(moetesakIterator);
        foundMoetesak.addAndGet(batch.size());
        parallelRunner.run(() -> moetesakService.reIndex(batch));
        lastExtended = proxy.maybeExtendLock(lastExtended);
        maybeClearEntityManager(foundMoetesak.get());
      }
      log.info("Finished reindexing of {} outdated Moetesak", foundMoetesak);
    } catch (Exception e) {
      log.error("Failed to reindex moetesak", e);
    }

    try (var innsynskravStream = innsynskravRepository.findUnIndexed(innsynskravSchemaTimestamp)) {
      var foundInnsynskrav = new AtomicInteger(0);
      var innsynskravIterator = innsynskravStream.iterator();
      while (innsynskravIterator.hasNext()) {
        var batch = getNextBatch(innsynskravIterator);
        foundInnsynskrav.addAndGet(batch.size());
        parallelRunner.run(() -> innsynskravService.reIndex(batch));
        lastExtended = proxy.maybeExtendLock(lastExtended);
        maybeClearEntityManager(foundInnsynskrav.get());
      }
      log.info("Finished reindexing of {} outdated Innsynskrav", foundInnsynskrav);
    } catch (Exception e) {
      log.error("Failed to reindex innsynskrav", e);
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
    var journalpostIterator =
        new ElasticsearchIterator<Void>(
            esClient,
            elasticsearchIndex,
            elasticsearchReindexGetBatchSize,
            getEsQuery("Journalpost"),
            List.of("publisertDato", "opprettetDato", "standardDato", "saksnummerGenerert"),
            Void.class);
    while (journalpostIterator.hasNext()) {
      var ids = journalpostIterator.nextBatch().stream().map(Hit::id).toList();
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
    var saksmappeIterator =
        new ElasticsearchIterator<Void>(
            esClient,
            elasticsearchIndex,
            elasticsearchReindexGetBatchSize,
            getEsQuery("Saksmappe"),
            List.of("publisertDato", "opprettetDato", "standardDato", "saksnummerGenerert"),
            Void.class);
    while (saksmappeIterator.hasNext()) {
      var ids = saksmappeIterator.nextBatch().stream().map(Hit::id).toList();
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
    var moetemappeIterator =
        new ElasticsearchIterator<Void>(
            esClient,
            elasticsearchIndex,
            elasticsearchReindexGetBatchSize,
            getEsQuery("Moetemappe"),
            List.of("publisertDato", "opprettetDato", "standardDato", "saksnummerGenerert"),
            Void.class);
    while (moetemappeIterator.hasNext()) {
      var ids = moetemappeIterator.nextBatch().stream().map(Hit::id).toList();
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
    var moetesakIterator =
        new ElasticsearchIterator<Void>(
            esClient,
            elasticsearchIndex,
            elasticsearchReindexGetBatchSize,
            getEsQuery("Møtesaksregistrering", "KommerTilBehandlingMøtesaksregistrering"),
            List.of("publisertDato", "opprettetDato", "standardDato", "saksnummerGenerert"),
            Void.class);
    while (moetesakIterator.hasNext()) {
      var ids = moetesakIterator.nextBatch().stream().map(Hit::id).toList();
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

    var foundInnsynskrav = 0;
    var removedInnsynskrav = 0;
    var innsynskravIterator =
        new ElasticsearchIterator<Void>(
            esClient,
            elasticsearchIndex,
            elasticsearchReindexGetBatchSize,
            getEsQuery("Innsynskrav", "Innsynskrav"),
            List.of("id", "created"),
            Void.class);
    while (innsynskravIterator.hasNext()) {
      var ids = innsynskravIterator.nextBatch().stream().map(Hit::id).toList();
      foundInnsynskrav += ids.size();
      var removeList = innsynskravRepository.findNonExistingIds(ids.toArray(new String[0]));
      removedInnsynskrav += removeList.size();
      deleteDocumentList(removeList);
      lastExtended = proxy.maybeExtendLock(lastExtended);
    }
    log.info(
        "Finished removal of stale Innsynskravs. Found {}, removed {}.",
        foundInnsynskrav,
        removedInnsynskrav);
  }

  /**
   * Get the Elasticsearch query for a specific entity.
   *
   * @param entityName
   */
  public Query getEsQuery(String... entityNames) {
    var fieldValueList = Arrays.stream(entityNames).map(FieldValue::of).toList();
    return Query.of(q -> q.terms(t -> t.field("type").terms(te -> te.value(fieldValueList))));
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
        StructuredArguments.raw("documents", gson.toJson(String.join(", ", idList) + "]")));

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

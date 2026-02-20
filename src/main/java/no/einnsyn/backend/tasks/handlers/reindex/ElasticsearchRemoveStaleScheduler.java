package no.einnsyn.backend.tasks.handlers.reindex;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.einnsyn.backend.common.indexable.IndexableRepository;
import no.einnsyn.backend.entities.innsynskrav.InnsynskravService;
import no.einnsyn.backend.entities.journalpost.JournalpostService;
import no.einnsyn.backend.entities.lagretsoek.LagretSoekService;
import no.einnsyn.backend.entities.moetemappe.MoetemappeService;
import no.einnsyn.backend.entities.moetesak.MoetesakService;
import no.einnsyn.backend.entities.saksmappe.SaksmappeService;
import no.einnsyn.backend.utils.ApplicationShutdownListenerService;
import no.einnsyn.backend.utils.ElasticsearchIterator;
import no.einnsyn.backend.utils.ParallelRunner;
import no.einnsyn.backend.utils.ShedlockExtenderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ElasticsearchRemoveStaleScheduler {

  private static final int LOCK_EXTEND_INTERVAL = 60 * 1000;

  private final ElasticsearchClient esClient;

  private final JournalpostService journalpostService;
  private final SaksmappeService saksmappeService;
  private final MoetemappeService moetemappeService;
  private final MoetesakService moetesakService;
  private final InnsynskravService innsynskravService;
  private final LagretSoekService lagretSoekService;

  private final ShedlockExtenderService shedlockExtenderService;
  private final ApplicationShutdownListenerService applicationShutdownListenerService;

  private final ParallelRunner parallelRunner;

  @Value("${application.elasticsearch.reindexer.getBatchSize:1000}")
  private int elasticsearchReindexGetBatchSize;

  @Value("${application.elasticsearch.index}")
  private String esIndex;

  @Value("${application.elasticsearch.percolatorIndex}")
  private String percolatorIndex;

  public ElasticsearchRemoveStaleScheduler(
      ElasticsearchClient esClient,
      JournalpostService journalpostService,
      SaksmappeService saksmappeService,
      MoetemappeService moetemappeService,
      MoetesakService moetesakService,
      InnsynskravService innsynskravService,
      LagretSoekService lagretSoekService,
      ShedlockExtenderService shedlockExtenderService,
      ApplicationShutdownListenerService applicationShutdownListenerService) {
    this.esClient = esClient;
    this.journalpostService = journalpostService;
    this.saksmappeService = saksmappeService;
    this.moetemappeService = moetemappeService;
    this.moetesakService = moetesakService;
    this.innsynskravService = innsynskravService;
    this.lagretSoekService = lagretSoekService;
    this.shedlockExtenderService = shedlockExtenderService;
    this.applicationShutdownListenerService = applicationShutdownListenerService;
    this.parallelRunner = new ParallelRunner(10);
  }

  private void removeForEntity(
      String entityName,
      List<String> sortBy,
      IndexableRepository<?> repository,
      String elasticsearchIndex) {
    var lastExtended = System.currentTimeMillis();
    var futures = ConcurrentHashMap.<CompletableFuture<Void>>newKeySet();
    log.info("Starting removal of stale {}.", entityName);

    var found = 0;
    var removed = new AtomicInteger(0);
    var iterator =
        new ElasticsearchIterator<Void>(
            esClient,
            elasticsearchIndex,
            elasticsearchReindexGetBatchSize,
            getEsQuery(entityName),
            sortBy,
            Void.class);

    while (iterator.hasNext()) {
      if (applicationShutdownListenerService.isShuttingDown()) {
        log.warn("Application is shutting down. Aborting removal of stale {}.", entityName);
        break;
      }

      var ids = iterator.nextBatch().stream().map(Hit::id).toList();
      found += ids.size();
      var future =
          parallelRunner.run(
              () -> {
                var removeList = repository.findNonExistingIds(ids.toArray(new String[0]));
                removed.addAndGet(removeList.size());
                deleteDocumentList(removeList, elasticsearchIndex, entityName);
              });

      futures.add(future);
      future.whenComplete(
          (_, exception) -> {
            futures.remove(future);
            if (exception != null) {
              log.error("Failed to remove documents from Elasticsearch: {}", ids, exception);
            }
          });

      lastExtended = shedlockExtenderService.maybeExtendLock(lastExtended, LOCK_EXTEND_INTERVAL);
    }

    try {
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
      log.info("Finished removal of {}/{} {} documents.", removed.get(), found, entityName);
    } catch (Exception e) {
      var cause = e.getCause() != null ? e.getCause() : e;
      log.atError()
          .setCause(cause)
          .setMessage("One or more removal tasks failed for {}. Error: {}")
          .addArgument(entityName)
          .addArgument(cause.getMessage())
          .log();
      log.info(
          "Attempted removal of {}/{} {} documents before failure.",
          removed.get(),
          found,
          entityName);
    }
  }

  private void removeWithoutType(String elasticsearchIndex) {
    var lastExtended = System.currentTimeMillis();
    var futures = ConcurrentHashMap.<CompletableFuture<Void>>newKeySet();
    log.info(
        "Starting removal of documents without type in Elasticsearch index {}.",
        elasticsearchIndex);

    var found = 0;
    var removed = new AtomicInteger(0);
    var iterator =
        new ElasticsearchIterator<Void>(
            esClient,
            elasticsearchIndex,
            elasticsearchReindexGetBatchSize,
            getEsQueryWithoutType(),
            List.of("id", "_doc"),
            Void.class);

    while (iterator.hasNext()) {
      if (applicationShutdownListenerService.isShuttingDown()) {
        log.warn(
            "Application is shutting down. Aborting cleanup of documents without type in index {}.",
            elasticsearchIndex);
        break;
      }

      var ids = iterator.nextBatch().stream().map(Hit::id).toList();
      found += ids.size();
      var future =
          parallelRunner.run(
              () -> {
                removed.addAndGet(ids.size());
                deleteDocumentList(ids, elasticsearchIndex, "MissingType");
              });

      futures.add(future);
      future.whenComplete(
          (_, exception) -> {
            futures.remove(future);
            if (exception != null) {
              log.error(
                  "Failed to clean up documents without type in Elasticsearch: {}", ids, exception);
            }
          });

      lastExtended = shedlockExtenderService.maybeExtendLock(lastExtended, LOCK_EXTEND_INTERVAL);
    }

    try {
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
      log.info(
          "Finished removal of {}/{} documents without type in index {}.",
          removed.get(),
          found,
          elasticsearchIndex);
    } catch (Exception e) {
      var cause = e.getCause() != null ? e.getCause() : e;
      log.atError()
          .setCause(cause)
          .setMessage("One or more cleanup tasks failed for documents without type. Error: {}")
          .addArgument(cause.getMessage())
          .log();
      log.info(
          "Attempted removal of {}/{} documents without type in index {} before failure.",
          removed.get(),
          found,
          elasticsearchIndex);
    }
  }

  /**
   * Remove documents from ES that does not exist in the database. This will loop through all items
   * in Elastic in batches, and query Postgres for the ids in each batch that are *not* in the
   * database. These will then be deleted from Elastic.
   */
  @Scheduled(cron = "${application.elasticsearch.reindexer.cron.removeStale:0 0 0 * * 6}")
  @SchedulerLock(name = "RemoveStaleEs", lockAtLeastFor = "1m")
  public void removeStaleDocuments() {

    removeForEntity(
        "Journalpost",
        List.of("publisertDato", "oppdatertDato", "standardDato", "saksnummerGenerert"),
        journalpostService.getRepository(),
        journalpostService.getElasticsearchIndex());

    removeForEntity(
        "Saksmappe",
        List.of("publisertDato", "oppdatertDato", "standardDato", "saksnummerGenerert"),
        saksmappeService.getRepository(),
        saksmappeService.getElasticsearchIndex());

    removeForEntity(
        "Moetemappe",
        List.of("publisertDato", "oppdatertDato", "standardDato", "saksnummerGenerert"),
        moetemappeService.getRepository(),
        moetemappeService.getElasticsearchIndex());

    removeForEntity(
        "Møtesaksregistrering",
        List.of("publisertDato", "oppdatertDato", "standardDato", "saksnummerGenerert"),
        moetesakService.getRepository(),
        moetesakService.getElasticsearchIndex());

    // Special case for Møtesaksregistrering, since we need to check for
    // KommerTilBehandlingMøtesaksregistrering
    removeForEntity(
        "KommerTilBehandlingMøtesaksregistrering",
        List.of("publisertDato", "oppdatertDato", "standardDato", "saksnummerGenerert"),
        moetesakService.getRepository(),
        moetesakService.getElasticsearchIndex());

    removeForEntity(
        "Innsynskrav",
        List.of("id", "created"),
        innsynskravService.getRepository(),
        innsynskravService.getElasticsearchIndex());

    removeForEntity(
        "LagretSoek",
        List.of("id"),
        lagretSoekService.getRepository(),
        lagretSoekService.getElasticsearchIndex());

    removeWithoutType(esIndex);
    removeWithoutType(percolatorIndex);
  }

  /**
   * Get the Elasticsearch query for a specific entity.
   *
   * @param entityNames the entity names to filter by
   * @return the Elasticsearch query
   */
  public Query getEsQuery(String... entityNames) {
    var fieldValueList = Arrays.stream(entityNames).map(FieldValue::of).toList();
    return Query.of(q -> q.terms(t -> t.field("type").terms(te -> te.value(fieldValueList))));
  }

  /**
   * Get query for documents where field `type` does not exist.
   *
   * @return the Elasticsearch query
   */
  public Query getEsQueryWithoutType() {
    return Query.of(q -> q.bool(b -> b.mustNot(m -> m.exists(e -> e.field("type")))));
  }

  /**
   * Helper method to delete a list of documents from Elasticsearch.
   *
   * @param idList the list of document IDs to delete
   * @param elasticsearchIndex the Elasticsearch index
   * @param entityName the name of the entity type for the documents being deleted
   */
  void deleteDocumentList(List<String> idList, String elasticsearchIndex, String entityName) {
    if (idList.isEmpty()) {
      return;
    }

    log.atInfo()
        .setMessage("Removing {} {} documents")
        .addArgument(idList.size())
        .addArgument(entityName)
        .addKeyValue("documents", String.join(", ", idList))
        .log();

    var br = new BulkRequest.Builder();
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

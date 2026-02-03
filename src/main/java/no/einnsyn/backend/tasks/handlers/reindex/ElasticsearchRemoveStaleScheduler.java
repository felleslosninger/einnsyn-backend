package no.einnsyn.backend.tasks.handlers.reindex;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import net.logstash.logback.argument.StructuredArguments;
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
  private final Gson gson;

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

  public ElasticsearchRemoveStaleScheduler(
      ElasticsearchClient esClient,
      Gson gson,
      JournalpostService journalpostService,
      SaksmappeService saksmappeService,
      MoetemappeService moetemappeService,
      MoetesakService moetesakService,
      InnsynskravService innsynskravService,
      LagretSoekService lagretSoekService,
      ShedlockExtenderService shedlockExtenderService,
      ApplicationShutdownListenerService applicationShutdownListenerService) {
    this.esClient = esClient;
    this.gson = gson;
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
                deleteDocumentList(removeList, elasticsearchIndex);
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
      log.info("Finished removal of {} {} documents.", found, entityName);
    } catch (Exception e) {
      log.atError()
          .setCause(e.getCause())
          .setMessage("One or more removal tasks failed for {}. Error: {}")
          .addArgument(entityName)
          .addArgument(e.getCause().getMessage())
          .log();
      log.info("Attempted removal of {} {} documents before failure.", found, entityName);
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
        List.of("publisertDato", "opprettetDato", "standardDato", "saksnummerGenerert"),
        journalpostService.getRepository(),
        journalpostService.getElasticsearchIndex());

    removeForEntity(
        "Saksmappe",
        List.of("publisertDato", "opprettetDato", "standardDato", "saksnummerGenerert"),
        saksmappeService.getRepository(),
        saksmappeService.getElasticsearchIndex());

    removeForEntity(
        "Moetemappe",
        List.of("publisertDato", "opprettetDato", "standardDato", "saksnummerGenerert"),
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
        List.of("publisertDato", "opprettetDato", "standardDato", "saksnummerGenerert"),
        moetesakService.getRepository(),
        moetesakService.getElasticsearchIndex());

    removeForEntity(
        "Innsynskrav",
        List.of("id", "created"),
        innsynskravService.getRepository(),
        innsynskravService.getElasticsearchIndex());

    // TODO: Enable removal when migration is complete
    // removeForEntity(
    //     "LagretSoek",
    //     List.of("id"),
    //     lagretSoekService.getRepository(),
    //     lagretSoekService.getElasticsearchIndex());
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
   * Helper method to delete a list of documents from Elasticsearch.
   *
   * @param idList the list of document IDs to delete
   * @param elasticsearchIndex the Elasticsearch index
   */
  void deleteDocumentList(List<String> idList, String elasticsearchIndex) {
    var br = new BulkRequest.Builder();

    log.debug("Removing {} documents", idList.size());

    if (idList.isEmpty()) {
      return;
    }

    log.info(
        "Removing {} documents",
        idList.size(),
        StructuredArguments.raw("documents", gson.toJson(String.join(", ", idList))));

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

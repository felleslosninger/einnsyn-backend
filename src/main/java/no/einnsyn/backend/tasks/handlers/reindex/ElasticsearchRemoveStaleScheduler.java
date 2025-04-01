package no.einnsyn.backend.tasks.handlers.reindex;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.google.gson.Gson;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockExtender;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import net.logstash.logback.argument.StructuredArguments;
import no.einnsyn.backend.common.indexable.IndexableRepository;
import no.einnsyn.backend.entities.innsynskrav.InnsynskravRepository;
import no.einnsyn.backend.entities.journalpost.JournalpostRepository;
import no.einnsyn.backend.entities.moetemappe.MoetemappeRepository;
import no.einnsyn.backend.entities.moetesak.MoetesakRepository;
import no.einnsyn.backend.entities.saksmappe.SaksmappeRepository;
import no.einnsyn.backend.utils.ElasticsearchIterator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ElasticsearchRemoveStaleScheduler {

  private static final int LOCK_EXTEND_INTERVAL = 60 * 1000;

  private final ElasticsearchClient esClient;
  private final Gson gson;

  private final JournalpostRepository journalpostRepository;
  private final SaksmappeRepository saksmappeRepository;
  private final MoetemappeRepository moetemappeRepository;
  private final MoetesakRepository moetesakRepository;
  private final InnsynskravRepository innsynskravRepository;

  @Value("${application.elasticsearch.reindexer.getBatchSize:1000}")
  private int elasticsearchReindexGetBatchSize;

  @Value("${application.elasticsearch.index}")
  private String elasticsearchIndex;

  public ElasticsearchRemoveStaleScheduler(
      ElasticsearchClient esClient,
      Gson gson,
      JournalpostRepository journalpostRepository,
      SaksmappeRepository saksmappeRepository,
      MoetemappeRepository moetemappeRepository,
      MoetesakRepository moetesakRepository,
      InnsynskravRepository innsynskravRepository) {
    this.esClient = esClient;
    this.gson = gson;
    this.journalpostRepository = journalpostRepository;
    this.saksmappeRepository = saksmappeRepository;
    this.moetemappeRepository = moetemappeRepository;
    this.moetesakRepository = moetesakRepository;
    this.innsynskravRepository = innsynskravRepository;
  }

  // Extend lock every 5 minutes
  public long maybeExtendLock(long lastExtended) {
    var now = System.currentTimeMillis();
    if (now - lastExtended > LOCK_EXTEND_INTERVAL) {
      LockExtender.extendActiveLock(
          Duration.of(LOCK_EXTEND_INTERVAL, ChronoUnit.MILLIS),
          Duration.of(LOCK_EXTEND_INTERVAL, ChronoUnit.MILLIS));
      return now;
    }
    return lastExtended;
  }

  private void removeForEntity(
      String entityName, List<String> sortBy, IndexableRepository<?> repository) {
    var lastExtended = System.currentTimeMillis();
    log.info("Starting removal of stale documents for {}", entityName);

    var found = 0;
    var removed = 0;
    var iterator =
        new ElasticsearchIterator<Void>(
            esClient,
            elasticsearchIndex,
            elasticsearchReindexGetBatchSize,
            getEsQuery(entityName),
            sortBy,
            Void.class);
    while (iterator.hasNext()) {
      var ids = iterator.nextBatch().stream().map(Hit::id).toList();
      found += ids.size();
      var removeList = repository.findNonExistingIds(ids.toArray(new String[0]));
      removed += removeList.size();
      deleteDocumentList(removeList);
      lastExtended = maybeExtendLock(lastExtended);
    }
    log.info("Finished removal of stale {}. Found {}, removed {}.", entityName, found, removed);
  }

  /**
   * Remove documents from ES that does not exist in the database. This will loop through all items
   * in Elastic in batches, and query Postgres for the ids in each batch that are *not* in the
   * database. These will then be deleted from Elastic.
   */
  @Scheduled(cron = "${application.elasticsearch.reindexer.cron.removeStale:0 0 0 * * 6}")
  @SchedulerLock(name = "RemoveStaleEs", lockAtLeastFor = "10m", lockAtMostFor = "10m")
  public void removeStaleDocuments() {
    log.info("Starting removal of stale documents");

    removeForEntity(
        "Journalpost",
        List.of("publisertDato", "opprettetDato", "standardDato", "saksnummerGenerert"),
        journalpostRepository);

    removeForEntity(
        "Saksmappe",
        List.of("publisertDato", "opprettetDato", "standardDato", "saksnummerGenerert"),
        saksmappeRepository);

    removeForEntity(
        "Moetemappe",
        List.of("publisertDato", "opprettetDato", "standardDato", "saksnummerGenerert"),
        moetemappeRepository);

    removeForEntity(
        "Møtesaksregistrering",
        List.of("publisertDato", "oppdatertDato", "standardDato", "saksnummerGenerert"),
        moetesakRepository);

    // Special case for Møtesaksregistrering, since we need to check for
    // KommerTilBehandlingMøtesaksregistrering
    removeForEntity(
        "KommerTilBehandlingMøtesaksregistrering",
        List.of("publisertDato", "opprettetDato", "standardDato", "saksnummerGenerert"),
        moetesakRepository);

    removeForEntity("Innsynskrav", List.of("id", "created"), innsynskravRepository);

    // TODO: LagretSak, when old entries are converted. (currently we have both old and new IDs)
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

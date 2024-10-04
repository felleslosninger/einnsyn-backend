package no.einnsyn.apiv3.tasks.handlers.subscription;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.PercolateQuery;
import co.elastic.clients.json.JsonData;
import java.util.List;
import no.einnsyn.apiv3.entities.base.models.BaseES;
import no.einnsyn.apiv3.entities.lagretsak.LagretSakRepository;
import no.einnsyn.apiv3.entities.lagretsoek.LagretSoekRepository;
import no.einnsyn.apiv3.entities.mappe.models.MappeES;
import no.einnsyn.apiv3.tasks.events.IndexEvent;
import no.einnsyn.apiv3.utils.ElasticsearchIterator;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionMatcher {

  private LagretSakRepository lagretSakRepository;
  private LagretSoekRepository lagretSoekRepository;
  private ElasticsearchClient esClient;

  public SubscriptionMatcher(
      LagretSakRepository lagretSakRepository,
      LagretSoekRepository lagretSoekRepository,
      ElasticsearchClient esClient) {
    this.lagretSakRepository = lagretSakRepository;
    this.lagretSoekRepository = lagretSoekRepository;
    this.esClient = esClient;
  }

  @Async
  @EventListener
  public void handleIndex(IndexEvent event) {
    var document = event.getDocument();

    // Check if arkivskaperTransitive is hidden

    if (document instanceof MappeES mappeDocument) {
      handleSak(mappeDocument);
    }

    handleSearch(document);
  }

  /**
   * Match MappeES documents against lagretSak
   *
   * @param mappeDocument
   */
  private void handleSak(MappeES mappeDocument) {
    // Update lagretSak where Saksmappe or Moetemappe matches
    lagretSakRepository.addHit(mappeDocument.getId());
  }

  /**
   * Match BaseES documents against percolator queries
   *
   * @param document
   */
  private void handleSearch(BaseES document) {
    var percolateQuery =
        PercolateQuery.of(b -> b.field("query").document(JsonData.of(document)))._toQuery();
    var iterator =
        new ElasticsearchIterator<Void>(
            esClient, "percolate_queries", 1000, percolateQuery, List.of("_doc"), Void.class);

    // Create new LagretSoekTreff
    while (iterator.hasNext()) {
      var hit = iterator.next();
      lagretSoekRepository.addHitByLegacyId(hit.id());
    }
  }
}

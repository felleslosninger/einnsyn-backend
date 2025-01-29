package no.einnsyn.backend.tasks.handlers.subscription;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.google.gson.Gson;
import java.io.StringReader;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.common.indexable.Indexable;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseES;
import no.einnsyn.backend.entities.base.models.BaseES;
import no.einnsyn.backend.entities.enhet.EnhetService;
import no.einnsyn.backend.entities.journalpost.JournalpostService;
import no.einnsyn.backend.entities.journalpost.models.JournalpostES;
import no.einnsyn.backend.entities.lagretsak.LagretSakRepository;
import no.einnsyn.backend.entities.lagretsoek.LagretSoekService;
import no.einnsyn.backend.entities.mappe.models.MappeES;
import no.einnsyn.backend.entities.moetemappe.MoetemappeService;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeES;
import no.einnsyn.backend.entities.moetesak.MoetesakService;
import no.einnsyn.backend.entities.moetesak.models.MoetesakES;
import no.einnsyn.backend.entities.saksmappe.SaksmappeService;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeES;
import no.einnsyn.backend.tasks.events.IndexEvent;
import no.einnsyn.backend.utils.ElasticsearchIterator;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SubscriptionMatcher {

  private final EnhetService enhetService;
  private final SaksmappeService saksmappeService;
  private final JournalpostService journalpostService;
  private final MoetemappeService moetemappeService;
  private final MoetesakService moetesakService;
  private final LagretSakRepository lagretSakRepository;
  private final LagretSoekService lagretSoekService;
  private final ElasticsearchClient esClient;
  private final Gson gson;

  public SubscriptionMatcher(
      EnhetService enhetService,
      SaksmappeService saksmappeService,
      JournalpostService journalpostService,
      MoetemappeService moetemappeService,
      MoetesakService moetesakService,
      LagretSakRepository lagretSakRepository,
      LagretSoekService lagretSoekService,
      ElasticsearchClient esClient,
      Gson gson) {
    this.enhetService = enhetService;
    this.saksmappeService = saksmappeService;
    this.journalpostService = journalpostService;
    this.moetemappeService = moetemappeService;
    this.moetesakService = moetesakService;
    this.lagretSakRepository = lagretSakRepository;
    this.lagretSoekService = lagretSoekService;
    this.esClient = esClient;
    this.gson = gson;
  }

  @Async("requestSideEffectExecutor")
  @EventListener
  public void handleIndex(IndexEvent event) {
    var document = event.getDocument();

    if (document instanceof ArkivBaseES arkivBaseDocument) {

      // TODO: Uncomment this when PR with ArkivBaseES.adminstrativEnhet is merged
      // if (enhetService.isSkjult(arkivBaseDocument.getAdministrativEnhet())) {
      //   return;
      // }

      if (document instanceof MappeES mappeDocument) {
        handleSak(mappeDocument);
      }

      // Match saved searches only
      if (event.isInsert() || turnedAccessible(document)) {
        handleSearch(document);
      }
    }
  }

  /**
   * Match MappeES documents against lagretSak
   *
   * @param mappeDocument
   */
  private void handleSak(MappeES mappeDocument) {
    // Update lagretSak where Saksmappe or Moetemappe matches
    if (mappeDocument instanceof SaksmappeES) {
      lagretSakRepository.addHitBySaksmappe(mappeDocument.getId());
    } else if (mappeDocument instanceof MoetemappeES) {
      lagretSakRepository.addHitByMoetemappe(mappeDocument.getId());
    }
  }

  /**
   * Match BaseES documents against percolator queries
   *
   * @param document
   */
  private void handleSearch(BaseES document) {

    // Filter by "søk" type (the old API also saves percolate searches for *mappe)
    var termQuery = Query.of(q -> q.term(t -> t.field("abonnement_type").value("søk")));

    // Percolate query
    var documentString = gson.toJson(document);
    var documentJsonData = JsonData.from(new StringReader(documentString));
    var percolateQuery =
        Query.of(q -> q.percolate(p -> p.field("query").document(documentJsonData)));

    // Combine queries
    var query = Query.of(q -> q.bool(b -> b.must(percolateQuery, termQuery)));

    var iterator =
        new ElasticsearchIterator<Void>(
            esClient, "percolator_queries", 1000, query, List.of("_doc"), Void.class);

    // Create new LagretSoekTreff for each hit
    while (iterator.hasNext()) {
      var hit = iterator.next();
      var uuid = UUID.fromString(hit.id());
      lagretSoekService.addHit(document, uuid);
    }
  }

  /**
   * If document's "accessibleAfter" property is after "lastIndexed", this reindex will make it
   * accessible, and subscribers should be notified.
   *
   * @param document
   * @return
   */
  private boolean turnedAccessible(BaseES document) {
    var id = document.getId();
    var object =
        switch (document) {
          case SaksmappeES saksmappe -> saksmappeService.findById(id);
          case JournalpostES journalpost -> journalpostService.findById(id);
          case MoetemappeES moetemappe -> moetemappeService.findById(id);
          case MoetesakES moetesak -> moetesakService.findById(id);
          default -> null;
        };
    if (object != null
        && object.getAccessibleAfter().isAfter(((Indexable) document).getLastIndexed())) {
      return true;
    }
    return false;
  }
}

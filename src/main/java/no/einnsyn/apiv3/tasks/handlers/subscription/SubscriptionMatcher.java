package no.einnsyn.apiv3.tasks.handlers.subscription;

import no.einnsyn.apiv3.common.indexable.Indexable;
import no.einnsyn.apiv3.entities.mappe.models.MappeES;
import no.einnsyn.apiv3.tasks.events.IndexEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionMatcher {

  @Async
  @EventListener
  public void handleIndex(IndexEvent event) {
    // Check if arkivskaperTransitive is hidden

    var document = event.getDocument();
    if (document instanceof MappeES mappeDocument) {
      handleMappe(mappeDocument);
    }

    // Percolate
    // SearchRequest percolateSearchRequest = getSearchRequest(document, null, pointInTime);

    // try {
    //   // return getClient().search(percolateSearchRequest, PercolatorQuery.class);
    // } catch (IOException e) {
    //   throw new RuntimeException(e);
    // }
  }

  private void handleMappe(MappeES mappeDocument) {
    // Find lagretSak where mappe matches
  }

  private void handleSearch(Indexable object) {
    // var percolator =
    //     PercolateQuery.of(
    //             b ->
    //                 b.document(JsonData.from(new StringReader(document)))
    //                     .index("percolator_queries")
    //                     .field("query"))
    //         ._toQuery();
  }
}

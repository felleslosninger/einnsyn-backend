package no.einnsyn.backend.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ElasticsearchIteratorTest {

  private final ElasticsearchClient esClient = mock(ElasticsearchClient.class);

  private ElasticsearchIterator<Void> iterator() {
    return new ElasticsearchIterator<>(
        esClient, "index", 2, Query.of(q -> q.matchAll(m -> m)), List.of("id"), Void.class);
  }

  private static SearchResponse<Void> response(String... ids) {
    var hits =
        Arrays.stream(ids)
            .map(id -> Hit.<Void>of(h -> h.index("index").id(id).sort(List.of(FieldValue.of(id)))))
            .toList();
    return SearchResponse.of(
        r ->
            r.took(1)
                .timedOut(false)
                .shards(s -> s.total(1).successful(1).failed(0))
                .hits(h -> h.hits(hits)));
  }

  /** A failed fetch of the following batch must not discard the batch the caller is handed. */
  @Test
  void nextBatchDoesNotFetchTheFollowingBatch() throws Exception {
    when(esClient.search(any(SearchRequest.class), eq(Void.class)))
        .thenReturn(response("a", "b"))
        .thenThrow(new IOException("Elasticsearch is unavailable"));
    var iterator = iterator();

    assertTrue(iterator.hasNext());
    assertEquals(List.of("a", "b"), iterator.nextBatch().stream().map(Hit::id).toList());
    verify(esClient, times(1)).search(any(SearchRequest.class), eq(Void.class));

    assertThrows(RuntimeException.class, iterator::hasNext);
  }

  @Test
  void nextBatchPagesThroughAllHits() throws Exception {
    when(esClient.search(any(SearchRequest.class), eq(Void.class)))
        .thenReturn(response("a", "b"))
        .thenReturn(response("c"))
        .thenReturn(response());
    var iterator = iterator();

    var batches = new ArrayList<List<String>>();
    while (iterator.hasNext()) {
      batches.add(iterator.nextBatch().stream().map(Hit::id).toList());
    }

    assertEquals(List.of(List.of("a", "b"), List.of("c")), batches);
    var requests = ArgumentCaptor.forClass(SearchRequest.class);
    verify(esClient, times(3)).search(requests.capture(), eq(Void.class));
    assertTrue(requests.getAllValues().get(0).searchAfter().isEmpty());
    assertEquals("b", requests.getAllValues().get(1).searchAfter().getFirst().stringValue());
    assertEquals("c", requests.getAllValues().get(2).searchAfter().getFirst().stringValue());
  }
}

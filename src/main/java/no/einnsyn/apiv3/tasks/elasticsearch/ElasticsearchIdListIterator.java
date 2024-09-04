package no.einnsyn.apiv3.tasks.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class ElasticsearchIdListIterator implements Iterator<List<String>> {

  private final ElasticsearchClient client;
  private final String indexName;
  private final String entityName;
  private final Integer batchSize;
  private List<String> currentBatch;
  private List<String> nextBatch;

  public ElasticsearchIdListIterator(
      ElasticsearchClient client, String indexName, String entityName, Integer batchSize) {
    this.client = client;
    this.indexName = indexName;
    this.entityName = entityName;
    this.batchSize = batchSize;
    this.currentBatch = fetchNextBatch(null);
  }

  @Override
  public boolean hasNext() {
    if (nextBatch == null) {
      nextBatch = fetchNextBatch(currentBatch.get(currentBatch.size() - 1));
    }
    return !currentBatch.isEmpty() || !nextBatch.isEmpty();
  }

  @Override
  public List<String> next() {
    currentBatch = nextBatch;
    nextBatch = null;
    if (currentBatch == null || currentBatch.isEmpty()) {
      throw new NoSuchElementException("No more elements in the iterator");
    }
    return currentBatch;
  }

  private List<String> fetchNextBatch(String searchAfter) {
    // Create a search request
    SearchRequest.Builder requestBuilder = new SearchRequest.Builder();
    requestBuilder.index(indexName);
    requestBuilder.query(q -> q.term(t -> t.field("type").value(entityName)));
    requestBuilder.sort(SortOptions.of(so -> so.field(f -> f.field("_id").order(SortOrder.Asc))));
    requestBuilder.source(s -> s.fetch(false));
    requestBuilder.size(batchSize);

    if (searchAfter != null && !searchAfter.isEmpty()) {
      requestBuilder.searchAfter(searchAfter);
    }

    try {
      SearchResponse<Void> searchResponse = client.search(requestBuilder.build(), Void.class);
      return searchResponse.hits().hits().stream().map(h -> h.id()).toList();
    } catch (Exception e) {
      throw new RuntimeException("Failed to fetch next batch", e);
    }
  }
}

package no.einnsyn.apiv3.tasks.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ElasticsearchIdListIterator implements Iterator<List<String>> {

  private final ElasticsearchClient client;
  private final String indexName;
  private final String entityName;
  private final Integer batchSize;
  private List<SearchRecord> currentBatch;
  private List<SearchRecord> nextBatch;

  public ElasticsearchIdListIterator(
      ElasticsearchClient client, String indexName, String entityName, Integer batchSize) {
    this.client = client;
    this.indexName = indexName;
    this.entityName = entityName;
    this.batchSize = batchSize;
  }

  @Override
  public boolean hasNext() {
    if (nextBatch == null) {
      if (currentBatch == null) {
        nextBatch = fetchNextBatch(null);
      } else {
        nextBatch = fetchNextBatch(currentBatch.get(currentBatch.size() - 1).getSort());
      }
    }
    return !nextBatch.isEmpty();
  }

  @Override
  public List<String> next() {
    if (!hasNext()) {
      throw new NoSuchElementException("No more elements in the iterator");
    }
    currentBatch = nextBatch;
    nextBatch = null;
    return currentBatch.stream().map(SearchRecord::getId).toList();
  }

  private List<SearchRecord> fetchNextBatch(List<FieldValue> searchAfter) {
    // Create a search request
    var requestBuilder = new SearchRequest.Builder();
    requestBuilder.index(indexName);
    requestBuilder.query(
        q ->
            q.terms(
                t -> t.field("type").terms(te -> te.value(List.of(FieldValue.of(entityName))))));
    requestBuilder.sort(
        SortOptions.of(so -> so.field(f -> f.field("standardDato").order(SortOrder.Asc))));
    requestBuilder.sort(
        SortOptions.of(so -> so.field(f -> f.field("opprettetDato").order(SortOrder.Asc))));
    requestBuilder.source(s -> s.fetch(false));
    requestBuilder.size(batchSize);

    if (searchAfter != null && !searchAfter.isEmpty()) {
      requestBuilder.searchAfter(searchAfter);
    }

    try {
      var searchRequest = requestBuilder.build();
      var searchResponse = client.search(searchRequest, Void.class);
      return searchResponse.hits().hits().stream()
          .map(
              h -> {
                return new SearchRecord(h.id(), h.sort());
              })
          .toList();
    } catch (Exception e) {
      throw new RuntimeException("Failed to fetch next batch: " + e.getMessage(), e);
    }
  }

  private class SearchRecord {
    private final String id;
    private final List<FieldValue> sort;

    public SearchRecord(String id, List<FieldValue> sort) {
      this.id = id;
      this.sort = sort;
    }

    public String getId() {
      return id;
    }

    public List<FieldValue> getSort() {
      return sort;
    }
  }
}

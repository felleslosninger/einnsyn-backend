package no.einnsyn.apiv3.utils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;

/** An iterator that iterates through all documents in the ES database. */
@Slf4j
public class ElasticsearchIterator<T> implements Iterator<List<Hit<T>>> {

  private final ElasticsearchClient esClient;
  private final String indexName;
  private final Integer batchSize;
  private final Query esQuery;
  private final List<String> sortBy;
  private final Class<T> clazz;
  private List<Hit<T>> currentBatch;
  private List<Hit<T>> nextBatch;

  public ElasticsearchIterator(
      ElasticsearchClient esClient,
      String indexName,
      int batchSize,
      Query esQuery,
      List<String> sortBy,
      Class<T> clazz) {
    this.esClient = esClient;
    this.indexName = indexName;
    this.batchSize = batchSize;
    this.esQuery = esQuery;
    this.sortBy = sortBy;
    this.clazz = clazz;
  }

  @Override
  public boolean hasNext() {
    if (nextBatch == null) {
      if (currentBatch == null) {
        nextBatch = fetchNextBatch(null);
      } else {
        nextBatch = fetchNextBatch(currentBatch.getLast());
      }
    }
    return !nextBatch.isEmpty();
  }

  @Override
  public List<Hit<T>> next() {
    if (!hasNext()) {
      throw new NoSuchElementException("No more elements in the iterator");
    }
    currentBatch = nextBatch;
    nextBatch = null;
    return currentBatch;
  }

  /**
   * Fetches the next batch of documents from ES.
   *
   * @param searchAfter
   * @return
   */
  private List<Hit<T>> fetchNextBatch(Hit<T> searchAfter) {
    var requestBuilder = new SearchRequest.Builder();
    requestBuilder.index(indexName);
    requestBuilder.query(esQuery);
    requestBuilder.source(s -> s.fetch(false));
    requestBuilder.size(batchSize);

    for (var sort : sortBy) {
      requestBuilder.sort(SortOptions.of(so -> so.field(f -> f.field(sort).order(SortOrder.Asc))));
    }

    if (searchAfter != null && searchAfter.sort() != null) {
      requestBuilder.searchAfter(searchAfter.sort());
    }

    try {
      var searchRequest = requestBuilder.build();
      var searchResponse = esClient.search(searchRequest, clazz);
      return searchResponse.hits().hits();
    } catch (Exception e) {
      throw new RuntimeException("Failed to fetch next batch: " + e.getMessage(), e);
    }
  }
}

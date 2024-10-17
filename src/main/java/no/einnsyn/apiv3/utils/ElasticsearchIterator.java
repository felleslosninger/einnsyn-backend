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

/** An iterator that iterates through all documents for an ES query. */
@Slf4j
public class ElasticsearchIterator<T> implements Iterator<Hit<T>> {

  private final ElasticsearchClient esClient;
  private final String indexName;
  private final Integer batchSize;
  private final Query esQuery;
  private final List<String> sortBy;
  private final Class<T> clazz;
  private List<Hit<T>> batch;
  private Iterator<Hit<T>> batchIterator;

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
    // Initialize the first batch
    if (batchIterator == null) {
      batch = fetchNextBatch(null);
      batchIterator = batch.iterator();
    }

    // Fetch next batch if the current batch exists and is not empty
    if (!batchIterator.hasNext() && !batch.isEmpty()) {
      batch = fetchNextBatch(batch.getLast());
      batchIterator = batch.iterator();
    }

    return batchIterator.hasNext();
  }

  @Override
  public Hit<T> next() {
    if (!hasNext()) {
      throw new NoSuchElementException("No more elements in the iterator");
    }

    return batchIterator.next();
  }

  /**
   * Fetches the next batch of documents from ES.
   *
   * @return
   */
  public List<Hit<T>> nextBatch() {
    if (!hasNext()) {
      throw new NoSuchElementException("No more elements in the iterator");
    }

    var currentBatch = batch;
    batch = fetchNextBatch(batch.getLast());
    batchIterator = batch.iterator();

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
    requestBuilder.size(batchSize);
    requestBuilder.trackTotalHits(track -> track.enabled(false));

    for (var sort : sortBy) {
      requestBuilder.sort(SortOptions.of(so -> so.field(f -> f.field(sort).order(SortOrder.Asc))));
    }

    if (searchAfter != null && searchAfter.sort() != null) {
      requestBuilder.searchAfter(searchAfter.sort());
    }

    if (clazz.equals(Void.class)) {
      requestBuilder.source(s -> s.fetch(false));
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

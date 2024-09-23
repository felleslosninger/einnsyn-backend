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

/** An iterator that iterates through all documents in the ES database. */
@Slf4j
public class ElasticsearchIdListIterator implements Iterator<List<String>> {

  private final ElasticsearchClient client;
  private final String indexName;
  private final String entityName;
  private final Integer batchSize;
  private List<EsDocument> currentBatch;
  private List<EsDocument> nextBatch;

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
        nextBatch = fetchNextBatch(currentBatch.getLast().getSort());
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
    return currentBatch.stream().map(EsDocument::getId).toList();
  }

  /**
   * Fetches the next batch of documents from ES.
   *
   * @param searchAfter
   * @return
   */
  private List<EsDocument> fetchNextBatch(List<FieldValue> searchAfter) {
    var requestBuilder = new SearchRequest.Builder();
    requestBuilder.index(indexName);
    requestBuilder.query(
        q ->
            q.terms(
                t -> t.field("type").terms(te -> te.value(List.of(FieldValue.of(entityName))))));
    requestBuilder.sort(
        SortOptions.of(so -> so.field(f -> f.field("publisertDato").order(SortOrder.Asc))));
    requestBuilder.sort(
        SortOptions.of(so -> so.field(f -> f.field("opprettetDato").order(SortOrder.Asc))));
    requestBuilder.sort(
        SortOptions.of(so -> so.field(f -> f.field("standardDato").order(SortOrder.Asc))));
    requestBuilder.sort(
        SortOptions.of(so -> so.field(f -> f.field("saksnummerGenerert").order(SortOrder.Asc))));

    requestBuilder.source(s -> s.fetch(false));
    requestBuilder.size(batchSize);

    if (searchAfter != null && !searchAfter.isEmpty()) {
      requestBuilder.searchAfter(searchAfter);
    }

    try {
      var searchRequest = requestBuilder.build();
      var searchResponse = client.search(searchRequest, Void.class);
      return searchResponse.hits().hits().stream()
          .map(h -> new EsDocument(h.id(), h.sort()))
          .toList();
    } catch (Exception e) {
      throw new RuntimeException("Failed to fetch next batch: " + e.getMessage(), e);
    }
  }

  /**
   * Represents one document in ES. In addition to the ID, we also need the sort values to be able
   * to fetch the next batch.
   */
  private class EsDocument {
    private final String id;
    private final List<FieldValue> sort;

    public EsDocument(String id, List<FieldValue> sort) {
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

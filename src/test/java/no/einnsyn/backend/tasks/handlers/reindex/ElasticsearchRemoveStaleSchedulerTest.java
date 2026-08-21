package no.einnsyn.backend.tasks.handlers.reindex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.transport.ElasticsearchTransport;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ElasticsearchRemoveStaleSchedulerTest {

  private CapturingElasticsearchClient esClient;
  private ElasticsearchRemoveStaleScheduler scheduler;

  @BeforeEach
  void setUp() {
    esClient = new CapturingElasticsearchClient();
    scheduler =
        new ElasticsearchRemoveStaleScheduler(
            esClient, null, null, null, null, null, null, null, null);
  }

  @Test
  void testDeleteDocumentListIncludesRoutingWhenPresent() {
    scheduler.deleteDocumentList(
        List.of(new ElasticsearchRemoveStaleScheduler.HitWithRouting("doc-1", "parent-1")),
        "test-index",
        "Innsynskrav");

    assertEquals(1, esClient.bulkCallCount);
    var deleteOperation = esClient.lastBulkRequest.operations().getFirst().delete();
    assertEquals("test-index", deleteOperation.index());
    assertEquals("doc-1", deleteOperation.id());
    assertEquals("parent-1", deleteOperation.routing());
  }

  @Test
  void testDeleteDocumentListOmitsRoutingWhenAbsent() {
    scheduler.deleteDocumentList(
        List.of(new ElasticsearchRemoveStaleScheduler.HitWithRouting("doc-1", null)),
        "test-index",
        "LagretSoek");

    assertEquals(1, esClient.bulkCallCount);
    var deleteOperation = esClient.lastBulkRequest.operations().getFirst().delete();
    assertEquals("test-index", deleteOperation.index());
    assertEquals("doc-1", deleteOperation.id());
    assertNull(deleteOperation.routing());
  }

  @Test
  void testDeleteDocumentListSkipsEmptyLists() {
    scheduler.deleteDocumentList(List.of(), "test-index", "LagretSoek");

    assertEquals(0, esClient.bulkCallCount);
    assertNull(esClient.lastBulkRequest);
  }

  private static final class CapturingElasticsearchClient extends ElasticsearchClient {

    private BulkRequest lastBulkRequest;
    private int bulkCallCount;

    private CapturingElasticsearchClient() {
      super((ElasticsearchTransport) null);
    }

    @Override
    public BulkResponse bulk(BulkRequest request) throws IOException {
      lastBulkRequest = request;
      bulkCallCount++;
      return new BulkResponse.Builder().errors(false).items(List.of()).took(0).build();
    }
  }
}

package no.einnsyn.apiv3.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import no.einnsyn.apiv3.EinnsynLegacyElasticTestBase;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.apiv3.tasks.elasticsearch.ElasticsearchReindexScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {"application.elasticsearchReindexBatchSize=20"})
class ElasticsearchReindexSchedulerTest extends EinnsynLegacyElasticTestBase {

  @Autowired ElasticsearchReindexScheduler elasticsearchReindexScheduler;

  @Value("${application.elasticsearchReindexBatchSize:20}")
  private int batchSize;

  @BeforeEach
  void setUp() throws Exception {
    resetEs();
  }

  /**
   * Test that saksmappe that fail to index on creation are reindexed.
   *
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  @Test
  void testReindexMissingSaksmappe() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    // Add ten documents, fail to index one of them
    when(esClient.index(any(Function.class)))
        .thenThrow(new IOException("Failed to index document"))
        .thenReturn(mock(IndexResponse.class));
    for (var i = 0; i < 10; i++) {
      response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", getSaksmappeJSON());
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    // We should have tried to index 10 documents
    captureIndexedDocuments(10);
    resetEs();

    // Reindex all (one) unindexed documents
    elasticsearchReindexScheduler.updateOutdatedDocuments();
    captureIndexedDocuments(1);

    delete("/arkiv/" + arkivDTO.getId());
  }

  /**
   * Test that journalposts that fail to index on creation are reindexed.
   *
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  @Test
  void testReindexMissingJournalopst() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Should have indexed the saksmappe
    captureIndexedDocuments(1);
    resetEs();

    // Add ten documents. Fail to index twice (in case saksmappe is indexed before journalpost)
    Mockito.reset(esClient);
    when(esClient.index(any(Function.class)))
        .thenThrow(new IOException("Failed to index document"))
        .thenThrow(new IOException("Failed to index document"))
        .thenReturn(mock(IndexResponse.class));
    for (var i = 0; i < 10; i++) {
      response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", getJournalpostJSON());
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    // We should have tried to index 20 documents (saksmappe + journalpost * 10)
    captureIndexedDocuments(20);
    resetEs();

    // Reindex all (one) unindexed documents
    elasticsearchReindexScheduler.updateOutdatedDocuments();
    captureIndexedDocuments(1);

    delete("/arkiv/" + arkivDTO.getId());
  }

  /**
   * Test that moetemappes that fail to index on creation are reindexed.
   *
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  @Test
  void testReindexMissingMoetemappe() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    // getMoetemappeJSON() adds one moetesak, remove it
    var moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.remove("moetesak");

    // Add ten documents, fail to index one of them
    resetEs();
    when(esClient.index(any(Function.class)))
        .thenThrow(new IOException("Failed to index document"))
        .thenReturn(mock(IndexResponse.class));
    for (var i = 0; i < 10; i++) {
      response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", moetemappeJSON);
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    // We should have tried to index 10 documents
    captureIndexedDocuments(10);
    resetEs();

    // Reindex all (one) unindexed documents
    elasticsearchReindexScheduler.updateOutdatedDocuments();
    captureIndexedDocuments(1);

    delete("/arkiv/" + arkivDTO.getId());
  }

  /**
   * Test that moetesaks that fail to index on creation are reindexed.
   *
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  @Test
  void testReindexMissingMoetesak() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", getMoetemappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    // Should have indexed the moetemappe and one moetesak
    captureIndexedDocuments(2);
    resetEs();

    // Add ten documents. Fail to index twice (in case moetemappe is indexed before moetesak)
    Mockito.reset(esClient);
    when(esClient.index(any(Function.class)))
        .thenThrow(new IOException("Failed to index document"))
        .thenThrow(new IOException("Failed to index document"))
        .thenReturn(mock(IndexResponse.class));
    for (var i = 0; i < 10; i++) {
      response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", getMoetesakJSON());
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    // We should have tried to index 20 documents (moetemappe + moetesak * 10)
    captureIndexedDocuments(20);
    resetEs();

    // Reindex all (one) unindexed documents
    elasticsearchReindexScheduler.updateOutdatedDocuments();
    captureIndexedDocuments(1);

    delete("/arkiv/" + arkivDTO.getId());
  }

  /**
   * Test that saksmappe that doesn't exist in the database are removed from Elasticsearch.
   *
   * @throws Exception
   */
  @SuppressWarnings({"unchecked", "null"})
  @Test
  void testReindexRemoveSaksmappeFromES() throws Exception {
    var bulkResponse = mock(BulkResponse.class);
    when(esClient.bulk(any(BulkRequest.class))).thenReturn(bulkResponse);

    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    // Add ten saksmappes
    var saksmappeIdList = new ArrayList<String>();
    for (var i = 0; i < 10; i++) {
      response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", getSaksmappeJSON());
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
      saksmappeIdList.add(saksmappeDTO.getId());
    }

    // Add 4 batches from ES, the last one is empty
    var sr1 = mockEsResponse(batchSize, saksmappeIdList);
    var sr2 = mockEsResponse(batchSize, saksmappeIdList);
    var sr3 = mockEsResponse(batchSize, saksmappeIdList);
    var empty = mockEsResponse(0, new ArrayList<String>());

    // Return dummy lists for queries against saksmappe
    when(esClient.search(
            argThat(
                (SearchRequest req) -> req != null && req.query().toString().contains("Saksmappe")),
            any()))
        .thenReturn(sr1, sr2, sr3, empty);

    // Return empty set for queries against other entities
    when(esClient.search(
            argThat(
                (SearchRequest req) ->
                    req != null && !req.query().toString().contains("Saksmappe")),
            any()))
        .thenReturn(empty);

    // Remove documents that doesn't exist in the database
    elasticsearchReindexScheduler.removeStaleDocuments();

    // We should have deleted 30 documents in 3 batches
    var deletedDocuments = captureBulkDeletedDocuments(3, 30);
    for (var document : deletedDocuments) {
      assertFalse(saksmappeIdList.contains(document));
    }

    delete("/arkiv/" + arkivDTO.getId());
  }

  /**
   * Test that journalposts that doesn't exist in the database are removed from Elasticsearch.
   *
   * @throws Exception
   */
  @SuppressWarnings({"unchecked", "null"})
  @Test
  void testReindexRemoveJournalpostFromES() throws Exception {
    var bulkResponse = mock(BulkResponse.class);
    when(esClient.bulk(any(BulkRequest.class))).thenReturn(bulkResponse);

    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Add ten journalposts
    var journalpostIdList = new ArrayList<String>();
    for (var i = 0; i < 10; i++) {
      response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", getJournalpostJSON());
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      var journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
      journalpostIdList.add(journalpostDTO.getId());
    }

    // Add 4 batches from ES, the last one is empty
    var sr1 = mockEsResponse(batchSize, journalpostIdList);
    var sr2 = mockEsResponse(batchSize, journalpostIdList);
    var sr3 = mockEsResponse(batchSize, journalpostIdList);
    var empty = mockEsResponse(0, new ArrayList<String>());

    // Return dummy lists for queries against journalpost
    when(esClient.search(
            argThat(
                (SearchRequest req) ->
                    req != null && req.query().toString().contains("Journalpost")),
            any()))
        .thenReturn(sr1, sr2, sr3, empty);

    // Return empty set for queries against other entities
    when(esClient.search(
            argThat(
                (SearchRequest req) ->
                    req != null && !req.query().toString().contains("Journalpost")),
            any()))
        .thenReturn(empty);

    // Remove documents that doesn't exist in the database
    elasticsearchReindexScheduler.removeStaleDocuments();

    // We should have deleted 30 documents in 3 batches
    var deletedDocuments = captureBulkDeletedDocuments(3, 30);
    for (var document : deletedDocuments) {
      assertFalse(journalpostIdList.contains(document));
    }

    delete("/arkiv/" + arkivDTO.getId());
  }

  /**
   * Test that moetemappes that doesn't exist in the database are removed from Elasticsearch.
   *
   * @throws Exception
   */
  @SuppressWarnings({"unchecked", "null"})
  @Test
  void testReindexRemoveMoetemappeFromES() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    var bulkResponse = mock(BulkResponse.class);
    when(esClient.bulk(any(BulkRequest.class))).thenReturn(bulkResponse);

    // Add ten moetemappes
    var moetemappeIdList = new ArrayList<String>();
    for (var i = 0; i < 10; i++) {
      response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", getMoetemappeJSON());
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
      moetemappeIdList.add(moetemappeDTO.getId());
    }

    // Add 4 batches from ES, the last one is empty
    var sr1 = mockEsResponse(batchSize, moetemappeIdList);
    var sr2 = mockEsResponse(batchSize, moetemappeIdList);
    var sr3 = mockEsResponse(batchSize, moetemappeIdList);
    var empty = mockEsResponse(0, new ArrayList<String>());

    // Return dummy lists for queries against moetemappe
    when(esClient.search(
            argThat(
                (SearchRequest req) ->
                    req != null && req.query().toString().contains("Moetemappe")),
            any()))
        .thenReturn(sr1, sr2, sr3, empty);

    // Return empty set for queries against other entities
    when(esClient.search(
            argThat(
                (SearchRequest req) ->
                    req != null && !req.query().toString().contains("Moetemappe")),
            any()))
        .thenReturn(empty);

    // Remove documents that doesn't exist in the database
    elasticsearchReindexScheduler.removeStaleDocuments();

    // We should have deleted 30 documents in 3 batches
    var deletedDocuments = captureBulkDeletedDocuments(3, 30);
    for (var document : deletedDocuments) {
      assertFalse(moetemappeIdList.contains(document));
    }

    delete("/arkiv/" + arkivDTO.getId());
  }

  /**
   * Test that moetesaks that doesn't exist in the database are removed from Elasticsearch.
   *
   * @throws Exception
   */
  @SuppressWarnings({"unchecked", "null"})
  @Test
  void testReindexRemoveMoetesakFromES() throws Exception {
    var bulkResponse = mock(BulkResponse.class);
    when(esClient.bulk(any(BulkRequest.class))).thenReturn(bulkResponse);

    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", getMoetemappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    // Add ten moetesaks
    var moetesakIdList = new ArrayList<String>();
    for (var i = 0; i < 10; i++) {
      response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", getMoetesakJSON());
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      var moetesakDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);
      moetesakIdList.add(moetesakDTO.getId());
    }

    // Add 4 batches from ES, the last one is empty
    var sr1 = mockEsResponse(batchSize, moetesakIdList);
    var sr2 = mockEsResponse(batchSize, moetesakIdList);
    var sr3 = mockEsResponse(batchSize, moetesakIdList);
    var empty = mockEsResponse(0, new ArrayList<String>());

    // Return dummy lists for queries against moetesak
    when(esClient.search(
            argThat(
                (SearchRequest req) -> req != null && req.query().toString().contains("Moetesak")),
            any()))
        .thenReturn(sr1, sr2, sr3, empty);

    // Return empty set for queries against other entities
    when(esClient.search(
            argThat(
                (SearchRequest req) -> req != null && !req.query().toString().contains("Moetesak")),
            any()))
        .thenReturn(empty);

    // Remove documents that doesn't exist in the database
    elasticsearchReindexScheduler.removeStaleDocuments();

    // We should have deleted 30 documents in 3 batches
    var deletedDocuments = captureBulkDeletedDocuments(3, 30);
    for (var document : deletedDocuments) {
      assertFalse(moetesakIdList.contains(document));
    }

    delete("/arkiv/" + arkivDTO.getId());
  }

  /**
   * Helper function to create a mock Elasticsearch response with <size> hits, where <idList> is
   * included in the response.
   *
   * @param size
   * @param idList
   * @return
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private SearchResponse<Object> mockEsResponse(int size, List<String> idList) {
    var searchResponse = mock(SearchResponse.class);
    var hitsMetadata = mock(HitsMetadata.class);
    var hits = new ArrayList<Hit>();

    // Add <size> dummy hits
    for (var j = 0; j < size - idList.size(); j++) {
      var id =
          new Random()
              .ints(97, 123)
              .limit(8)
              .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
              .toString();
      var hit = mock(Hit.class);
      when(hit.id()).thenReturn("id_" + id);
      hits.add(hit);
    }

    // Add existing list
    for (var i = 0; i < idList.size() && i < size; i++) {
      var hit = mock(Hit.class);
      when(hit.id()).thenReturn(idList.get(i));
      hits.add(hit);
    }

    when(searchResponse.hits()).thenReturn(hitsMetadata);
    when(hitsMetadata.hits()).thenReturn(hits);
    return searchResponse;
  }
}

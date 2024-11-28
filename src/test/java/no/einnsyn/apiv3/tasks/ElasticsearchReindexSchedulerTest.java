package no.einnsyn.backend.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Function;
import no.einnsyn.backend.EinnsynLegacyElasticTestBase;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.innsynskrav.models.InnsynskravES;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestillingDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.backend.tasks.handlers.reindex.ElasticsearchReindexScheduler;
import no.einnsyn.backend.testutils.ElasticsearchMocks;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
      "application.elasticsearch.reindexer.getBatchSize=20",
      "application.elasticsearch.reindexer.indexBatchSize=20"
    })
@ActiveProfiles("test")
class ElasticsearchReindexSchedulerTest extends EinnsynLegacyElasticTestBase {

  @Autowired ElasticsearchReindexScheduler elasticsearchReindexScheduler;

  @Value("${application.elasticsearchReindexBatchSize:20}")
  private int batchSize;

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
    var indexResponseMock = getIndexResponseMock();
    when(esClient.index(any(Function.class)))
        .thenThrow(new IOException("Failed to index document"))
        .thenReturn(indexResponseMock);
    for (var i = 0; i < 10; i++) {
      response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", getSaksmappeJSON());
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    // We should have tried to index 10 documents
    captureIndexedDocuments(10);
    resetEsMock();

    // Reindex all (one) unindexed documents
    elasticsearchReindexScheduler.updateOutdatedDocuments();
    captureBulkIndexedDocuments(1, 1);
    resetEsMock();

    delete("/arkiv/" + arkivDTO.getId());
    captureDeletedDocuments(10);
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
    resetEsMock();

    // Add ten documents. Fail to index twice (in case saksmappe is indexed before journalpost)
    var indexResponseMock = getIndexResponseMock();
    when(esClient.index(any(Function.class)))
        .thenThrow(new IOException("Failed to index document"))
        .thenThrow(new IOException("Failed to index document"))
        .thenReturn(indexResponseMock);
    for (var i = 0; i < 10; i++) {
      response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", getJournalpostJSON());
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    // We should have tried to index 20 documents ((saksmappe + journalpost) * 10)
    captureIndexedDocuments(20);
    resetEsMock();

    // Reindex all (one) unindexed documents
    elasticsearchReindexScheduler.updateOutdatedDocuments();
    captureBulkIndexedDocuments(1, 1);
    resetEsMock();

    delete("/arkiv/" + arkivDTO.getId());
    captureDeletedDocuments(11); // Saksmappe + 10 journalposts
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
    var indexResponseMock = getIndexResponseMock();
    when(esClient.index(any(Function.class)))
        .thenThrow(new IOException("Failed to index document"))
        .thenReturn(indexResponseMock);
    for (var i = 0; i < 10; i++) {
      response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", moetemappeJSON);
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    // We should have tried to index 10 documents
    captureIndexedDocuments(10);
    resetEsMock();

    // Reindex all (one) unindexed documents
    elasticsearchReindexScheduler.updateOutdatedDocuments();
    captureBulkIndexedDocuments(1, 1);
    resetEsMock();

    delete("/arkiv/" + arkivDTO.getId());
    captureDeletedDocuments(10);
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
    resetEsMock();

    // Add ten documents. Fail to index twice (in case moetemappe is indexed before moetesak)
    var indexResponseMock = getIndexResponseMock();
    when(esClient.index(any(Function.class)))
        .thenThrow(new IOException("Failed to index document"))
        .thenThrow(new IOException("Failed to index document"))
        .thenReturn(indexResponseMock);
    for (var i = 0; i < 10; i++) {
      response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", getMoetesakJSON());
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    // We should have tried to index 20 documents (moetemappe + moetesak * 10)
    captureIndexedDocuments(20);
    resetEsMock();

    // Reindex all (one) unindexed documents
    elasticsearchReindexScheduler.updateOutdatedDocuments();
    captureBulkIndexedDocuments(1, 1);
    resetEsMock();

    delete("/arkiv/" + arkivDTO.getId());
    captureDeletedDocuments(12);
  }

  /**
   * Test that saksmappe that doesn't exist in the database are removed from Elasticsearch.
   *
   * @throws Exception
   */
  @SuppressWarnings({"unchecked", "null"})
  @Test
  void testReindexRemoveSaksmappeFromES() throws Exception {
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

    captureIndexedDocuments(10);
    resetEsMock();

    // Add 4 batches from ES, the last one is empty
    var sr1 = ElasticsearchMocks.searchResponse(batchSize, saksmappeIdList);
    var sr2 = ElasticsearchMocks.searchResponse(batchSize, saksmappeIdList);
    var sr3 = ElasticsearchMocks.searchResponse(batchSize, saksmappeIdList);
    var empty = ElasticsearchMocks.searchResponse(0, new ArrayList<String>());

    // Return dummy lists for queries against saksmappe
    when(esClient.search(
            argThat(
                (SearchRequest req) ->
                    req != null
                        && req.index().contains("test")
                        && req.query().toString().contains("Saksmappe")),
            any()))
        .thenReturn(sr1, sr2, sr3, empty);

    // Return empty set for queries against other entities
    when(esClient.search(
            argThat(
                (SearchRequest req) ->
                    req != null
                        && req.index().contains("test")
                        && !req.query().toString().contains("Saksmappe")),
            any()))
        .thenReturn(empty);

    // Remove documents that doesn't exist in the database
    elasticsearchReindexScheduler.removeStaleDocuments();

    // We should have deleted 30 documents in 3 batches
    var deletedDocuments = captureBulkDeletedDocuments(3, 30);
    resetEsMock();
    for (var document : deletedDocuments) {
      assertFalse(saksmappeIdList.contains(document));
    }

    delete("/arkiv/" + arkivDTO.getId());
    captureDeletedDocuments(10);
  }

  /**
   * Test that journalposts that doesn't exist in the database are removed from Elasticsearch.
   *
   * @throws Exception
   */
  @SuppressWarnings({"unchecked", "null"})
  @Test
  void testReindexRemoveJournalpostFromES() throws Exception {
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
    captureIndexedDocuments(21);
    resetEsMock();

    // Add 4 batches from ES, the last one is empty
    var sr1 = ElasticsearchMocks.searchResponse(batchSize, journalpostIdList);
    var sr2 = ElasticsearchMocks.searchResponse(batchSize, journalpostIdList);
    var sr3 = ElasticsearchMocks.searchResponse(batchSize, journalpostIdList);
    var empty = ElasticsearchMocks.searchResponse(0, new ArrayList<String>());

    // Return dummy lists for queries against journalpost
    when(esClient.search(
            argThat(
                (SearchRequest req) ->
                    req != null
                        && req.index().contains("test")
                        && req.query().toString().contains("Journalpost")),
            any()))
        .thenReturn(sr1, sr2, sr3, empty);

    // Return empty set for queries against other entities
    when(esClient.search(
            argThat(
                (SearchRequest req) ->
                    req != null
                        && req.index().contains("test")
                        && !req.query().toString().contains("Journalpost")),
            any()))
        .thenReturn(empty);

    // Remove documents that doesn't exist in the database
    elasticsearchReindexScheduler.removeStaleDocuments();

    // We should have deleted 30 documents in 3 batches
    var deletedDocuments = captureBulkDeletedDocuments(3, 30);
    resetEsMock();
    for (var document : deletedDocuments) {
      assertFalse(journalpostIdList.contains(document));
    }

    delete("/arkiv/" + arkivDTO.getId());
    captureDeletedDocuments(11);
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

    // Add ten moetemappes
    var moetemappeIdList = new ArrayList<String>();
    for (var i = 0; i < 10; i++) {
      response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", getMoetemappeJSON());
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
      moetemappeIdList.add(moetemappeDTO.getId());
    }

    captureIndexedDocuments(20); // Each moetemappe contains one moetesak
    resetEsMock();

    // Add 4 batches from ES, the last one is empty
    var sr1 = ElasticsearchMocks.searchResponse(batchSize, moetemappeIdList);
    var sr2 = ElasticsearchMocks.searchResponse(batchSize, moetemappeIdList);
    var sr3 = ElasticsearchMocks.searchResponse(batchSize, moetemappeIdList);
    var empty = ElasticsearchMocks.searchResponse(0, new ArrayList<String>());

    // Return dummy lists for queries against moetemappe
    when(esClient.search(
            argThat(
                (SearchRequest req) ->
                    req != null
                        && req.index().contains("test")
                        && req.query().toString().contains("Moetemappe")),
            any()))
        .thenReturn(sr1, sr2, sr3, empty);

    // Return empty set for queries against other entities
    when(esClient.search(
            argThat(
                (SearchRequest req) ->
                    req != null
                        && req.index().contains("test")
                        && !req.query().toString().contains("Moetemappe")),
            any()))
        .thenReturn(empty);

    // Remove documents that doesn't exist in the database
    elasticsearchReindexScheduler.removeStaleDocuments();
    var deletedDocuments = captureBulkDeletedDocuments(3, 30);
    resetEsMock();
    for (var document : deletedDocuments) {
      assertFalse(moetemappeIdList.contains(document));
    }

    delete("/arkiv/" + arkivDTO.getId());
    captureDeletedDocuments(20);
  }

  /**
   * Test that moetesaks that doesn't exist in the database are removed from Elasticsearch.
   *
   * @throws Exception
   */
  @SuppressWarnings({"unchecked", "null"})
  @Test
  void testReindexRemoveMoetesakFromES() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", getMoetemappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    captureIndexedDocuments(2);
    resetEsMock();

    // Add ten moetesaks
    var moetesakIdList = new ArrayList<String>();
    for (var i = 0; i < 10; i++) {
      response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", getMoetesakJSON());
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      var moetesakDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);
      moetesakIdList.add(moetesakDTO.getId());
    }

    captureIndexedDocuments(20); // (moetesak + moetemappe) * 10
    resetEsMock();

    // Add 4 batches from ES, the last one is empty
    var sr1 = ElasticsearchMocks.searchResponse(batchSize, moetesakIdList);
    var sr2 = ElasticsearchMocks.searchResponse(batchSize, moetesakIdList);
    var sr3 = ElasticsearchMocks.searchResponse(batchSize, moetesakIdList);
    var empty = ElasticsearchMocks.searchResponse(0, new ArrayList<String>());

    // Return dummy lists for queries against moetesak
    when(esClient.search(
            argThat(
                (SearchRequest req) ->
                    req != null
                        && req.index().contains("test")
                        && req.query().toString().contains("Møtesaksregistrering")),
            any()))
        .thenReturn(sr1, sr2, sr3, empty);

    // Return empty set for queries against other entities
    when(esClient.search(
            argThat(
                (SearchRequest req) ->
                    req != null
                        && req.index().contains("test")
                        && !req.query().toString().contains("Møtesaksregistrering")),
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
    captureDeletedDocuments(10 + 2);
  }

  @SuppressWarnings("unchecked")
  @Test
  void testReindexMissingInnsynskrav() throws Exception {
    // Add Arkiv, Saksmappe with Journalposts
    var response = post("/arkiv", getArkivJSON());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("journalpost", new JSONArray().put(getJournalpostJSON()));
    response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Should have indexed one Saksmappe and one Journalpost
    var capturedDocuments = captureIndexedDocuments(2);
    resetEsMock();
    assertNotNull(capturedDocuments.get(saksmappeDTO.getId()));
    assertNotNull(capturedDocuments.get(saksmappeDTO.getJournalpost().getFirst().getId()));

    var indexResponseMock = getIndexResponseMock();
    when(esClient.index(any(Function.class)))
        .thenThrow(new IOException("Failed to index document"))
        .thenReturn(indexResponseMock);

    // Create InnsynskravBestilling
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("journalpost", saksmappeDTO.getJournalpost().getFirst().getId());
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray().put(innsynskravJSON));
    response = post("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);

    // Should tried to index Innsynskrav
    capturedDocuments = captureIndexedDocuments(1);
    resetEsMock();
    assertNotNull(
        capturedDocuments.get(innsynskravBestillingDTO.getInnsynskrav().getFirst().getId()));

    // Reindex unindexed Innsynskrav
    elasticsearchReindexScheduler.updateOutdatedDocuments();
    capturedDocuments = captureBulkIndexedDocuments(1, 1);
    resetEsMock();
    assertNotNull(
        capturedDocuments.get(innsynskravBestillingDTO.getInnsynskrav().getFirst().getId()));

    // Delete
    delete("/arkiv/" + arkivDTO.getId());
    deleteAdmin("/innsynskravBestilling/" + innsynskravBestillingDTO.getId());
  }

  @Test
  void testReindexInnsynskravWithDeletedJournalpost() throws Exception {
    // Add Arkiv, Saksmappe with Journalposts
    var response = post("/arkiv", getArkivJSON());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("journalpost", new JSONArray().put(getJournalpostJSON()));
    response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Should have indexed one Saksmappe and one Journalpost
    var capturedDocuments = captureIndexedDocuments(2);
    resetEsMock();
    assertNotNull(capturedDocuments.get(saksmappeDTO.getId()));
    assertNotNull(capturedDocuments.get(saksmappeDTO.getJournalpost().getFirst().getId()));

    // Create InnsynskravBestilling
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("journalpost", saksmappeDTO.getJournalpost().getFirst().getId());
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray().put(innsynskravJSON));
    response = post("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);

    // Should have tried to index Innsynskrav
    capturedDocuments = captureIndexedDocuments(1);
    resetEsMock();
    var indexedInnsynskrav =
        (InnsynskravES)
            capturedDocuments.get(innsynskravBestillingDTO.getInnsynskrav().getFirst().getId());
    assertNotNull(indexedInnsynskrav);
    assertNotNull(indexedInnsynskrav.getStatRelation());
    assertNotNull(indexedInnsynskrav.getStatRelation().getParent());

    // Delete Journalpost
    delete("/journalpost/" + saksmappeDTO.getJournalpost().getFirst().getId());
    captureDeletedDocuments(1);

    // Reindex unindexed Innsynskrav
    elasticsearchReindexScheduler.updateOutdatedDocuments();
    capturedDocuments = captureBulkIndexedDocuments(1, 1);
    resetEsMock();
    indexedInnsynskrav =
        (InnsynskravES)
            capturedDocuments.get(innsynskravBestillingDTO.getInnsynskrav().getFirst().getId());
    assertNotNull(indexedInnsynskrav);
    assertNotNull(indexedInnsynskrav.getStatRelation());
    assertNull(indexedInnsynskrav.getStatRelation().getParent());

    // Delete
    delete("/arkiv/" + arkivDTO.getId());
    deleteAdmin("/innsynskravBestilling/" + innsynskravBestillingDTO.getId());
  }
}

package no.einnsyn.backend.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Function;
import no.einnsyn.backend.EinnsynLegacyElasticTestBase;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.innsynskrav.models.InnsynskravES;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestillingDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
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

  @Autowired TaskTestService taskTestService;

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

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    // Add ten documents, fail to index one of them
    doThrow(new IOException("Failed to index document"))
        .doCallRealMethod()
        .when(esClient)
        .index(any(Function.class));
    for (var i = 0; i < 10; i++) {
      response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    // We should have tried to index 10 documents
    captureIndexedDocuments(10);
    resetEs();

    // Reindex all (one) unindexed documents
    taskTestService.updateOutdatedDocuments();
    captureIndexedDocuments(1);
    resetEs();

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

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Should have indexed the saksmappe
    captureIndexedDocuments(1);
    resetEs();

    // Add ten documents. Fail to index twice (in case saksmappe is indexed before journalpost)
    doThrow(new IOException("Failed to index document"))
        .doThrow(new IOException("Failed to index document"))
        .doCallRealMethod()
        .when(esClient)
        .index(any(Function.class));

    for (var i = 0; i < 10; i++) {
      response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", getJournalpostJSON());
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    // We should have tried to index 20 documents ((saksmappe + journalpost) * 10)
    captureIndexedDocuments(20);
    resetEs();

    // Reindex all (one) unindexed documents
    taskTestService.updateOutdatedDocuments();
    captureIndexedDocuments(1);
    resetEs();

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

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    // getMoetemappeJSON() adds one moetesak, remove it
    var moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.remove("moetesak");

    // Add ten documents, fail to index one of them
    doThrow(new IOException("Failed to index document"))
        .doCallRealMethod()
        .when(esClient)
        .index(any(Function.class));
    for (var i = 0; i < 10; i++) {
      response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    // We should have tried to index 10 documents
    captureIndexedDocuments(10);
    resetEs();

    // Reindex all (one) unindexed documents
    taskTestService.updateOutdatedDocuments();
    captureIndexedDocuments(1);
    resetEs();

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

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", getMoetemappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    // Should have indexed the moetemappe and one moetesak
    captureIndexedDocuments(2);
    resetEs();

    // Add ten documents. Fail to index twice (in case moetemappe is indexed before moetesak)
    doThrow(new IOException("Failed to index document"))
        .doThrow(new IOException("Failed to index document"))
        .doCallRealMethod()
        .when(esClient)
        .index(any(Function.class));
    for (var i = 0; i < 10; i++) {
      response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", getMoetesakJSON());
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    // We should have tried to index 20 documents (moetemappe + moetesak * 10)
    captureIndexedDocuments(20);
    resetEs();

    // Reindex all (one) unindexed documents
    taskTestService.updateOutdatedDocuments();
    captureIndexedDocuments(1);
    resetEs();

    delete("/arkiv/" + arkivDTO.getId());
    captureDeletedDocuments(12);
  }

  /**
   * Test that saksmappe that doesn't exist in the database are removed from Elasticsearch.
   *
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  @Test
  void testReindexRemoveSaksmappeFromES() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    // Add saksmappes
    var saksmappeIdList = new ArrayList<String>();
    for (var i = 0; i < 40; i++) {
      response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
      saksmappeIdList.add(saksmappeDTO.getId());
    }

    captureIndexedDocuments(40);
    resetEs();

    // Refresh index
    esClient.indices().refresh(r -> r.index("test"));

    // Remove 21 documents from the database, fail to delete all of them from ES
    doThrow(new IOException("Failed to delete document"))
        .when(esClient)
        .delete(any(Function.class));
    for (var i = 0; i < 21; i++) {
      delete("/saksmappe/" + saksmappeIdList.get(i));
    }
    // Remove deleted IDs from saksmappeIdList
    saksmappeIdList.subList(0, 21).clear();

    // Should have tried to delete 21 documents from ES
    captureDeletedDocuments(21);
    resetEs();

    // Reset throw exception
    doCallRealMethod().when(esClient).delete(any(Function.class));

    // Remove documents that doesn't exist in the database
    taskTestService.removeStaleDocuments();

    // We should have deleted 21 documents in 2 batches
    var deletedDocuments = captureBulkDeletedDocuments(2, 21);
    resetEs();
    for (var document : deletedDocuments) {
      assertFalse(saksmappeIdList.contains(document));
    }

    delete("/arkiv/" + arkivDTO.getId());
    captureDeletedDocuments(19);
  }

  /**
   * Test that journalposts that doesn't exist in the database are removed from Elasticsearch.
   *
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  @Test
  void testReindexRemoveJournalpostFromES() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Should have indexed the saksmappe
    captureIndexedDocuments(1);
    resetEs();

    // Add journalposts
    var journalpostIdList = new ArrayList<String>();
    for (var i = 0; i < 40; i++) {
      response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", getJournalpostJSON());
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      var journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
      journalpostIdList.add(journalpostDTO.getId());
    }

    captureIndexedDocuments(80); // (saksmappe + journalpost) * 40
    resetEs();

    // Refresh index
    esClient.indices().refresh(r -> r.index("test"));

    // Remove 21 documents from the database, fail to delete all of them from ES
    doThrow(new IOException("Failed to delete document"))
        .when(esClient)
        .delete(any(Function.class));
    for (var i = 0; i < 21; i++) {
      delete("/journalpost/" + journalpostIdList.get(i));
    }
    // Remove deleted IDs from list
    journalpostIdList.subList(0, 21).clear();

    // Should have tried to delete 21 documents from ES
    captureDeletedDocuments(21);
    resetEs();

    // Reset throw exception
    doCallRealMethod().when(esClient).delete(any(Function.class));

    // Remove documents that doesn't exist in the database
    taskTestService.removeStaleDocuments();

    // We should have deleted 21 documents in 2 batches
    var deletedDocuments = captureBulkDeletedDocuments(2, 21);
    resetEs();
    for (var document : deletedDocuments) {
      assertFalse(journalpostIdList.contains(document));
    }

    delete("/arkiv/" + arkivDTO.getId());
    captureDeletedDocuments(20); // 19 journalpost + saksmappe
  }

  /**
   * Test that moetemappes that doesn't exist in the database are removed from Elasticsearch.
   *
   * @throws Exception
   */
  @SuppressWarnings({"unchecked"})
  @Test
  void testReindexRemoveMoetemappeFromES() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    // Add moetemappes
    var moetemappeIdList = new ArrayList<String>();
    for (var i = 0; i < 40; i++) {
      var moetemappeJSON = getMoetemappeJSON();
      moetemappeJSON.remove("moetesak");
      response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
      moetemappeIdList.add(moetemappeDTO.getId());
    }

    captureIndexedDocuments(40);
    resetEs();

    // Refresh index
    esClient.indices().refresh(r -> r.index("test"));

    // Remove 21 documents from the database, fail to delete all of them from ES
    doThrow(new IOException("Failed to delete document"))
        .when(esClient)
        .delete(any(Function.class));
    for (var i = 0; i < 21; i++) {
      delete("/moetemappe/" + moetemappeIdList.get(i));
    }
    // Remove deleted IDs from list
    moetemappeIdList.subList(0, 21).clear();

    // Should have tried to delete 21 documents from ES
    captureDeletedDocuments(21);
    resetEs();

    // Reset throw exception
    doCallRealMethod().when(esClient).delete(any(Function.class));

    // Remove documents that doesn't exist in the database
    taskTestService.removeStaleDocuments();
    var deletedDocuments = captureBulkDeletedDocuments(2, 21);
    resetEs();
    for (var document : deletedDocuments) {
      assertFalse(moetemappeIdList.contains(document));
    }

    delete("/arkiv/" + arkivDTO.getId());
    captureDeletedDocuments(19);
  }

  /**
   * Test that moetesaks that doesn't exist in the database are removed from Elasticsearch.
   *
   * @throws Exception
   */
  @SuppressWarnings({"unchecked"})
  @Test
  void testReindexRemoveMoetesakFromES() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    var moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.remove("moetesak");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    captureIndexedDocuments(1);
    resetEs();

    // Add ten moetesaks
    var moetesakIdList = new ArrayList<String>();
    for (var i = 0; i < 40; i++) {
      response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", getMoetesakJSON());
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      var moetesakDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);
      moetesakIdList.add(moetesakDTO.getId());
    }

    captureIndexedDocuments(80); // (moetesak + moetemappe) * 40
    resetEs();

    // Refresh index
    esClient.indices().refresh(r -> r.index("test"));

    // Remove 21 documents from the database, fail to delete all of them from ES
    doThrow(new IOException("Failed to delete document"))
        .when(esClient)
        .delete(any(Function.class));
    for (var i = 0; i < 21; i++) {
      delete("/moetesak/" + moetesakIdList.get(i));
    }
    // Remove deleted IDs from list
    moetesakIdList.subList(0, 21).clear();

    // Should have tried to delete 21 documents from ES
    captureDeletedDocuments(21);
    resetEs();

    // Reset throw exception
    doCallRealMethod().when(esClient).delete(any(Function.class));

    // Remove documents that doesn't exist in the database
    taskTestService.removeStaleDocuments();

    // We should have deleted 21 documents in 2 batches
    var deletedDocuments = captureBulkDeletedDocuments(2, 21);
    for (var document : deletedDocuments) {
      assertFalse(moetesakIdList.contains(document));
    }

    delete("/arkiv/" + arkivDTO.getId());
    captureDeletedDocuments(20);
  }

  @SuppressWarnings("unchecked")
  @Test
  void testReindexMissingInnsynskrav() throws Exception {
    // Add Arkiv, Saksmappe with Journalposts
    var response = post("/arkiv", getArkivJSON());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("journalpost", new JSONArray().put(getJournalpostJSON()));
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    var journalpostList = getJournalpostList(saksmappeDTO.getId()).getItems();

    // Should have indexed one Saksmappe and one Journalpost
    var capturedDocuments = captureIndexedDocuments(2);
    resetEs();
    assertNotNull(capturedDocuments.get(saksmappeDTO.getId()));
    assertNotNull(capturedDocuments.get(journalpostList.getFirst().getId()));

    doThrow(new IOException("Failed to index document"))
        .doCallRealMethod()
        .when(esClient)
        .index(any(Function.class));

    // Create InnsynskravBestilling
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("journalpost", journalpostList.getFirst().getId());
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray().put(innsynskravJSON));
    response = post("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);

    // Should tried to index Innsynskrav
    capturedDocuments = captureIndexedDocuments(1);
    resetEs();
    assertNotNull(
        capturedDocuments.get(innsynskravBestillingDTO.getInnsynskrav().getFirst().getId()));

    // Reindex unindexed Innsynskrav
    taskTestService.updateOutdatedDocuments();
    capturedDocuments = captureIndexedDocuments(1);
    resetEs();
    assertNotNull(
        capturedDocuments.get(innsynskravBestillingDTO.getInnsynskrav().getFirst().getId()));

    // Delete
    delete("/arkiv/" + arkivDTO.getId());
    deleteAdmin("/innsynskravBestilling/" + innsynskravBestillingDTO.getId());
    deleteInnsynskravFromBestilling(innsynskravBestillingDTO);
  }

  @Test
  void testReindexInnsynskravWithDeletedJournalpost() throws Exception {
    // Add Arkiv, Saksmappe with Journalposts
    var response = post("/arkiv", getArkivJSON());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("journalpost", new JSONArray().put(getJournalpostJSON()));
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Should have indexed one Saksmappe and one Journalpost
    var capturedDocuments = captureIndexedDocuments(2);
    resetEs();
    assertNotNull(capturedDocuments.get(saksmappeDTO.getId()));
    var journalpostList = getJournalpostList(saksmappeDTO.getId()).getItems();
    assertNotNull(capturedDocuments.get(journalpostList.getFirst().getId()));

    // Create InnsynskravBestilling
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("journalpost", journalpostList.getFirst().getId());
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray().put(innsynskravJSON));
    response = post("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);

    // Should have tried to index Innsynskrav
    capturedDocuments = captureIndexedDocuments(1);
    resetEs();
    var indexedInnsynskrav =
        (InnsynskravES)
            capturedDocuments.get(innsynskravBestillingDTO.getInnsynskrav().getFirst().getId());
    assertNotNull(indexedInnsynskrav);
    assertNotNull(indexedInnsynskrav.getStatRelation());
    assertNotNull(indexedInnsynskrav.getStatRelation().getParent());

    // Delete Journalpost
    delete("/journalpost/" + journalpostList.getFirst().getId());
    captureDeletedDocuments(1);

    // Should have re-indexed Saksmappe
    capturedDocuments = captureIndexedDocuments(1);
    resetEs();

    // Reindex unindexed Innsynskrav
    taskTestService.updateOutdatedDocuments();
    capturedDocuments = captureIndexedDocuments(1);
    resetEs();
    indexedInnsynskrav =
        (InnsynskravES)
            capturedDocuments.get(innsynskravBestillingDTO.getInnsynskrav().getFirst().getId());
    assertNotNull(indexedInnsynskrav);
    assertNotNull(indexedInnsynskrav.getStatRelation());
    // The parent should be kept from the previous document:
    assertNotNull(indexedInnsynskrav.getStatRelation().getParent());

    // Delete
    delete("/arkiv/" + arkivDTO.getId());
    deleteAdmin("/innsynskravBestilling/" + innsynskravBestillingDTO.getId());
    deleteInnsynskravFromBestilling(innsynskravBestillingDTO);
  }
}

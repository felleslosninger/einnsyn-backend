package no.einnsyn.apiv3.entities.moetemappe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;

import java.util.concurrent.TimeUnit;
import no.einnsyn.apiv3.EinnsynLegacyElasticTestBase;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeES;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakES;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class MoetemappeLegacyESTest extends EinnsynLegacyElasticTestBase {

  ArkivDTO arkivDTO;

  @BeforeAll
  void setUp() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
  }

  @AfterAll
  void tearDown() throws Exception {
    var response = delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @BeforeEach
  void resetMocks() {
    reset(esClient);
  }

  @Test
  void addMoetemappeES() throws Exception {
    var response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", getMoetemappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    assertNotNull(moetemappeDTO.getId());

    waiter.await(100, TimeUnit.MILLISECONDS);

    // One Moetemappe, one Moetesak
    var documentMap = captureIndexedDocuments(2);
    compareMoetemappe(moetemappeDTO, (MoetemappeES) documentMap.get(moetemappeDTO.getId()));
    var moetesakDTO = moetemappeDTO.getMoetesak().get(0).getExpandedObject();
    compareMoetesak(moetesakDTO, (MoetesakES) documentMap.get(moetesakDTO.getId()));

    // Clean up
    response = delete("/moetemappe/" + moetemappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // The Moetemappe and Moetesak should be deleted from ES
    var deletedDocuments = captureDeletedDocuments(2);
    assertTrue(deletedDocuments.contains(moetemappeDTO.getId()));
    assertTrue(deletedDocuments.contains(moetesakDTO.getId()));
  }

  @Test
  void updateMoetemappeES() throws Exception {
    var response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", getMoetemappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    assertNotNull(moetemappeDTO.getId());

    // One Moetemappe, one Moetesak
    var documentMap = captureIndexedDocuments(2);
    compareMoetemappe(moetemappeDTO, (MoetemappeES) documentMap.get(moetemappeDTO.getId()));
    var moetesakDTO = moetemappeDTO.getMoetesak().get(0).getExpandedObject();
    compareMoetesak(moetesakDTO, (MoetesakES) documentMap.get(moetesakDTO.getId()));

    reset(esClient);
    var updateJSON = new JSONObject();
    updateJSON.put("offentligTittel", "----");
    updateJSON.put("offentligTittelSensitiv", "????");
    response = put("/moetemappe/" + moetemappeDTO.getId(), updateJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    // One Moetemappe, one Moetesak
    documentMap = captureIndexedDocuments(2);
    compareMoetemappe(moetemappeDTO, (MoetemappeES) documentMap.get(moetemappeDTO.getId()));
    moetesakDTO = moetesakService.get(moetemappeDTO.getMoetesak().get(0).getId());
    compareMoetesak(moetesakDTO, (MoetesakES) documentMap.get(moetesakDTO.getId()));

    // Nothing should be deleted
    assertTrue(captureDeletedDocuments(0).isEmpty());

    // This has already been tested in compare*, but let's be explicit:
    assertEquals(moetemappeDTO.getOffentligTittel(), updateJSON.getString("offentligTittel"));
    assertEquals(
        moetemappeDTO.getOffentligTittelSensitiv(),
        updateJSON.getString("offentligTittelSensitiv"));

    // Clean up
    response = delete("/moetemappe/" + moetemappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // The Moetemappe and Moetesak should be deleted from ES
    var deletedDocuments = captureDeletedDocuments(2);
    assertTrue(deletedDocuments.contains(moetemappeDTO.getId()));
    assertTrue(deletedDocuments.contains(moetesakDTO.getId()));
  }

  @Test
  void deleteMoetesakFromMoetemappeES() throws Exception {
    var response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", getMoetemappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    assertNotNull(moetemappeDTO.getId());

    // One Moetemappe, one Moetesak
    var documentMap = captureIndexedDocuments(2);
    compareMoetemappe(moetemappeDTO, (MoetemappeES) documentMap.get(moetemappeDTO.getId()));
    var moetesakDTO = moetemappeDTO.getMoetesak().get(0).getExpandedObject();
    compareMoetesak(moetesakDTO, (MoetesakES) documentMap.get(moetesakDTO.getId()));

    reset(esClient);
    response = delete("/moetesak/" + moetemappeDTO.getMoetesak().get(0).getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = get("/moetemappe/" + moetemappeDTO.getId());
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    assertEquals(0, moetemappeDTO.getMoetesak().size());

    // One Moetemappe, 0 Moetesak
    documentMap = captureIndexedDocuments(1);
    compareMoetemappe(moetemappeDTO, (MoetemappeES) documentMap.get(moetemappeDTO.getId()));

    // Deleted one moetesak
    var deletedDocuments = captureDeletedDocuments(1);
    assertTrue(deletedDocuments.contains(moetesakDTO.getId()));

    // Clean up
    reset(esClient);
    response = delete("/moetemappe/" + moetemappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // The moetemappe should be deleted from ES
    assertTrue(captureDeletedDocuments(1).contains(moetemappeDTO.getId()));
  }

  @Test
  void deleteMoetedokumentFromMoetemappeES() throws Exception {
    var response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", getMoetemappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    assertNotNull(moetemappeDTO.getId());

    // One Moetemappe, one Moetesak
    var documentMap = captureIndexedDocuments(2);
    compareMoetemappe(moetemappeDTO, (MoetemappeES) documentMap.get(moetemappeDTO.getId()));
    var moetesakDTO = moetemappeDTO.getMoetesak().get(0).getExpandedObject();
    compareMoetesak(moetesakDTO, (MoetesakES) documentMap.get(moetesakDTO.getId()));

    reset(esClient);
    response = delete("/moetedokument/" + moetemappeDTO.getMoetedokument().get(0).getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = get("/moetemappe/" + moetemappeDTO.getId());
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    // One Moetemappe should be reindexed
    documentMap = captureIndexedDocuments(1);
    compareMoetemappe(moetemappeDTO, (MoetemappeES) documentMap.get(moetemappeDTO.getId()));

    // No documents should be deleted (Moetedokument isn't a separate entity in ES)
    assertTrue(captureDeletedDocuments(0).isEmpty());

    // Clean up
    response = delete("/moetemappe/" + moetemappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Make sure the Moetemappe & Moetesak is deleted
    var deletedDocuments = captureDeletedDocuments(2);
    assertTrue(deletedDocuments.contains(moetemappeDTO.getId()));
    assertTrue(deletedDocuments.contains(moetesakDTO.getId()));
  }
}

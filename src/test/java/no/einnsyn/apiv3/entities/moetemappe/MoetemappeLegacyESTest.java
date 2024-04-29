package no.einnsyn.apiv3.entities.moetemappe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    var documents = captureIndexedDocuments(2);
    compareMoetemappe(moetemappeDTO, (MoetemappeES) documents[0]);
    compareMoetesak(
        moetemappeDTO.getMoetesak().get(0).getExpandedObject(), (MoetesakES) documents[1]);

    // Clean up
    response = delete("/moetemappe/" + moetemappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void updateMoetemappeES() throws Exception {
    var response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", getMoetemappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    assertNotNull(moetemappeDTO.getId());

    // One Moetemappe, one Moetesak
    var documents = captureIndexedDocuments(2);
    compareMoetemappe(moetemappeDTO, (MoetemappeES) documents[0]);

    reset(esClient);
    var updateJSON = new JSONObject();
    updateJSON.put("offentligTittel", "----");
    response = put("/moetemappe/" + moetemappeDTO.getId(), updateJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    assertEquals(moetemappeDTO.getOffentligTittel(), updateJSON.getString("offentligTittel"));

    // One Moetemappe, one Moetesak
    documents = captureIndexedDocuments(2);
    compareMoetemappe(moetemappeDTO, (MoetemappeES) documents[0]);

    // Clean up
    response = delete("/moetemappe/" + moetemappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void deleteMoetesakFromMoetemappeES() throws Exception {
    var response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", getMoetemappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    assertNotNull(moetemappeDTO.getId());

    // One Moetemappe, one Moetesak
    var documents = captureIndexedDocuments(2);
    compareMoetemappe(moetemappeDTO, (MoetemappeES) documents[0]);

    reset(esClient);
    response = delete("/moetesak/" + moetemappeDTO.getMoetesak().get(0).getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = get("/moetemappe/" + moetemappeDTO.getId());
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    assertEquals(0, moetemappeDTO.getMoetesak().size());

    // One Moetemappe, 0 Moetesak
    documents = captureIndexedDocuments(1);
    compareMoetemappe(moetemappeDTO, (MoetemappeES) documents[0]);

    // Clean up
    response = delete("/moetemappe/" + moetemappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void deleteMoetedokumentFromMoetemappeES() throws Exception {
    var response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", getMoetemappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    assertNotNull(moetemappeDTO.getId());

    // One Moetemappe, one Moetesak
    var documents = captureIndexedDocuments(1 + moetemappeDTO.getMoetesak().size());
    compareMoetemappe(moetemappeDTO, (MoetemappeES) documents[0]);

    reset(esClient);
    response = delete("/moetedokument/" + moetemappeDTO.getMoetedokument().get(0).getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = get("/moetemappe/" + moetemappeDTO.getId());
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    // One Moetemappe
    documents = captureIndexedDocuments(1);
    compareMoetemappe(moetemappeDTO, (MoetemappeES) documents[0]);

    // Clean up
    response = delete("/moetemappe/" + moetemappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}

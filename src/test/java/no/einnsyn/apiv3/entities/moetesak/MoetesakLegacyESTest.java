package no.einnsyn.apiv3.entities.moetesak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;

import no.einnsyn.apiv3.EinnsynLegacyElasticTestBase;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeES;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakES;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class MoetesakLegacyESTest extends EinnsynLegacyElasticTestBase {

  ArkivDTO arkivDTO;
  MoetemappeDTO moetemappeDTO;

  @BeforeAll
  void setUp() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", getMoetemappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
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
  void testMoetesakES() throws Exception {
    var moetesakJSON = getMoetesakJSON();
    var response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", moetesakJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetesakDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);

    // Should have reindexed both Moetesak and Moetemappe
    var documentMap = captureIndexedDocuments(2);
    compareMoetesak(moetesakDTO, (MoetesakES) documentMap.get(moetesakDTO.getId()));
    moetemappeDTO = moetemappeService.get(moetemappeDTO.getId());
    compareMoetemappe(moetemappeDTO, (MoetemappeES) documentMap.get(moetemappeDTO.getId()));

    // Clean up
    response = delete("/moetesak/" + moetesakDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Should have deleted Moetesak from ES
    var deletedDocuments = captureDeletedDocuments(1);
    assertTrue(deletedDocuments.contains(moetesakDTO.getId()));
  }

  @Test
  void updateMoetesakES() throws Exception {
    var moetesakJSON = getMoetesakJSON();
    var response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", moetesakJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetesakDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);

    reset(esClient);
    var updatedMoetesakJSON = getMoetesakJSON();
    updatedMoetesakJSON.put("moetesaksaar", "1999");
    response = put("/moetesak/" + moetesakDTO.getId(), updatedMoetesakJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var updatedMoetesakDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);

    // Should have reindexed both Moetesak and Moetemappe
    var documentMap = captureIndexedDocuments(2);
    compareMoetesak(updatedMoetesakDTO, (MoetesakES) documentMap.get(updatedMoetesakDTO.getId()));
    moetemappeDTO = moetemappeService.get(moetemappeDTO.getId());
    compareMoetemappe(moetemappeDTO, (MoetemappeES) documentMap.get(moetemappeDTO.getId()));

    // This has already been checked in compare*, but let's be explicit:
    assertEquals(
        "1999", ((MoetesakES) documentMap.get(updatedMoetesakDTO.getId())).getMøtesaksår());

    // Clean up
    response = delete("/moetesak/" + updatedMoetesakDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Should have deleted Moetesak from ES
    var deletedDocuments = captureDeletedDocuments(1);
    assertTrue(deletedDocuments.contains(updatedMoetesakDTO.getId()));
  }
}

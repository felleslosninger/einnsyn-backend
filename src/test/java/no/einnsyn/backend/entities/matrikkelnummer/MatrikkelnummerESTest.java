package no.einnsyn.backend.entities.matrikkelnummer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import no.einnsyn.backend.EinnsynLegacyElasticTestBase;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.matrikkelnummer.models.MatrikkelnummerDTO;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeES;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MatrikkelnummerESTest extends EinnsynLegacyElasticTestBase {

  @Autowired private MatrikkelnummerRepository matrikkelnummerRepository;

  private ArkivDTO arkivDTO;
  private ArkivdelDTO arkivdelDTO;

  @BeforeAll
  void setup() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
  }

  @AfterAll
  void teardown() throws Exception {
    delete("/arkiv/" + arkivDTO.getId());
  }

  @Test
  void reindexParentWhenMatrikkelnummerAddedOrDeleted() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Saksmappe creation triggers one index call
    captureIndexedDocuments(1);
    resetEs();

    // Add a matrikkelnummer — should trigger reindex of the parent Saksmappe
    response =
        post(
            "/saksmappe/" + saksmappeDTO.getId() + "/matrikkelnummer",
            getMatrikkelnummerJSON("0301", 10, 99));
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var matrikkelnummerDTO = gson.fromJson(response.getBody(), MatrikkelnummerDTO.class);
    assertNotNull(matrikkelnummerDTO.getId());

    // Parent Saksmappe must be reindexed after Matrikkelnummer is added
    var documentMap = captureIndexedDocuments(1);
    assertNotNull(documentMap.get(saksmappeDTO.getId()), "Saksmappe should be reindexed");
    compareSaksmappe(
        saksmappeService.get(saksmappeDTO.getId()),
        (SaksmappeES) documentMap.get(saksmappeDTO.getId()));
    resetEs();

    // Delete matrikkelnummer directly — should also trigger parent reindex
    assertEquals(
        HttpStatus.OK, delete("/matrikkelnummer/" + matrikkelnummerDTO.getId()).getStatusCode());
    assertTrue(matrikkelnummerRepository.findById(matrikkelnummerDTO.getId()).isEmpty());

    documentMap = captureIndexedDocuments(1);
    assertNotNull(
        documentMap.get(saksmappeDTO.getId()),
        "Saksmappe should be reindexed after matrikkelnummer deletion");

    delete("/saksmappe/" + saksmappeDTO.getId());
  }

  /**
   * Test that a systemId used as path parameter is resolved to the canonical eInnsyn ID before the
   * parent is scheduled for indexing. ElasticsearchHandlerInterceptor dispatches queued entries by
   * ID prefix, so an unresolved systemId is silently dropped and the parent is never reindexed.
   */
  @Test
  void reindexParentWhenMatrikkelnummerAddedBySystemId() throws Exception {
    var systemId = "0f2b6c19-7e4a-4d51-9a3c-8b5e1d6f2a70";
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("systemId", systemId);

    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Saksmappe creation triggers one index call
    captureIndexedDocuments(1);
    resetEs();

    // Add a matrikkelnummer, using the Saksmappe's systemId as path parameter
    response =
        post("/saksmappe/" + systemId + "/matrikkelnummer", getMatrikkelnummerJSON("0301", 11, 98));
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    // The parent must be reindexed, under its canonical ID
    var documentMap = captureIndexedDocuments(1);
    assertNotNull(
        documentMap.get(saksmappeDTO.getId()),
        "Saksmappe should be reindexed under its canonical ID");

    delete("/saksmappe/" + saksmappeDTO.getId());
  }

  /**
   * Same as above for Moetesak, whose DTO class was missing from IdResolver's entity/service map.
   * Resolution is silent when an entry is missing, so this covers the map rather than the
   * converter.
   */
  @Test
  void reindexParentWhenMoetesakMatrikkelnummerAddedBySystemId() throws Exception {
    var systemId = "3c8e1a45-6b27-4f93-a1d0-7e5c2b9f4a68";
    var moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.remove("moetesak");
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    var moetesakJSON = getMoetesakJSON();
    moetesakJSON.put("systemId", systemId);
    response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", moetesakJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetesakDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);
    resetEs();

    // Add a matrikkelnummer, using the Moetesak's systemId as path parameter
    response =
        post("/moetesak/" + systemId + "/matrikkelnummer", getMatrikkelnummerJSON("0301", 12, 97));
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    // Moetesak and its parent Moetemappe are both reindexed, under their canonical IDs
    var documentMap = captureIndexedDocuments(2);
    assertNotNull(
        documentMap.get(moetesakDTO.getId()),
        "Moetesak should be reindexed under its canonical ID");
    assertNotNull(
        documentMap.get(moetemappeDTO.getId()),
        "Moetemappe should be reindexed under its canonical ID");
    delete("/moetemappe/" + moetemappeDTO.getId());
  }

  @Test
  void matrikkelFieldsAreIndexedInSaksmappe() throws Exception {
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put(
        "matrikkelnummer",
        new JSONArray()
            .put(getMatrikkelnummerJSON("0301", 42, 7))
            .put(getMatrikkelnummerJSON("0301", 42, 8)));

    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    var documentMap = captureIndexedDocuments(1);
    var saksmappeES = (SaksmappeES) documentMap.get(saksmappeDTO.getId());
    assertNotNull(saksmappeES);

    // Verify matrikkelnummer is present and has correct count
    assertNotNull(saksmappeES.getMatrikkelnummer());
    assertEquals(2, saksmappeES.getMatrikkelnummer().size());

    var mnES0 = saksmappeES.getMatrikkelnummer().get(0);
    assertEquals("0301", mnES0.getKommunenummer());
    assertEquals(42, mnES0.getGaardsnummer());
    assertEquals(7, mnES0.getBruksnummer());
    assertEquals(0, mnES0.getFestenummer());
    assertEquals(0, mnES0.getSeksjonsnummer());
    assertEquals(
        List.of("42/7", "0301-42/7", "0301/42/7", "0301-42/7/0/0", "0301/42/7/0/0"),
        mnES0.getMatrikkelId());

    var mnES1 = saksmappeES.getMatrikkelnummer().get(1);
    assertEquals("0301", mnES1.getKommunenummer());
    assertEquals(42, mnES1.getGaardsnummer());
    assertEquals(8, mnES1.getBruksnummer());
    assertEquals(0, mnES1.getFestenummer());
    assertEquals(0, mnES1.getSeksjonsnummer());
    assertEquals(
        List.of("42/8", "0301-42/8", "0301/42/8", "0301-42/8/0/0", "0301/42/8/0/0"),
        mnES1.getMatrikkelId());

    // compareSaksmappe also verifies matrikkelnummer via compareMatrikkelnummer
    compareSaksmappe(saksmappeService.get(saksmappeDTO.getId()), saksmappeES);

    delete("/saksmappe/" + saksmappeDTO.getId());
  }
}

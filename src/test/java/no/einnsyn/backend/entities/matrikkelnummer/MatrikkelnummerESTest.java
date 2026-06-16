package no.einnsyn.backend.entities.matrikkelnummer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import no.einnsyn.backend.EinnsynLegacyElasticTestBase;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.matrikkelnummer.models.MatrikkelnummerDTO;
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
  void reindexParentWhenMatrikkelnummerAdded() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Saksmappe creation triggers one index call
    captureIndexedDocuments(1);
    resetEs();

    // Add a matrikkelnummer — should trigger reindex of the parent Saksmappe
    var saksmappeJSON = new org.json.JSONObject();
    saksmappeJSON.put(
        "matrikkelnummer", new JSONArray().put(getMatrikkelnummerJSON("0301", 10, 99)));

    response = patch("/saksmappe/" + saksmappeDTO.getId(), saksmappeJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    var matrikkelnummerDTO = saksmappeDTO.getMatrikkelnummer().getFirst().getExpandedObject();
    if (matrikkelnummerDTO == null) {
      matrikkelnummerDTO = new MatrikkelnummerDTO();
      matrikkelnummerDTO.setId(saksmappeDTO.getMatrikkelnummer().getFirst().getId());
    }
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

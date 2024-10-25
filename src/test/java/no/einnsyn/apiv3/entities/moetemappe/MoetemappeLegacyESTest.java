package no.einnsyn.apiv3.entities.moetemappe;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import no.einnsyn.apiv3.EinnsynLegacyElasticTestBase;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.enhet.models.EnhetDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeES;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakES;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SuppressWarnings("unchecked")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
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

  @Test
  void addMoetemappeES() throws Exception {
    var response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", getMoetemappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    assertNotNull(moetemappeDTO.getId());

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
    resetEsMock();
    compareMoetemappe(moetemappeDTO, (MoetemappeES) documentMap.get(moetemappeDTO.getId()));
    var moetesakDTO = moetemappeDTO.getMoetesak().get(0).getExpandedObject();
    compareMoetesak(moetesakDTO, (MoetesakES) documentMap.get(moetesakDTO.getId()));

    var updateJSON = new JSONObject();
    updateJSON.put("offentligTittel", "----");
    updateJSON.put("offentligTittelSensitiv", "????");
    response = put("/moetemappe/" + moetemappeDTO.getId(), updateJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    // One Moetemappe, one Moetesak
    documentMap = captureIndexedDocuments(2);
    // Nothing should be deleted
    captureDeletedDocuments(0);
    resetEsMock();
    compareMoetemappe(moetemappeDTO, (MoetemappeES) documentMap.get(moetemappeDTO.getId()));
    moetesakDTO = moetesakService.get(moetemappeDTO.getMoetesak().get(0).getId());
    compareMoetesak(moetesakDTO, (MoetesakES) documentMap.get(moetesakDTO.getId()));

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
    resetEsMock();

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
    resetEsMock();

    // Clean up
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
    resetEsMock();
    compareMoetemappe(moetemappeDTO, (MoetemappeES) documentMap.get(moetemappeDTO.getId()));
    var moetesakDTO = moetemappeDTO.getMoetesak().get(0).getExpandedObject();
    compareMoetesak(moetesakDTO, (MoetesakES) documentMap.get(moetesakDTO.getId()));

    response = delete("/moetedokument/" + moetemappeDTO.getMoetedokument().get(0).getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = get("/moetemappe/" + moetemappeDTO.getId());
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    // One Moetemappe should be reindexed
    documentMap = captureIndexedDocuments(1);
    compareMoetemappe(moetemappeDTO, (MoetemappeES) documentMap.get(moetemappeDTO.getId()));

    // No documents should be deleted (Moetedokument isn't a separate entity in ES)
    captureDeletedDocuments(0);
    resetEsMock();

    // Clean up
    response = delete("/moetemappe/" + moetemappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Make sure the Moetemappe & Moetesak is deleted
    var deletedDocuments = captureDeletedDocuments(2);
    assertTrue(deletedDocuments.contains(moetemappeDTO.getId()));
    assertTrue(deletedDocuments.contains(moetesakDTO.getId()));
  }

  @Test
  void testMoetemappeWithAdmEnhet() throws Exception {
    var moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.remove("moetesak");
    moetemappeJSON.put("utvalg", "UNDER");
    var response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", moetemappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    // Should have indexed one Moetemappe
    var documentMap = captureIndexedDocuments(1);
    resetEsMock();
    var moetemappeES = (MoetemappeES) documentMap.get(moetemappeDTO.getId());
    compareMoetemappe(moetemappeDTO, moetemappeES);

    var journalenhetDTO = gson.fromJson(get("/enhet/" + journalenhetId).getBody(), EnhetDTO.class);
    var underenhetDTO = gson.fromJson(get("/enhet/" + underenhetId).getBody(), EnhetDTO.class);

    assertEquals(
        List.of(underenhetDTO.getExternalId(), journalenhetDTO.getExternalId(), rootEnhetIri),
        moetemappeES.getArkivskaperTransitive());
    assertEquals(
        List.of(underenhetDTO.getNavn(), journalenhetDTO.getNavn(), rootEnhetNavn),
        moetemappeES.getArkivskaperNavn());

    // Clean up
    response = delete("/moetemappe/" + moetemappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNull(moetemappeRepository.findById(moetemappeDTO.getId()).orElse(null));

    // Should have deleted one Moetemappe
    var deletedDocuments = captureDeletedDocuments(1);
    assertTrue(deletedDocuments.contains(moetemappeDTO.getId()));
  }
}

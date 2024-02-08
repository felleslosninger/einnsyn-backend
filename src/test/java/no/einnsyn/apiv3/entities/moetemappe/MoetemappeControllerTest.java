package no.einnsyn.apiv3.entities.moetemappe;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import no.einnsyn.apiv3.entities.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.enhet.models.EnhetDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class MoetemappeControllerTest extends EinnsynControllerTestBase {

  ArkivDTO arkivDTO;

  @BeforeAll
  void setUp() throws Exception {
    var arkivJSON = getArkivJSON();
    var response = post("/arkiv", arkivJSON);
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
  }

  @AfterAll
  void tearDown() throws Exception {
    var response = delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkiv/" + arkivDTO.getId()).getStatusCode());
  }

  @Test
  void testMoetemappeLifecycle() throws Exception {
    var moetemappeJSON = getMoetemappeJSON();
    var response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", moetemappeJSON);
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    var moetemappeId = moetemappeDTO.getId();
    assertNotNull(moetemappeId);
    assertEquals(moetemappeJSON.getString("moetenummer"), moetemappeDTO.getMoetenummer());
    assertEquals(moetemappeJSON.getString("moetedato"), moetemappeDTO.getMoetedato());
    assertEquals(moetemappeJSON.getString("moetested"), moetemappeDTO.getMoetested());
    assertEquals(moetemappeJSON.getString("videoLink"), moetemappeDTO.getVideoLink());
    assertEquals("Moetemappe", moetemappeDTO.getEntity());
    assertEquals(arkivDTO.getId(), moetemappeDTO.getParent().getId());

    moetemappeJSON.put("moetenummer", "1111");
    response = put("/moetemappe/" + moetemappeDTO.getId(), moetemappeJSON);
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    assertEquals(moetemappeJSON.getString("moetenummer"), moetemappeDTO.getMoetenummer());
    assertEquals("Moetemappe", moetemappeDTO.getEntity());
    assertEquals(moetemappeId, moetemappeDTO.getId());
    assertEquals(arkivDTO.getId(), moetemappeDTO.getParent().getId());

    response = get("/moetemappe/" + moetemappeDTO.getId());
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    assertEquals(moetemappeJSON.getString("moetenummer"), moetemappeDTO.getMoetenummer());
    assertEquals(moetemappeJSON.getString("moetedato"), moetemappeDTO.getMoetedato());
    assertEquals(moetemappeJSON.getString("moetested"), moetemappeDTO.getMoetested());
    assertEquals(moetemappeJSON.getString("videoLink"), moetemappeDTO.getVideoLink());
    assertEquals("Moetemappe", moetemappeDTO.getEntity());
    assertEquals(moetemappeId, moetemappeDTO.getId());
    assertEquals(arkivDTO.getId(), moetemappeDTO.getParent().getId());

    response = delete("/moetemappe/" + moetemappeDTO.getId());
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(moetemappeId, moetemappeDTO.getId());
    assertEquals(Boolean.TRUE, moetemappeDTO.getDeleted());

    response = get("/moetemappe/" + moetemappeDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void testUtvalgObjekt() throws Exception {
    var enhet1JSON = getEnhetJSON();
    enhet1JSON.put("enhetskode", "SUBENHET");
    enhet1JSON.put("parent", journalenhetId);
    var response = post("/enhet", enhet1JSON);
    var enhet1DTO = gson.fromJson(response.getBody(), EnhetDTO.class);

    var enhet2JSON = getEnhetJSON();
    enhet2JSON.put("enhetskode", "SUB; A; B ; C ---;;");
    enhet2JSON.put("parent", journalenhetId);
    response = post("/enhet", enhet2JSON);
    var enhet2DTO = gson.fromJson(response.getBody(), EnhetDTO.class);

    var moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.put("utvalg", "SUBENHET");
    response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", moetemappeJSON);
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    var mm1Id = moetemappeDTO.getId();
    assertEquals(enhet1DTO.getId(), moetemappeDTO.getUtvalgObjekt().getId());

    moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.put("utvalg", "SUB");
    response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", moetemappeJSON);
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    var mm2Id = moetemappeDTO.getId();
    assertEquals(enhet2DTO.getId(), moetemappeDTO.getUtvalgObjekt().getId());

    moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.put("utvalg", "A");
    response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", moetemappeJSON);
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    var mm3Id = moetemappeDTO.getId();
    assertEquals(enhet2DTO.getId(), moetemappeDTO.getUtvalgObjekt().getId());

    moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.put("utvalg", "B");
    response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", moetemappeJSON);
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    var mm4Id = moetemappeDTO.getId();
    assertEquals(enhet2DTO.getId(), moetemappeDTO.getUtvalgObjekt().getId());

    moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.put("utvalg", "C ---");
    response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", moetemappeJSON);
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    var mm5Id = moetemappeDTO.getId();
    assertEquals(enhet2DTO.getId(), moetemappeDTO.getUtvalgObjekt().getId());

    moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.put("utvalg", "");
    response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", moetemappeJSON);
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    var mm6Id = moetemappeDTO.getId();
    assertEquals(journalenhetId, moetemappeDTO.getUtvalgObjekt().getId());

    moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.put("utvalg", "nomatch");
    response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", moetemappeJSON);
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    var mm7Id = moetemappeDTO.getId();
    assertEquals(journalenhetId, moetemappeDTO.getUtvalgObjekt().getId());

    // Clean up
    response = delete("/enhet/" + enhet1DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/enhet/" + enhet1DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/moetemappe/" + mm1Id).getStatusCode());
    assertEquals(HttpStatus.OK, get("/moetemappe/" + mm2Id).getStatusCode());
    assertEquals(HttpStatus.OK, get("/moetemappe/" + mm3Id).getStatusCode());
    assertEquals(HttpStatus.OK, get("/moetemappe/" + mm4Id).getStatusCode());
    assertEquals(HttpStatus.OK, get("/moetemappe/" + mm5Id).getStatusCode());
    assertEquals(HttpStatus.OK, get("/moetemappe/" + mm6Id).getStatusCode());
    assertEquals(HttpStatus.OK, get("/moetemappe/" + mm7Id).getStatusCode());

    response = delete("/enhet/" + enhet2DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/enhet/" + enhet2DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/moetemappe/" + mm2Id).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/moetemappe/" + mm3Id).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/moetemappe/" + mm4Id).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/moetemappe/" + mm5Id).getStatusCode());
    assertEquals(HttpStatus.OK, get("/moetemappe/" + mm6Id).getStatusCode());
    assertEquals(HttpStatus.OK, get("/moetemappe/" + mm7Id).getStatusCode());

    response = delete("/moetemappe/" + mm6Id);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/moetemappe/" + mm6Id).getStatusCode());
    assertEquals(HttpStatus.OK, get("/moetemappe/" + mm7Id).getStatusCode());

    response = delete("/moetemappe/" + mm7Id);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/moetemappe/" + mm7Id).getStatusCode());
  }

  @Test
  void testReferanseForrigeNeste() throws Exception {
    var moetemappeJSON = getMoetemappeJSON();
    var result = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", moetemappeJSON);
    var moetemappe1DTO = gson.fromJson(result.getBody(), MoetemappeDTO.class);
    assertNotNull(moetemappe1DTO.getId());
    assertNull(moetemappe1DTO.getReferanseForrigeMoete());
    assertNull(moetemappe1DTO.getReferanseNesteMoete());

    moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.put("referanseForrigeMoete", moetemappe1DTO.getId());
    result = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", moetemappeJSON);
    var moetemappe2DTO = gson.fromJson(result.getBody(), MoetemappeDTO.class);
    assertNotNull(moetemappe2DTO.getId());
    assertEquals(moetemappe1DTO.getId(), moetemappe2DTO.getReferanseForrigeMoete().getId());
    assertNull(moetemappe2DTO.getReferanseNesteMoete());

    moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.put("referanseForrigeMoete", moetemappe2DTO.getId());
    result = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", moetemappeJSON);
    var moetemappe3DTO = gson.fromJson(result.getBody(), MoetemappeDTO.class);
    assertNotNull(moetemappe3DTO.getId());
    assertEquals(moetemappe2DTO.getId(), moetemappe3DTO.getReferanseForrigeMoete().getId());
    assertNull(moetemappe3DTO.getReferanseNesteMoete());

    // Check if moetemappe1DTO has been updated
    result = get("/moetemappe/" + moetemappe1DTO.getId());
    moetemappe1DTO = gson.fromJson(result.getBody(), MoetemappeDTO.class);
    assertNull(moetemappe1DTO.getReferanseForrigeMoete());
    assertEquals(moetemappe2DTO.getId(), moetemappe1DTO.getReferanseNesteMoete().getId());

    // Check if moetemappe2DTO has been updated
    result = get("/moetemappe/" + moetemappe2DTO.getId());
    moetemappe2DTO = gson.fromJson(result.getBody(), MoetemappeDTO.class);
    assertEquals(moetemappe1DTO.getId(), moetemappe2DTO.getReferanseForrigeMoete().getId());
    assertEquals(moetemappe3DTO.getId(), moetemappe2DTO.getReferanseNesteMoete().getId());

    // Clean up
    result = delete("/moetemappe/" + moetemappe1DTO.getId());
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/moetemappe/" + moetemappe1DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, get("/moetemappe/" + moetemappe2DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, get("/moetemappe/" + moetemappe3DTO.getId()).getStatusCode());

    result = delete("/moetemappe/" + moetemappe2DTO.getId());
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/moetemappe/" + moetemappe1DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/moetemappe/" + moetemappe2DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, get("/moetemappe/" + moetemappe3DTO.getId()).getStatusCode());

    result = delete("/moetemappe/" + moetemappe3DTO.getId());
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/moetemappe/" + moetemappe1DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/moetemappe/" + moetemappe2DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/moetemappe/" + moetemappe3DTO.getId()).getStatusCode());
  }

  @Test
  void testInsertWithChildren() throws Exception {
    var moetedokument1JSON = getMoetedokumentJSON();
    var moetedokument2JSON = getMoetedokumentJSON();
    var moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.put(
        "moetedokument", new JSONArray(List.of(moetedokument1JSON, moetedokument2JSON)));

    var moetesak1JSON = getMoetesakJSON();
    var moetesak2JSON = getMoetesakJSON();
    moetemappeJSON.put("moetesak", new JSONArray(List.of(moetesak1JSON, moetesak2JSON)));

    var response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", moetemappeJSON);
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    var moetemappeId = moetemappeDTO.getId();
    assertNotNull(moetemappeId);

    // Clean up
    response = delete("/moetemappe/" + moetemappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    response = get("/moetemappe/" + moetemappeDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void testVariousUpdates() throws Exception {
    var moetemappeJSON = getMoetemappeJSON();
    var response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", moetemappeJSON);
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    var moetemappeId = moetemappeDTO.getId();
    assertNotNull(moetemappeId);

    moetemappeJSON = new JSONObject();
    moetemappeJSON.put("moetenummer", "1111");
    response = put("/moetemappe/" + moetemappeDTO.getId(), moetemappeJSON);
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    assertEquals(moetemappeJSON.getString("moetenummer"), moetemappeDTO.getMoetenummer());
    assertEquals(moetemappeId, moetemappeDTO.getId());

    // Clean up
    response = delete("/moetemappe/" + moetemappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    response = get("/moetemappe/" + moetemappeDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }
}

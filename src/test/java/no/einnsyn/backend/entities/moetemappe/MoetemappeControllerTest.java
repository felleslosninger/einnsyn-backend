package no.einnsyn.backend.entities.moetemappe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.reflect.TypeToken;
import java.util.List;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.responses.models.ListResponseBody;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import no.einnsyn.backend.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.moetesak.models.MoetesakDTO;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MoetemappeControllerTest extends EinnsynControllerTestBase {

  ArkivDTO arkivDTO;
  ArkivdelDTO arkivdelDTO;

  @BeforeAll
  void setUp() throws Exception {
    var arkivJSON = getArkivJSON();
    var response = post("/arkiv", arkivJSON);
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(arkivDTO.getId());

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    assertNotNull(arkivdelDTO.getId());
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
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    var moetemappeId = moetemappeDTO.getId();
    assertNotNull(moetemappeId);
    assertEquals(moetemappeJSON.getString("moetenummer"), moetemappeDTO.getMoetenummer());
    assertEquals(moetemappeJSON.getString("moetedato"), moetemappeDTO.getMoetedato());
    assertEquals(moetemappeJSON.getString("moetested"), moetemappeDTO.getMoetested());
    assertEquals(moetemappeJSON.getString("videoLink"), moetemappeDTO.getVideoLink());
    assertEquals("Moetemappe", moetemappeDTO.getEntity());
    assertEquals(arkivdelDTO.getId(), moetemappeDTO.getArkivdel().getId());

    moetemappeJSON.put("moetenummer", "1111");
    response = patch("/moetemappe/" + moetemappeDTO.getId(), moetemappeJSON);
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    assertEquals(moetemappeJSON.getString("moetenummer"), moetemappeDTO.getMoetenummer());
    assertEquals("Moetemappe", moetemappeDTO.getEntity());
    assertEquals(moetemappeId, moetemappeDTO.getId());
    assertEquals(arkivdelDTO.getId(), moetemappeDTO.getArkivdel().getId());

    response = get("/moetemappe/" + moetemappeDTO.getId());
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    assertEquals(moetemappeJSON.getString("moetenummer"), moetemappeDTO.getMoetenummer());
    assertEquals(moetemappeJSON.getString("moetedato"), moetemappeDTO.getMoetedato());
    assertEquals(moetemappeJSON.getString("moetested"), moetemappeDTO.getMoetested());
    assertEquals(moetemappeJSON.getString("videoLink"), moetemappeDTO.getVideoLink());
    assertEquals("Moetemappe", moetemappeDTO.getEntity());
    assertEquals(moetemappeId, moetemappeDTO.getId());
    assertEquals(arkivdelDTO.getId(), moetemappeDTO.getArkivdel().getId());

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
    var response = post("/enhet/" + journalenhetId + "/underenhet", enhet1JSON);
    var enhet1DTO = gson.fromJson(response.getBody(), EnhetDTO.class);

    var enhet2JSON = getEnhetJSON();
    enhet2JSON.put("enhetskode", "SUB; A; B ; C ---;;");
    response = post("/enhet/" + journalenhetId + "/underenhet", enhet2JSON);
    var enhet2DTO = gson.fromJson(response.getBody(), EnhetDTO.class);

    var moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.put("utvalg", "SUBENHET");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    var mm1Id = moetemappeDTO.getId();
    assertEquals(enhet1DTO.getId(), moetemappeDTO.getUtvalgObjekt().getId());

    moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.put("utvalg", "SUB");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    var mm2Id = moetemappeDTO.getId();
    assertEquals(enhet2DTO.getId(), moetemappeDTO.getUtvalgObjekt().getId());

    moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.put("utvalg", "A");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    var mm3Id = moetemappeDTO.getId();
    assertEquals(enhet2DTO.getId(), moetemappeDTO.getUtvalgObjekt().getId());

    moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.put("utvalg", "B");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    var mm4Id = moetemappeDTO.getId();
    assertEquals(enhet2DTO.getId(), moetemappeDTO.getUtvalgObjekt().getId());

    moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.put("utvalg", "C ---");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    var mm5Id = moetemappeDTO.getId();
    assertEquals(enhet2DTO.getId(), moetemappeDTO.getUtvalgObjekt().getId());
    moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.put("utvalg", "nomatch");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    var mm6Id = moetemappeDTO.getId();
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

    response = delete("/enhet/" + enhet2DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/enhet/" + enhet2DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/moetemappe/" + mm2Id).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/moetemappe/" + mm3Id).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/moetemappe/" + mm4Id).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/moetemappe/" + mm5Id).getStatusCode());

    response = delete("/moetemappe/" + mm6Id);
    assertEquals(HttpStatus.NOT_FOUND, get("/moetemappe/" + mm6Id).getStatusCode());
  }

  @Test
  void testMoetedokument() throws Exception {
    var moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.put("moetedokument", new JSONArray()); // Unset default
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    var moetemappeId = moetemappeDTO.getId();

    var moetedokument1JSON = getMoetedokumentJSON();
    response = post("/moetemappe/" + moetemappeId + "/moetedokument", moetedokument1JSON);
    var moetedokument1DTO = gson.fromJson(response.getBody(), MoetedokumentDTO.class);
    assertEquals(moetedokument1JSON.getString("beskrivelse"), moetedokument1DTO.getBeskrivelse());
    assertEquals(
        moetedokument1JSON.getJSONArray("korrespondansepart").length(),
        moetedokument1DTO.getKorrespondansepart().size());
    assertEquals(
        moetedokument1JSON.getJSONArray("dokumentbeskrivelse").length(),
        moetedokument1DTO.getDokumentbeskrivelse().size());

    var moetedokument2JSON = getMoetedokumentJSON();
    response = post("/moetemappe/" + moetemappeId + "/moetedokument", moetedokument2JSON);
    var moetedokument2DTO = gson.fromJson(response.getBody(), MoetedokumentDTO.class);

    var moetedokument3JSON = getMoetedokumentJSON();
    response = post("/moetemappe/" + moetemappeId + "/moetedokument", moetedokument3JSON);
    var moetedokument3DTO = gson.fromJson(response.getBody(), MoetedokumentDTO.class);

    // Insert another to make sure we're filtering by correct moetemappe
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", getMoetemappeJSON());
    var anotherMoetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    response =
        post(
            "/moetemappe/" + anotherMoetemappeDTO.getId() + "/moetedokument",
            getMoetedokumentJSON());
    var anotherMoetedokumentDTO = gson.fromJson(response.getBody(), MoetedokumentDTO.class);
    assertNotNull(anotherMoetedokumentDTO.getId());

    // DESC
    response = get("/moetemappe/" + moetemappeId + "/moetedokument");
    var type = new TypeToken<ListResponseBody<MoetedokumentDTO>>() {}.getType();
    ListResponseBody<MoetedokumentDTO> moetedokumentDTOList =
        gson.fromJson(response.getBody(), type);
    assertEquals(3, moetedokumentDTOList.getItems().size());
    assertEquals(moetedokument1DTO.getId(), moetedokumentDTOList.getItems().get(2).getId());
    assertEquals(moetedokument2DTO.getId(), moetedokumentDTOList.getItems().get(1).getId());
    assertEquals(moetedokument3DTO.getId(), moetedokumentDTOList.getItems().get(0).getId());

    // DESC startingAfter
    response =
        get(
            "/moetemappe/"
                + moetemappeId
                + "/moetedokument?startingAfter="
                + moetedokument2DTO.getId());
    moetedokumentDTOList = gson.fromJson(response.getBody(), type);
    assertEquals(1, moetedokumentDTOList.getItems().size());
    assertEquals(moetedokument1DTO.getId(), moetedokumentDTOList.getItems().get(0).getId());

    // DESC endingBefore
    response =
        get(
            "/moetemappe/"
                + moetemappeId
                + "/moetedokument?endingBefore="
                + moetedokument2DTO.getId());
    moetedokumentDTOList = gson.fromJson(response.getBody(), type);
    assertEquals(1, moetedokumentDTOList.getItems().size());
    assertEquals(moetedokument3DTO.getId(), moetedokumentDTOList.getItems().get(0).getId());

    // ASC
    response = get("/moetemappe/" + moetemappeId + "/moetedokument?sortOrder=asc");
    moetedokumentDTOList = gson.fromJson(response.getBody(), type);
    assertEquals(3, moetedokumentDTOList.getItems().size());
    assertEquals(moetedokument1DTO.getId(), moetedokumentDTOList.getItems().get(0).getId());
    assertEquals(moetedokument2DTO.getId(), moetedokumentDTOList.getItems().get(1).getId());
    assertEquals(moetedokument3DTO.getId(), moetedokumentDTOList.getItems().get(2).getId());

    // ASC startingAfter
    response =
        get(
            "/moetemappe/"
                + moetemappeId
                + "/moetedokument?sortOrder=asc&startingAfter="
                + moetedokument2DTO.getId());
    moetedokumentDTOList = gson.fromJson(response.getBody(), type);
    assertEquals(1, moetedokumentDTOList.getItems().size());
    assertEquals(moetedokument3DTO.getId(), moetedokumentDTOList.getItems().get(0).getId());

    // ASC endingBefore
    response =
        get(
            "/moetemappe/"
                + moetemappeId
                + "/moetedokument?sortOrder=asc&endingBefore="
                + moetedokument2DTO.getId());
    moetedokumentDTOList = gson.fromJson(response.getBody(), type);
    assertEquals(1, moetedokumentDTOList.getItems().size());
    assertEquals(moetedokument1DTO.getId(), moetedokumentDTOList.getItems().get(0).getId());

    // Clean up
    response = delete("/moetemappe/" + moetemappeId);
    assertEquals(HttpStatus.NOT_FOUND, get("/moetemappe/" + moetemappeId).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/moetemappe/" + moetedokument1DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/moetemappe/" + moetedokument2DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/moetemappe/" + moetedokument3DTO.getId()).getStatusCode());

    response = delete("/moetemappe/" + anotherMoetemappeDTO.getId());
    assertEquals(
        HttpStatus.NOT_FOUND,
        get("/moetemappe/" + anotherMoetedokumentDTO.getId()).getStatusCode());
  }

  @Test
  void testMoetesak() throws Exception {
    var moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.put("moetesak", new JSONArray()); // Unset default
    var result = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    var moetemappeDTO = gson.fromJson(result.getBody(), MoetemappeDTO.class);
    var moetemappeId = moetemappeDTO.getId();

    result = post("/moetemappe/" + moetemappeId + "/moetesak", getMoetesakJSON());
    var moetesak1DTO = gson.fromJson(result.getBody(), MoetesakDTO.class);

    result = post("/moetemappe/" + moetemappeId + "/moetesak", getMoetesakJSON());
    var moetesak2DTO = gson.fromJson(result.getBody(), MoetesakDTO.class);

    result = post("/moetemappe/" + moetemappeId + "/moetesak", getMoetesakJSON());
    var moetesak3DTO = gson.fromJson(result.getBody(), MoetesakDTO.class);

    // Insert another to make sure we're filtering by correct moetemappe
    result = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", getMoetemappeJSON());
    var anotherMoetemappeDTO = gson.fromJson(result.getBody(), MoetemappeDTO.class);
    result = post("/moetemappe/" + anotherMoetemappeDTO.getId() + "/moetesak", getMoetesakJSON());
    var anotherMoetesakDTO = gson.fromJson(result.getBody(), MoetesakDTO.class);
    assertNotNull(anotherMoetesakDTO.getId());

    // DESC
    result = get("/moetemappe/" + moetemappeId + "/moetesak");
    var type = new TypeToken<ListResponseBody<MoetesakDTO>>() {}.getType();
    ListResponseBody<MoetesakDTO> moetesakDTOList = gson.fromJson(result.getBody(), type);
    assertEquals(3, moetesakDTOList.getItems().size());
    assertEquals(moetesak1DTO.getId(), moetesakDTOList.getItems().get(2).getId());
    assertEquals(moetesak2DTO.getId(), moetesakDTOList.getItems().get(1).getId());
    assertEquals(moetesak3DTO.getId(), moetesakDTOList.getItems().get(0).getId());

    // DESC startingAfter
    result = get("/moetemappe/" + moetemappeId + "/moetesak?startingAfter=" + moetesak2DTO.getId());
    moetesakDTOList = gson.fromJson(result.getBody(), type);
    assertEquals(1, moetesakDTOList.getItems().size());
    assertEquals(moetesak1DTO.getId(), moetesakDTOList.getItems().get(0).getId());

    // DESC endingBefore
    result = get("/moetemappe/" + moetemappeId + "/moetesak?endingBefore=" + moetesak2DTO.getId());
    moetesakDTOList = gson.fromJson(result.getBody(), type);
    assertEquals(1, moetesakDTOList.getItems().size());
    assertEquals(moetesak3DTO.getId(), moetesakDTOList.getItems().get(0).getId());

    // ASC
    result = get("/moetemappe/" + moetemappeId + "/moetesak?sortOrder=asc");
    moetesakDTOList = gson.fromJson(result.getBody(), type);
    assertEquals(3, moetesakDTOList.getItems().size());
    assertEquals(moetesak1DTO.getId(), moetesakDTOList.getItems().get(0).getId());
    assertEquals(moetesak2DTO.getId(), moetesakDTOList.getItems().get(1).getId());
    assertEquals(moetesak3DTO.getId(), moetesakDTOList.getItems().get(2).getId());

    // ASC startingAfter
    result =
        get(
            "/moetemappe/"
                + moetemappeId
                + "/moetesak?sortOrder=asc&startingAfter="
                + moetesak2DTO.getId());
    moetesakDTOList = gson.fromJson(result.getBody(), type);
    assertEquals(1, moetesakDTOList.getItems().size());
    assertEquals(moetesak3DTO.getId(), moetesakDTOList.getItems().get(0).getId());

    // ASC endingBefore
    result =
        get(
            "/moetemappe/"
                + moetemappeId
                + "/moetesak?sortOrder=asc&endingBefore="
                + moetesak2DTO.getId());
    moetesakDTOList = gson.fromJson(result.getBody(), type);
    assertEquals(1, moetesakDTOList.getItems().size());
    assertEquals(moetesak1DTO.getId(), moetesakDTOList.getItems().get(0).getId());

    // Clean up
    result = delete("/moetemappe/" + moetemappeId);
    assertEquals(HttpStatus.NOT_FOUND, get("/moetemappe/" + moetemappeId).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/moetesak/" + moetesak1DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/moetesak/" + moetesak2DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/moetesak/" + moetesak3DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, get("/moetemappe/" + anotherMoetemappeDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, get("/moetesak/" + anotherMoetesakDTO.getId()).getStatusCode());

    result = delete("/moetemappe/" + anotherMoetemappeDTO.getId());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/moetemappe/" + anotherMoetemappeDTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/moetesak/" + anotherMoetesakDTO.getId()).getStatusCode());
  }

  @Test
  void testReferanseForrigeNeste() throws Exception {
    var moetemappeJSON = getMoetemappeJSON();
    var result = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    var moetemappe1DTO = gson.fromJson(result.getBody(), MoetemappeDTO.class);
    assertNotNull(moetemappe1DTO.getId());
    assertNull(moetemappe1DTO.getReferanseForrigeMoete());
    assertNull(moetemappe1DTO.getReferanseNesteMoete());

    moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.put("referanseForrigeMoete", moetemappe1DTO.getId());
    result = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    var moetemappe2DTO = gson.fromJson(result.getBody(), MoetemappeDTO.class);
    assertNotNull(moetemappe2DTO.getId());
    assertEquals(moetemappe1DTO.getId(), moetemappe2DTO.getReferanseForrigeMoete().getId());
    assertNull(moetemappe2DTO.getReferanseNesteMoete());

    moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.put("referanseForrigeMoete", moetemappe2DTO.getId());
    result = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
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

    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
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
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    var moetemappeId = moetemappeDTO.getId();
    assertNotNull(moetemappeId);

    moetemappeJSON = new JSONObject();
    moetemappeJSON.put("moetenummer", "1111");
    response = patch("/moetemappe/" + moetemappeDTO.getId(), moetemappeJSON);
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    assertEquals(moetemappeJSON.getString("moetenummer"), moetemappeDTO.getMoetenummer());
    assertEquals(moetemappeId, moetemappeDTO.getId());

    // Clean up
    response = delete("/moetemappe/" + moetemappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    response = get("/moetemappe/" + moetemappeDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void checkLegacyArkivskaperFromJournalenhet() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", getMoetemappeJSON());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    var journalenhet = enhetRepository.findById(journalenhetId).orElse(null);
    var moetemappe = moetemappeRepository.findById(moetemappeDTO.getId()).orElse(null);
    assertEquals(journalenhet.getIri(), moetemappe.getArkivskaper());

    delete("/moetemappe/" + moetemappeDTO.getId());
    assertNull(moetemappeRepository.findById(moetemappeDTO.getId()).orElse(null));
  }

  @Test
  void checkLegacyArkivskaperFromAdmEnhet() throws Exception {
    var moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.put("utvalg", "UNDER");
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    var admEnhet = enhetRepository.findById(underenhetId).orElse(null);
    var moetemappe = moetemappeRepository.findById(moetemappeDTO.getId()).orElse(null);
    assertEquals(admEnhet.getIri(), moetemappe.getArkivskaper());

    delete("/moetemappe/" + moetemappeDTO.getId());
    assertNull(moetemappeRepository.findById(moetemappeDTO.getId()).orElse(null));
  }

  @Test
  void testMoetemappePagination() throws Exception {
    // Add three Moetemappes
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", getMoetemappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappe1DTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", getMoetemappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappe2DTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", getMoetemappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappe3DTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    // Add one to another arkiv
    response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkiv2DTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    response = post("/arkiv/" + arkiv2DTO.getId() + "/arkivdel", getArkivdelJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivdel2DTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    response = post("/arkivdel/" + arkivdel2DTO.getId() + "/moetemappe", getMoetemappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    var type = new TypeToken<ListResponseBody<MoetemappeDTO>>() {}.getType();
    ListResponseBody<MoetemappeDTO> resultList;

    // DESC
    response = get("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe");
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(3, resultList.getItems().size());
    assertEquals(moetemappe3DTO.getId(), resultList.getItems().get(0).getId());
    assertEquals(moetemappe2DTO.getId(), resultList.getItems().get(1).getId());
    assertEquals(moetemappe1DTO.getId(), resultList.getItems().get(2).getId());

    // DESC startingAfter
    response =
        get(
            "/arkivdel/"
                + arkivdelDTO.getId()
                + "/moetemappe?startingAfter="
                + moetemappe2DTO.getId());
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(1, resultList.getItems().size());
    assertEquals(moetemappe1DTO.getId(), resultList.getItems().get(0).getId());

    // DESC endingBefore
    response =
        get(
            "/arkivdel/"
                + arkivdelDTO.getId()
                + "/moetemappe?endingBefore="
                + moetemappe2DTO.getId());
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(1, resultList.getItems().size());
    assertEquals(moetemappe3DTO.getId(), resultList.getItems().get(0).getId());

    // ASC
    response = get("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe?sortOrder=asc");
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(3, resultList.getItems().size());
    assertEquals(moetemappe1DTO.getId(), resultList.getItems().get(0).getId());
    assertEquals(moetemappe2DTO.getId(), resultList.getItems().get(1).getId());
    assertEquals(moetemappe3DTO.getId(), resultList.getItems().get(2).getId());

    // ASC startingAfter
    response =
        get(
            "/arkivdel/"
                + arkivdelDTO.getId()
                + "/moetemappe?sortOrder=asc&startingAfter="
                + moetemappe2DTO.getId());
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(1, resultList.getItems().size());
    assertEquals(moetemappe3DTO.getId(), resultList.getItems().get(0).getId());

    // ASC endingBefore
    response =
        get(
            "/arkivdel/"
                + arkivdelDTO.getId()
                + "/moetemappe?sortOrder=asc&endingBefore="
                + moetemappe2DTO.getId());
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(1, resultList.getItems().size());
    assertEquals(moetemappe1DTO.getId(), resultList.getItems().get(0).getId());

    // Delete the Moetemappe
    assertEquals(HttpStatus.OK, delete("/moetemappe/" + moetemappe1DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/moetemappe/" + moetemappe2DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/moetemappe/" + moetemappe3DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkiv2DTO.getId()).getStatusCode());
  }
}

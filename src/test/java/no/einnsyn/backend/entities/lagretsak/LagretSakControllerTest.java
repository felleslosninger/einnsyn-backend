package no.einnsyn.backend.entities.lagretsak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.authentication.bruker.models.TokenResponse;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.bruker.models.BrukerDTO;
import no.einnsyn.backend.entities.lagretsak.models.LagretSakDTO;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.shaded.com.google.common.reflect.TypeToken;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LagretSakControllerTest extends EinnsynControllerTestBase {

  BrukerDTO brukerDTO;
  ArkivDTO arkivDTO;
  ArkivdelDTO arkivdelDTO;
  SaksmappeDTO saksmappeDTO;
  MoetemappeDTO moetemappeDTO;
  String accessToken;

  @BeforeAll
  void setup() throws Exception {
    // Create user
    var brukerJSON = getBrukerJSON();
    var response = post("/bruker", brukerJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    brukerDTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    var brukerObj = brukerService.findById(brukerDTO.getId());

    // Activate user
    response = patch("/bruker/" + brukerDTO.getId() + "/activate/" + brukerObj.getSecret());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Get token
    var loginRequest = new JSONObject();
    loginRequest.put("username", brukerDTO.getEmail());
    loginRequest.put("password", brukerJSON.getString("password"));
    response = post("/auth/token", loginRequest);
    var tokenResponse = gson.fromJson(response.getBody(), TokenResponse.class);
    accessToken = tokenResponse.getToken();

    // Create arkiv
    response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    // Create Arkivdel
    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    // Create saksmappe
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Create moetemappe
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", getMoetemappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
  }

  @AfterAll
  void cleanup() throws Exception {
    deleteAdmin("/bruker/" + brukerDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, getAdmin("/bruker/" + brukerDTO.getId()).getStatusCode());

    delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, getAdmin("/arkiv/" + arkivDTO.getId()).getStatusCode());
  }

  @Test
  void testLagretSakLifecycle() throws Exception {
    var lagretSakJSON = getLagretSakJSON();
    lagretSakJSON.put("saksmappe", saksmappeDTO.getId());

    // Unauthorized add, not logged in
    var response = post("/bruker/" + brukerDTO.getId() + "/lagretSak", lagretSakJSON);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Logged in
    response = post("/bruker/" + brukerDTO.getId() + "/lagretSak", lagretSakJSON, accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var lagretSakDTO = gson.fromJson(response.getBody(), LagretSakDTO.class);

    // Unauthorized get, not logged in
    response = get("/lagretSak/" + lagretSakDTO.getId());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Get
    response = get("/lagretSak/" + lagretSakDTO.getId(), accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Unauthorized delete, not logged in
    response = deleteAnon("/lagretSak/" + lagretSakDTO.getId());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Delete
    response = delete("/lagretSak/" + lagretSakDTO.getId(), accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Get
    response = get("/lagretSak/" + lagretSakDTO.getId(), accessToken);
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void testLagretSakPagination() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    var saksmappe1DTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe1DTO.getId());

    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    var saksmappe2DTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe2DTO.getId());

    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    var saksmappe3DTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe3DTO.getId());

    var lagretSakJSON = getLagretSakJSON();
    lagretSakJSON.put("saksmappe", saksmappe1DTO.getId());
    response = post("/bruker/" + brukerDTO.getId() + "/lagretSak", lagretSakJSON, accessToken);
    var lagretSak1DTO = gson.fromJson(response.getBody(), LagretSakDTO.class);
    assertNotNull(lagretSak1DTO.getId());

    lagretSakJSON.put("saksmappe", saksmappe2DTO.getId());
    response = post("/bruker/" + brukerDTO.getId() + "/lagretSak", lagretSakJSON, accessToken);
    var lagretSak2DTO = gson.fromJson(response.getBody(), LagretSakDTO.class);
    assertNotNull(lagretSak2DTO.getId());

    lagretSakJSON.put("saksmappe", saksmappe3DTO.getId());
    response = post("/bruker/" + brukerDTO.getId() + "/lagretSak", lagretSakJSON, accessToken);
    var lagretSak3DTO = gson.fromJson(response.getBody(), LagretSakDTO.class);
    assertNotNull(lagretSak3DTO.getId());

    var type = new TypeToken<PaginatedList<LagretSakDTO>>() {}.getType();
    PaginatedList<LagretSakDTO> resultList;

    // DESC
    response = get("/bruker/" + brukerDTO.getId() + "/lagretSak", accessToken);
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(3, resultList.getItems().size());
    assertEquals(lagretSak3DTO.getId(), resultList.getItems().get(0).getId());
    assertEquals(lagretSak2DTO.getId(), resultList.getItems().get(1).getId());
    assertEquals(lagretSak1DTO.getId(), resultList.getItems().get(2).getId());

    // DESC startingAfter
    response =
        get(
            "/bruker/" + brukerDTO.getId() + "/lagretSak?startingAfter=" + lagretSak2DTO.getId(),
            accessToken);
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(1, resultList.getItems().size());
    assertEquals(lagretSak1DTO.getId(), resultList.getItems().get(0).getId());

    // DESC endingBefore
    response =
        get(
            "/bruker/" + brukerDTO.getId() + "/lagretSak?endingBefore=" + lagretSak2DTO.getId(),
            accessToken);
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(1, resultList.getItems().size());
    assertEquals(lagretSak3DTO.getId(), resultList.getItems().get(0).getId());

    // ASC
    response = get("/bruker/" + brukerDTO.getId() + "/lagretSak?sortOrder=asc", accessToken);
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(3, resultList.getItems().size());
    assertEquals(lagretSak1DTO.getId(), resultList.getItems().get(0).getId());
    assertEquals(lagretSak2DTO.getId(), resultList.getItems().get(1).getId());
    assertEquals(lagretSak3DTO.getId(), resultList.getItems().get(2).getId());

    // ASC startingAfter
    response =
        get(
            "/bruker/"
                + brukerDTO.getId()
                + "/lagretSak?sortOrder=asc&startingAfter="
                + lagretSak2DTO.getId(),
            accessToken);
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(1, resultList.getItems().size());
    assertEquals(lagretSak3DTO.getId(), resultList.getItems().get(0).getId());

    // ASC endingBefore
    response =
        get(
            "/bruker/"
                + brukerDTO.getId()
                + "/lagretSak?sortOrder=asc&endingBefore="
                + lagretSak2DTO.getId(),
            accessToken);
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(1, resultList.getItems().size());
    assertEquals(lagretSak1DTO.getId(), resultList.getItems().get(0).getId());

    // Delete the LagretSaks
    assertEquals(
        HttpStatus.OK, delete("/lagretSak/" + lagretSak1DTO.getId(), accessToken).getStatusCode());
    assertEquals(
        HttpStatus.OK, delete("/lagretSak/" + lagretSak2DTO.getId(), accessToken).getStatusCode());
    assertEquals(
        HttpStatus.OK, delete("/lagretSak/" + lagretSak3DTO.getId(), accessToken).getStatusCode());

    // Delete Saksmappe
    assertEquals(HttpStatus.OK, delete("/saksmappe/" + saksmappe1DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/saksmappe/" + saksmappe2DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/saksmappe/" + saksmappe3DTO.getId()).getStatusCode());
  }
}

package no.einnsyn.apiv3.entities.lagretsoek;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.authentication.bruker.models.TokenResponse;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.bruker.models.BrukerDTO;
import no.einnsyn.apiv3.entities.lagretsoek.models.LagretSoekDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
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
class LagretSoekControllerTest extends EinnsynControllerTestBase {

  BrukerDTO brukerDTO;
  ArkivDTO arkivDTO;
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
    response = put("/bruker/" + brukerDTO.getId() + "/activate/" + brukerObj.getSecret());
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

    // Create saksmappe
    response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Create moetemappe
    response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", getMoetemappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
  }

  @AfterAll
  void cleanup() throws Exception {
    delete("/bruker/" + brukerDTO.getId());
    delete("/arkiv/" + arkivDTO.getId());
  }

  @Test
  void testLagretSoekLifecycle() throws Exception {
    var LagretSoekJSON = getLagretSoekJSON();

    // Unauthorized add, not logged in
    var response = post("/bruker/" + brukerDTO.getId() + "/lagretSoek", LagretSoekJSON);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Logged in
    response = post("/bruker/" + brukerDTO.getId() + "/lagretSoek", LagretSoekJSON, accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var LagretSoekDTO = gson.fromJson(response.getBody(), LagretSoekDTO.class);

    // Unauthorized get, not logged in
    response = get("/lagretSoek/" + LagretSoekDTO.getId());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Get
    response = get("/lagretSoek/" + LagretSoekDTO.getId(), accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Unauthorized delete, not logged in
    response = delete("/lagretSoek/" + LagretSoekDTO.getId());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Delete
    response = delete("/lagretSoek/" + LagretSoekDTO.getId(), accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Get
    response = get("/lagretSoek/" + LagretSoekDTO.getId(), accessToken);
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void testLagretSoekPagination() throws Exception {

    var lagretSoekJSON = getLagretSoekJSON();
    lagretSoekJSON.put("label", "soek1");
    var response =
        post("/bruker/" + brukerDTO.getId() + "/lagretSoek", lagretSoekJSON, accessToken);
    var LagretSoek1DTO = gson.fromJson(response.getBody(), LagretSoekDTO.class);
    assertNotNull(LagretSoek1DTO.getId());

    lagretSoekJSON.put("label", "soek2");
    response = post("/bruker/" + brukerDTO.getId() + "/lagretSoek", lagretSoekJSON, accessToken);
    var LagretSoek2DTO = gson.fromJson(response.getBody(), LagretSoekDTO.class);
    assertNotNull(LagretSoek2DTO.getId());

    lagretSoekJSON.put("label", "soek3");
    response = post("/bruker/" + brukerDTO.getId() + "/lagretSoek", lagretSoekJSON, accessToken);
    var LagretSoek3DTO = gson.fromJson(response.getBody(), LagretSoekDTO.class);
    assertNotNull(LagretSoek3DTO.getId());

    var type = new TypeToken<ResultList<LagretSoekDTO>>() {}.getType();
    ResultList<LagretSoekDTO> resultList;

    // DESC
    response = get("/bruker/" + brukerDTO.getId() + "/lagretSoek", accessToken);
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(3, resultList.getItems().size());
    assertEquals(LagretSoek3DTO.getId(), resultList.getItems().get(0).getId());
    assertEquals(LagretSoek2DTO.getId(), resultList.getItems().get(1).getId());
    assertEquals(LagretSoek1DTO.getId(), resultList.getItems().get(2).getId());

    // DESC startingAfter
    response =
        get(
            "/bruker/" + brukerDTO.getId() + "/lagretSoek?startingAfter=" + LagretSoek2DTO.getId(),
            accessToken);
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(1, resultList.getItems().size());
    assertEquals(LagretSoek1DTO.getId(), resultList.getItems().get(0).getId());

    // DESC endingBefore
    response =
        get(
            "/bruker/" + brukerDTO.getId() + "/lagretSoek?endingBefore=" + LagretSoek2DTO.getId(),
            accessToken);
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(1, resultList.getItems().size());
    assertEquals(LagretSoek3DTO.getId(), resultList.getItems().get(0).getId());

    // ASC
    response = get("/bruker/" + brukerDTO.getId() + "/lagretSoek?sortOrder=asc", accessToken);
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(3, resultList.getItems().size());
    assertEquals(LagretSoek1DTO.getId(), resultList.getItems().get(0).getId());
    assertEquals(LagretSoek2DTO.getId(), resultList.getItems().get(1).getId());
    assertEquals(LagretSoek3DTO.getId(), resultList.getItems().get(2).getId());

    // ASC startingAfter
    response =
        get(
            "/bruker/"
                + brukerDTO.getId()
                + "/lagretSoek?sortOrder=asc&startingAfter="
                + LagretSoek2DTO.getId(),
            accessToken);
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(1, resultList.getItems().size());
    assertEquals(LagretSoek3DTO.getId(), resultList.getItems().get(0).getId());

    // ASC endingBefore
    response =
        get(
            "/bruker/"
                + brukerDTO.getId()
                + "/lagretSoek?sortOrder=asc&endingBefore="
                + LagretSoek2DTO.getId(),
            accessToken);
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(1, resultList.getItems().size());
    assertEquals(LagretSoek1DTO.getId(), resultList.getItems().get(0).getId());

    // Delete the LagretSoeks
    assertEquals(
        HttpStatus.OK,
        delete("/lagretSoek/" + LagretSoek1DTO.getId(), accessToken).getStatusCode());
    assertEquals(
        HttpStatus.OK,
        delete("/lagretSoek/" + LagretSoek2DTO.getId(), accessToken).getStatusCode());
    assertEquals(
        HttpStatus.OK,
        delete("/lagretSoek/" + LagretSoek3DTO.getId(), accessToken).getStatusCode());
  }
}

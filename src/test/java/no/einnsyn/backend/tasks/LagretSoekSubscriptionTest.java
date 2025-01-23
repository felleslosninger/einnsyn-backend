package no.einnsyn.backend.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import jakarta.mail.internet.MimeMessage;
import java.util.function.Function;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.authentication.bruker.models.TokenResponse;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.bruker.models.BrukerDTO;
import no.einnsyn.backend.entities.lagretsoek.models.LagretSoekDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.awaitility.Awaitility;
import org.json.JSONObject;
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
class LagretSoekSubscriptionTest extends EinnsynControllerTestBase {

  @Autowired LagretSakSoekSubscriptionTestService lagretSakSoekSubscriptionTestService;

  ArkivDTO arkivDTO;
  ArkivdelDTO arkivdelDTO;
  BrukerDTO brukerDTO;
  String accessToken;

  @BeforeAll
  public void setup() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    // Create user
    var brukerJSON = getBrukerJSON();
    response = post("/bruker", brukerJSON);
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
  }

  @AfterAll
  public void tearDown() throws Exception {
    delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkiv/" + arkivDTO.getId()).getStatusCode());

    deleteAdmin("/bruker/" + brukerDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, getAdmin("/bruker/" + brukerDTO.getId()).getStatusCode());
  }

  @SuppressWarnings("unchecked")
  @Test
  void testMatchingLagretSoek() throws Exception {
    // Create a LagretSoek
    var response =
        post("/bruker/" + brukerDTO.getId() + "/lagretSoek", getLagretSoekJSON(), accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var lagretSoekDTO = gson.fromJson(response.getBody(), LagretSoekDTO.class);

    // Await until indexed
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(1)).index(any(Function.class)));
    resetEs();

    // Refresh percolator index
    esClient.indices().refresh(r -> r.index(percolatorIndex));

    // No emails should have been sent
    verify(javaMailSender, never()).createMimeMessage();
    verify(javaMailSender, never()).send(any(MimeMessage.class));

    // Add a saksmappe with a title that matches the LagretSoek ("foo")
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("offentligTittel", "foo");
    saksmappeJSON.put("offentligTittelSensitiv", "foo");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Await until indexed
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(1)).index(any(Function.class)));
    resetEs();
    awaitSideEffects();

    // Should send one mail after calling notifyLagretSoek()
    lagretSakSoekSubscriptionTestService.notifyLagretSoek();
    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(1)).send(any(MimeMessage.class)));

    // Delete the Saksmappe
    response = delete("/saksmappe/" + saksmappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Delete the LagretSoek
    response = delete("/lagretSoek/" + lagretSoekDTO.getId(), accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}

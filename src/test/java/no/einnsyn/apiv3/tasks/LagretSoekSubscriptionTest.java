package no.einnsyn.apiv3.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.function.Function;
import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.authentication.bruker.models.TokenResponse;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.bruker.models.BrukerDTO;
import no.einnsyn.apiv3.entities.lagretsoek.models.LagretSoekDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.apiv3.testutils.ElasticsearchMocks;
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

  private ArkivDTO arkivDTO;
  private BrukerDTO brukerDTO;
  private String accessToken;

  @BeforeAll
  public void setup() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

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
    var lagretSoek = lagretSoekService.findById(lagretSoekDTO.getId());

    // Make elasticsearch match the LagretSoek
    var legacyId = lagretSoek.getLegacyId().toString();
    var response1 = ElasticsearchMocks.searchResponse(1, List.of(legacyId));
    var response2 = ElasticsearchMocks.searchResponse(0, List.of());
    when(esClient.search(any(SearchRequest.class), any()))
        .thenReturn(response1)
        .thenReturn(response2);

    // No emails should have been sent
    verify(javaMailSender, never()).createMimeMessage();
    verify(javaMailSender, never()).send(any(MimeMessage.class));

    // Add a saksmappe
    response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Await until indexed
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(1)).index(any(Function.class)));
    resetEsMock();

    lagretSakSoekSubscriptionTestService.notifyLagretSoek();

    // Should have sent one mail
    Awaitility.await().untilAsserted(() -> verify(javaMailSender, times(1)).createMimeMessage());
    verify(javaMailSender, times(1)).send(any(MimeMessage.class));

    // Delete the Saksmappe
    response = delete("/saksmappe/" + saksmappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Delete the LagretSoek
    response = delete("/lagretSoek/" + lagretSoekDTO.getId(), accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}

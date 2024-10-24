package no.einnsyn.apiv3.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import jakarta.mail.internet.MimeMessage;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.authentication.bruker.models.TokenResponse;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.bruker.models.BrukerDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import org.awaitility.Awaitility;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LagretSakSubscriptionTest extends EinnsynControllerTestBase {

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
    response = put("/bruker/" + brukerDTO.getId() + "/activate/" + brukerObj.getSecret());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Get token
    var loginRequest = new JSONObject();
    loginRequest.put("username", brukerDTO.getEmail());
    loginRequest.put("password", brukerJSON.getString("password"));
    response = post("/auth/token", loginRequest);
    var tokenResponse = gson.fromJson(response.getBody(), TokenResponse.class);
    accessToken = tokenResponse.getToken();
  }

  @SuppressWarnings("unchecked")
  @Test
  void testLagretSaksmappeSubscription() throws Exception {
    // Create a Saksmappe
    var saksmappeJSON = getSaksmappeJSON();
    var response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Create a lagretSak
    var lagretSakJSON = getLagretSakJSON();
    lagretSakJSON.put("saksmappe", saksmappeDTO.getId());
    response = post("/bruker/" + brukerDTO.getId() + "/lagretSak", lagretSakJSON, accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    // Update the Saksmappe
    saksmappeJSON.put("offentligTittel", "Updated tittel");
    response = put("/saksmappe/" + saksmappeDTO.getId(), saksmappeJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Await until indexed
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(1)).index(any(Function.class)));
    resetEsMock();

    lagretSakSoekSubscriptionTestService.notifyLagretSak();

    Awaitility.await().untilAsserted(() -> verify(javaMailSender, times(1)).createMimeMessage());
    verify(javaMailSender, times(1)).send(any(MimeMessage.class));

    // Add a Journalpost to the Saksmappe
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", getJournalpostJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    // Await until indexed
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(1)).index(any(Function.class)));
    resetEsMock();

    lagretSakSoekSubscriptionTestService.notifyLagretSak();

    Awaitility.await().untilAsserted(() -> verify(javaMailSender, times(2)).createMimeMessage());
    verify(javaMailSender, times(2)).send(any(MimeMessage.class));

    // Delete the Saksmappe
    response = delete("/saksmappe/" + saksmappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @SuppressWarnings("unchecked")
  @Test
  void testLagretMoetemappeSubscription() throws Exception {
    // Create a Moetemappe
    var moetemappeJSON = getMoetemappeJSON();
    var response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", moetemappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    // Create a lagretSak
    var lagretSakJSON = getLagretSakJSON();
    lagretSakJSON.put("moetemappe", moetemappeDTO.getId());
    response = post("/bruker/" + brukerDTO.getId() + "/lagretSak", lagretSakJSON, accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    // Update the Moetemappe
    moetemappeJSON.put("offentligTittel", "Updated tittel");
    response = put("/moetemappe/" + moetemappeDTO.getId(), moetemappeJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Await until indexed
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(1)).index(any(Function.class)));
    resetEsMock();

    lagretSakSoekSubscriptionTestService.notifyLagretSak();

    Awaitility.await().untilAsserted(() -> verify(javaMailSender, times(1)).createMimeMessage());
    verify(javaMailSender, times(1)).send(any(MimeMessage.class));

    // Add a Moetesak to the Moetemappe
    response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", getMoetesakJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    // Await until indexed
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(1)).index(any(Function.class)));
    resetEsMock();

    lagretSakSoekSubscriptionTestService.notifyLagretSak();

    Awaitility.await().untilAsserted(() -> verify(javaMailSender, times(2)).createMimeMessage());
    verify(javaMailSender, times(2)).send(any(MimeMessage.class));

    // Delete the Moetemappe
    response = delete("/moetemappe/" + moetemappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @SuppressWarnings("unchecked")
  @Test
  void testLagretMoeteAndSaksmappeSubscription() throws Exception {
    // Create a saksmappe
    var saksmappeJSON = getSaksmappeJSON();
    var response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Create a moetemappe
    var moetemappeJSON = getMoetemappeJSON();
    response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", moetemappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    // Create a LagretSak (Saksmappe)
    var lagretSakJSON = getLagretSakJSON();
    lagretSakJSON.put("saksmappe", saksmappeDTO.getId());
    response = post("/bruker/" + brukerDTO.getId() + "/lagretSak", lagretSakJSON, accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    // Create a LagretSak (Moetemappe)
    var lagretMoeteJSON = getLagretSakJSON();
    lagretMoeteJSON.put("moetemappe", moetemappeDTO.getId());
    response = post("/bruker/" + brukerDTO.getId() + "/lagretSak", lagretMoeteJSON, accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    // Update the saksmappe
    saksmappeJSON.put("offentligTittel", "Updated tittel");
    response = put("/saksmappe/" + saksmappeDTO.getId(), saksmappeJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Update the moetemappe
    moetemappeJSON.put("offentligTittel", "Updated tittel");
    response = put("/moetemappe/" + moetemappeDTO.getId(), moetemappeJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Await until indexed
    Awaitility.await().untilAsserted(() -> verify(esClient, atLeast(1)).index(any(Function.class)));
    resetEsMock();

    lagretSakSoekSubscriptionTestService.notifyLagretSak();

    waiter.await(50, TimeUnit.MILLISECONDS);
    Awaitility.await().untilAsserted(() -> verify(javaMailSender, times(2)).createMimeMessage());
    verify(javaMailSender, times(2)).send(any(MimeMessage.class));

    // Delete the saksmappe
    response = delete("/saksmappe/" + saksmappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Delete the moetemappe
    response = delete("/moetemappe/" + moetemappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}

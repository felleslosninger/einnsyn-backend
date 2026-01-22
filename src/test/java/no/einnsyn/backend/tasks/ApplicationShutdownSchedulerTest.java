package no.einnsyn.backend.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import no.einnsyn.backend.EinnsynLegacyElasticTestBase;
import no.einnsyn.backend.authentication.bruker.models.TokenResponse;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.bruker.models.BrukerDTO;
import no.einnsyn.backend.entities.innsynskravbestilling.InnsynskravBestillingTestService;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestillingDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.backend.utils.ApplicationShutdownListenerService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApplicationShutdownSchedulerTest extends EinnsynLegacyElasticTestBase {

  @Autowired private TaskTestService taskTestService;
  @Autowired private InnsynskravBestillingTestService innsynskravTestService;
  @MockitoBean private ApplicationShutdownListenerService applicationShutdownListenerService;

  /**
   * Test that reindexing stops when application is shutting down
   *
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  @Test
  void reindexShutdownListenerTest() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    // Add ten documents, fail to index one of them
    doThrow(new IOException("Failed to index document"))
        .doCallRealMethod()
        .when(esClient)
        .index(any(Function.class));
    for (var i = 0; i < 10; i++) {
      response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    // We should have tried to index 10 documents
    captureIndexedDocuments(10);
    resetEs();

    // Shutting down
    when(applicationShutdownListenerService.isShuttingDown()).thenReturn(true);
    taskTestService.updateOutdatedDocuments();
    awaitSideEffects();
    captureIndexedDocuments(0);

    // Not shutting down
    when(applicationShutdownListenerService.isShuttingDown()).thenReturn(false);
    taskTestService.updateOutdatedDocuments();
    awaitSideEffects();
    captureIndexedDocuments(1);

    delete("/arkiv/" + arkivDTO.getId());
    captureDeletedDocuments(10);
  }

  @SuppressWarnings("unchecked")
  @Test
  void removeStaleShutdownListenerTest() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    // Add saksmappe
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    captureIndexedDocuments(1);
    esClient.indices().refresh(r -> r.index(elasticsearchIndex));

    // Remove document from database so it becomes stale in ES
    doThrow(new IOException("Failed to delete document"))
        .when(esClient)
        .delete(any(Function.class));
    delete("/saksmappe/" + saksmappeDTO.getId());
    captureDeletedDocuments(1);
    resetEs();
    awaitSideEffects();

    // Shutting down
    when(applicationShutdownListenerService.isShuttingDown()).thenReturn(true);
    taskTestService.removeStaleDocuments();
    awaitSideEffects();
    captureBulkDeletedDocuments(0, 0);

    // Not shutting down
    when(applicationShutdownListenerService.isShuttingDown()).thenReturn(false);
    taskTestService.removeStaleDocuments();
    awaitSideEffects();
    captureBulkDeletedDocuments(1, 1);

    delete("/arkiv/" + arkivDTO.getId());
  }

  @Test
  void subscriptionShutdownListenerTest() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    // Create user
    var brukerJSON = getBrukerJSON();
    response = post("/bruker", brukerJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var brukerDTO = gson.fromJson(response.getBody(), BrukerDTO.class);
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
    var accessToken = tokenResponse.getToken();

    // Create a Saksmappe
    var saksmappeJSON = getSaksmappeJSON();
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Create a lagretSak
    var lagretSakJSON = getLagretSakJSON();
    lagretSakJSON.put("saksmappe", saksmappeDTO.getId());
    response = post("/bruker/" + brukerDTO.getId() + "/lagretSak", lagretSakJSON, accessToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    // Update the Saksmappe
    saksmappeJSON.put("offentligTittel", "Updated tittel");
    response = patch("/saksmappe/" + saksmappeDTO.getId(), saksmappeJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    resetMail();

    // Shutting down
    when(applicationShutdownListenerService.isShuttingDown()).thenReturn(true);
    taskTestService.notifyLagretSak();
    awaitSideEffects();
    verify(javaMailSender, times(0)).send(any(MimeMessage.class));

    // Not shutting down
    when(applicationShutdownListenerService.isShuttingDown()).thenReturn(false);
    taskTestService.notifyLagretSak();
    awaitSideEffects();
    verify(javaMailSender, times(1)).send(any(MimeMessage.class));

    // Clean up
    response = delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = delete("/bruker/" + brukerDTO.getId(), accessToken);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void innsynskravCleanupShutdownListenerTest() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    // Insert Saksmappe
    var saksmappeJSON = getSaksmappeJSON();
    var saksmappeResponse = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    var saksmappeDTO = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappeDTO);

    // Insert Journalpost to Saksmappe
    var jp = getJournalpostJSON();
    var journalpostResponse = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", jp);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    var journalpostDTO = gson.fromJson(journalpostResponse.getBody(), JournalpostDTO.class);
    assertNotNull(journalpostDTO);

    // Add InnsynskravBestilling as anonymous
    var bestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("journalpost", journalpostDTO.getId());
    bestillingJSON.put("innsynskrav", new JSONArray().put(innsynskravJSON));

    // Create InnsynskravBestilling
    response = post("/innsynskravBestilling", bestillingJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var bestillingDTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    assertNotNull(bestillingDTO);
    var bestillingId = bestillingDTO.getId();

    // Verify the InnsynskravBestilling
    var verificationSecret = innsynskravTestService.getVerificationSecret(bestillingId);
    response =
        patch("/innsynskravBestilling/" + bestillingId + "/verify/" + verificationSecret, null);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    bestillingDTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    assertNotNull(bestillingDTO);
    assertEquals(true, bestillingDTO.getVerified());

    // Set _created to yesterday
    taskTestService.modifyInnsynskravBestillingCreatedDate(bestillingId, -2, ChronoUnit.DAYS);

    // Shutting down, bestilling should not be deleted
    when(applicationShutdownListenerService.isShuttingDown()).thenReturn(true);
    taskTestService.cleanOldInnsynskravBestillings();
    awaitSideEffects();
    response = getAdmin("/innsynskravBestilling/" + bestillingId);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Not shutting down, bestilling should be deleted
    when(applicationShutdownListenerService.isShuttingDown()).thenReturn(false);
    taskTestService.cleanOldInnsynskravBestillings();
    awaitSideEffects();
    response = getAdmin("/innsynskravBestilling/" + bestillingId);
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    // Clean up
    response = deleteAdmin("/innsynskrav/" + bestillingDTO.getInnsynskrav().get(0).getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = deleteAdmin("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}

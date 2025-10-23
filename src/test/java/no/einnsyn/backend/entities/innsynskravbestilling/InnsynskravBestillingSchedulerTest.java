package no.einnsyn.backend.entities.innsynskravbestilling;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.internet.MimeMessage;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestillingDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.backend.tasks.TaskTestService;
import no.einnsyn.clients.ip.exceptions.IPConnectionException;
import org.awaitility.Awaitility;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
      "application.innsynskravRetryInterval=1",
      "application.innsynskravAnonymousMaxAge=1"
    })
@ActiveProfiles("test")
class InnsynskravBestillingSchedulerTest extends EinnsynControllerTestBase {

  @Lazy @Autowired private InnsynskravBestillingTestService innsynskravTestService;

  ArkivDTO arkivDTO;
  ArkivdelDTO arkivdelDTO;
  @Autowired private TaskTestService taskTestService;

  @BeforeEach
  void resetMocks() {
    Mockito.reset(ipSender);
  }

  @BeforeAll
  void addArkiv() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
  }

  @AfterAll
  void removeArkiv() throws Exception {
    delete("/arkiv/" + arkivDTO.getId());
  }

  @Test
  void testSchedulerWhenEformidlingIsDownOnce() throws Exception {
    // Insert Saksmappe
    var saksmappeJSON = getSaksmappeJSON();
    var saksmappeResponse = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    var saksmappeDTO = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);

    // Insert Journalpost to Saksmappe
    var jp = getJournalpostJSON();
    var journalpostResponse = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", jp);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    var journalpost = gson.fromJson(journalpostResponse.getBody(), JournalpostDTO.class);

    // Create InnsynskravBestilling
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("journalpost", journalpost.getId());
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray().put(innsynskravJSON));
    var response = post("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    assertEquals(1, innsynskravBestillingDTO.getInnsynskrav().size());

    // Wait until the user confirmation email is sent
    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(1)).send(any(MimeMessage.class)));
    reset(javaMailSender);

    // Make IPSender fail the first time, then succed the second time
    when(ipSender.sendInnsynskrav(
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(Integer.class)))
        .thenThrow(new IPConnectionException("", null))
        .thenReturn("test");

    // Verify innsynskravBestilling, and that the innsynskrav isn't sent
    var verificationSecret =
        innsynskravTestService.getVerificationSecret(innsynskravBestillingDTO.getId());
    response =
        patch(
            "/innsynskravBestilling/"
                + innsynskravBestillingDTO.getId()
                + "/verify/"
                + verificationSecret,
            null);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var innsynskravBestilling2DTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    assertEquals(1, innsynskravBestilling2DTO.getInnsynskrav().size());

    innsynskravTestService.assertNotSent(innsynskravBestilling2DTO.getId());

    // Wait until the user confirmation email is sent
    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(1)).send(any(MimeMessage.class)));
    reset(javaMailSender);

    // One call to IPSender
    verify(ipSender, atLeast(1))
        .sendInnsynskrav(
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(Integer.class));

    // The first innsynskrav should not be sent
    innsynskravTestService.assertNotSent(innsynskravBestillingDTO.getId());

    // The second one should succeed
    innsynskravTestService.triggerScheduler();

    // Still no email, but one more call to IPSender
    Awaitility.await()
        .untilAsserted(
            () ->
                verify(ipSender, atLeast(2))
                    .sendInnsynskrav(
                        any(String.class),
                        any(String.class),
                        any(String.class),
                        any(String.class),
                        any(String.class),
                        any(String.class),
                        any(String.class),
                        any(Integer.class)));
    verify(javaMailSender, times(0)).send(any(MimeMessage.class));

    // The innsynskrav should be sent
    Awaitility.await()
        .untilAsserted(() -> innsynskravTestService.assertSent(innsynskravBestillingDTO.getId()));

    // Wait one more interval, there should be no more calls to IPSender
    innsynskravTestService.triggerScheduler();
    Awaitility.await()
        .pollDelay(100, TimeUnit.MILLISECONDS)
        .untilAsserted(
            () ->
                verify(ipSender, atLeast(2))
                    .sendInnsynskrav(
                        any(String.class),
                        any(String.class),
                        any(String.class),
                        any(String.class),
                        any(String.class),
                        any(String.class),
                        any(String.class),
                        any(Integer.class)));

    // Delete InnsynskravBestilling
    var innsynskravResponse4 =
        deleteAdmin("/innsynskravBestilling/" + innsynskravBestilling2DTO.getId());
    assertEquals(HttpStatus.OK, innsynskravResponse4.getStatusCode());

    // Delete saksmappe (with journalposts)
    delete("/saksmappe/" + saksmappeDTO.getId());
  }

  @Test
  @Commit
  void fallbackToEmailWhenEformidlingIsDown() throws Exception {
    // Insert Saksmappe
    var saksmappeJSON = getSaksmappeJSON();
    var saksmappeResponse = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    var saksmappeDTO = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);

    // Insert Journalpost to Saksmappe
    var journalpostJSON = getJournalpostJSON();
    var journalpostResponse =
        post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    var journalpostDTO = gson.fromJson(journalpostResponse.getBody(), JournalpostDTO.class);

    // Create InnsynskravBestilling
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("journalpost", journalpostDTO.getId());
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray().put(innsynskravJSON));
    var innsynskravResponse = post("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.CREATED, innsynskravResponse.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(innsynskravResponse.getBody(), InnsynskravBestillingDTO.class);
    assertEquals(1, innsynskravBestillingDTO.getInnsynskrav().size());

    // Wait until the user confirmation email is sent
    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(1)).send(any(MimeMessage.class)));
    reset(javaMailSender);

    // Make IPSender always fail
    when(ipSender.sendInnsynskrav(
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(Integer.class)))
        .thenThrow(new IPConnectionException("", null));

    // Verify innsynskravBestilling, and that the innsynskrav isn't sent
    var verificationSecret =
        innsynskravTestService.getVerificationSecret(innsynskravBestillingDTO.getId());
    innsynskravResponse =
        patch(
            "/innsynskravBestilling/"
                + innsynskravBestillingDTO.getId()
                + "/verify/"
                + verificationSecret,
            null);
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    innsynskravBestillingDTO =
        gson.fromJson(innsynskravResponse.getBody(), InnsynskravBestillingDTO.class);
    assertEquals(1, innsynskravBestillingDTO.getInnsynskrav().size());
    innsynskravTestService.assertNotSent(innsynskravBestillingDTO.getId());

    // Wait until the user confirmation email is sent
    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(1)).send(any(MimeMessage.class)));
    reset(javaMailSender);

    // There should be one more email sent and three (failed) calls to
    // IPSender
    innsynskravTestService.triggerScheduler(); // eFormidling try #2
    innsynskravTestService.triggerScheduler(); // eFormidling try #3
    innsynskravTestService.triggerScheduler(); // email fallback

    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(1)).send(any(MimeMessage.class)));
    Awaitility.await()
        .untilAsserted(
            () ->
                verify(ipSender, atLeast(3))
                    .sendInnsynskrav(
                        any(String.class),
                        any(String.class),
                        any(String.class),
                        any(String.class),
                        any(String.class),
                        any(String.class),
                        any(String.class),
                        any(Integer.class)));

    // Verify that the InnsynskravBestilling was sent
    innsynskravTestService.assertSent(innsynskravBestillingDTO.getId());

    // Delete InnsynskravBestilling
    var innsynskravResponse5 =
        deleteAdmin("/innsynskravBestilling/" + innsynskravBestillingDTO.getId());
    assertEquals(HttpStatus.OK, innsynskravResponse5.getStatusCode());

    // Delete saksmappe
    delete("/saksmappe/" + saksmappeDTO.getId());
  }

  @Test
  void testOneFailingAndOneWorkingInnsynskravSending() throws Exception {
    // Insert Saksmappe
    var saksmappeJSON = getSaksmappeJSON();
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Insert Journalpost to Saksmappe
    var jp = getJournalpostJSON();
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", jp);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpost = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Insert Journalpost belonging to another Enhet to saksmappe
    var jp2 = getJournalpostJSON();
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", jp2, journalenhet2Key);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpost2 = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Create InnsynskravBestilling
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("journalpost", journalpost.getId());
    var innsynskrav2JSON = getInnsynskravJSON();
    innsynskrav2JSON.put("journalpost", journalpost2.getId());
    innsynskravBestillingJSON.put(
        "innsynskrav", new JSONArray().put(innsynskravJSON).put(innsynskrav2JSON));
    response = post("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    var innsynskravBestillingId = innsynskravBestillingDTO.getId();
    assertEquals(2, innsynskravBestillingDTO.getInnsynskrav().size());

    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(1)).send(any(MimeMessage.class)));
    reset(javaMailSender);

    // Make IPSender fail on journalenhet2, only once
    var journalenhet2 = enhetRepository.findById(journalenhet2Id).orElse(null);
    when(ipSender.sendInnsynskrav(
            any(String.class),
            any(String.class),
            eq(journalenhet2.getOrgnummer()),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(Integer.class)))
        .thenThrow(new IPConnectionException("", null))
        .thenReturn("");

    // Make it work on journalenhet
    var journalenhet = enhetRepository.findById(journalenhetId).orElse(null);
    when(ipSender.sendInnsynskrav(
            any(String.class),
            any(String.class),
            eq(journalenhet.getOrgnummer()),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(Integer.class)))
        .thenReturn("");

    // Verify innsynskravBestilling, and that one innsynskrav isn't sent
    var verificationSecret = innsynskravTestService.getVerificationSecret(innsynskravBestillingId);
    response =
        patch(
            "/innsynskravBestilling/" + innsynskravBestillingId + "/verify/" + verificationSecret,
            null);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravBestillingDTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    assertEquals(2, innsynskravBestillingDTO.getInnsynskrav().size());

    // The second innsynskrav (to journalenhet2) should be sent
    Awaitility.await()
        .untilAsserted(() -> innsynskravTestService.assertSent(innsynskravBestillingId, 1));
    // The first one should still not be sent
    innsynskravTestService.assertNotSent(innsynskravBestillingId, 0);

    // Verify that the order confirmation email is sent
    verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    reset(javaMailSender);

    // Verify that IPSender is called once for each innsynskrav
    verify(ipSender, times(1))
        .sendInnsynskrav(
            any(String.class),
            any(String.class),
            eq(journalenhet.getOrgnummer()),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(Integer.class));
    verify(ipSender, times(1))
        .sendInnsynskrav(
            any(String.class),
            any(String.class),
            eq(journalenhet2.getOrgnummer()),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(Integer.class));

    // Run scheduler, there should be one more call to IPSender for journalenhet2
    innsynskravTestService.triggerScheduler();
    Awaitility.await()
        .pollDelay(100, TimeUnit.MILLISECONDS)
        .untilAsserted(
            () ->
                verify(ipSender, times(1))
                    .sendInnsynskrav(
                        any(String.class),
                        any(String.class),
                        eq(journalenhet.getOrgnummer()),
                        any(String.class),
                        any(String.class),
                        any(String.class),
                        any(String.class),
                        any(Integer.class)));
    Awaitility.await()
        .untilAsserted(
            () ->
                verify(ipSender, times(2))
                    .sendInnsynskrav(
                        any(String.class),
                        any(String.class),
                        eq(journalenhet2.getOrgnummer()),
                        any(String.class),
                        any(String.class),
                        any(String.class),
                        any(String.class),
                        any(Integer.class)));

    // No more emails should be sent
    verify(javaMailSender, times(0)).send(any(MimeMessage.class));

    // Verify that the last innsynskrav is sent
    innsynskravTestService.assertSent(innsynskravBestillingId, 0);
    innsynskravTestService.assertSent(innsynskravBestillingId, 1);

    // Delete InnsynskravBestilling
    var innsynskravResponse4 = deleteAdmin("/innsynskravBestilling/" + innsynskravBestillingId);
    assertEquals(HttpStatus.OK, innsynskravResponse4.getStatusCode());

    // Delete saksmappe
    var deleteResponse = deleteAdmin("/saksmappe/" + saksmappeDTO.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
  }

  @Test
  void schedulerShouldAnonymizeOldInnsynskravBestillingsByGuestUsers() throws Exception {
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

    // add some InnsynskravBestilling as guest
    var bestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("journalpost", journalpostDTO.getId());
    bestillingJSON.put("innsynskrav", new JSONArray().put(innsynskravJSON));

    // Create InnsynskravBestilling
    var bestillingResponse1 = post("/innsynskravBestilling", bestillingJSON);
    assertEquals(HttpStatus.CREATED, bestillingResponse1.getStatusCode());
    var bestillingDTO1 =
        gson.fromJson(bestillingResponse1.getBody(), InnsynskravBestillingDTO.class);
    assertNotNull(bestillingDTO1);
    var bestillingId1 = bestillingDTO1.getId();

    // Verify the InnsynskravBestilling
    var verificationSecret = innsynskravTestService.getVerificationSecret(bestillingId1);
    bestillingResponse1 =
        patch("/innsynskravBestilling/" + bestillingId1 + "/verify/" + verificationSecret, null);
    assertEquals(HttpStatus.OK, bestillingResponse1.getStatusCode());
    bestillingDTO1 = gson.fromJson(bestillingResponse1.getBody(), InnsynskravBestillingDTO.class);
    assertNotNull(bestillingDTO1);
    assertEquals(true, bestillingDTO1.getVerified());

    // One with _created = yesterday
    var bestillingResponse2 = post("/innsynskravBestilling", bestillingJSON);
    assertEquals(HttpStatus.CREATED, bestillingResponse2.getStatusCode());
    var bestillingDTO2 =
        gson.fromJson(bestillingResponse2.getBody(), InnsynskravBestillingDTO.class);
    assertNotNull(bestillingDTO2);
    var bestillingId2 = bestillingDTO2.getId();

    var verificationSecret2 = innsynskravTestService.getVerificationSecret(bestillingId2);
    bestillingResponse2 =
        patch("/innsynskravBestilling/" + bestillingId2 + "/verify/" + verificationSecret2, null);
    assertEquals(HttpStatus.OK, bestillingResponse2.getStatusCode());
    bestillingDTO2 = gson.fromJson(bestillingResponse2.getBody(), InnsynskravBestillingDTO.class);
    assertNotNull(bestillingDTO2);
    assertEquals(true, bestillingDTO2.getVerified());
    var bestilling2Innsynskrav = bestillingDTO2.getInnsynskrav();

    taskTestService.modifyInnsynskravBestillingCreatedDate(bestillingId2, -1, ChronoUnit.DAYS);

    // Trigger scheduler method
    taskTestService.cleanOldInnsynskravBestillings();

    // Verify that the old Bestilling has been deleted, while the newer one still exists
    var untouchedBestillingResponse = getAdmin("/innsynskravBestilling/" + bestillingId1);
    assertEquals(HttpStatus.OK, untouchedBestillingResponse.getStatusCode());
    bestillingDTO1 =
        gson.fromJson(untouchedBestillingResponse.getBody(), InnsynskravBestillingDTO.class);
    assertNotNull(bestillingDTO1);
    assertEquals("test@example.com", bestillingDTO1.getEmail());

    var deletedBestillingResponse = getAdmin("/innsynskravBestilling/" + bestillingId2);
    assertEquals(HttpStatus.NOT_FOUND, deletedBestillingResponse.getStatusCode());

    // Cleanup...
    // Delete InnsynskravBestilling
    var innsynskravResponse1 = deleteAdmin("/innsynskravBestilling/" + bestillingId1);
    assertEquals(HttpStatus.OK, innsynskravResponse1.getStatusCode());
    // Delete orphaned Innsynskrav
    for (var innsynskrav : bestilling2Innsynskrav) {
      var innsynskravResponse2 = deleteAdmin("/innsynskrav/" + innsynskrav.getId());
      assertEquals(HttpStatus.OK, innsynskravResponse2.getStatusCode());
    }

    // Delete saksmappe
    var deleteResponse = deleteAdmin("/saksmappe/" + saksmappeDTO.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
  }
}

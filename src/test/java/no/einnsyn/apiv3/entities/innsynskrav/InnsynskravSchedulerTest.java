package no.einnsyn.apiv3.entities.innsynskrav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityManager;
import java.util.concurrent.CountDownLatch;
import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.apiv3.tasks.handlers.innsynskrav.InnsynskravScheduler;
import no.einnsyn.clients.ip.IPSender;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {"application.innsynskravRetryInterval=1"})
@ActiveProfiles("test")
@Commit
class InnsynskravSchedulerTest extends EinnsynControllerTestBase {

  @MockBean IPSender ipSender;
  @Autowired private InnsynskravScheduler innsynskravScheduler;
  @Autowired private EntityManager entityManager;

  private final CountDownLatch waiter = new CountDownLatch(1);

  ArkivDTO arkivDTO;

  @BeforeEach
  void resetMocks() {
    Mockito.reset(ipSender);
  }

  @BeforeAll
  void addArkiv() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
  }

  @AfterAll
  void removeArkiv() throws Exception {
    delete("/arkiv/" + arkivDTO.getId());
  }

  @Test
  void testSchedulerWhenEformidlingIsDownOnce() throws Exception {
    // Insert Saksmappe
    var saksmappeJSON = getSaksmappeJSON();
    var saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    var saksmappeDTO = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);

    // Insert Journalpost to Saksmappe
    var jp = getJournalpostJSON();
    var journalpostResponse = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", jp);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    var journalpost = gson.fromJson(journalpostResponse.getBody(), JournalpostDTO.class);

    // Create Innsynskrav
    var innsynskravJSON = getInnsynskravJSON();
    var innsynskravDelJSON = getInnsynskravDelJSON();
    innsynskravDelJSON.put("journalpost", journalpost.getId());
    innsynskravJSON.put("innsynskravDel", new JSONArray().put(innsynskravDelJSON));
    System.err.println("------- POST INNSYNSKRAV --------");
    var innsynskravResponse = post("/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, innsynskravResponse.getStatusCode());
    var innsynskravDTO = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertEquals(1, innsynskravDTO.getInnsynskravDel().size());
    var innsynskrav = innsynskravRepository.findById(innsynskravDTO.getId()).orElse(null);

    // Wait until the user confirmation email is sent
    Awaitility.await().untilAsserted(() -> verify(javaMailSender, times(1)).createMimeMessage());
    resetJavaMailSenderMock();

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

    // Verify innsynskrav, and that the innsynskravDel isn't sent
    innsynskravResponse =
        put(
            "/innsynskrav/"
                + innsynskrav.getId()
                + "/verify/"
                + innsynskrav.getVerificationSecret(),
            null);
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    var innsynskravResponseDTO = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertEquals(1, innsynskravResponseDTO.getInnsynskravDel().size());

    innsynskravResponse =
        getAdmin("/innsynskrav/" + innsynskravResponseDTO.getId() + "?expand[]=innsynskravDel");
    innsynskravResponseDTO = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertNull(innsynskravResponseDTO.getInnsynskravDel().get(0).getExpandedObject().getSent());
    assertEquals(true, innsynskravResponseDTO.getVerified());

    // Wait until the user confirmation email is sent
    Awaitility.await().untilAsserted(() -> verify(javaMailSender, times(1)).createMimeMessage());
    resetJavaMailSenderMock();

    // The first one should be triggered autmatically, and should fail
    verify(javaMailSender, times(0)).createMimeMessage();
    verify(javaMailSender, times(0)).send(any(MimeMessage.class));
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
    entityManager.refresh(innsynskrav);
    // The first innsynskravDel should not be sent
    assertNull(innsynskrav.getInnsynskravDel().getFirst().getSent());

    // The second one should succeed
    innsynskravScheduler.sendUnsentInnsynskrav();
    // Still no email, but one more call to IPSender
    verify(javaMailSender, times(0)).createMimeMessage();
    verify(javaMailSender, times(0)).send(any(MimeMessage.class));
    verify(ipSender, atLeast(2))
        .sendInnsynskrav(
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(Integer.class));
    entityManager.refresh(innsynskrav);
    // The second innsynskravDel should be sent
    assertNotNull(innsynskrav.getInnsynskravDel().getFirst().getSent());

    // Wait one more interval, there should be no more calls to IPSender
    innsynskravScheduler.sendUnsentInnsynskrav();
    verify(ipSender, atLeast(2))
        .sendInnsynskrav(
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(Integer.class));

    // Delete innsynskrav
    var innsynskravResponse4 = deleteAdmin("/innsynskrav/" + innsynskravResponseDTO.getId());
    assertEquals(HttpStatus.OK, innsynskravResponse4.getStatusCode());

    // Delete saksmappe (with journalposts)
    delete("/saksmappe/" + saksmappeDTO.getId());
  }

  @Test
  @Commit
  void fallbackToEmailWhenEformidlingIsDown() throws Exception {
    // Insert Saksmappe
    var saksmappeJSON = getSaksmappeJSON();
    var saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    var saksmappeDTO = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);

    // Insert Journalpost to Saksmappe
    var journalpostJSON = getJournalpostJSON();
    var journalpostResponse =
        post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    var journalpostDTO = gson.fromJson(journalpostResponse.getBody(), JournalpostDTO.class);

    // Create Innsynskrav
    var innsynskravJSON = getInnsynskravJSON();
    var innsynskravDelJSON = getInnsynskravDelJSON();
    innsynskravDelJSON.put("journalpost", journalpostDTO.getId());
    innsynskravJSON.put("innsynskravDel", new JSONArray().put(innsynskravDelJSON));
    var innsynskravResponse = post("/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, innsynskravResponse.getStatusCode());
    var innsynskravResponseDTO = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertEquals(1, innsynskravResponseDTO.getInnsynskravDel().size());
    var innsynskrav = innsynskravRepository.findById(innsynskravResponseDTO.getId()).orElse(null);

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

    // Verify innsynskrav, and that the innsynskravDel isn't sent
    innsynskravResponse =
        put(
            "/innsynskrav/"
                + innsynskrav.getId()
                + "/verify/"
                + innsynskrav.getVerificationSecret(),
            null);
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    innsynskravResponseDTO = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertEquals(1, innsynskravResponseDTO.getInnsynskravDel().size());

    innsynskravResponse =
        getAdmin("/innsynskrav/" + innsynskravResponseDTO.getId() + "?expand[]=innsynskravDel");
    innsynskravResponseDTO = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertNull(innsynskravResponseDTO.getInnsynskravDel().get(0).getExpandedObject().getSent());
    assertEquals(true, innsynskravResponseDTO.getVerified());

    verify(javaMailSender, times(0)).createMimeMessage();

    // Wait for scheduler to run, there should be one more email sent and three (failed) calls to
    // IPSender
    innsynskravScheduler.sendUnsentInnsynskrav(); // eFormidling try #1
    innsynskravScheduler.sendUnsentInnsynskrav(); // eFormidling try #2
    innsynskravScheduler.sendUnsentInnsynskrav(); // eFormidling try #3
    innsynskravScheduler.sendUnsentInnsynskrav(); // email fallback
    innsynskravScheduler.sendUnsentInnsynskrav();
    verify(javaMailSender, times(3)).createMimeMessage();
    verify(javaMailSender, times(3)).send(any(MimeMessage.class));
    verify(ipSender, atLeast(3))
        .sendInnsynskrav(
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(Integer.class));

    // Verify that the innsynskrav was sent
    var innsynskravResponse4 =
        getAdmin("/innsynskrav/" + innsynskravResponseDTO.getId() + "?expand[]=innsynskravDel");
    assertEquals(HttpStatus.OK, innsynskravResponse4.getStatusCode());
    var innsynskrav4 = gson.fromJson(innsynskravResponse4.getBody(), InnsynskravDTO.class);
    assertEquals(1, innsynskrav4.getInnsynskravDel().size());
    assertNotNull(innsynskrav4.getInnsynskravDel().get(0).getExpandedObject().getSent());

    // Delete innsynskrav
    var innsynskravResponse5 = deleteAdmin("/innsynskrav/" + innsynskravResponseDTO.getId());
    assertEquals(HttpStatus.OK, innsynskravResponse5.getStatusCode());

    // Delete saksmappe
    delete("/saksmappe/" + saksmappeDTO.getId());
  }

  @Test
  void testOneFailingAndOneWorkingInnsynskravSending() throws Exception {
    var mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    // Insert Saksmappe
    var saksmappeJSON = getSaksmappeJSON();
    var saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    var saksmappeDTO = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);

    // Insert Journalpost to Saksmappe
    var jp = getJournalpostJSON();
    var journalpostResponse = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", jp);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    var journalpost = gson.fromJson(journalpostResponse.getBody(), JournalpostDTO.class);

    // Insert Journalpost belonging to another Enhet to saksmappe
    var jp2 = getJournalpostJSON();
    var journalpostResponse2 =
        post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", jp2, journalenhet2Key);
    assertEquals(HttpStatus.CREATED, journalpostResponse2.getStatusCode());
    var journalpost2 = gson.fromJson(journalpostResponse2.getBody(), JournalpostDTO.class);

    // Create Innsynskrav
    var innsynskravJSON = getInnsynskravJSON();
    var innsynskravDelJSON = getInnsynskravDelJSON();
    innsynskravDelJSON.put("journalpost", journalpost.getId());
    var innsynskravDel2JSON = getInnsynskravDelJSON();
    innsynskravDel2JSON.put("journalpost", journalpost2.getId());
    innsynskravJSON.put(
        "innsynskravDel", new JSONArray().put(innsynskravDelJSON).put(innsynskravDel2JSON));
    var innsynskravResponse = post("/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, innsynskravResponse.getStatusCode());
    var innsynskravResponseDTO = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertEquals(2, innsynskravResponseDTO.getInnsynskravDel().size());
    var innsynskrav = innsynskravRepository.findById(innsynskravResponseDTO.getId()).orElse(null);

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

    // Verify innsynskrav, and that one innsynskravDel isn't sent
    innsynskravResponse =
        put(
            "/innsynskrav/"
                + innsynskrav.getId()
                + "/verify/"
                + innsynskrav.getVerificationSecret(),
            null);
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    innsynskravResponseDTO = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertEquals(2, innsynskravResponseDTO.getInnsynskravDel().size());

    var innsynskravId = innsynskravResponseDTO.getId();
    Awaitility.await()
        .untilAsserted(
            () -> {
              var iresponse =
                  getAdmin("/innsynskrav/" + innsynskravId + "?expand[]=innsynskravDel");
              var iDTO = gson.fromJson(iresponse.getBody(), InnsynskravDTO.class);
              for (var innsynskravDel : iDTO.getInnsynskravDel()) {
                if (innsynskravDel
                    .getExpandedObject()
                    .getEnhet()
                    .getId()
                    .equals(journalenhet2.getId())) {
                  assertNull(innsynskravDel.getExpandedObject().getSent());
                } else {
                  assertNotNull(innsynskravDel.getExpandedObject().getSent());
                }
              }
              assertEquals(true, iDTO.getVerified());
            });

    // Verify that two emails were sent, and there were one call to IPSender for each
    // journalenhet
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

    // Run scheduler, there should be one more call to IPSender
    innsynskravScheduler.sendUnsentInnsynskrav();
    verify(javaMailSender, times(2)).createMimeMessage();
    verify(javaMailSender, times(2)).send(mimeMessage);
    verify(ipSender, atLeast(1))
        .sendInnsynskrav(
            any(String.class),
            any(String.class),
            eq(journalenhet.getOrgnummer()),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(Integer.class));
    verify(ipSender, atLeast(2))
        .sendInnsynskrav(
            any(String.class),
            any(String.class),
            eq(journalenhet2.getOrgnummer()),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(Integer.class));

    // Verify that the last innsynskravDel is sent
    var innsynskravResponse3 =
        getAdmin("/innsynskrav/" + innsynskravResponseDTO.getId() + "?expand[]=innsynskravDel");
    assertEquals(HttpStatus.OK, innsynskravResponse3.getStatusCode());
    var innsynskrav3 = gson.fromJson(innsynskravResponse3.getBody(), InnsynskravDTO.class);
    assertEquals(2, innsynskrav3.getInnsynskravDel().size());
    assertNotNull(innsynskrav3.getInnsynskravDel().get(0).getExpandedObject().getSent());
    assertNotNull(innsynskrav3.getInnsynskravDel().get(1).getExpandedObject().getSent());

    // Delete innsynskrav
    var innsynskravResponse4 = deleteAdmin("/innsynskrav/" + innsynskravResponseDTO.getId());
    assertEquals(HttpStatus.OK, innsynskravResponse4.getStatusCode());

    // Delete saksmappe
    var deleteResponse = deleteAdmin("/saksmappe/" + saksmappeDTO.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
  }
}

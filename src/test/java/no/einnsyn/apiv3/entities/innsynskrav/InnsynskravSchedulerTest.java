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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {"application.innsynskravRetryInterval=1"})
@ActiveProfiles("test")
class InnsynskravSchedulerTest extends EinnsynControllerTestBase {

  @MockBean IPSender ipSender;
  @Autowired private InnsynskravScheduler innsynskravScheduler;
  @Autowired private EntityManager entityManager;
  @Autowired private InnsynskravSchedulerTestService innsynskravSchedulerTestService;

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
    var innsynskravResponse = post("/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, innsynskravResponse.getStatusCode());
    var innsynskravDTO = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertEquals(1, innsynskravDTO.getInnsynskravDel().size());

    // Wait until the user confirmation email is sent
    verify(javaMailSender, times(1)).createMimeMessage();
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
    var verificationSecret =
        innsynskravSchedulerTestService.getVerificationSecret(innsynskravDTO.getId());
    innsynskravResponse =
        put("/innsynskrav/" + innsynskravDTO.getId() + "/verify/" + verificationSecret, null);
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    var innsynskravResponseDTO = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertEquals(1, innsynskravResponseDTO.getInnsynskravDel().size());

    innsynskravResponse =
        getAdmin("/innsynskrav/" + innsynskravResponseDTO.getId() + "?expand[]=innsynskravDel");
    innsynskravResponseDTO = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertNull(
        innsynskravResponseDTO.getInnsynskravDel().get(0).getExpandedObject().getSent(),
        "InnsynskravDel should not be sent");
    assertEquals(true, innsynskravResponseDTO.getVerified());

    // Wait until the user confirmation email is sent
    verify(javaMailSender, times(1)).createMimeMessage();
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
    // The first innsynskravDel should not be sent
    innsynskravSchedulerTestService.assertNotSent(innsynskravDTO.getId());

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
    // The innsynskravDel should be sent
    innsynskravSchedulerTestService.assertSent(innsynskravDTO.getId());

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
    var innsynskravDTO = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertEquals(1, innsynskravDTO.getInnsynskravDel().size());

    // Wait until the user confirmation email is sent
    verify(javaMailSender, times(1)).createMimeMessage();
    resetJavaMailSenderMock();

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
    var verificationSecret =
        innsynskravSchedulerTestService.getVerificationSecret(innsynskravDTO.getId());
    innsynskravResponse =
        put("/innsynskrav/" + innsynskravDTO.getId() + "/verify/" + verificationSecret, null);
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    innsynskravDTO = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertEquals(1, innsynskravDTO.getInnsynskravDel().size());
    innsynskravSchedulerTestService.assertNotSent(innsynskravDTO.getId());

    // Wait until the user confirmation email is sent
    verify(javaMailSender, times(1)).createMimeMessage();
    resetJavaMailSenderMock();

    // There should be one more email sent and three (failed) calls to
    // IPSender
    innsynskravScheduler.sendUnsentInnsynskrav(); // eFormidling try #2
    innsynskravScheduler.sendUnsentInnsynskrav(); // eFormidling try #3
    innsynskravScheduler.sendUnsentInnsynskrav(); // email fallback
    verify(javaMailSender, times(1)).createMimeMessage();
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
    innsynskravSchedulerTestService.assertSent(innsynskravDTO.getId());

    // Delete innsynskrav
    var innsynskravResponse5 = deleteAdmin("/innsynskrav/" + innsynskravDTO.getId());
    assertEquals(HttpStatus.OK, innsynskravResponse5.getStatusCode());

    // Delete saksmappe
    delete("/saksmappe/" + saksmappeDTO.getId());
  }

  @Test
  @Transactional
  void testOneFailingAndOneWorkingInnsynskravSending() throws Exception {
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

    verify(javaMailSender, times(1)).createMimeMessage();
    resetJavaMailSenderMock();

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

    entityManager.refresh(innsynskrav);
    var innsynskravDelList = innsynskrav.getInnsynskravDel();
    for (var innsynskravDel : innsynskravDelList) {
      if (innsynskravDel.getEnhet().getId().equals(journalenhet2.getId())) {
        assertNull(innsynskravDel.getSent(), "InnsynskravDel should not be sent");
      } else {
        assertNotNull(innsynskravDel.getSent(), "InnsynskravDel should be sent");
      }
    }

    // Verify that the order confirmation email is sent
    verify(javaMailSender, times(1)).createMimeMessage();
    resetJavaMailSenderMock();

    // Verify that IPSender is called once for each innsynskravDel
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
    verify(ipSender, times(2))
        .sendInnsynskrav(
            any(String.class),
            any(String.class),
            eq(journalenhet2.getOrgnummer()),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(Integer.class));

    // No more emails should be sent
    verify(javaMailSender, times(0)).createMimeMessage();

    // Verify that the last innsynskravDel is sent
    entityManager.refresh(innsynskrav);
    assertEquals(2, innsynskrav.getInnsynskravDel().size());
    assertNotNull(innsynskrav.getInnsynskravDel().getFirst().getSent());
    assertNotNull(innsynskrav.getInnsynskravDel().getLast().getSent());

    // Delete innsynskrav
    var innsynskravResponse4 = deleteAdmin("/innsynskrav/" + innsynskravResponseDTO.getId());
    assertEquals(HttpStatus.OK, innsynskravResponse4.getStatusCode());

    // Delete saksmappe
    var deleteResponse = deleteAdmin("/saksmappe/" + saksmappeDTO.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
  }

  private void assertNotSent(String innsynskravId) throws Exception {
    var innsynskrav = innsynskravRepository.findById(innsynskravId).orElse(null);
    entityManager.refresh(innsynskrav);
    assertNotNull(innsynskrav, "Innsynskrav not found");
    assertNull(innsynskrav.getInnsynskravDel().getFirst().getSent(), "Innsynskrav sent");
  }
}

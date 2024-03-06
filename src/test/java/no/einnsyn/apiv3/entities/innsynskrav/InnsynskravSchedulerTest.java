package no.einnsyn.apiv3.entities.innsynskrav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.clients.ip.IPSender;
import no.einnsyn.clients.ip.exceptions.IPConnectionException;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {"application.innsynskravRetryInterval=500"})
class InnsynskravSchedulerTest extends EinnsynControllerTestBase {

  @MockBean JavaMailSender javaMailSender;
  @MockBean IPSender ipSender;

  private final CountDownLatch waiter = new CountDownLatch(1);

  ArkivDTO arkivDTO;

  @BeforeEach
  void resetMocks() {
    Mockito.reset(ipSender, javaMailSender);
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
    var mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    // Insert Saksmappe
    var saksmappeJSON = getSaksmappeJSON();
    var saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    var saksmappeDTO = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);

    // Insert Journalpost to Saksmappe
    var jp = getJournalpostJSON();
    jp.put("saksmappe", saksmappeDTO.getId());
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
    var innsynskravJ = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertEquals(1, innsynskravJ.getInnsynskravDel().size());
    var innsynskrav = innsynskravRepository.findById(innsynskravJ.getId()).orElse(null);

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
        getAdmin("/innsynskrav/" + innsynskravJ.getId() + "?expand[]=innsynskravDel");
    innsynskravResponseDTO = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertNull(innsynskravResponseDTO.getInnsynskravDel().get(0).getExpandedObject().getSent());
    assertEquals(true, innsynskravResponseDTO.getVerified());

    // Wait for scheduler to run at least once
    waiter.await(1000, TimeUnit.MILLISECONDS);

    // Verify that the innsynskravDel is sent
    innsynskravResponse =
        getAdmin("/innsynskrav/" + innsynskravJ.getId() + "?expand[]=innsynskravDel");
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    innsynskravResponseDTO = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertEquals(1, innsynskravResponseDTO.getInnsynskravDel().size());
    assertNotNull(innsynskravResponseDTO.getInnsynskravDel().get(0).getExpandedObject().getSent());

    // Two emails should be sent
    verify(javaMailSender, times(2)).createMimeMessage();
    verify(javaMailSender, times(2)).send(mimeMessage);

    // Two calls to IPSender (one failed)
    verify(ipSender, times(2))
        .sendInnsynskrav(
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(Integer.class));

    // Wait at least one more tick, make sure no more emails or IPSender calls are made
    waiter.await(1000, TimeUnit.MILLISECONDS);
    verify(javaMailSender, times(2)).createMimeMessage();
    verify(javaMailSender, times(2)).send(mimeMessage);
    verify(ipSender, times(2))
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
    var innsynskravResponse4 = deleteAdmin("/innsynskrav/" + innsynskravJ.getId());
    assertEquals(HttpStatus.OK, innsynskravResponse4.getStatusCode());

    // Delete saksmappe (with journalposts)
    delete("/saksmappe/" + saksmappeDTO.getId());
  }

  @Test
  void fallbackToEmailWhenEformidlingIsDown() throws Exception {
    var mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    // Insert Saksmappe
    var saksmappeJSON = getSaksmappeJSON();
    var saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    var saksmappeDTO = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);

    // Insert Journalpost to Saksmappe
    var journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("saksmappe", saksmappeDTO.getId());
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

    // Wait for scheduler to run, there should be one more email sent and three (failed) calls to
    // IPSender
    waiter.await(4000, TimeUnit.MILLISECONDS);
    verify(javaMailSender, times(3)).createMimeMessage();
    verify(javaMailSender, times(3)).send(mimeMessage);
    verify(ipSender, times(3))
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
    jp.put("saksmappe", saksmappeDTO.getId());
    var journalpostResponse = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", jp);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    var journalpost = gson.fromJson(journalpostResponse.getBody(), JournalpostDTO.class);

    // Insert Journalpost belonging to another Enhet to saksmappe
    var jp2 = getJournalpostJSON();
    var journalpostResponse2 =
        postWithApiKey(
            "/saksmappe/" + saksmappeDTO.getId() + "/journalpost",
            jp2,
            journalenhet2Key,
            journalenhet2Secret);
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

    waiter.await(30, TimeUnit.MILLISECONDS);

    innsynskravResponse =
        getAdmin("/innsynskrav/" + innsynskravResponseDTO.getId() + "?expand[]=innsynskravDel");
    innsynskravResponseDTO = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    for (var innsynskravDel : innsynskravResponseDTO.getInnsynskravDel()) {
      if (innsynskravDel.getExpandedObject().getEnhet().getId().equals(journalenhet2.getId())) {
        assertNull(innsynskravDel.getExpandedObject().getSent());
      } else {
        assertNotNull(innsynskravDel.getExpandedObject().getSent());
      }
    }
    assertEquals(true, innsynskravResponseDTO.getVerified());

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

    // Wait for scheduler to run, and there should be one more call to IPSender
    waiter.await(1200, TimeUnit.MILLISECONDS);
    verify(javaMailSender, times(2)).createMimeMessage();
    verify(javaMailSender, times(2)).send(mimeMessage);
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

package no.einnsyn.apiv3.entities.innsynskrav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import no.einnsyn.apiv3.entities.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravJSON;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostJSON;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeJSON;
import no.einnsyn.clients.ip.IPSender;
import no.einnsyn.clients.ip.exceptions.IPConnectionException;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {"application.retryInnsynskravInterval=500"})
public class InnsynskravSchedulerTest extends EinnsynControllerTestBase {

  @MockBean
  JavaMailSender javaMailSender;

  @MockBean
  IPSender ipSender;

  private final CountDownLatch waiter = new CountDownLatch(1);

  @BeforeEach
  void resetMocks() {
    Mockito.reset(ipSender, javaMailSender);
  }

  @Test
  public void testSchedulerWhenEformidlingIsDownOnce() throws Exception {
    MimeMessage mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    // Insert Saksmappe
    var saksmappeJSON = getSaksmappeJSON();
    var saksmappeResponse = post("/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    var saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeJSON.class);

    // Insert Journalpost to Saksmappe
    var jp = getJournalpostJSON();
    jp.put("saksmappe", saksmappe.getId());
    var journalpostResponse = post("/journalpost", jp);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    var journalpost = gson.fromJson(journalpostResponse.getBody(), JournalpostJSON.class);

    // Create Innsynskrav
    JSONObject innsynskravJSON = getInnsynskravJSON();
    JSONObject innsynskravDelJSON = getInnsynskravDelJSON();
    innsynskravDelJSON.put("journalpost", journalpost.getId());
    innsynskravJSON.put("innsynskravDel", new JSONArray().put(innsynskravDelJSON));
    var innsynskravResponse = post("/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, innsynskravResponse.getStatusCode());
    var innsynskravJ = gson.fromJson(innsynskravResponse.getBody(), InnsynskravJSON.class);
    assertEquals(1, innsynskravJ.getInnsynskravDel().size());
    var innsynskrav = innsynskravRepository.findById(innsynskravJ.getId());

    // Make IPSender fail the first time, then succed the second time
    when(ipSender.sendInnsynskrav(any(String.class), any(String.class), any(String.class),
        any(String.class), any(String.class), any(String.class), any(String.class),
        any(Integer.class))).thenThrow(new IPConnectionException("", null)).thenReturn("test");

    // Verify innsynskrav, and that the innsynskravDel isn't sent
    var innsynskravResponse2 = get("/innsynskrav/" + innsynskrav.getId() + "/verify/"
        + innsynskrav.getVerificationSecret() + "?expand[]=innsynskravDel");
    assertEquals(HttpStatus.OK, innsynskravResponse2.getStatusCode());
    var innsynskrav2 = gson.fromJson(innsynskravResponse2.getBody(), InnsynskravJSON.class);
    assertEquals(1, innsynskrav2.getInnsynskravDel().size());
    assertNull(innsynskrav2.getInnsynskravDel().get(0).getExpandedObject().getSent());
    assertEquals(true, innsynskrav2.getVerified());

    // Verify that two emails were sent
    verify(javaMailSender, times(2)).createMimeMessage();
    verify(javaMailSender, times(2)).send(mimeMessage);

    // One call to IPSender
    verify(ipSender, times(1)).sendInnsynskrav(any(String.class), any(String.class),
        any(String.class), any(String.class), any(String.class), any(String.class),
        any(String.class), any(Integer.class));

    // Wait for scheduler to run
    waiter.await(500, TimeUnit.MILLISECONDS);

    // Verify that the innsynskravDel is sent
    var innsynskravResponse3 =
        get("/innsynskrav/" + innsynskravJ.getId() + "?expand[]=innsynskravDel");
    assertEquals(HttpStatus.OK, innsynskravResponse3.getStatusCode());
    var innsynskrav3 = gson.fromJson(innsynskravResponse3.getBody(), InnsynskravJSON.class);
    assertEquals(1, innsynskrav3.getInnsynskravDel().size());
    assertNotNull(innsynskrav3.getInnsynskravDel().get(0).getExpandedObject().getSent());

    // Still two emails sent
    verify(javaMailSender, times(2)).createMimeMessage();
    verify(javaMailSender, times(2)).send(mimeMessage);

    // One more call to IPSender
    verify(ipSender, times(2)).sendInnsynskrav(any(String.class), any(String.class),
        any(String.class), any(String.class), any(String.class), any(String.class),
        any(String.class), any(Integer.class));

    // Wait one more tick, make sure no more emails or IPSender calls are made
    waiter.await(500, TimeUnit.MILLISECONDS);
    verify(javaMailSender, times(2)).createMimeMessage();
    verify(javaMailSender, times(2)).send(mimeMessage);
    verify(ipSender, times(2)).sendInnsynskrav(any(String.class), any(String.class),
        any(String.class), any(String.class), any(String.class), any(String.class),
        any(String.class), any(Integer.class));

    // Delete innsynskrav
    var innsynskravResponse4 = delete("/innsynskrav/" + innsynskravJ.getId());
    assertEquals(HttpStatus.OK, innsynskravResponse4.getStatusCode());
  }


  @Test
  void fallbackToEmailWhenEformidlingIsDown() throws Exception {
    MimeMessage mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    // Insert Saksmappe
    var saksmappeJSON = getSaksmappeJSON();
    var saksmappeResponse = post("/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    var saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeJSON.class);

    // Insert Journalpost to Saksmappe
    var jp = getJournalpostJSON();
    jp.put("saksmappe", saksmappe.getId());
    var journalpostResponse = post("/journalpost", jp);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    var journalpost = gson.fromJson(journalpostResponse.getBody(), JournalpostJSON.class);

    // Create Innsynskrav
    JSONObject innsynskravJSON = getInnsynskravJSON();
    JSONObject innsynskravDelJSON = getInnsynskravDelJSON();
    innsynskravDelJSON.put("journalpost", journalpost.getId());
    innsynskravJSON.put("innsynskravDel", new JSONArray().put(innsynskravDelJSON));
    var innsynskravResponse = post("/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, innsynskravResponse.getStatusCode());
    var innsynskravJ = gson.fromJson(innsynskravResponse.getBody(), InnsynskravJSON.class);
    assertEquals(1, innsynskravJ.getInnsynskravDel().size());
    var innsynskrav = innsynskravRepository.findById(innsynskravJ.getId());

    // Make IPSender always fail
    when(ipSender.sendInnsynskrav(any(String.class), any(String.class), any(String.class),
        any(String.class), any(String.class), any(String.class), any(String.class),
        any(Integer.class))).thenThrow(new IPConnectionException("", null));

    // Verify innsynskrav, and that the innsynskravDel isn't sent
    var innsynskravResponse2 = get("/innsynskrav/" + innsynskrav.getId() + "/verify/"
        + innsynskrav.getVerificationSecret() + "?expand[]=innsynskravDel");
    assertEquals(HttpStatus.OK, innsynskravResponse2.getStatusCode());
    var innsynskrav2 = gson.fromJson(innsynskravResponse2.getBody(), InnsynskravJSON.class);
    assertEquals(1, innsynskrav2.getInnsynskravDel().size());
    assertNull(innsynskrav2.getInnsynskravDel().get(0).getExpandedObject().getSent());
    assertEquals(true, innsynskrav2.getVerified());

    // Verify that two emails were sent, and there was one call to IPSender
    verify(javaMailSender, times(2)).createMimeMessage();
    verify(javaMailSender, times(2)).send(mimeMessage);
    verify(ipSender, times(1)).sendInnsynskrav(any(String.class), any(String.class),
        any(String.class), any(String.class), any(String.class), any(String.class),
        any(String.class), any(Integer.class));

    // Wait for scheduler to run, and there should be one more call to IPSender
    waiter.await(500, TimeUnit.MILLISECONDS);
    verify(javaMailSender, times(2)).createMimeMessage();
    verify(javaMailSender, times(2)).send(mimeMessage);
    verify(ipSender, times(2)).sendInnsynskrav(any(String.class), any(String.class),
        any(String.class), any(String.class), any(String.class), any(String.class),
        any(String.class), any(Integer.class));

    // Verify that the innsynskravDel still isn't sent
    var innsynskravResponse3 =
        get("/innsynskrav/" + innsynskravJ.getId() + "?expand[]=innsynskravDel");
    assertEquals(HttpStatus.OK, innsynskravResponse3.getStatusCode());
    var innsynskrav3 = gson.fromJson(innsynskravResponse3.getBody(), InnsynskravJSON.class);
    assertEquals(1, innsynskrav3.getInnsynskravDel().size());
    assertNull(innsynskrav3.getInnsynskravDel().get(0).getExpandedObject().getSent());

    // Verify that we're now on the second try
    var innsynskravDelId = innsynskrav3.getInnsynskravDel().get(0).getId();
    var innsynskravDel = innsynskravDelRepository.findById(innsynskravDelId);
    assertEquals(2, innsynskravDel.getRetryCount());

    // Wait for scheduler to run, and there should be one more call to IPSender
    waiter.await(500, TimeUnit.MILLISECONDS);
    verify(javaMailSender, times(2)).createMimeMessage();
    verify(javaMailSender, times(2)).send(mimeMessage);
    verify(ipSender, times(3)).sendInnsynskrav(any(String.class), any(String.class),
        any(String.class), any(String.class), any(String.class), any(String.class),
        any(String.class), any(Integer.class));

    // Wait for scheduler to run, there should be one more email sent
    waiter.await(500, TimeUnit.MILLISECONDS);
    verify(javaMailSender, times(3)).createMimeMessage();
    verify(javaMailSender, times(3)).send(mimeMessage);
    verify(ipSender, times(3)).sendInnsynskrav(any(String.class), any(String.class),
        any(String.class), any(String.class), any(String.class), any(String.class),
        any(String.class), any(Integer.class));

    // Verify that the innsynskrav was sent
    var innsynskravResponse4 =
        get("/innsynskrav/" + innsynskravJ.getId() + "?expand[]=innsynskravDel");
    assertEquals(HttpStatus.OK, innsynskravResponse4.getStatusCode());
    var innsynskrav4 = gson.fromJson(innsynskravResponse4.getBody(), InnsynskravJSON.class);
    assertEquals(1, innsynskrav4.getInnsynskravDel().size());
    assertNotNull(innsynskrav4.getInnsynskravDel().get(0).getExpandedObject().getSent());

    // Delete innsynskrav
    var innsynskravResponse5 = delete("/innsynskrav/" + innsynskravJ.getId());
    assertEquals(HttpStatus.OK, innsynskravResponse5.getStatusCode());
  }


  @Test
  void testOneFailingAndOneWorkingInnsynskravSending() throws Exception {
    MimeMessage mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    // Insert Saksmappe
    var saksmappeJSON = getSaksmappeJSON();
    var saksmappeResponse = post("/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    var saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeJSON.class);

    // Insert Journalpost to Saksmappe
    var jp = getJournalpostJSON();
    jp.put("saksmappe", saksmappe.getId());
    var journalpostResponse = post("/journalpost", jp);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    var journalpost = gson.fromJson(journalpostResponse.getBody(), JournalpostJSON.class);

    // Insert Journalpost belonging to another Enhet to saksmappe
    var jp2 = getJournalpostJSON();
    jp2.put("saksmappe", saksmappe.getId());
    var journalpostResponse2 = post("/journalpost", jp2, journalenhet2Id);
    assertEquals(HttpStatus.CREATED, journalpostResponse2.getStatusCode());
    var journalpost2 = gson.fromJson(journalpostResponse2.getBody(), JournalpostJSON.class);

    // Create Innsynskrav
    JSONObject innsynskravJSON = getInnsynskravJSON();
    JSONObject innsynskravDelJSON = getInnsynskravDelJSON();
    innsynskravDelJSON.put("journalpost", journalpost.getId());
    JSONObject innsynskravDel2JSON = getInnsynskravDelJSON();
    innsynskravDel2JSON.put("journalpost", journalpost2.getId());
    innsynskravJSON.put("innsynskravDel",
        new JSONArray().put(innsynskravDelJSON).put(innsynskravDel2JSON));
    var innsynskravResponse = post("/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, innsynskravResponse.getStatusCode());
    var innsynskravJ = gson.fromJson(innsynskravResponse.getBody(), InnsynskravJSON.class);
    assertEquals(2, innsynskravJ.getInnsynskravDel().size());
    var innsynskrav = innsynskravRepository.findById(innsynskravJ.getId());

    // Make IPSender fail on journalenhet2, only once
    var journalenhet2 = enhetRepository.findById(journalenhet2Id).get();
    when(ipSender.sendInnsynskrav(any(String.class), any(String.class),
        eq(journalenhet2.getOrgnummer()), any(String.class), any(String.class), any(String.class),
        any(String.class), any(Integer.class))).thenThrow(new IPConnectionException("", null))
            .thenReturn("");

    // Make it work on journalenhet
    var journalenhet = enhetRepository.findById(journalenhetId).get();
    when(ipSender.sendInnsynskrav(any(String.class), any(String.class),
        eq(journalenhet.getOrgnummer()), any(String.class), any(String.class), any(String.class),
        any(String.class), any(Integer.class))).thenReturn("");

    // Verify innsynskrav, and that one innsynskravDel isn't sent
    var innsynskravResponse2 = get("/innsynskrav/" + innsynskrav.getId() + "/verify/"
        + innsynskrav.getVerificationSecret() + "?expand[]=innsynskravDel");
    assertEquals(HttpStatus.OK, innsynskravResponse2.getStatusCode());
    var innsynskrav2 = gson.fromJson(innsynskravResponse2.getBody(), InnsynskravJSON.class);
    assertEquals(2, innsynskrav2.getInnsynskravDel().size());
    assertNotNull(innsynskrav2.getInnsynskravDel().get(0).getExpandedObject().getSent());
    assertNull(innsynskrav2.getInnsynskravDel().get(1).getExpandedObject().getSent());
    assertEquals(true, innsynskrav2.getVerified());

    // Verify that two emails were sent, and there were one call to IPSender for each journalenhet
    verify(javaMailSender, times(2)).createMimeMessage();
    verify(javaMailSender, times(2)).send(mimeMessage);
    verify(ipSender, times(1)).sendInnsynskrav(any(String.class), any(String.class),
        eq(journalenhet.getOrgnummer()), any(String.class), any(String.class), any(String.class),
        any(String.class), any(Integer.class));
    verify(ipSender, times(1)).sendInnsynskrav(any(String.class), any(String.class),
        eq(journalenhet2.getOrgnummer()), any(String.class), any(String.class), any(String.class),
        any(String.class), any(Integer.class));

    // Wait for scheduler to run, and there should be one more call to IPSender
    waiter.await(500, TimeUnit.MILLISECONDS);
    verify(javaMailSender, times(2)).createMimeMessage();
    verify(javaMailSender, times(2)).send(mimeMessage);
    verify(ipSender, times(1)).sendInnsynskrav(any(String.class), any(String.class),
        eq(journalenhet.getOrgnummer()), any(String.class), any(String.class), any(String.class),
        any(String.class), any(Integer.class));
    verify(ipSender, times(2)).sendInnsynskrav(any(String.class), any(String.class),
        eq(journalenhet2.getOrgnummer()), any(String.class), any(String.class), any(String.class),
        any(String.class), any(Integer.class));

    // Verify that the last innsynskravDel is sent
    var innsynskravResponse3 =
        get("/innsynskrav/" + innsynskravJ.getId() + "?expand[]=innsynskravDel");
    assertEquals(HttpStatus.OK, innsynskravResponse3.getStatusCode());
    var innsynskrav3 = gson.fromJson(innsynskravResponse3.getBody(), InnsynskravJSON.class);
    assertEquals(2, innsynskrav3.getInnsynskravDel().size());
    assertNotNull(innsynskrav3.getInnsynskravDel().get(0).getExpandedObject().getSent());
    assertNotNull(innsynskrav3.getInnsynskravDel().get(1).getExpandedObject().getSent());

    // Delete innsynskrav
    var innsynskravResponse4 = delete("/innsynskrav/" + innsynskravJ.getId());
    assertEquals(HttpStatus.OK, innsynskravResponse4.getStatusCode());
  }

}

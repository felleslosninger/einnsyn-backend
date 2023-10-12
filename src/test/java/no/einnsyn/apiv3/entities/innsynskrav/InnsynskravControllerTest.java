package no.einnsyn.apiv3.entities.innsynskrav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import no.einnsyn.apiv3.entities.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.innsynskrav.models.Innsynskrav;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravJSON;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDel;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostJSON;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeJSON;
import no.einnsyn.clients.ip.IPSender;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class InnsynskravControllerTest extends EinnsynControllerTestBase {

  @MockBean
  JavaMailSender javaMailSender;

  @MockBean
  IPSender ipSender;

  JournalpostJSON journalpost = null;
  SaksmappeJSON saksmappe = null;

  @Value("${email.from}")
  private String emailFrom;

  private final CountDownLatch waiter = new CountDownLatch(1);


  /**
   * Insert Saksmappe and Journalpost first
   */
  @BeforeAll
  public void setup() throws Exception {
    // Insert Saksmappe
    JSONObject saksmappeJSON = getSaksmappeJSON();
    ResponseEntity<String> saksmappeResponse = post("/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    saksmappe = mapper.readValue(saksmappeResponse.getBody(), SaksmappeJSON.class);

    // Insert Journalpost with saksmappe
    JSONObject jp = getJournalpostJSON();
    jp.put("saksmappe", saksmappe.getId());
    ResponseEntity<String> journalpostResponse = post("/journalpost", jp);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    journalpost = mapper.readValue(journalpostResponse.getBody(), JournalpostJSON.class);

    // TODO: Insert Journalpost belonging to another Enhet

  }


  @Test
  public void testInnsynskravSingleJournalpostUnverifiedUserEFormidling() throws Exception {
    MimeMessage mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    JSONObject innsynskravJSON = getInnsynskravJSON();
    JSONObject innsynskravDelJSON = getInnsynskravDelJSON();
    innsynskravDelJSON.put("journalpost", journalpost.getId());
    innsynskravJSON.put("innsynskravDel", new JSONArray().put(innsynskravDelJSON));

    // Insert Innsynskrav
    ResponseEntity<String> innsynskravResponse = post("/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, innsynskravResponse.getStatusCode());
    InnsynskravJSON innsynskrav =
        mapper.readValue(innsynskravResponse.getBody(), InnsynskravJSON.class);
    assertEquals("test@example.com", innsynskrav.getEpost());
    assertEquals(1, innsynskrav.getInnsynskravDel().size());

    // Check that the Innsynskrav is in the DB
    Innsynskrav innsynskravObject = innsynskravRepository.findById(innsynskrav.getId());
    assertEquals(innsynskrav.getId(), innsynskravObject.getId());

    // Check that InnsynskravService tried to send an email. The email is sent async, so we have to
    // wait a bit
    waiter.await(50, TimeUnit.MILLISECONDS);
    verify(javaMailSender, times(1)).createMimeMessage();
    verify(javaMailSender, times(1)).send(mimeMessage);

    // Verify email content
    var language = innsynskravObject.getLanguage();
    var locale = Locale.forLanguageTag(language);
    var languageBundle = ResourceBundle.getBundle("mailtemplates/confirmAnonymousOrder", locale);
    assertEquals(mimeMessage.getFrom()[0].toString(), new InternetAddress(emailFrom).toString());
    assertEquals(mimeMessage.getHeader("to")[0].toString(), innsynskrav.getEpost());
    assertEquals(mimeMessage.getSubject(), languageBundle.getString("subject"));

    // Check that the InnsynskravDel is in the DB
    InnsynskravDel innsynskravDelObject =
        innsynskravDelRepository.findById(innsynskrav.getInnsynskravDel().get(0).getId());
    assertEquals(innsynskrav.getInnsynskravDel().get(0).getId(), innsynskravDelObject.getId());

    // Verify the Innsynskrav
    innsynskravResponse = get("/innsynskrav/" + innsynskrav.getId() + "/verify/"
        + innsynskravObject.getVerificationSecret());
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    innsynskrav = mapper.readValue(innsynskravResponse.getBody(), InnsynskravJSON.class);
    assertEquals(true, innsynskrav.getVerified());

    // Check that another email was sent


    // Delete the Innsynskrav
    ResponseEntity<String> deleteResponse = delete("/innsynskrav/" + innsynskrav.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    innsynskrav = mapper.readValue(deleteResponse.getBody(), InnsynskravJSON.class);
    assertEquals(true, innsynskrav.getDeleted());

  }

}

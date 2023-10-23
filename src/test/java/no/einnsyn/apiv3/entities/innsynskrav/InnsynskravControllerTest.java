package no.einnsyn.apiv3.entities.innsynskrav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
import jakarta.mail.internet.MimeMultipart;
import jakarta.transaction.Transactional;
import no.einnsyn.apiv3.entities.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.enhet.models.EnhetJSON;
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

  EnhetJSON enhetNoEF = null;
  SaksmappeJSON saksmappe = null;
  JournalpostJSON journalpost = null;
  SaksmappeJSON saksmappeNoEF = null;
  JournalpostJSON journalpostNoEF = null;

  @Value("${email.from}")
  private String emailFrom;

  private final CountDownLatch waiter = new CountDownLatch(1);


  /**
   * Insert Saksmappe and Journalpost first
   */
  @BeforeEach
  void setup() throws Exception {
    // Insert Saksmappe
    var saksmappeJSON = getSaksmappeJSON();
    var saksmappeResponse = post("/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    saksmappe = mapper.readValue(saksmappeResponse.getBody(), SaksmappeJSON.class);

    // Insert Journalpost to Saksmappe
    var jp = getJournalpostJSON();
    jp.put("saksmappe", saksmappe.getId());
    var journalpostResponse = post("/journalpost", jp);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    journalpost = mapper.readValue(journalpostResponse.getBody(), JournalpostJSON.class);

    // Insert an Enhet that does not have eFormidling enabled
    JSONObject enhetNoEfJSON = getEnhetJSON();
    enhetNoEfJSON.put("navn", "EnhetWithNoEFormidling");
    enhetNoEfJSON.put("eFormidling", false);
    var enhetResponse = post("/enhet", enhetNoEfJSON);
    assertEquals(HttpStatus.CREATED, enhetResponse.getStatusCode());
    enhetNoEF = mapper.readValue(enhetResponse.getBody(), EnhetJSON.class);

    // Set the Enhet as the temporary journalenhet
    String journalenhet = EinnsynObjectService.TEMPORARY_ADM_ENHET_ID;
    EinnsynObjectService.TEMPORARY_ADM_ENHET_ID = enhetNoEF.getId();


    // Insert saksmappe owned by the Enhet
    saksmappeJSON = getSaksmappeJSON();
    saksmappeResponse = post("/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    saksmappeNoEF = mapper.readValue(saksmappeResponse.getBody(), SaksmappeJSON.class);

    // Insert Journalpost to saksmappe
    jp = getJournalpostJSON();
    jp.put("saksmappe", saksmappeNoEF.getId());
    journalpostResponse = post("/journalpost", jp);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    journalpostNoEF = mapper.readValue(journalpostResponse.getBody(), JournalpostJSON.class);

    // Revert journalenhet
    EinnsynObjectService.TEMPORARY_ADM_ENHET_ID = journalenhet;
  }


  @AfterEach
  void cleanup() throws Exception {
    // Delete the Innsynskrav
    if (saksmappe != null) {
      delete("/saksmappe/" + saksmappe.getId());
    }
    if (journalpost != null) {
      delete("/journalpost/" + journalpost.getId());
    }
    if (saksmappeNoEF != null) {
      delete("/saksmappe/" + saksmappeNoEF.getId());
    }
    if (journalpostNoEF != null) {
      delete("/journalpost/" + journalpostNoEF.getId());
    }
    if (enhetNoEF != null) {
      delete("/enhet/" + enhetNoEF.getId());
    }
  }


  @BeforeEach
  void resetMocks() {
    Mockito.reset(ipSender, javaMailSender);
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
    var languageBundle = ResourceBundle.getBundle("mailtemplates/mailtemplates", locale);
    assertEquals(mimeMessage.getFrom()[0].toString(), new InternetAddress(emailFrom).toString());
    assertEquals(mimeMessage.getHeader("to")[0].toString(), innsynskrav.getEpost());
    assertEquals(mimeMessage.getSubject(),
        languageBundle.getString("confirmAnonymousOrderSubject"));

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

    var enhet = innsynskravDelObject.getJournalenhet();

    // Verify that IPSender was called
    waiter.await(100, TimeUnit.MILLISECONDS);
    // @formatter:off
    verify(ipSender, times(1)).sendInnsynskrav(
      any(String.class), // Order.xml, should be compared to a precompiled version
      any(String.class), // transaction id
      eq(enhet.getOrgnummer()), // handteresAv
      eq(enhet.getOrgnummer()),
      eq(enhet.getInnsynskravEpost()),
      any(String.class), // mail content
      any(String.class), // IP orgnummer
      any(Integer.class) // expectedResponseTimeoutDays
    );
    // @formatter:on

    // Verify that confirmation email was sent
    verify(javaMailSender, times(2)).createMimeMessage();
    verify(javaMailSender, times(2)).send(mimeMessage);

    // Delete the Innsynskrav
    ResponseEntity<String> deleteResponse = delete("/innsynskrav/" + innsynskrav.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    innsynskrav = mapper.readValue(deleteResponse.getBody(), InnsynskravJSON.class);
    assertEquals(true, innsynskrav.getDeleted());
  }


  @Test
  public void testInnsynskravSingleJournalpostUnverifiedUserEmail() throws Exception {
    MimeMessage mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    JSONObject innsynskravJSON = getInnsynskravJSON();
    JSONObject innsynskravDelJSON = getInnsynskravDelJSON();
    innsynskravDelJSON.put("journalpost", journalpostNoEF.getId());
    innsynskravJSON.put("innsynskravDel", new JSONArray().put(innsynskravDelJSON));

    // Create Innsynskrav
    ResponseEntity<String> innsynskravResponse = post("/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, innsynskravResponse.getStatusCode());
    InnsynskravJSON innsynskrav =
        mapper.readValue(innsynskravResponse.getBody(), InnsynskravJSON.class);

    // Check that the Innsynskrav is in the DB
    Innsynskrav innsynskravObject = innsynskravRepository.findById(innsynskrav.getId());
    assertEquals(innsynskrav.getId(), innsynskravObject.getId());

    // Check that InnsynskravService tried to send an email
    waiter.await(100, TimeUnit.MILLISECONDS);
    verify(javaMailSender, times(1)).createMimeMessage();
    verify(javaMailSender, times(1)).createMimeMessage();
    verify(javaMailSender, times(1)).send(mimeMessage);

    // Verify the Innsynskrav
    innsynskravResponse = get("/innsynskrav/" + innsynskrav.getId() + "/verify/"
        + innsynskravObject.getVerificationSecret());
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    innsynskrav = mapper.readValue(innsynskravResponse.getBody(), InnsynskravJSON.class);
    assertEquals(true, innsynskrav.getVerified());

    // Check that InnsynskravService tried to send two more mails (one to the user and one to the
    // Enhet)
    waiter.await(100, TimeUnit.MILLISECONDS);
    verify(javaMailSender, times(3)).createMimeMessage();
    verify(javaMailSender, times(3)).send(mimeMessage);

    // Verify that eFormidling wasn't used
    verify(ipSender, times(0)).sendInnsynskrav(any(String.class), any(String.class),
        any(String.class), any(String.class), any(String.class), any(String.class),
        any(String.class), any(Integer.class));

    // Delete the Innsynskrav
    ResponseEntity<String> deleteResponse = delete("/innsynskrav/" + innsynskrav.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    innsynskrav = mapper.readValue(deleteResponse.getBody(), InnsynskravJSON.class);
    assertEquals(true, innsynskrav.getDeleted());
  }


  @Test
  @Transactional
  void testInnsynskravUnverifiedUserEformidlingAndEmail() throws Exception {
    MimeMessage mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    JSONObject innsynskravJSON = getInnsynskravJSON();
    JSONObject innsynskravDelJSON = getInnsynskravDelJSON();
    innsynskravDelJSON.put("journalpost", journalpost.getId());
    JSONObject innsynskravDelNoEFJSON = getInnsynskravDelJSON();
    innsynskravDelNoEFJSON.put("journalpost", journalpostNoEF.getId());
    innsynskravJSON.put("innsynskravDel",
        new JSONArray().put(innsynskravDelJSON).put(innsynskravDelNoEFJSON));

    // Create Innsynskrav
    ResponseEntity<String> innsynskravResponse = post("/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, innsynskravResponse.getStatusCode());
    InnsynskravJSON innsynskrav =
        mapper.readValue(innsynskravResponse.getBody(), InnsynskravJSON.class);

    // Check that the Innsynskrav is in the DB
    Innsynskrav innsynskravObject = innsynskravRepository.findById(innsynskrav.getId());
    assertEquals(innsynskrav.getId(), innsynskravObject.getId());
    assertEquals(2, innsynskravObject.getInnsynskravDel().size());

    // Check that InnsynskravService tried to send an email
    waiter.await(100, TimeUnit.MILLISECONDS);
    verify(javaMailSender, times(1)).createMimeMessage();
    verify(javaMailSender, times(1)).send(mimeMessage);

    // Check that InnsynskravSenderService didn't send anything to IPSender
    verify(ipSender, times(0)).sendInnsynskrav(any(String.class), any(String.class),
        any(String.class), any(String.class), any(String.class), any(String.class),
        any(String.class), any(Integer.class));

    // Verify the Innsynskrav
    innsynskravResponse = get("/innsynskrav/" + innsynskrav.getId() + "/verify/"
        + innsynskravObject.getVerificationSecret());
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    innsynskrav = mapper.readValue(innsynskravResponse.getBody(), InnsynskravJSON.class);
    assertEquals(true, innsynskrav.getVerified());

    // Check that InnsynskravService tried to send two more emails
    waiter.await(100, TimeUnit.MILLISECONDS);
    verify(javaMailSender, times(3)).createMimeMessage();
    verify(javaMailSender, times(3)).send(mimeMessage);

    // Check that InnsynskravSenderService sent to IPSender
    // @formatter:off
    verify(ipSender, times(1)).sendInnsynskrav(
      any(String.class),
      any(String.class),
      any(String.class), // HandteresAv
      any(String.class), // Administrativ enhet
      eq("innsynskravepost@example.com"),
      any(String.class), // Email text. TODO: Verify that the journalpost titles are mentioned
      any(String.class),
      any(Integer.class)
    );
    // @formatter:on

    // Delete the Innsynskrav
    ResponseEntity<String> deleteResponse = delete("/innsynskrav/" + innsynskrav.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    innsynskrav = mapper.readValue(deleteResponse.getBody(), InnsynskravJSON.class);
    assertEquals(true, innsynskrav.getDeleted());

    // Verify that the innsynskravDels are deleted
    InnsynskravDel innsynskravDelObject =
        innsynskravDelRepository.findById(innsynskravObject.getInnsynskravDel().get(0).getId());
    assertNull(innsynskravDelObject);
    innsynskravDelObject =
        innsynskravDelRepository.findById(innsynskravObject.getInnsynskravDel().get(1).getId());
    assertNull(innsynskravDelObject);

    // Verify that the innsynskrav is deleted
    innsynskravObject = innsynskravRepository.findById(innsynskrav.getId());
    assertNull(innsynskravObject);
  }


  // Test sending an innsynskrav where a journalpost has been deleted before verifying the
  // innsynskrav
  @Test
  void testInnsynskravWithDeletedJournalpost() throws Exception {
    MimeMessage mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    // Insert saksmappe with two journalposts, one will be deleted
    JSONObject journalpostToKeepJSON = getJournalpostJSON();
    journalpostToKeepJSON.put("offentligTittel", "JournalpostToKeep");
    journalpostToKeepJSON.put("offentligTittelSensitiv", "JournalpostToKeepSensitiv");
    JSONObject journalpostToDeleteJSON = getJournalpostJSON();
    journalpostToDeleteJSON.put("offentligTittel", "journalpostToDelete");
    journalpostToDeleteJSON.put("offentligTittelSensitiv", "journalpostToDeleteSensitiv");
    JSONObject saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("journalpost",
        new JSONArray().put(journalpostToKeepJSON).put(journalpostToDeleteJSON));
    var saksmappeResponse = post("/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    var saksmappe = mapper.readValue(saksmappeResponse.getBody(), SaksmappeJSON.class);
    var journalpostToKeep = saksmappe.getJournalpost().get(0);
    var journalpostToDelete = saksmappe.getJournalpost().get(1);

    JSONObject innsynskravJSON = getInnsynskravJSON();
    JSONObject innsynskravDelToKeepJSON = getInnsynskravDelJSON();
    innsynskravDelToKeepJSON.put("journalpost", journalpostToKeep.getId());
    JSONObject innsynskravDelToDeleteJSON = getInnsynskravDelJSON();
    innsynskravDelToDeleteJSON.put("journalpost", journalpostToDelete.getId());
    innsynskravJSON.put("innsynskravDel",
        new JSONArray().put(innsynskravDelToKeepJSON).put(innsynskravDelToDeleteJSON));

    // Create Innsynskrav
    var innsynskravResponse = post("/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, innsynskravResponse.getStatusCode());
    var innsynskrav = mapper.readValue(innsynskravResponse.getBody(), InnsynskravJSON.class);

    // Check that the Innsynskrav is in the DB
    Innsynskrav innsynskravObject = innsynskravRepository.findById(innsynskrav.getId());
    assertEquals(innsynskrav.getId(), innsynskravObject.getId());

    // Verify that InnsynskravService tried to send an email
    waiter.await(100, TimeUnit.MILLISECONDS);
    verify(javaMailSender, times(1)).createMimeMessage();
    verify(javaMailSender, times(1)).send(mimeMessage);

    // Delete the journalpost that should be sent through eFormidling
    var deleteResponse = delete("/journalpost/" + journalpostToDelete.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    var deletedJournalpost = mapper.readValue(deleteResponse.getBody(), JournalpostJSON.class);
    assertEquals(true, deletedJournalpost.getDeleted());

    // Verify that the journalpost is deleted
    var deletedJournalpostObject = journalpostRepository.findById(deletedJournalpost.getId());
    assertNull(deletedJournalpostObject);

    // Verify the Innsynskrav
    innsynskravResponse = get("/innsynskrav/" + innsynskrav.getId() + "/verify/"
        + innsynskravObject.getVerificationSecret());
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    innsynskrav = mapper.readValue(innsynskravResponse.getBody(), InnsynskravJSON.class);
    assertEquals(true, innsynskrav.getVerified());

    // Check that InnsynskravService tried to send another mail
    waiter.await(50, TimeUnit.MILLISECONDS);
    verify(javaMailSender, times(2)).createMimeMessage();
    verify(javaMailSender, times(2)).send(mimeMessage);

    // Check the content of mimeMessage
    // This is the confirmation mail sent to the user
    var txtContent = getTxtContent(mimeMessage);
    var htmlContent = getHtmlContent(mimeMessage);
    var attachmentContent = getAttachment(mimeMessage);

    assertNull(attachmentContent);
    assertEquals(true,
        txtContent.contains(journalpostToKeepJSON.get("offentligTittel").toString()));
    assertEquals(false,
        txtContent.contains(journalpostToDeleteJSON.get("offentligTittel").toString()));
    assertEquals(true,
        htmlContent.contains(journalpostToKeepJSON.get("offentligTittel").toString()));
    assertEquals(false,
        htmlContent.contains(journalpostToDeleteJSON.get("offentligTittel").toString()));
    // assertEquals(true, attachmentContent
    // .contains("<dokumentnr>" + journalpost.getJournalpostnummer() + "</dokumentnr>"));

    // Check the content of data sent through eFormidling


    // Check that InnsynskravSenderService tried to send through eFormidling
    verify(ipSender, times(1)).sendInnsynskrav(any(String.class), any(String.class),
        any(String.class), any(String.class), any(String.class), any(String.class),
        any(String.class), any(Integer.class));

    // Delete the Innsynskrav
    deleteResponse = delete("/innsynskrav/" + innsynskrav.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    innsynskrav = mapper.readValue(deleteResponse.getBody(), InnsynskravJSON.class);
    assertEquals(true, innsynskrav.getDeleted());

    // Delete the Saksmappe
    deleteResponse = delete("/saksmappe/" + saksmappe.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    saksmappe = mapper.readValue(deleteResponse.getBody(), SaksmappeJSON.class);
    assertEquals(true, saksmappe.getDeleted());
  }


  private String getTxtContent(MimeMessage mimeMessage) throws Exception {
    var content = mimeMessage.getContent();
    var mmContent = (MimeMultipart) content;
    var emailBodyWrapper = mmContent.getBodyPart(0);
    var emailBody = ((MimeMultipart) emailBodyWrapper.getContent()).getBodyPart(0);
    var txtBodyPart = ((MimeMultipart) emailBody.getContent()).getBodyPart(0);
    var txtContent = txtBodyPart.getContent().toString();
    return txtContent;
  }

  private String getHtmlContent(MimeMessage mimeMessage) throws Exception {
    var content = mimeMessage.getContent();
    var mmContent = (MimeMultipart) content;
    var emailBodyWrapper = mmContent.getBodyPart(0);
    var emailBody = ((MimeMultipart) emailBodyWrapper.getContent()).getBodyPart(0);
    var htmlBodyPart = ((MimeMultipart) emailBody.getContent()).getBodyPart(1);
    var htmlContent = new String(htmlBodyPart.getInputStream().readAllBytes(), "UTF-8");
    return htmlContent;
  }

  private String getAttachment(MimeMessage mimeMessage) throws Exception {
    var content = mimeMessage.getContent();
    var mmContent = (MimeMultipart) content;
    if (mmContent.getCount() > 1) {
      var attachment = mmContent.getBodyPart(1);
      return attachment.getContent().toString();
    } else {
      return null;
    }
  }

}

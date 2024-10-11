package no.einnsyn.apiv3.entities.innsynskrav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.reflect.TypeToken;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.authentication.bruker.models.TokenResponse;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.apikey.models.ApiKeyDTO;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.bruker.models.BrukerDTO;
import no.einnsyn.apiv3.entities.enhet.models.EnhetDTO;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.clients.ip.IPSender;
import no.einnsyn.clients.ip.exceptions.IPConnectionException;
import org.awaitility.Awaitility;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class InnsynskravControllerTest extends EinnsynControllerTestBase {

  @MockBean IPSender ipSender;

  @Autowired PlatformTransactionManager transactionManager;

  ArkivDTO arkivDTO;
  EnhetDTO enhetNoEFDTO;
  String enhetNoEFSecretKey;
  SaksmappeDTO saksmappeDTO;
  JournalpostDTO journalpostDTO;
  SaksmappeDTO saksmappeNoEFormidlingDTO;
  JournalpostDTO journalpostNoEFormidlingDTO;
  MimeMessage mimeMessage;

  @Value("${application.email.from}")
  private String emailFrom;

  private final CountDownLatch waiter = new CountDownLatch(1);

  /** Insert Saksmappe and Journalpost first */
  @BeforeEach
  void setup() throws Exception {
    // Insert Saksmappe
    var arkivJSON = getArkivJSON();
    var arkivResponse = post("/arkiv", arkivJSON);
    arkivDTO = gson.fromJson(arkivResponse.getBody(), ArkivDTO.class);
    var saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    saksmappeDTO = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);

    // Insert Journalpost to Saksmappe
    var jp = getJournalpostJSON();
    var journalpostResponse = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", jp);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    journalpostDTO = gson.fromJson(journalpostResponse.getBody(), JournalpostDTO.class);

    // Insert an Enhet that does not have eFormidling enabled
    var enhetNoEfJSON = getEnhetJSON();
    enhetNoEfJSON.put("navn", "EnhetWithNoEFormidling");
    enhetNoEfJSON.put("eFormidling", false);
    var enhetResponse = post("/enhet/" + journalenhetId + "/underenhet", enhetNoEfJSON);
    assertEquals(HttpStatus.CREATED, enhetResponse.getStatusCode());
    enhetNoEFDTO = gson.fromJson(enhetResponse.getBody(), EnhetDTO.class);

    // Create ApiKey for enhetNoEF
    var apiKeyJSON = getApiKeyJSON();
    apiKeyJSON.put("enhet", enhetNoEFDTO.getId());
    var apiKeyResponse = post("/enhet/" + enhetNoEFDTO.getId() + "/apiKey", apiKeyJSON);
    var apiKeyDTO = gson.fromJson(apiKeyResponse.getBody(), ApiKeyDTO.class);
    enhetNoEFSecretKey = apiKeyDTO.getSecretKey();

    // Insert saksmappe owned by the Enhet
    saksmappeResponse =
        post("/arkiv/" + arkivDTO.getId() + "/saksmappe", getSaksmappeJSON(), enhetNoEFSecretKey);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    saksmappeNoEFormidlingDTO = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);

    // Insert Journalpost to saksmappe
    jp = getJournalpostJSON();
    journalpostResponse =
        post(
            "/saksmappe/" + saksmappeNoEFormidlingDTO.getId() + "/journalpost",
            jp,
            enhetNoEFSecretKey);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    journalpostNoEFormidlingDTO =
        gson.fromJson(journalpostResponse.getBody(), JournalpostDTO.class);

    // Create dummy MimeMessage
    mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
  }

  @AfterEach
  void cleanup() throws Exception {
    delete("/saksmappe/" + saksmappeDTO.getId());
    delete("/journalpost/" + journalpostDTO.getId());
    delete("/saksmappe/" + saksmappeNoEFormidlingDTO.getId());
    delete("/journalpost/" + journalpostNoEFormidlingDTO.getId());
    delete("/enhet/" + enhetNoEFDTO.getId());
    delete("/arkiv/" + arkivDTO.getId());
  }

  @BeforeEach
  void resetMocks() {
    Mockito.reset(ipSender, javaMailSender);
  }

  @Test
  void testInnsynskravSingleJournalpostUnverifiedUserEFormidling() throws Exception {
    var innsynskravJSON = getInnsynskravJSON();
    var innsynskravDelJSON = getInnsynskravDelJSON();
    innsynskravDelJSON.put("journalpost", journalpostDTO.getId());
    innsynskravJSON.put("innsynskravDel", new JSONArray().put(innsynskravDelJSON));

    // Insert Innsynskrav
    var innsynskravResponse = post("/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, innsynskravResponse.getStatusCode());
    var innsynskrav = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertEquals("test@example.com", innsynskrav.getEmail());
    assertEquals(1, innsynskrav.getInnsynskravDel().size());

    // Check that the Innsynskrav is in the DB
    var innsynskravObject = innsynskravRepository.findById(innsynskrav.getId()).orElse(null);
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
    assertEquals(mimeMessage.getHeader("to")[0], innsynskrav.getEmail());
    assertEquals(
        mimeMessage.getSubject(), languageBundle.getString("confirmAnonymousOrderSubject"));

    // Check that the InnsynskravDel is in the DB
    var innsynskravDelObject =
        innsynskravDelRepository
            .findById(innsynskrav.getInnsynskravDel().get(0).getId())
            .orElse(null);
    assertEquals(innsynskrav.getInnsynskravDel().get(0).getId(), innsynskravDelObject.getId());

    // Verify the Innsynskrav
    innsynskravResponse =
        put(
            "/innsynskrav/"
                + innsynskrav.getId()
                + "/verify/"
                + innsynskravObject.getVerificationSecret(),
            null);
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    innsynskrav = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskrav.getVerified());

    var enhet = innsynskravDelObject.getEnhet();
    var handteresAv = enhet.getHandteresAv() != null ? enhet.getHandteresAv() : enhet;

    // Verify that IPSender was called
    waiter.await(100, TimeUnit.MILLISECONDS);
    verify(ipSender, times(1))
        .sendInnsynskrav(
            any(String.class), // Order.xml, should be compared to a precompiled version
            any(String.class), // transaction id
            eq(handteresAv.getOrgnummer()), // handteresAv
            eq(enhet.getOrgnummer()),
            eq(enhet.getInnsynskravEpost()),
            any(String.class), // mail content
            any(String.class), // IP orgnummer
            any(Integer.class) // expectedResponseTimeoutDays
            );

    // Verify that confirmation email was sent
    verify(javaMailSender, times(2)).createMimeMessage();
    verify(javaMailSender, times(2)).send(mimeMessage);

    // Delete the Innsynskrav
    var deleteResponse = deleteAdmin("/innsynskrav/" + innsynskrav.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    innsynskrav = gson.fromJson(deleteResponse.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskrav.getDeleted());
  }

  @Test
  void testInnsynskravSingleJournalpostUnverifiedUserEmail() throws Exception {
    var mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    var innsynskravJSON = getInnsynskravJSON();
    var innsynskravDelJSON = getInnsynskravDelJSON();
    innsynskravDelJSON.put("journalpost", journalpostNoEFormidlingDTO.getId());
    innsynskravJSON.put("innsynskravDel", new JSONArray().put(innsynskravDelJSON));

    // Create Innsynskrav
    var innsynskravResponse = post("/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, innsynskravResponse.getStatusCode());
    var innsynskrav = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);

    // Check that the Innsynskrav is in the DB
    var innsynskravObject = innsynskravRepository.findById(innsynskrav.getId()).orElse(null);
    assertEquals(innsynskrav.getId(), innsynskravObject.getId());

    // Check that InnsynskravService tried to send an email
    waiter.await(1000, TimeUnit.MILLISECONDS);
    verify(javaMailSender, times(1)).createMimeMessage();
    verify(javaMailSender, times(1)).send(mimeMessage);

    // Verify the Innsynskrav
    innsynskravResponse =
        put(
            "/innsynskrav/"
                + innsynskrav.getId()
                + "/verify/"
                + innsynskravObject.getVerificationSecret(),
            null);
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    innsynskrav = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskrav.getVerified());

    // Check that InnsynskravService tried to send two more mails (one to the user and one to the
    // Enhet)
    waiter.await(1100, TimeUnit.MILLISECONDS); // Allow retry on update conflict
    verify(javaMailSender, times(3)).createMimeMessage();
    verify(javaMailSender, times(3)).send(mimeMessage);

    // Verify that eFormidling wasn't used
    verify(ipSender, times(0))
        .sendInnsynskrav(
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(Integer.class));

    // Delete the Innsynskrav
    var deleteResponse = deleteAdmin("/innsynskrav/" + innsynskrav.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    innsynskrav = gson.fromJson(deleteResponse.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskrav.getDeleted());
  }

  @Test
  @Transactional
  void testInnsynskravUnverifiedUserEformidlingAndEmail() throws Exception {
    var mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    var innsynskravJSON = getInnsynskravJSON();
    var innsynskravDelJSON = getInnsynskravDelJSON();
    innsynskravDelJSON.put("journalpost", journalpostDTO.getId());
    var innsynskravDelNoEFJSON = getInnsynskravDelJSON();
    innsynskravDelNoEFJSON.put("journalpost", journalpostNoEFormidlingDTO.getId());
    innsynskravJSON.put(
        "innsynskravDel", new JSONArray().put(innsynskravDelJSON).put(innsynskravDelNoEFJSON));

    // Create Innsynskrav
    var innsynskravResponse = post("/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, innsynskravResponse.getStatusCode());
    var innsynskrav = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);

    // Check that the Innsynskrav is in the DB
    var innsynskravObject = innsynskravRepository.findById(innsynskrav.getId()).orElse(null);
    assertEquals(innsynskrav.getId(), innsynskravObject.getId());
    assertEquals(2, innsynskravObject.getInnsynskravDel().size());

    // Check that InnsynskravService tried to send an email
    waiter.await(100, TimeUnit.MILLISECONDS);
    verify(javaMailSender, times(1)).createMimeMessage();
    verify(javaMailSender, times(1)).send(mimeMessage);

    // Check that InnsynskravSenderService didn't send anything to IPSender
    verify(ipSender, times(0))
        .sendInnsynskrav(
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(Integer.class));

    // Verify the Innsynskrav
    innsynskravResponse =
        put(
            "/innsynskrav/"
                + innsynskrav.getId()
                + "/verify/"
                + innsynskravObject.getVerificationSecret(),
            null);
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    innsynskrav = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskrav.getVerified());

    // Check that InnsynskravService tried to send two more emails
    waiter.await(100, TimeUnit.MILLISECONDS);
    verify(javaMailSender, times(3)).createMimeMessage();
    verify(javaMailSender, times(3)).send(mimeMessage);

    // Check that InnsynskravSenderService sent to IPSender
    verify(ipSender, times(1))
        .sendInnsynskrav(
            any(String.class),
            any(String.class),
            any(String.class), // HandteresAv
            any(String.class), // Administrativ enhet
            eq("innsynskravepost@example.com"),
            any(String.class), // Email text. TODO: Verify that the journalpost titles are mentioned
            any(String.class),
            any(Integer.class));

    // Verify that the Innsynskrav and InnsynskravDels are in the DB
    assertEquals(HttpStatus.OK, getAdmin("/innsynskrav/" + innsynskrav.getId()).getStatusCode());
    assertEquals(
        HttpStatus.OK,
        getAdmin("/innsynskravDel/" + innsynskravObject.getInnsynskravDel().get(0).getId())
            .getStatusCode());
    assertEquals(
        HttpStatus.OK,
        getAdmin("/innsynskravDel/" + innsynskravObject.getInnsynskravDel().get(1).getId())
            .getStatusCode());

    // Delete the Innsynskrav
    var deleteResponse = deleteAdmin("/innsynskrav/" + innsynskrav.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    innsynskrav = gson.fromJson(deleteResponse.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskrav.getDeleted());

    // Verify that the innsynskravDels are deleted
    assertEquals(
        HttpStatus.NOT_FOUND,
        getAdmin("/innsynskravDel/" + innsynskravObject.getInnsynskravDel().get(0).getId())
            .getStatusCode());

    // Verify that the innsynskrav is deleted
    assertEquals(HttpStatus.NOT_FOUND, get("/innsynskrav/" + innsynskrav.getId()).getStatusCode());
  }

  // Test sending an innsynskrav where a journalpost has been deleted before verifying the
  // innsynskrav
  @Test
  void testInnsynskravWithDeletedJournalpost() throws Exception {
    var mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    // Insert saksmappe with two journalposts, one will be deleted
    var arkivJSON = getArkivJSON();
    var arkivResponse = post("/arkiv", arkivJSON);
    var arkivDTO = gson.fromJson(arkivResponse.getBody(), ArkivDTO.class);
    var journalpostToKeepJSON = getJournalpostJSON();
    journalpostToKeepJSON.put("offentligTittel", "JournalpostToKeep");
    journalpostToKeepJSON.put("offentligTittelSensitiv", "JournalpostToKeepSensitiv");
    var journalpostToDeleteJSON = getJournalpostJSON();
    journalpostToDeleteJSON.put("offentligTittel", "journalpostToDelete");
    journalpostToDeleteJSON.put("offentligTittelSensitiv", "journalpostToDeleteSensitiv");
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put(
        "journalpost", new JSONArray().put(journalpostToKeepJSON).put(journalpostToDeleteJSON));
    var saksmappeResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    var saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    var journalpostToKeep = saksmappe.getJournalpost().get(0);
    var journalpostToDelete = saksmappe.getJournalpost().get(1);

    var innsynskravJSON = getInnsynskravJSON();
    var innsynskravDelToKeepJSON = getInnsynskravDelJSON();
    innsynskravDelToKeepJSON.put("journalpost", journalpostToKeep.getId());
    var innsynskravDelToDeleteJSON = getInnsynskravDelJSON();
    innsynskravDelToDeleteJSON.put("journalpost", journalpostToDelete.getId());
    innsynskravJSON.put(
        "innsynskravDel",
        new JSONArray().put(innsynskravDelToKeepJSON).put(innsynskravDelToDeleteJSON));

    // Create Innsynskrav
    var innsynskravResponse = post("/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, innsynskravResponse.getStatusCode());
    var innsynskrav = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);

    // Check that the Innsynskrav is in the DB
    var innsynskravObject = innsynskravRepository.findById(innsynskrav.getId()).orElse(null);
    assertEquals(innsynskrav.getId(), innsynskravObject.getId());

    // Verify that InnsynskravService tried to send an email
    waiter.await(100, TimeUnit.MILLISECONDS);
    verify(javaMailSender, times(1)).createMimeMessage();
    verify(javaMailSender, times(1)).send(mimeMessage);

    // Delete the journalpost that should be sent through eFormidling
    var deleteResponse = deleteAdmin("/journalpost/" + journalpostToDelete.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    var deletedJournalpost = gson.fromJson(deleteResponse.getBody(), JournalpostDTO.class);
    assertEquals(true, deletedJournalpost.getDeleted());

    // Verify that the journalpost is deleted
    var deletedJournalpostObject =
        journalpostRepository.findById(deletedJournalpost.getId()).orElse(null);
    assertNull(deletedJournalpostObject);

    // Verify the Innsynskrav
    innsynskravResponse =
        put(
            "/innsynskrav/"
                + innsynskrav.getId()
                + "/verify/"
                + innsynskravObject.getVerificationSecret(),
            null);
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    innsynskrav = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
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
    assertTrue(txtContent.contains(journalpostToKeepJSON.get("offentligTittel").toString()));
    assertFalse(txtContent.contains(journalpostToDeleteJSON.get("offentligTittel").toString()));
    assertTrue(htmlContent.contains(journalpostToKeepJSON.get("offentligTittel").toString()));
    assertFalse(htmlContent.contains(journalpostToDeleteJSON.get("offentligTittel").toString()));
    // assertEquals(true, attachmentContent
    // .contains("<dokumentnr>" + journalpost.getJournalpostnummer() + "</dokumentnr>"));

    // Check the content of data sent through eFormidling

    // Check that InnsynskravSenderService tried to send through eFormidling
    verify(ipSender, times(1))
        .sendInnsynskrav(
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(Integer.class));

    // Delete the Innsynskrav
    deleteResponse = deleteAdmin("/innsynskrav/" + innsynskrav.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    innsynskrav = gson.fromJson(deleteResponse.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskrav.getDeleted());

    // Delete the Saksmappe
    deleteResponse = delete("/saksmappe/" + saksmappe.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    saksmappe = gson.fromJson(deleteResponse.getBody(), SaksmappeDTO.class);
    assertEquals(true, saksmappe.getDeleted());

    // Delete arkiv
    delete("/arkiv/" + arkivDTO.getId());
  }

  @Test
  void testInnsynskravWithFailingEformidling() throws Exception {
    var mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    var innsynskravJSON = getInnsynskravJSON();
    var innsynskravDelJSON = getInnsynskravDelJSON();
    innsynskravDelJSON.put("journalpost", journalpostDTO.getId());
    innsynskravJSON.put("innsynskravDel", new JSONArray().put(innsynskravDelJSON));

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
        .thenReturn("foo");

    // Insert Innsynskrav
    var innsynskravResponse = post("/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, innsynskravResponse.getStatusCode());
    var innsynskravJ = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertEquals("test@example.com", innsynskravJ.getEmail());
    assertEquals(1, innsynskravJ.getInnsynskravDel().size());
    var innsynskrav = innsynskravRepository.findById(innsynskravJ.getId()).orElse(null);

    // Verify the Innsynskrav
    innsynskravResponse =
        put(
            "/innsynskrav/"
                + innsynskravJ.getId()
                + "/verify/"
                + innsynskrav.getVerificationSecret(),
            null);
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    innsynskravJ = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskravJ.getVerified());

    // Wait for async send to be triggered
    Awaitility.await()
        .pollInterval(30, TimeUnit.MILLISECONDS)
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              // Check that InnsynskravSenderService tried to send through eFormidling
              verify(ipSender, times(1))
                  .sendInnsynskrav(
                      any(String.class),
                      any(String.class),
                      any(String.class),
                      any(String.class),
                      any(String.class),
                      any(String.class),
                      any(String.class),
                      any(Integer.class));

              // Check that InnsynskravSenderService tried to send two emails (one with verification
              // link, one confirmation)
              verify(javaMailSender, times(2)).createMimeMessage();
            });

    // Check that the innsynskravDel isn't verified
    innsynskravResponse =
        getAdmin("/innsynskrav/" + innsynskravJ.getId() + "?expand[]=innsynskravDel");
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    innsynskravJ = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    var expandableField = innsynskravJ.getInnsynskravDel().get(0);
    assertNotNull(expandableField.getExpandedObject(), "innsynskravDel is not expanded");
    assertNull(
        expandableField.getExpandedObject().getSent(),
        "innsynskravDel should not have a sent timestamp");

    // Try to send again, shouldn't send another mail, but should invoke ipSender once more
    innsynskravSenderService.sendInnsynskrav(innsynskravJ.getId());
    Awaitility.await()
        .pollInterval(50, TimeUnit.MILLISECONDS)
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              verify(javaMailSender, times(2)).createMimeMessage();
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
            });

    // Check that the innsynskravDel is verified
    innsynskravResponse =
        getAdmin("/innsynskrav/" + innsynskravJ.getId() + "?expand[]=innsynskravDel");
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    innsynskravJ = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    expandableField = innsynskravJ.getInnsynskravDel().get(0);
    assertNotNull(expandableField.getExpandedObject(), "innsynskravDel is not expanded");

    var innsynskravId = innsynskravJ.getId();
    // Wait for the async update of the "sent" timestamp
    Awaitility.await()
        .pollInterval(50, TimeUnit.MILLISECONDS)
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(() -> assertSent(innsynskravId));

    innsynskravResponse =
        getAdmin("/innsynskrav/" + innsynskravJ.getId() + "?expand[]=innsynskravDel");
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    innsynskravJ = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    expandableField = innsynskravJ.getInnsynskravDel().get(0);
    assertNotNull(
        expandableField.getExpandedObject().getSent(),
        "innsynskravDel should have a sent timestamp");

    // Delete the Innsynskrav
    var deleteResponse = deleteAdmin("/innsynskrav/" + innsynskravJ.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    innsynskravJ = gson.fromJson(deleteResponse.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskravJ.getDeleted());
  }

  private void assertSent(String innsynskravId) throws Exception {
    // Check that the innsynskravDel has a "sent" timestamp
    var innsynskravResponse =
        getAdmin("/innsynskrav/" + innsynskravId + "?expand[]=innsynskravDel");
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    var expandableField =
        gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class)
            .getInnsynskravDel()
            .get(0);
    assertNotNull(
        expandableField.getExpandedObject().getSent(),
        "innsynskravDel should have a sent timestamp");
  }

  // Test that InnsynskravSenderService falls back to email after 3 failed eFormidling calls
  @Test
  void testInnsynskravEmailFallback() throws Exception {
    var mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    var innsynskravJSON = getInnsynskravJSON();
    var innsynskravDelJSON = getInnsynskravDelJSON();
    innsynskravDelJSON.put("journalpost", journalpostDTO.getId());
    innsynskravJSON.put("innsynskravDel", new JSONArray().put(innsynskravDelJSON));

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
        .thenThrow(new IPConnectionException("", null));

    // Insert Innsynskrav
    var innsynskravResponse = post("/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, innsynskravResponse.getStatusCode());
    var innsynskravJ = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertEquals("test@example.com", innsynskravJ.getEmail());
    assertEquals(1, innsynskravJ.getInnsynskravDel().size());
    var innsynskrav = innsynskravRepository.findById(innsynskravJ.getId()).orElse(null);

    // Verify the Innsynskrav
    innsynskravResponse =
        put(
            "/innsynskrav/"
                + innsynskravJ.getId()
                + "/verify/"
                + innsynskrav.getVerificationSecret(),
            null);
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    innsynskravJ = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskravJ.getVerified());

    // Sending is done async, so we need to wait for it to get triggered
    Awaitility.await()
        .pollInterval(50, TimeUnit.MILLISECONDS)
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              // Check that InnsynskravSenderService tried to send through eFormidling
              verify(ipSender, times(1))
                  .sendInnsynskrav(
                      any(String.class),
                      any(String.class),
                      any(String.class),
                      any(String.class),
                      any(String.class),
                      any(String.class),
                      any(String.class),
                      any(Integer.class));

              // Two mails should be sent (Verification link and confirmation)
              verify(javaMailSender, times(2)).createMimeMessage();
            });

    // Check that the innsynskravDel isn't verified
    innsynskravResponse =
        getAdmin("/innsynskrav/" + innsynskravJ.getId() + "?expand[]=innsynskravDel");
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    innsynskravJ = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    var expandableField = innsynskravJ.getInnsynskravDel().get(0);
    assertNotNull(expandableField.getExpandedObject(), "innsynskravDel is not expanded");
    assertNull(expandableField.getExpandedObject().getSent());

    // Try to send again, shouldn't send another mail, but should invoke ipSender once more
    innsynskravSenderService.sendInnsynskrav(innsynskravJ.getId());
    Awaitility.await()
        .pollInterval(50, TimeUnit.MILLISECONDS)
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              verify(javaMailSender, times(2)).createMimeMessage();
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
            });

    // Check that the innsynskravDel isn't verified
    innsynskravResponse =
        getAdmin("/innsynskrav/" + innsynskravJ.getId() + "?expand[]=innsynskravDel");
    innsynskravJ = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    expandableField = innsynskravJ.getInnsynskravDel().get(0);
    assertNull(expandableField.getExpandedObject().getSent());

    // Try to send again, shouldn't send another mail, but should invoke ipSender once more
    innsynskravSenderService.sendInnsynskrav(innsynskravJ.getId());
    Awaitility.await()
        .pollInterval(50, TimeUnit.MILLISECONDS)
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              verify(javaMailSender, times(2)).createMimeMessage();
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
            });

    // Check that the innsynskravDel isn't verified
    innsynskravResponse =
        getAdmin("/innsynskrav/" + innsynskravJ.getId() + "?expand[]=innsynskravDel");
    innsynskravJ = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    expandableField = innsynskravJ.getInnsynskravDel().get(0);
    assertNull(expandableField.getExpandedObject().getSent());

    // Try to send again, now it should fall back to email
    innsynskravSenderService.sendInnsynskrav(innsynskravJ.getId());
    waiter.await(50, TimeUnit.MILLISECONDS);
    Awaitility.await()
        .pollInterval(50, TimeUnit.MILLISECONDS)
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              verify(javaMailSender, times(3)).createMimeMessage();
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
            });

    // Check that the innsynskravDel is verified
    innsynskravResponse =
        getAdmin("/innsynskrav/" + innsynskravJ.getId() + "?expand[]=innsynskravDel");
    innsynskravJ = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    expandableField = innsynskravJ.getInnsynskravDel().get(0);
    assertNotNull(expandableField.getExpandedObject().getSent());

    // Delete the Innsynskrav
    var deleteResponse = deleteAdmin("/innsynskrav/" + innsynskravJ.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    innsynskravJ = gson.fromJson(deleteResponse.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskravJ.getDeleted());
  }

  // Check that we get a 401 when trying to verify an innsynskrav with the wrong secret
  @Test
  void testInnsynskravVerifyWrongSecret() throws Exception {
    var mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    var innsynskravJSON = getInnsynskravJSON();
    var innsynskravDelJSON = getInnsynskravDelJSON();
    innsynskravDelJSON.put("journalpost", journalpostDTO.getId());
    innsynskravJSON.put("innsynskravDel", new JSONArray().put(innsynskravDelJSON));

    // Insert Innsynskrav
    var innsynskravResponse = post("/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, innsynskravResponse.getStatusCode());
    var innsynskrav = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);

    // Check that the Innsynskrav is in the DB
    var innsynskravObject = innsynskravRepository.findById(innsynskrav.getId()).orElse(null);
    assertEquals(innsynskrav.getId(), innsynskravObject.getId());

    // Verify the Innsynskrav with the wrong secret
    innsynskravResponse = put("/innsynskrav/" + innsynskrav.getId() + "/verify/wrongsecret", null);
    assertEquals(HttpStatus.FORBIDDEN, innsynskravResponse.getStatusCode());

    // Verify the Innsynskrav with the correct secret
    innsynskravResponse =
        put(
            "/innsynskrav/"
                + innsynskrav.getId()
                + "/verify/"
                + innsynskravObject.getVerificationSecret(),
            null);
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    innsynskrav = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskrav.getVerified());

    // Delete the Innsynskrav
    var deleteResponse = deleteAdmin("/innsynskrav/" + innsynskrav.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    innsynskrav = gson.fromJson(deleteResponse.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskrav.getDeleted());
  }

  @Test
  void testInnsynskravWhenLoggedIn() throws Exception {
    var mimeMessage = new MimeMessage((Session) null);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

    // Create and activate Bruker
    var brukerJSON = getBrukerJSON();
    var brukerResponse = post("/bruker", brukerJSON);
    assertEquals(HttpStatus.CREATED, brukerResponse.getStatusCode());
    var insertedBruker = gson.fromJson(brukerResponse.getBody(), BrukerDTO.class);
    var insertedBrukerObj = brukerService.findById(insertedBruker.getId());
    brukerResponse =
        put(
            "/bruker/" + insertedBruker.getId() + "/activate/" + insertedBrukerObj.getSecret(),
            null);
    assertEquals(HttpStatus.OK, brukerResponse.getStatusCode());

    // Login
    var loginRequest = new JSONObject();
    loginRequest.put("username", brukerJSON.get("email"));
    loginRequest.put("password", brukerJSON.get("password"));
    var loginResponse = post("/auth/token", loginRequest);
    assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
    var tokenResponse = gson.fromJson(loginResponse.getBody(), TokenResponse.class);
    var token = tokenResponse.getToken();

    // Insert Innsynskrav
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("email", insertedBruker.getEmail());
    var innsynskravDelJSON = getInnsynskravDelJSON();
    innsynskravDelJSON.put("journalpost", journalpostDTO.getId());
    innsynskravJSON.put("innsynskravDel", new JSONArray().put(innsynskravDelJSON));
    var innsynskravResponse = post("/innsynskrav", innsynskravJSON, token);
    assertEquals(HttpStatus.CREATED, innsynskravResponse.getStatusCode());
    var innsynskrav = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertEquals(insertedBruker.getEmail(), innsynskrav.getEmail());
    assertEquals(insertedBruker.getId(), innsynskrav.getBruker().getId());

    // Delete the Innsynskrav
    var deleteResponse = delete("/innsynskrav/" + innsynskrav.getId(), token);
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    innsynskrav = gson.fromJson(deleteResponse.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskrav.getDeleted());

    // Delete the Bruker
    deleteResponse = delete("/bruker/" + insertedBruker.getId(), token);
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    insertedBruker = gson.fromJson(deleteResponse.getBody(), BrukerDTO.class);
    assertEquals(true, insertedBruker.getDeleted());
  }

  @Test
  void testInnsynskravDelByInnsynskrav() throws Exception {
    // Add Saksmappe with ten journalposts
    var response = post("/arkiv", getArkivJSON());
    var _arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    post("/arkiv/" + _arkivDTO.getId() + "/saksmappe", getSaksmappeJSON());
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put(
        "journalpost",
        new JSONArray(
            List.of(
                getJournalpostJSON(),
                getJournalpostJSON(),
                getJournalpostJSON(),
                getJournalpostJSON(),
                getJournalpostJSON(),
                getJournalpostJSON(),
                getJournalpostJSON(),
                getJournalpostJSON(),
                getJournalpostJSON(),
                getJournalpostJSON())));
    response = post("/arkiv/" + _arkivDTO.getId() + "/saksmappe", saksmappeJSON);
    var _saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    var journalpostDTOs = _saksmappeDTO.getJournalpost();

    // Insert Innsynskrav with 10 InnsynskravDel
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put(
        "innsynskravDel",
        new JSONArray(
            List.of(
                getInnsynskravDelJSON().put("journalpost", journalpostDTOs.get(0).getId()),
                getInnsynskravDelJSON().put("journalpost", journalpostDTOs.get(1).getId()),
                getInnsynskravDelJSON().put("journalpost", journalpostDTOs.get(2).getId()),
                getInnsynskravDelJSON().put("journalpost", journalpostDTOs.get(3).getId()),
                getInnsynskravDelJSON().put("journalpost", journalpostDTOs.get(4).getId()),
                getInnsynskravDelJSON().put("journalpost", journalpostDTOs.get(5).getId()),
                getInnsynskravDelJSON().put("journalpost", journalpostDTOs.get(6).getId()),
                getInnsynskravDelJSON().put("journalpost", journalpostDTOs.get(7).getId()),
                getInnsynskravDelJSON().put("journalpost", journalpostDTOs.get(8).getId()),
                getInnsynskravDelJSON().put("journalpost", journalpostDTOs.get(9).getId()))));
    response = post("/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskrav1DTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);

    // Insert Innsynskrav with 5 InnsynskravDel
    innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put(
        "innsynskravDel",
        new JSONArray(
            List.of(
                getInnsynskravDelJSON().put("journalpost", journalpostDTOs.get(0).getId()),
                getInnsynskravDelJSON().put("journalpost", journalpostDTOs.get(1).getId()),
                getInnsynskravDelJSON().put("journalpost", journalpostDTOs.get(2).getId()),
                getInnsynskravDelJSON().put("journalpost", journalpostDTOs.get(3).getId()),
                getInnsynskravDelJSON().put("journalpost", journalpostDTOs.get(4).getId()))));
    response = post("/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskrav2DTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);

    var innsynskrav1DelList =
        innsynskrav1DTO.getInnsynskravDel().stream()
            .map(ExpandableField::getExpandedObject)
            .toList();
    var type = new TypeToken<ResultList<InnsynskravDelDTO>>() {}.getType();
    testGenericList(
        type,
        innsynskrav1DelList,
        "/innsynskrav/" + innsynskrav1DTO.getId() + "/innsynskravDel",
        adminKey);

    var innsynskrav2DelList =
        innsynskrav2DTO.getInnsynskravDel().stream()
            .map(ExpandableField::getExpandedObject)
            .toList();
    testGenericList(
        type,
        innsynskrav2DelList,
        "/innsynskrav/" + innsynskrav2DTO.getId() + "/innsynskravDel",
        adminKey);

    // Clean up
    assertEquals(
        HttpStatus.OK, deleteAdmin("/innsynskrav/" + innsynskrav1DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.OK, deleteAdmin("/innsynskrav/" + innsynskrav2DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/arkiv/" + _arkivDTO.getId()).getStatusCode());
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
    var htmlContent =
        new String(htmlBodyPart.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
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

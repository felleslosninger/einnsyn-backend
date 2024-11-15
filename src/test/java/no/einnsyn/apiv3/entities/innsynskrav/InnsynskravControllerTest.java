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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.authentication.bruker.models.TokenResponse;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.apikey.models.ApiKeyDTO;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.apiv3.entities.bruker.models.BrukerDTO;
import no.einnsyn.apiv3.entities.enhet.models.EnhetDTO;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.clients.ip.IPSender;
import no.einnsyn.clients.ip.exceptions.IPConnectionException;
import org.apache.commons.io.IOUtils;
import org.awaitility.Awaitility;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InnsynskravControllerTest extends EinnsynControllerTestBase {

  @MockBean IPSender ipSender;
  @Lazy @Autowired private InnsynskravTestService innsynskravTestService;

  ArkivDTO arkivDTO;
  EnhetDTO enhetNoEFDTO;
  EnhetDTO enhetOrderV2DTO;
  String enhetNoEFSecretKey;
  String enhetOrderv2SecretKey;
  SaksmappeDTO saksmappeDTO;
  JournalpostDTO journalpostDTO;
  SaksmappeDTO saksmappeNoEFormidlingDTO;
  JournalpostDTO journalpostNoEFormidlingDTO;
  MimeMessage mimeMessage;

  @Value("${application.email.from}")
  private String emailFrom;

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

    // Insert an enhet that uses order.xml v2
    var enhetOrderV2JSON = getEnhetJSON();
    enhetOrderV2JSON.put("navn", "EnhetOrderV2");
    enhetOrderV2JSON.put("orderXmlVersjon", 2);
    enhetOrderV2JSON.put("eFormidling", true);
    enhetResponse = post("/enhet/" + journalenhetId + "/underenhet", enhetOrderV2JSON);
    assertEquals(HttpStatus.CREATED, enhetResponse.getStatusCode());
    enhetOrderV2DTO = gson.fromJson(enhetResponse.getBody(), EnhetDTO.class);

    // Create ApiKey for enhetNoEF
    var apiKeyJSON = getApiKeyJSON();
    apiKeyJSON.put("enhet", enhetNoEFDTO.getId());
    var apiKeyResponse = post("/enhet/" + enhetNoEFDTO.getId() + "/apiKey", apiKeyJSON);
    var apiKeyDTO = gson.fromJson(apiKeyResponse.getBody(), ApiKeyDTO.class);
    enhetNoEFSecretKey = apiKeyDTO.getSecretKey();

    // Create ApiKey for enhetOrderV2
    apiKeyJSON.put("enhet", enhetOrderV2DTO.getId());
    apiKeyResponse = post("/enhet/" + enhetOrderV2DTO.getId() + "/apiKey", apiKeyJSON);
    apiKeyDTO = gson.fromJson(apiKeyResponse.getBody(), ApiKeyDTO.class);
    enhetOrderv2SecretKey = apiKeyDTO.getSecretKey();

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
    delete("/enhet/" + enhetOrderV2DTO.getId());
    delete("/arkiv/" + arkivDTO.getId());
  }

  @BeforeEach
  void resetMocks() {
    Mockito.reset(ipSender);
  }

  @Test
  void testInnsynskravSingleJournalpostUnverifiedUserEFormidling() throws Exception {
    var innsynskravJSON = getInnsynskravJSON();
    var innsynskravDelJSON = getInnsynskravDelJSON();
    innsynskravDelJSON.put("journalpost", journalpostDTO.getId());
    innsynskravJSON.put("innsynskravDel", new JSONArray().put(innsynskravDelJSON));

    // Insert Innsynskrav
    var response = post("/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravDTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);
    var innsynskravId = innsynskravDTO.getId();
    assertEquals("test@example.com", innsynskravDTO.getEmail());
    assertEquals(1, innsynskravDTO.getInnsynskravDel().size());
    var enhetId =
        innsynskravDTO.getInnsynskravDel().getFirst().getExpandedObject().getEnhet().getId();

    // Check that InnsynskravService tried to send an email. The email is sent async, so we have to
    // wait a bit
    Awaitility.await().untilAsserted(() -> verify(javaMailSender, times(1)).createMimeMessage());
    verify(javaMailSender, times(1)).send(mimeMessage);

    // Verify email content
    var language = innsynskravDTO.getLanguage();
    var locale = Locale.forLanguageTag(language);
    var languageBundle = ResourceBundle.getBundle("mailtemplates/mailtemplates", locale);
    assertEquals(mimeMessage.getFrom()[0].toString(), new InternetAddress(emailFrom).toString());
    assertEquals(mimeMessage.getHeader("to")[0], innsynskravDTO.getEmail());
    assertEquals(
        mimeMessage.getSubject(), languageBundle.getString("confirmAnonymousOrderSubject"));

    // Check that the InnsynskravDel is in the DB, with a verification secret
    var verificationSecret = innsynskravTestService.getVerificationSecret(innsynskravId);
    assertNotNull(verificationSecret);

    response = get("/enhet/" + enhetId + "?expand=handteresAv");
    var enhetDTO = gson.fromJson(response.getBody(), EnhetDTO.class);
    var handteresAvDTO =
        enhetDTO.getHandteresAv() != null
            ? enhetDTO.getHandteresAv().getExpandedObject()
            : enhetDTO;

    // Verify the Innsynskrav
    response = patch("/innsynskrav/" + innsynskravId + "/verify/" + verificationSecret, null);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravDTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskravDTO.getVerified());

    var expectedXml =
        IOUtils.toString(
            Objects.requireNonNull(
                InnsynskravControllerTest.class
                    .getClassLoader()
                    .getResourceAsStream("order-v1-reduced.xml")),
            StandardCharsets.UTF_8);
    var orderCaptor = ArgumentCaptor.forClass(String.class);

    // Verify that IPSender was called
    Awaitility.await()
        .untilAsserted(
            () ->
                verify(ipSender, times(1))
                    .sendInnsynskrav(
                        orderCaptor.capture(), // Order.xml
                        any(String.class), // transaction id
                        eq(handteresAvDTO.getOrgnummer()), // handteresAv
                        eq(enhetDTO.getOrgnummer()),
                        eq(enhetDTO.getInnsynskravEpost()),
                        any(String.class), // mail content
                        any(String.class), // IP orgnummer
                        any(Integer.class) // expectedResponseTimeoutDays
                        ));

    // Verify contents of xml. "id" and "bestillingsdato" will change at runtime
    var actualXml = orderCaptor.getValue();
    var cleanedXml =
        actualXml
            .replaceFirst("<id>ik_.*</id>", "<id>ik_something</id>")
            .replaceFirst(
                "<bestillingsdato>.*</bestillingsdato>",
                "<bestillingsdato>dd-mm-yyyy</bestillingsdato>");
    assertEquals(expectedXml, cleanedXml);

    var orderJSON = (JSONObject) XML.toJSONObject(actualXml).get("bestilling");
    assertEquals(innsynskravId, orderJSON.get("id"));
    var bestillingsdato = orderJSON.get("bestillingsdato");
    var v1DateFormat = new SimpleDateFormat("dd.MM.yyyy");
    assertEquals(v1DateFormat.format(new Date()), bestillingsdato);

    // Verify that confirmation email was sent
    verify(javaMailSender, times(2)).createMimeMessage();
    verify(javaMailSender, times(2)).send(mimeMessage);

    // Delete the Innsynskrav
    response = deleteAdmin("/innsynskrav/" + innsynskravId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravDTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskravDTO.getDeleted());
  }

  @Test
  void testInnsynskravMultipleJournalpostsOrderV2LoggedInUser() throws Exception {
    var arkivJSON = getArkivJSON();
    arkivJSON.put("systemId", "b87ca23f-ffee-4e20-ab8e-a6361130bb50");
    var arkivResponse = post("/arkiv", arkivJSON);
    var arkivSysidDTO = gson.fromJson(arkivResponse.getBody(), ArkivDTO.class);
    // Insert arkivdel
    var arkivdelJSON = getArkivdelJSON();
    arkivdelJSON.put("systemId", "ea3ce57a-327e-4ff1-b29c-ff6c671e530c");
    var arkivdelResponse = post("/arkiv/" + arkivSysidDTO.getId() + "/arkivdel", arkivdelJSON);
    var arkivdelDTO = gson.fromJson(arkivdelResponse.getBody(), ArkivdelDTO.class);

    // Create saksmappe
    var saksmappeResponse =
        post(
            "/arkiv/" + arkivDTO.getId() + "/saksmappe", getSaksmappeJSON(), enhetOrderv2SecretKey);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    var saksmappeOrderV2DTO = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);

    // Create saksmappe on arkivdel
    var saksmappeArkdelResponse =
        post(
            "/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON(), enhetOrderv2SecretKey);
    assertEquals(HttpStatus.CREATED, saksmappeArkdelResponse.getStatusCode());
    var saksmappeArkdelOrderV2DTO = gson.fromJson(saksmappeArkdelResponse.getBody(), SaksmappeDTO.class);

    // create journalposts. 1 with behandlingsansvarlig
    // Plain JP
    var jp = getJournalpostJSON();
    jp.put("systemId", "303d18bd-d173-4d5f-994a-d08cb929e79f");
    var journalpostResponse =
        post(
            "/saksmappe/" + saksmappeArkdelOrderV2DTO.getId() + "/journalpost",
            jp,
            enhetOrderv2SecretKey);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    var journalpostOrderv2PlainDTO =
        gson.fromJson(journalpostResponse.getBody(), JournalpostDTO.class);
    // JP with KP
    jp = getJournalpostJSON();
    var kp = getKorrespondansepartJSON();
    kp.put("erBehandlingsansvarlig", "true");
    kp.put("saksbehandler", "Sigrid Sakshandsamar");
    kp.put("administrativEnhet", "Eininga for sakshandsaming");
    jp.put("korrespondansepart", new JSONArray(List.of(kp)));
    jp.put("journalpostnummer", 2);
    jp.put("journalsekvensnummer", 6);
    journalpostResponse =
        post(
            "/saksmappe/" + saksmappeOrderV2DTO.getId() + "/journalpost",
            jp,
            enhetOrderv2SecretKey);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    var journalpostOrderv2WithKorrPartDTO =
        gson.fromJson(journalpostResponse.getBody(), JournalpostDTO.class);

    // Create and activate Bruker
    var brukerJSON = getBrukerJSON();
    var response = post("/bruker", brukerJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var brukerDTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    var bruker = brukerService.findById(brukerDTO.getId());
    response = patch("/bruker/" + brukerDTO.getId() + "/activate/" + bruker.getSecret(), null);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Login
    var loginRequest = new JSONObject();
    loginRequest.put("username", brukerJSON.get("email"));
    loginRequest.put("password", brukerJSON.get("password"));
    response = post("/auth/token", loginRequest);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var tokenResponse = gson.fromJson(response.getBody(), TokenResponse.class);
    var token = tokenResponse.getToken();

    // Insert Innsynskrav
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("email", brukerDTO.getEmail());
    var innsynskravDel1JSON = getInnsynskravDelJSON();
    innsynskravDel1JSON.put("journalpost", journalpostOrderv2PlainDTO.getId());
    var innsynskravDel2JSON = getInnsynskravDelJSON();
    innsynskravDel2JSON.put("journalpost", journalpostOrderv2WithKorrPartDTO.getId());

    innsynskravJSON.put("innsynskravDel", new JSONArray(List.of(innsynskravDel1JSON, innsynskravDel2JSON)));
    response = post("/innsynskrav", innsynskravJSON, token);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravDTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);
    var innsynskravId = innsynskravDTO.getId();
    assertEquals(brukerDTO.getEmail(), innsynskravDTO.getEmail());
    assertEquals(brukerDTO.getId(), innsynskravDTO.getBruker().getId());

    // verify sending attempt
    // -confirmation email
    // -IPSender
    var expectedXml =
        IOUtils.toString(
            Objects.requireNonNull(
                InnsynskravControllerTest.class
                    .getClassLoader()
                    .getResourceAsStream("order-v2.xml")),
            StandardCharsets.UTF_8);
    var orderCaptor = ArgumentCaptor.forClass(String.class);

    // Verify that IPSender was called
    Awaitility.await()
        .untilAsserted(
            () ->
                verify(ipSender, times(1))
                    .sendInnsynskrav(
                        orderCaptor.capture(), // Order.xml
                        any(String.class), // transaction id
                        eq(enhetOrderV2DTO.getOrgnummer()), // handteresAv
                        eq(enhetOrderV2DTO.getOrgnummer()),
                        eq(enhetOrderV2DTO.getInnsynskravEpost()),
                        any(String.class), // mail content
                        any(String.class), // IP orgnummer
                        any(Integer.class) // expectedResponseTimeoutDays
                    ));
    // verify contents of order.xml
    var actualXml = orderCaptor.getValue();
    var cleanedXml =
        actualXml
            .replaceFirst("<id>ik_.*</id>", "<id>ik_something</id>")
            .replaceFirst(
                "<bestillingsdato>.*</bestillingsdato>",
                "<bestillingsdato>yyyy-mm-dd</bestillingsdato>")
            .replaceAll("<id>http.*</id>", "<id>http://jp_somekindofid</id>");
    assertEquals(expectedXml, cleanedXml);

    var orderJSON = (JSONObject) XML.toJSONObject(actualXml).get("bestilling");
    assertEquals(innsynskravId, orderJSON.get("id"));
    var bestillingsdato = orderJSON.get("bestillingsdato");
    var v1DateFormat = new SimpleDateFormat("dd.MM.yyyy");
    assertEquals(v1DateFormat.format(new Date()), bestillingsdato);

    // TODO: verify correct JP-IDs

    // Cleanup
    // Journalposts
    response = delete("journalpost/" + journalpostOrderv2PlainDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = delete("journalpost/" + journalpostOrderv2WithKorrPartDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Saksmappes
    response = delete("saksmappe/" + saksmappeOrderV2DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = delete("saksmappe/" + saksmappeArkdelOrderV2DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Delete the Innsynskrav
    response = delete("/innsynskrav/" + innsynskravId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravDTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskravDTO.getDeleted());

    // Delete the Bruker
    response = delete("/bruker/" + brukerDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    brukerDTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    assertEquals(true, brukerDTO.getDeleted());

    // Arkiv+Arkivdel
  }

  @Test
  void testInnsynskravSingleJournalpostUnverifiedUserEmail() throws Exception {
    var innsynskravJSON = getInnsynskravJSON();
    var innsynskravDelJSON = getInnsynskravDelJSON();
    innsynskravDelJSON.put("journalpost", journalpostNoEFormidlingDTO.getId());
    innsynskravJSON.put("innsynskravDel", new JSONArray().put(innsynskravDelJSON));

    // Create Innsynskrav
    var innsynskravResponse = post("/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, innsynskravResponse.getStatusCode());
    var innsynskravDTO = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    var innsynskravId = innsynskravDTO.getId();

    // Check that InnsynskravService tried to send an email
    Awaitility.await().untilAsserted(() -> verify(javaMailSender, times(1)).createMimeMessage());
    verify(javaMailSender, times(1)).send(mimeMessage);

    // Verify the Innsynskrav
    var verificationSecret = innsynskravTestService.getVerificationSecret(innsynskravId);
    innsynskravResponse =
        patch("/innsynskrav/" + innsynskravId + "/verify/" + verificationSecret, null);
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    innsynskravDTO = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskravDTO.getVerified());

    // Check that InnsynskravService tried to send two more mails (one to the user and one to the
    // Enhet)
    Awaitility.await().untilAsserted(() -> verify(javaMailSender, times(3)).createMimeMessage());
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
    var deleteResponse = deleteAdmin("/innsynskrav/" + innsynskravDTO.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    innsynskravDTO = gson.fromJson(deleteResponse.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskravDTO.getDeleted());
  }

  @Test
  void testInnsynskravUnverifiedUserEformidlingAndEmail() throws Exception {
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
    var innsynskravDTO = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    var innsynskravId = innsynskravDTO.getId();

    // Check that InnsynskravService tried to send an email
    Awaitility.await().untilAsserted(() -> verify(javaMailSender, times(1)).createMimeMessage());
    verify(javaMailSender, times(1)).send(any(MimeMessage.class));

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
    var verificationSecret = innsynskravTestService.getVerificationSecret(innsynskravId);
    innsynskravResponse =
        patch("/innsynskrav/" + innsynskravId + "/verify/" + verificationSecret, null);
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    innsynskravDTO = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskravDTO.getVerified());

    // Check that InnsynskravService tried to send two more emails
    Awaitility.await().untilAsserted(() -> verify(javaMailSender, times(3)).createMimeMessage());
    verify(javaMailSender, times(3)).send(any(MimeMessage.class));

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
    assertEquals(HttpStatus.OK, getAdmin("/innsynskrav/" + innsynskravDTO.getId()).getStatusCode());
    innsynskravTestService.assertSent(innsynskravId, 0);
    innsynskravTestService.assertSent(innsynskravId, 1);

    // Delete the Innsynskrav
    var deleteResponse = deleteAdmin("/innsynskrav/" + innsynskravDTO.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    innsynskravDTO = gson.fromJson(deleteResponse.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskravDTO.getDeleted());

    // Verify that the innsynskravDels are deleted
    assertEquals(
        HttpStatus.NOT_FOUND,
        getAdmin("/innsynskravDel/" + innsynskravDTO.getInnsynskravDel().get(0).getId())
            .getStatusCode());

    // Verify that the innsynskrav is deleted
    assertEquals(HttpStatus.NOT_FOUND, get("/innsynskrav/" + innsynskravId).getStatusCode());
  }

  // Test sending an innsynskrav where a journalpost has been deleted before verifying the
  // innsynskrav
  @Test
  void testInnsynskravWithDeletedJournalpost() throws Exception {

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
    var innsynskravDTO = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);

    // Verify that InnsynskravService tried to send an email
    Awaitility.await().untilAsserted(() -> verify(javaMailSender, times(1)).createMimeMessage());
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
    var verificationSecret = innsynskravTestService.getVerificationSecret(innsynskravDTO.getId());
    innsynskravResponse =
        patch("/innsynskrav/" + innsynskravDTO.getId() + "/verify/" + verificationSecret, null);
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    innsynskravDTO = gson.fromJson(innsynskravResponse.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskravDTO.getVerified());

    // Check that InnsynskravService tried to send another mail
    Awaitility.await().untilAsserted(() -> verify(javaMailSender, times(2)).createMimeMessage());
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
    deleteResponse = deleteAdmin("/innsynskrav/" + innsynskravDTO.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    innsynskravDTO = gson.fromJson(deleteResponse.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskravDTO.getDeleted());

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
    var response = post("/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravDTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);
    var innsynskravId = innsynskravDTO.getId();
    assertEquals("test@example.com", innsynskravDTO.getEmail());
    assertEquals(1, innsynskravDTO.getInnsynskravDel().size());

    // Verify the Innsynskrav
    var verificationSecret = innsynskravTestService.getVerificationSecret(innsynskravId);
    response = patch("/innsynskrav/" + innsynskravId + "/verify/" + verificationSecret, null);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravDTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskravDTO.getVerified());

    // Check that InnsynskravSenderService tried to send through eFormidling
    // This is done in an async thread, so we need to wait
    Awaitility.await()
        .untilAsserted(
            () ->
                verify(ipSender, times(1))
                    .sendInnsynskrav(
                        any(String.class),
                        any(String.class),
                        any(String.class),
                        any(String.class),
                        any(String.class),
                        any(String.class),
                        any(String.class),
                        any(Integer.class)));

    // Check that InnsynskravSenderService tried to send two emails (one with verification
    // link, one confirmation)
    verify(javaMailSender, times(2)).createMimeMessage();

    // Check that the innsynskravDel isn't sent
    innsynskravTestService.assertNotSent(innsynskravId);

    // Try to send again, shouldn't send another mail, but should invoke ipSender once more
    innsynskravSenderService.sendInnsynskrav(innsynskravId);
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

    // Check that the innsynskravDel is sent
    innsynskravTestService.assertSent(innsynskravId);

    // Delete the Innsynskrav
    response = deleteAdmin("/innsynskrav/" + innsynskravId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravDTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskravDTO.getDeleted());
  }

  // Test that InnsynskravSenderService falls back to email after 3 failed eFormidling calls
  @Test
  void testInnsynskravEmailFallback() throws Exception {
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
    var response = post("/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravDTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);
    var innsynskravId = innsynskravDTO.getId();
    assertEquals("test@example.com", innsynskravDTO.getEmail());
    assertEquals(1, innsynskravDTO.getInnsynskravDel().size());

    // Verify the Innsynskrav
    var verificationSecret = innsynskravTestService.getVerificationSecret(innsynskravId);
    response = patch("/innsynskrav/" + innsynskravId + "/verify/" + verificationSecret, null);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravDTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskravDTO.getVerified());

    // Sending is done async, so we need to wait for it to get triggered
    Awaitility.await()
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
    response = getAdmin("/innsynskrav/" + innsynskravId + "?expand[]=innsynskravDel");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravDTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);
    var expandableField = innsynskravDTO.getInnsynskravDel().get(0);
    assertNotNull(expandableField.getExpandedObject(), "innsynskravDel is not expanded");
    assertNull(expandableField.getExpandedObject().getSent());

    // Try to send again, shouldn't send another mail, but should invoke ipSender once more
    innsynskravSenderService.sendInnsynskrav(innsynskravId);
    Awaitility.await()
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
    response = getAdmin("/innsynskrav/" + innsynskravId + "?expand[]=innsynskravDel");
    innsynskravDTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);
    expandableField = innsynskravDTO.getInnsynskravDel().get(0);
    assertNull(expandableField.getExpandedObject().getSent());

    // Try to send again, shouldn't send another mail, but should invoke ipSender once more
    innsynskravSenderService.sendInnsynskrav(innsynskravDTO.getId());
    Awaitility.await()
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
    response = getAdmin("/innsynskrav/" + innsynskravId + "?expand[]=innsynskravDel");
    innsynskravDTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);
    expandableField = innsynskravDTO.getInnsynskravDel().get(0);
    assertNull(expandableField.getExpandedObject().getSent());

    // Try to send again, now it should fall back to email
    innsynskravSenderService.sendInnsynskrav(innsynskravId);
    Awaitility.await()
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
    response = getAdmin("/innsynskrav/" + innsynskravId + "?expand[]=innsynskravDel");
    innsynskravDTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);
    expandableField = innsynskravDTO.getInnsynskravDel().get(0);
    assertNotNull(expandableField.getExpandedObject().getSent());

    // Delete the Innsynskrav
    response = deleteAdmin("/innsynskrav/" + innsynskravDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravDTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskravDTO.getDeleted());
  }

  // Check that we get a 401 when trying to verify an innsynskrav with the wrong secret
  @Test
  void testInnsynskravVerifyWrongSecret() throws Exception {
    var innsynskravJSON = getInnsynskravJSON();
    var innsynskravDelJSON = getInnsynskravDelJSON();
    innsynskravDelJSON.put("journalpost", journalpostDTO.getId());
    innsynskravJSON.put("innsynskravDel", new JSONArray().put(innsynskravDelJSON));

    // Insert Innsynskrav
    var response = post("/innsynskrav", innsynskravJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravDTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);
    var innsynskravId = innsynskravDTO.getId();

    // Verify the Innsynskrav with the wrong secret
    response = patch("/innsynskrav/" + innsynskravId + "/verify/wrongsecret", null);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Verify the Innsynskrav with the correct secret
    var verificationSecret = innsynskravTestService.getVerificationSecret(innsynskravId);
    response = patch("/innsynskrav/" + innsynskravId + "/verify/" + verificationSecret, null);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravDTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskravDTO.getVerified());

    // Delete the Innsynskrav
    response = deleteAdmin("/innsynskrav/" + innsynskravDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravDTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskravDTO.getDeleted());
  }

  @Test
  void testInnsynskravWhenLoggedIn() throws Exception {
    // Create and activate Bruker
    var brukerJSON = getBrukerJSON();
    var response = post("/bruker", brukerJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var brukerDTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    var bruker = brukerService.findById(brukerDTO.getId());
    response = patch("/bruker/" + brukerDTO.getId() + "/activate/" + bruker.getSecret(), null);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Login
    var loginRequest = new JSONObject();
    loginRequest.put("username", brukerJSON.get("email"));
    loginRequest.put("password", brukerJSON.get("password"));
    response = post("/auth/token", loginRequest);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var tokenResponse = gson.fromJson(response.getBody(), TokenResponse.class);
    var token = tokenResponse.getToken();

    // Insert Innsynskrav
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("email", brukerDTO.getEmail());
    var innsynskravDelJSON = getInnsynskravDelJSON();
    innsynskravDelJSON.put("journalpost", journalpostDTO.getId());
    innsynskravJSON.put("innsynskravDel", new JSONArray().put(innsynskravDelJSON));
    response = post("/innsynskrav", innsynskravJSON, token);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravDTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);
    var innsynskravId = innsynskravDTO.getId();
    assertEquals(brukerDTO.getEmail(), innsynskravDTO.getEmail());
    assertEquals(brukerDTO.getId(), innsynskravDTO.getBruker().getId());

    // Verify that the innsynskrav got sent automatically
    Awaitility.await().untilAsserted(() -> innsynskravTestService.assertSent(innsynskravId));

    // Delete the Innsynskrav
    response = delete("/innsynskrav/" + innsynskravId, token);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravDTO = gson.fromJson(response.getBody(), InnsynskravDTO.class);
    assertEquals(true, innsynskravDTO.getDeleted());

    // Delete the Bruker
    response = delete("/bruker/" + brukerDTO.getId(), token);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    brukerDTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    assertEquals(true, brukerDTO.getDeleted());
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

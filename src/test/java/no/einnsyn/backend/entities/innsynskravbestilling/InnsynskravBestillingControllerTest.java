package no.einnsyn.backend.entities.innsynskravbestilling;

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
import java.util.TimeZone;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.authentication.bruker.models.TokenResponse;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.apikey.models.ApiKeyDTO;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.bruker.models.BrukerDTO;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import no.einnsyn.backend.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestillingDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.clients.ip.exceptions.IPConnectionException;
import org.apache.commons.io.IOUtils;
import org.awaitility.Awaitility;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InnsynskravBestillingControllerTest extends EinnsynControllerTestBase {

  @Lazy @Autowired private InnsynskravBestillingTestService innsynskravTestService;

  ArkivDTO arkivDTO;
  ArkivdelDTO arkivdelDTO;
  EnhetDTO enhetNoEFDTO;
  EnhetDTO enhetOrderV2DTO;
  String enhetNoEFSecretKey;
  String enhetOrderv2SecretKey;
  SaksmappeDTO saksmappeDTO;
  JournalpostDTO journalpostDTO;
  SaksmappeDTO saksmappeNoEFormidlingDTO;
  JournalpostDTO journalpostNoEFormidlingDTO;

  @Value("${application.email.from}")
  private String emailFrom;

  /** Insert Saksmappe and Journalpost first */
  @BeforeEach
  void setup() throws Exception {
    // Insert Saksmappe
    var arkivResponse = post("/arkiv", getArkivJSON());
    arkivDTO = gson.fromJson(arkivResponse.getBody(), ArkivDTO.class);
    var arkivdelResponse = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    arkivdelDTO = gson.fromJson(arkivdelResponse.getBody(), ArkivdelDTO.class);
    var saksmappeResponse =
        post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
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
        post(
            "/arkivdel/" + arkivdelDTO.getId() + "/saksmappe",
            getSaksmappeJSON(),
            enhetNoEFSecretKey);
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
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("journalpost", journalpostDTO.getId());
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray().put(innsynskravJSON));

    // Insert InnsynskravBestilling
    var response = post("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    var innsynskravBestillingId = innsynskravBestillingDTO.getId();
    assertEquals("test@example.com", innsynskravBestillingDTO.getEmail());
    assertEquals(1, innsynskravBestillingDTO.getInnsynskrav().size());
    var enhetId =
        innsynskravBestillingDTO.getInnsynskrav().getFirst().getExpandedObject().getEnhet().getId();

    // Check that InnsynskravBestillingService tried to send an email.
    var mimeMessageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(1)).send(mimeMessageCaptor.capture()));

    // Verify email content
    var mimeMessage = mimeMessageCaptor.getValue();
    var language = innsynskravBestillingDTO.getLanguage();
    var locale = Locale.forLanguageTag(language);
    var languageBundle = ResourceBundle.getBundle("mailtemplates/mailtemplates", locale);
    assertEquals(mimeMessage.getFrom()[0].toString(), new InternetAddress(emailFrom).toString());
    assertEquals(mimeMessage.getHeader("to")[0], innsynskravBestillingDTO.getEmail());
    assertEquals(
        mimeMessage.getSubject(), languageBundle.getString("confirmAnonymousOrderSubject"));

    // Check that the Innsynskrav is in the DB, with a verification secret
    var verificationSecret = innsynskravTestService.getVerificationSecret(innsynskravBestillingId);
    assertNotNull(verificationSecret);

    response = get("/enhet/" + enhetId + "?expand=handteresAv");
    var enhetDTO = gson.fromJson(response.getBody(), EnhetDTO.class);
    var handteresAvDTO =
        enhetDTO.getHandteresAv() != null
            ? enhetDTO.getHandteresAv().getExpandedObject()
            : enhetDTO;

    // Verify the InnsynskravBestilling
    response =
        patch(
            "/innsynskravBestilling/" + innsynskravBestillingId + "/verify/" + verificationSecret,
            null);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravBestillingDTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    assertEquals(true, innsynskravBestillingDTO.getVerified());
    var innsynskravBestilling = innsynskravBestillingService.findById(innsynskravBestillingId);

    var expectedXml =
        IOUtils.toString(
            Objects.requireNonNull(
                InnsynskravBestillingControllerTest.class
                    .getClassLoader()
                    .getResourceAsStream("order-v1.xml")),
            StandardCharsets.UTF_8);
    var orderCaptor = ArgumentCaptor.forClass(String.class);
    var mailCaptor = ArgumentCaptor.forClass(String.class);

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
                        mailCaptor.capture(), // mail content
                        any(String.class), // IP orgnummer
                        any(Integer.class) // expectedResponseTimeoutDays
                        ));

    // Verify the XML contents. The fields "id" and "bestillingsdato" will change at runtime, so we
    // update the placeholders with real values
    var actualXml = orderCaptor.getValue();
    var orderXmlV1DateFormat = new SimpleDateFormat("dd.MM.yyyy");
    orderXmlV1DateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Oslo"));
    var orderXmlV1DateString =
        orderXmlV1DateFormat.format(Date.from(innsynskravBestilling.getCreated()));

    expectedXml =
        expectedXml
            .replaceFirst("ik_something", innsynskravBestillingDTO.getId())
            .replaceFirst("dd\\.mm\\.yyyy", orderXmlV1DateString);

    assertEquals(expectedXml, actualXml);

    // Verify the email body that was sent along with the XML
    var actualMail = mailCaptor.getValue();
    assertTrue(actualMail.contains(innsynskravBestillingDTO.getEmail()));
    assertTrue(actualMail.contains("Dokument: " + journalpostDTO.getOffentligTittel()));
    assertTrue(actualMail.contains("Sak: " + saksmappeDTO.getOffentligTittel()));
    // The email uses the same date format as OrderXML v1
    assertTrue(actualMail.contains("Bestillingsdato: " + orderXmlV1DateString));
    assertTrue(actualMail.contains("Saksbehandler: [Ufordelt]"));
    assertTrue(actualMail.contains("Enhet: \n"));

    // Verify that confirmation email was sent
    verify(javaMailSender, times(2)).send(any(MimeMessage.class));

    // Delete the InnsynskravBestilling
    response = deleteAdmin("/innsynskravBestilling/" + innsynskravBestillingId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravBestillingDTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    assertEquals(true, innsynskravBestillingDTO.getDeleted());
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

    // Insert dummy Arkivdel without a systemId (created automatically by dispatcher when no
    // arkivdel is sent)
    var dummyArkivdelResponse =
        post("/arkiv/" + arkivSysidDTO.getId() + "/arkivdel", getArkivJSON());
    var dummyArkivdelDTO = gson.fromJson(dummyArkivdelResponse.getBody(), ArkivdelDTO.class);

    // Create saksmappe on arkivdel
    var saksmappeArkdelResponse =
        post(
            "/arkivdel/" + arkivdelDTO.getId() + "/saksmappe",
            getSaksmappeJSON(),
            enhetOrderv2SecretKey);
    assertEquals(HttpStatus.CREATED, saksmappeArkdelResponse.getStatusCode());
    var saksmappeArkdelOrderV2DTO =
        gson.fromJson(saksmappeArkdelResponse.getBody(), SaksmappeDTO.class);

    // Create saksmappe on dummy Arkivdel
    var saksmappeDummyArkdelResponse =
        post(
            "/arkivdel/" + dummyArkivdelDTO.getId() + "/saksmappe",
            getSaksmappeJSON(),
            enhetOrderv2SecretKey);
    assertEquals(HttpStatus.CREATED, saksmappeDummyArkdelResponse.getStatusCode());
    var saksmappeDummyArkdelOrderV2DTO =
        gson.fromJson(saksmappeDummyArkdelResponse.getBody(), SaksmappeDTO.class);

    // Create 3 journalposts. 1 simple, 1 with erBehandlingsansvarlig = true and one using legacy
    // method to resolve saksbehandler
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
            "/saksmappe/" + saksmappeDummyArkdelOrderV2DTO.getId() + "/journalpost",
            jp,
            enhetOrderv2SecretKey);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    var journalpostOrderv2WithKorrPartDTO =
        gson.fromJson(journalpostResponse.getBody(), JournalpostDTO.class);

    // JP with legacy Saksbehandler
    jp = getJournalpostJSON();
    var kp1 = getKorrespondansepartJSON();
    kp1.put("saksbehandler", "Svein Sakshandsamar");
    kp1.put("administrativEnhet", "Den andre eininga for sakshandsaming");
    kp1.put("korrespondanseparttype", "mottaker");
    var kp2 = getKorrespondansepartJSON();
    kp2.put("saksbehandler", "[Ufordelt]");
    kp2.put("administrativEnhet", "[Ufordelt]");
    jp.put("korrespondansepart", new JSONArray(List.of(kp1, kp2)));
    jp.put("journalpostnummer", 3);
    jp.put("journalsekvensnummer", 3);
    journalpostResponse =
        post(
            "/saksmappe/" + saksmappeDummyArkdelOrderV2DTO.getId() + "/journalpost",
            jp,
            enhetOrderv2SecretKey);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    var journalpostOrderV2WithLegacyKorrPartDTO =
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

    // Insert InnsynskravBestilling
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    innsynskravBestillingJSON.put("email", brukerDTO.getEmail());
    var innsynskrav1JSON = getInnsynskravJSON();
    innsynskrav1JSON.put("journalpost", journalpostOrderv2PlainDTO.getId());
    var innsynskrav2JSON = getInnsynskravJSON();
    innsynskrav2JSON.put("journalpost", journalpostOrderv2WithKorrPartDTO.getId());
    var innsynskrav3JSON = getInnsynskravJSON();
    innsynskrav3JSON.put("journalpost", journalpostOrderV2WithLegacyKorrPartDTO.getId());

    innsynskravBestillingJSON.put(
        "innsynskrav",
        new JSONArray(List.of(innsynskrav1JSON, innsynskrav2JSON, innsynskrav3JSON)));
    response = post("/innsynskravBestilling", innsynskravBestillingJSON, token);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    var innsynskravBestillingId = innsynskravBestillingDTO.getId();
    assertEquals(brukerDTO.getEmail(), innsynskravBestillingDTO.getEmail());
    assertEquals(brukerDTO.getId(), innsynskravBestillingDTO.getBruker().getId());
    var innsynskravBestilling = innsynskravBestillingService.findById(innsynskravBestillingId);

    // Verify sending attempt
    // Confirmation email?
    // IPSender
    var expectedXml =
        IOUtils.toString(
            Objects.requireNonNull(
                InnsynskravBestillingControllerTest.class
                    .getClassLoader()
                    .getResourceAsStream("order-v2.xml")),
            StandardCharsets.UTF_8);
    var orderCaptor = ArgumentCaptor.forClass(String.class);
    var mailCaptor = ArgumentCaptor.forClass(String.class);

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
                        mailCaptor.capture(), // mail content
                        any(String.class), // IP orgnummer
                        any(Integer.class) // expectedResponseTimeoutDays
                        ));

    // Verify contents of order.xml. Replace placeholders with runtime values.
    var actualXml = orderCaptor.getValue();
    var norwegianShortDateFormat = new SimpleDateFormat("dd.MM.yyyy");
    norwegianShortDateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Oslo"));
    var isoDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    isoDateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Oslo"));
    var orderXmlV2DateString = isoDateFormat.format(Date.from(innsynskravBestilling.getCreated()));
    var norwegianDateString =
        norwegianShortDateFormat.format(Date.from(innsynskravBestilling.getCreated()));

    expectedXml =
        expectedXml
            .replaceFirst("ik_something", innsynskravBestillingDTO.getId())
            .replaceFirst("123456789", enhetOrderV2DTO.getOrgnummer())
            .replaceFirst("test@example.com", brukerDTO.getEmail())
            .replaceFirst("yyyy-mm-dd", orderXmlV2DateString)
            .replaceFirst("jp_firstDocument", journalpostOrderv2WithKorrPartDTO.getId())
            .replaceFirst("jp_secondDocument", journalpostOrderv2PlainDTO.getId())
            .replaceFirst("jp_thirdDocument", journalpostOrderV2WithLegacyKorrPartDTO.getId());

    assertEquals(expectedXml, actualXml);

    // Verify the email body that was sent along with the XML
    // There should be two Journalposts with different korrespondanseparts
    var actualMail = mailCaptor.getValue();
    assertTrue(actualMail.contains(innsynskravBestillingDTO.getEmail()));
    assertTrue(actualMail.contains("Dokument: " + journalpostDTO.getOffentligTittel()));
    assertTrue(actualMail.contains("Sak: " + saksmappeDTO.getOffentligTittel()));
    assertTrue(actualMail.contains("Bestillingsdato: " + norwegianDateString));
    assertTrue(actualMail.contains("Saksbehandler: [Ufordelt]"));
    assertTrue(actualMail.contains("Saksbehandler: " + kp.get("saksbehandler")));
    assertTrue(actualMail.contains("Saksbehandler: " + kp1.get("saksbehandler")));
    assertTrue(actualMail.contains("Enhet: \n"));
    assertTrue(actualMail.contains("Enhet: " + kp.get("administrativEnhet")));
    assertTrue(actualMail.contains("Enhet: " + kp1.get("administrativEnhet")));

    // Cleanup
    // Journalposts
    response = delete("/journalpost/" + journalpostOrderv2PlainDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = delete("/journalpost/" + journalpostOrderv2WithKorrPartDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Saksmappes
    response = delete("/saksmappe/" + saksmappeDummyArkdelOrderV2DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = delete("/saksmappe/" + saksmappeArkdelOrderV2DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Delete the InnsynskravBestilling
    response = deleteAdmin("/innsynskravBestilling/" + innsynskravBestillingId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravBestillingDTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    assertEquals(true, innsynskravBestillingDTO.getDeleted());

    // Delete the Bruker
    response = deleteAdmin("/bruker/" + brukerDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    brukerDTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    assertEquals(true, brukerDTO.getDeleted());

    // Delete Arkiv+Arkivdel
    response = delete("/arkivdel/" + arkivdelDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = delete("/arkiv/" + arkivSysidDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testInnsynskravSingleJournalpostUnverifiedUserEmail() throws Exception {
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("journalpost", journalpostNoEFormidlingDTO.getId());
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray().put(innsynskravJSON));

    // Create InnsynskravBestilling
    var innsynskravResponse = post("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.CREATED, innsynskravResponse.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(innsynskravResponse.getBody(), InnsynskravBestillingDTO.class);
    var innsynskravBestillingId = innsynskravBestillingDTO.getId();

    // Check that InnsynskravBestillingService tried to send an email
    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(1)).send(any(MimeMessage.class)));

    // Verify the InnsynskravBestilling
    var verificationSecret = innsynskravTestService.getVerificationSecret(innsynskravBestillingId);
    innsynskravResponse =
        patch(
            "/innsynskravBestilling/" + innsynskravBestillingId + "/verify/" + verificationSecret,
            null);
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    innsynskravBestillingDTO =
        gson.fromJson(innsynskravResponse.getBody(), InnsynskravBestillingDTO.class);
    assertEquals(true, innsynskravBestillingDTO.getVerified());

    // Check that InnsynskravBestillingService tried to send two more mails (one to the user and one
    // to the
    // Enhet)
    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(3)).send(any(MimeMessage.class)));

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

    // Delete the InnsynskravBestilling
    var deleteResponse = deleteAdmin("/innsynskravBestilling/" + innsynskravBestillingDTO.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    innsynskravBestillingDTO =
        gson.fromJson(deleteResponse.getBody(), InnsynskravBestillingDTO.class);
    assertEquals(true, innsynskravBestillingDTO.getDeleted());
  }

  @Test
  void testInnsynskravUnverifiedUserEformidlingAndEmail() throws Exception {
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("journalpost", journalpostDTO.getId());
    var innsynskravNoEFJSON = getInnsynskravJSON();
    innsynskravNoEFJSON.put("journalpost", journalpostNoEFormidlingDTO.getId());
    innsynskravBestillingJSON.put(
        "innsynskrav", new JSONArray().put(innsynskravJSON).put(innsynskravNoEFJSON));

    // Create InnsynskravBestilling
    var innsynskravResponse = post("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.CREATED, innsynskravResponse.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(innsynskravResponse.getBody(), InnsynskravBestillingDTO.class);
    var innsynskravBestillingId = innsynskravBestillingDTO.getId();

    // Check that InnsynskravBestillingService tried to send an email
    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(1)).send(any(MimeMessage.class)));

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

    // Verify the InnsynskravBestilling
    var verificationSecret = innsynskravTestService.getVerificationSecret(innsynskravBestillingId);
    innsynskravResponse =
        patch(
            "/innsynskravBestilling/" + innsynskravBestillingId + "/verify/" + verificationSecret,
            null);
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    innsynskravBestillingDTO =
        gson.fromJson(innsynskravResponse.getBody(), InnsynskravBestillingDTO.class);
    assertEquals(true, innsynskravBestillingDTO.getVerified());

    // Check that InnsynskravBestillingService tried to send two more emails
    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(3)).send(any(MimeMessage.class)));

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

    // Verify that the InnsynskravBestilling and Innsynskravs are in the DB
    assertEquals(
        HttpStatus.OK,
        getAdmin("/innsynskravBestilling/" + innsynskravBestillingDTO.getId()).getStatusCode());
    innsynskravTestService.assertSent(innsynskravBestillingId, 0);
    innsynskravTestService.assertSent(innsynskravBestillingId, 1);

    // Delete the InnsynskravBestilling
    var deleteResponse = deleteAdmin("/innsynskravBestilling/" + innsynskravBestillingDTO.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    innsynskravBestillingDTO =
        gson.fromJson(deleteResponse.getBody(), InnsynskravBestillingDTO.class);
    assertEquals(true, innsynskravBestillingDTO.getDeleted());

    // Verify that the innsynskravs are deleted
    assertEquals(
        HttpStatus.NOT_FOUND,
        getAdmin("/innsynskrav/" + innsynskravBestillingDTO.getInnsynskrav().get(0).getId())
            .getStatusCode());

    // Verify that the InnsynskravBestilling is deleted
    assertEquals(
        HttpStatus.NOT_FOUND,
        get("/innsynskravBestilling/" + innsynskravBestillingId).getStatusCode());
  }

  // Test sending an InnsynskravBestilling where a journalpost has been deleted before verifying the
  // InnsynskravBestilling
  @Test
  void testInnsynskravWithDeletedJournalpost() throws Exception {

    // Insert saksmappe with two journalposts, one will be deleted
    var arkivResponse = post("/arkiv", getArkivJSON());
    var arkivDTO = gson.fromJson(arkivResponse.getBody(), ArkivDTO.class);
    var arkivdelResponse = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    var arkivdelDTO = gson.fromJson(arkivdelResponse.getBody(), ArkivdelDTO.class);
    var journalpostToKeepJSON = getJournalpostJSON();
    journalpostToKeepJSON.put("offentligTittel", "JournalpostToKeep");
    journalpostToKeepJSON.put("offentligTittelSensitiv", "JournalpostToKeepSensitiv");
    var journalpostToDeleteJSON = getJournalpostJSON();
    journalpostToDeleteJSON.put("offentligTittel", "journalpostToDelete");
    journalpostToDeleteJSON.put("offentligTittelSensitiv", "journalpostToDeleteSensitiv");
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put(
        "journalpost", new JSONArray(List.of(journalpostToKeepJSON, journalpostToDeleteJSON)));
    var saksmappeResponse = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    var saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    var journalpostList =
        getJournalpostList(saksmappe.getId()).getItems(); // This list is sorted DESC
    var journalpostToKeep = journalpostList.get(1);
    var journalpostToDelete = journalpostList.get(0);

    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravToKeepJSON = getInnsynskravJSON();
    innsynskravToKeepJSON.put("journalpost", journalpostToKeep.getId());
    var innsynskravToDeleteJSON = getInnsynskravJSON();
    innsynskravToDeleteJSON.put("journalpost", journalpostToDelete.getId());
    innsynskravBestillingJSON.put(
        "innsynskrav", new JSONArray().put(innsynskravToKeepJSON).put(innsynskravToDeleteJSON));

    // Create InnsynskravBestilling
    var innsynskravResponse = post("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.CREATED, innsynskravResponse.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(innsynskravResponse.getBody(), InnsynskravBestillingDTO.class);

    // Verify that InnsynskravBestillingService tried to send an email
    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(1)).send(any(MimeMessage.class)));

    // Delete the journalpost that should be sent through eFormidling
    var deleteResponse = deleteAdmin("/journalpost/" + journalpostToDelete.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    var deletedJournalpost = gson.fromJson(deleteResponse.getBody(), JournalpostDTO.class);
    assertEquals(true, deletedJournalpost.getDeleted());

    // Verify that the journalpost is deleted
    var deletedJournalpostObject =
        journalpostRepository.findById(deletedJournalpost.getId()).orElse(null);
    assertNull(deletedJournalpostObject);

    // Verify the InnsynskravBestilling
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
    assertEquals(true, innsynskravBestillingDTO.getVerified());

    // Check that InnsynskravBestillingService tried to send another mail
    var mimeMessageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(2)).send(mimeMessageCaptor.capture()));

    // Check the content of mimeMessage
    // This is the confirmation mail sent to the user
    var mimeMessage = mimeMessageCaptor.getValue();
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

    // Check that InnsynskravBestillingSenderService tried to send through eFormidling
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

    // Delete the InnsynskravBestilling
    deleteResponse = deleteAdmin("/innsynskravBestilling/" + innsynskravBestillingDTO.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    innsynskravBestillingDTO =
        gson.fromJson(deleteResponse.getBody(), InnsynskravBestillingDTO.class);
    assertEquals(true, innsynskravBestillingDTO.getDeleted());

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
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("journalpost", journalpostDTO.getId());
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray().put(innsynskravJSON));

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

    // Insert InnsynskravBestilling
    var response = post("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    var innsynskravBestillingId = innsynskravBestillingDTO.getId();
    assertEquals("test@example.com", innsynskravBestillingDTO.getEmail());
    assertEquals(1, innsynskravBestillingDTO.getInnsynskrav().size());

    // Verify the InnsynskravBestilling
    var verificationSecret = innsynskravTestService.getVerificationSecret(innsynskravBestillingId);
    response =
        patch(
            "/innsynskravBestilling/" + innsynskravBestillingId + "/verify/" + verificationSecret,
            null);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravBestillingDTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    assertEquals(true, innsynskravBestillingDTO.getVerified());

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
    verify(javaMailSender, times(2)).send(any(MimeMessage.class));

    // Check that the innsynskrav isn't sent
    innsynskravTestService.assertNotSent(innsynskravBestillingId);

    // Try to send again, shouldn't send another mail, but should invoke ipSender once more
    innsynskravSenderService.sendInnsynskravBestilling(innsynskravBestillingId);
    verify(javaMailSender, times(2)).send(any(MimeMessage.class));
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

    // Check that the innsynskrav is sent
    innsynskravTestService.assertSent(innsynskravBestillingId);

    // Delete the InnsynskravBestilling
    response = deleteAdmin("/innsynskravBestilling/" + innsynskravBestillingId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravBestillingDTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    assertEquals(true, innsynskravBestillingDTO.getDeleted());
  }

  // Test that InnsynskravSenderService falls back to email after 3 failed eFormidling calls
  @Test
  void testInnsynskravEmailFallback() throws Exception {
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("journalpost", journalpostDTO.getId());
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray().put(innsynskravJSON));

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

    // Insert InnsynskravBestilling
    var response = post("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    var innsynskravBestillingId = innsynskravBestillingDTO.getId();
    assertEquals("test@example.com", innsynskravBestillingDTO.getEmail());
    assertEquals(1, innsynskravBestillingDTO.getInnsynskrav().size());

    // Verify the InnsynskravBestilling
    var verificationSecret = innsynskravTestService.getVerificationSecret(innsynskravBestillingId);
    response =
        patch(
            "/innsynskravBestilling/" + innsynskravBestillingId + "/verify/" + verificationSecret,
            null);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravBestillingDTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    assertEquals(true, innsynskravBestillingDTO.getVerified());

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
              verify(javaMailSender, times(2)).send(any(MimeMessage.class));
            });

    // Check that the innsynskrav isn't verified
    response =
        getAdmin("/innsynskravBestilling/" + innsynskravBestillingId + "?expand[]=innsynskrav");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravBestillingDTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    var expandableField = innsynskravBestillingDTO.getInnsynskrav().get(0);
    assertNotNull(expandableField.getExpandedObject(), "innsynskrav is not expanded");
    assertNull(expandableField.getExpandedObject().getSent());

    // Try to send again, shouldn't send another mail, but should invoke ipSender once more
    innsynskravSenderService.sendInnsynskravBestilling(innsynskravBestillingId);
    Awaitility.await()
        .untilAsserted(
            () -> {
              verify(javaMailSender, times(2)).send(any(MimeMessage.class));
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

    // Check that the innsynskrav isn't verified
    response =
        getAdmin("/innsynskravBestilling/" + innsynskravBestillingId + "?expand[]=innsynskrav");
    innsynskravBestillingDTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    expandableField = innsynskravBestillingDTO.getInnsynskrav().get(0);
    assertNull(expandableField.getExpandedObject().getSent());

    // Try to send again, shouldn't send another mail, but should invoke ipSender once more
    innsynskravSenderService.sendInnsynskravBestilling(innsynskravBestillingDTO.getId());
    Awaitility.await()
        .untilAsserted(
            () -> {
              verify(javaMailSender, times(2)).send(any(MimeMessage.class));
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

    // Check that the innsynskrav isn't verified
    response =
        getAdmin("/innsynskravBestilling/" + innsynskravBestillingId + "?expand[]=innsynskrav");
    innsynskravBestillingDTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    expandableField = innsynskravBestillingDTO.getInnsynskrav().get(0);
    assertNull(expandableField.getExpandedObject().getSent());

    // Try to send again, now it should fall back to email
    innsynskravSenderService.sendInnsynskravBestilling(innsynskravBestillingId);
    Awaitility.await()
        .untilAsserted(
            () -> {
              verify(javaMailSender, times(3)).send(any(MimeMessage.class));
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

    // Check that the innsynskrav is verified
    response =
        getAdmin("/innsynskravBestilling/" + innsynskravBestillingId + "?expand[]=innsynskrav");
    innsynskravBestillingDTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    expandableField = innsynskravBestillingDTO.getInnsynskrav().get(0);
    assertNotNull(expandableField.getExpandedObject().getSent());

    // Delete the InnsynskravBestilling
    response = deleteAdmin("/innsynskravBestilling/" + innsynskravBestillingDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravBestillingDTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    assertEquals(true, innsynskravBestillingDTO.getDeleted());
  }

  // Check that we get a 401 when trying to verify an InnsynskravBestilling with the wrong secret
  @Test
  void testInnsynskravVerifyWrongSecret() throws Exception {
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("journalpost", journalpostDTO.getId());
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray().put(innsynskravJSON));

    // Insert InnsynskravBestilling
    var response = post("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    var innsynskravBestillingId = innsynskravBestillingDTO.getId();

    // Verify the InnsynskravBestilling with the wrong secret
    response =
        patch("/innsynskravBestilling/" + innsynskravBestillingId + "/verify/wrongsecret", null);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Verify the InnsynskravBestilling with the correct secret
    var verificationSecret = innsynskravTestService.getVerificationSecret(innsynskravBestillingId);
    response =
        patch(
            "/innsynskravBestilling/" + innsynskravBestillingId + "/verify/" + verificationSecret,
            null);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravBestillingDTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    assertEquals(true, innsynskravBestillingDTO.getVerified());

    // Delete the InnsynskravBestilling
    response = deleteAdmin("/innsynskravBestilling/" + innsynskravBestillingDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravBestillingDTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    assertEquals(true, innsynskravBestillingDTO.getDeleted());
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

    // Insert InnsynskravBestilling
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    innsynskravBestillingJSON.put("email", brukerDTO.getEmail());
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("journalpost", journalpostDTO.getId());
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray().put(innsynskravJSON));
    response = post("/innsynskravBestilling", innsynskravBestillingJSON, token);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    var innsynskravBestillingId = innsynskravBestillingDTO.getId();
    assertEquals(brukerDTO.getEmail(), innsynskravBestillingDTO.getEmail());
    assertEquals(brukerDTO.getId(), innsynskravBestillingDTO.getBruker().getId());

    // Verify that the InnsynskravBestilling got sent automatically, and that a receipt was sent to
    // the user.
    // Both by email, as eFormidling is not used for this Enhet.
    Awaitility.await()
        .untilAsserted(
            () -> {
              verify(javaMailSender, times(2)).send(any(MimeMessage.class));
              innsynskravTestService.assertSent(innsynskravBestillingId);
            });

    // Delete the InnsynskravBestilling
    response = delete("/innsynskravBestilling/" + innsynskravBestillingId, token);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravBestillingDTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    assertEquals(true, innsynskravBestillingDTO.getDeleted());

    // Delete the Bruker
    response = delete("/bruker/" + brukerDTO.getId(), token);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    brukerDTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    assertEquals(true, brukerDTO.getDeleted());
  }

  @Test
  void testInnsynskravByInnsynskrav() throws Exception {
    // Add Saksmappe with ten journalposts
    var response = post("/arkiv", getArkivJSON());
    var _arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    response = post("/arkiv/" + _arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    var _arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    post("/arkivdel/" + _arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
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
    response = post("/arkivdel/" + _arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    var _saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    var journalpostDTOs = getJournalpostList(_saksmappeDTO.getId()).getItems();

    // Insert InnsynskravBestilling with 10 Innsynskrav
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    innsynskravBestillingJSON.put(
        "innsynskrav",
        new JSONArray(
            List.of(
                getInnsynskravJSON().put("journalpost", journalpostDTOs.get(0).getId()),
                getInnsynskravJSON().put("journalpost", journalpostDTOs.get(1).getId()),
                getInnsynskravJSON().put("journalpost", journalpostDTOs.get(2).getId()),
                getInnsynskravJSON().put("journalpost", journalpostDTOs.get(3).getId()),
                getInnsynskravJSON().put("journalpost", journalpostDTOs.get(4).getId()),
                getInnsynskravJSON().put("journalpost", journalpostDTOs.get(5).getId()),
                getInnsynskravJSON().put("journalpost", journalpostDTOs.get(6).getId()),
                getInnsynskravJSON().put("journalpost", journalpostDTOs.get(7).getId()),
                getInnsynskravJSON().put("journalpost", journalpostDTOs.get(8).getId()),
                getInnsynskravJSON().put("journalpost", journalpostDTOs.get(9).getId()))));
    response = post("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskrav1DTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);

    // Insert InnsynskravBestilling with 5 Innsynskrav
    innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    innsynskravBestillingJSON.put(
        "innsynskrav",
        new JSONArray(
            List.of(
                getInnsynskravJSON().put("journalpost", journalpostDTOs.get(0).getId()),
                getInnsynskravJSON().put("journalpost", journalpostDTOs.get(1).getId()),
                getInnsynskravJSON().put("journalpost", journalpostDTOs.get(2).getId()),
                getInnsynskravJSON().put("journalpost", journalpostDTOs.get(3).getId()),
                getInnsynskravJSON().put("journalpost", journalpostDTOs.get(4).getId()))));
    response = post("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskrav2DTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);

    var innsynskrav1DelList =
        innsynskrav1DTO.getInnsynskrav().stream().map(ExpandableField::getExpandedObject).toList();
    var type = new TypeToken<PaginatedList<InnsynskravDTO>>() {}.getType();
    testGenericList(
        type,
        innsynskrav1DelList,
        "/innsynskravBestilling/" + innsynskrav1DTO.getId() + "/innsynskrav",
        adminKey);

    var innsynskrav2DelList =
        innsynskrav2DTO.getInnsynskrav().stream().map(ExpandableField::getExpandedObject).toList();
    testGenericList(
        type,
        innsynskrav2DelList,
        "/innsynskravBestilling/" + innsynskrav2DTO.getId() + "/innsynskrav",
        adminKey);

    // Clean up
    assertEquals(
        HttpStatus.OK,
        deleteAdmin("/innsynskravBestilling/" + innsynskrav1DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.OK,
        deleteAdmin("/innsynskravBestilling/" + innsynskrav2DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/arkiv/" + _arkivDTO.getId()).getStatusCode());
  }

  @Test
  void testAvhending() throws Exception {
    // Insert avhendet journalpost
    var journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("avhendetTil", enhetNoEFDTO.getId());
    var response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var avhendetJournalpost = gson.fromJson(response.getBody(), JournalpostDTO.class);

    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskrav1JSON = getInnsynskravJSON();
    innsynskrav1JSON.put("journalpost", journalpostDTO.getId());
    var innsynskrav2JSON = getInnsynskravJSON();
    innsynskrav2JSON.put("journalpost", avhendetJournalpost.getId());
    innsynskravBestillingJSON.put(
        "innsynskrav", new JSONArray().put(innsynskrav1JSON).put(innsynskrav2JSON));

    // Insert InnsynskravBestilling
    response = post("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    var innsynskravBestillingId = innsynskravBestillingDTO.getId();
    assertEquals("test@example.com", innsynskravBestillingDTO.getEmail());
    assertEquals(2, innsynskravBestillingDTO.getInnsynskrav().size());

    // Verify enhets on Innsynskrav
    assertEquals(
        journalpostDTO.getJournalenhet().getId(),
        innsynskravBestillingDTO
            .getInnsynskrav()
            .getFirst()
            .getExpandedObject()
            .getEnhet()
            .getId());
    assertEquals(
        enhetNoEFDTO.getId(),
        innsynskravBestillingDTO.getInnsynskrav().getLast().getExpandedObject().getEnhet().getId());

    // Check that InnsynskravBestillingService tried to send an email.
    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(1)).send(any(MimeMessage.class)));

    // Check that the Innsynskrav is in the DB, with a verification secret
    var verificationSecret = innsynskravTestService.getVerificationSecret(innsynskravBestillingId);
    assertNotNull(verificationSecret);

    // Verify the InnsynskravBestilling
    response =
        patch(
            "/innsynskravBestilling/" + innsynskravBestillingId + "/verify/" + verificationSecret,
            null);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravBestillingDTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    assertEquals(true, innsynskravBestillingDTO.getVerified());

    // Verify that IPSender was called
    Awaitility.await()
        .untilAsserted(
            () ->
                verify(ipSender, times(1))
                    .sendInnsynskrav(
                        any(), // Order.xml
                        any(String.class), // transaction id
                        any(),
                        any(),
                        any(),
                        any(String.class), // mail content
                        any(String.class), // IP orgnummer
                        any(Integer.class) // expectedResponseTimeoutDays
                        ));

    // Verify that confirmation email was sent to user, and email to enhetNoEF
    verify(javaMailSender, times(3)).send(any(MimeMessage.class));

    // Delete the InnsynskravBestilling
    response = deleteAdmin("/innsynskravBestilling/" + innsynskravBestillingId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravBestillingDTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    assertEquals(true, innsynskravBestillingDTO.getDeleted());

    // Delete journalpost
    response = deleteAdmin("/journalpost/" + avhendetJournalpost.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  private String getTxtContent(MimeMessage mimeMessage) throws Exception {
    var content = mimeMessage.getContent();
    var mmContent = (MimeMultipart) content;

    // Check if this is multipart/alternative (no attachments) or multipart/mixed (with attachments)
    if (mmContent.getContentType().toLowerCase().contains("multipart/alternative")) {
      // multipart/alternative: text is at index 0
      var textPart = mmContent.getBodyPart(0);
      return textPart.getContent().toString();
    } else {
      // multipart/mixed: text/html content is nested in first part as multipart/alternative
      var firstPart = mmContent.getBodyPart(0);
      if (firstPart.getContent() instanceof MimeMultipart nestedMultipart) {
        var textPart = nestedMultipart.getBodyPart(0);
        return textPart.getContent().toString();
      } else {
        // Fallback: assume first part is text
        return firstPart.getContent().toString();
      }
    }
  }

  private String getHtmlContent(MimeMessage mimeMessage) throws Exception {
    var content = mimeMessage.getContent();
    var mmContent = (MimeMultipart) content;

    // Check if this is multipart/alternative (no attachments) or multipart/mixed (with attachments)
    if (mmContent.getContentType().toLowerCase().contains("multipart/alternative")) {
      // multipart/alternative: HTML is at index 1
      var htmlPart = mmContent.getBodyPart(1);
      return new String(htmlPart.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    } else {
      // multipart/mixed: text/html content is nested in first part as multipart/alternative
      var firstPart = mmContent.getBodyPart(0);
      if (firstPart.getContent() instanceof MimeMultipart nestedMultipart) {
        var htmlPart = nestedMultipart.getBodyPart(1);
        return new String(htmlPart.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      } else {
        // Fallback: assume this part contains HTML
        return new String(firstPart.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      }
    }
  }

  private String getAttachment(MimeMessage mimeMessage) throws Exception {
    var content = mimeMessage.getContent();
    var mmContent = (MimeMultipart) content;

    // Only multipart/mixed can have attachments
    if (mmContent.getContentType().toLowerCase().contains("multipart/mixed")
        && mmContent.getCount() > 1) {
      // Attachment is at index 1 (after the text/html content at index 0)
      var attachment = mmContent.getBodyPart(1);
      return attachment.getContent().toString();
    } else {
      return null;
    }
  }
}

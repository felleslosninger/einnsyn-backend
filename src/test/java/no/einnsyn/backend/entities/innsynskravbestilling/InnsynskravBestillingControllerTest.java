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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.regex.Pattern;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.authentication.bruker.models.TokenResponse;
import no.einnsyn.backend.common.exceptions.models.NetworkException;
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

  @Value("${application.innsynskrav.maxInnsynskravPerInnsynskravBestilling}")
  private Integer maxInnsynskravPerInnsynskravBestilling;

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
    var innsynskravBestilling = innsynskravBestillingService.find(innsynskravBestillingId);

    String expectedXml;
    try (var is =
        Objects.requireNonNull(
            InnsynskravBestillingControllerTest.class
                .getClassLoader()
                .getResourceAsStream("order-v1.xml"))) {
      expectedXml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
    var orderCaptor = ArgumentCaptor.forClass(String.class);
    var mailCaptor = ArgumentCaptor.forClass(String.class);

    // Verify that the integrasjonspunkt client was called
    Awaitility.await()
        .untilAsserted(
            () ->
                verify(ipSender, times(1))
                    .sendInnsynskrav(
                        orderCaptor.capture(), // Order.xml
                        eq(handteresAvDTO.getOrgnummer()), // handteresAv
                        eq(enhetDTO.getOrgnummer()),
                        eq(enhetDTO.getInnsynskravEpost()),
                        mailCaptor.capture() // mail content
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
    deleteInnsynskravFromBestilling(innsynskravBestillingDTO);
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
    var saksmappeDummyArkdelJSON = getSaksmappeJSON();
    saksmappeDummyArkdelJSON.put("sakssekvensnummer", 2);
    var saksmappeDummyArkdelResponse =
        post(
            "/arkivdel/" + dummyArkivdelDTO.getId() + "/saksmappe",
            saksmappeDummyArkdelJSON,
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
    var kp3 = getKorrespondansepartJSON();
    kp3.put("saksbehandler", "Ivar Intern");
    kp3.put("administrativEnhet", "Intern eining");
    kp3.put("korrespondanseparttype", "intern_mottaker");
    jp.put("korrespondansepart", new JSONArray(List.of(kp1, kp2, kp3)));
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
    var bruker = brukerService.find(brukerDTO.getId());
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
        new JSONArray(List.of(innsynskrav3JSON, innsynskrav1JSON, innsynskrav2JSON)));
    response = post("/innsynskravBestilling", innsynskravBestillingJSON, token);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    var innsynskravBestillingId = innsynskravBestillingDTO.getId();
    assertEquals(brukerDTO.getEmail(), innsynskravBestillingDTO.getEmail());
    assertEquals(brukerDTO.getId(), innsynskravBestillingDTO.getBruker().getId());
    var innsynskravBestilling = innsynskravBestillingService.find(innsynskravBestillingId);

    // Verify sending attempt
    // Confirmation email?
    // Integrasjonspunkt client
    String expectedXml;
    try (var is =
        Objects.requireNonNull(
            InnsynskravBestillingControllerTest.class
                .getClassLoader()
                .getResourceAsStream("order-v2.xml"))) {
      expectedXml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
    var orderCaptor = ArgumentCaptor.forClass(String.class);
    var mailCaptor = ArgumentCaptor.forClass(String.class);

    // Verify that the integrasjonspunkt client was called
    Awaitility.await()
        .untilAsserted(
            () ->
                verify(ipSender, times(1))
                    .sendInnsynskrav(
                        orderCaptor.capture(), // Order.xml
                        eq(enhetOrderV2DTO.getOrgnummer()), // handteresAv
                        eq(enhetOrderV2DTO.getOrgnummer()),
                        eq(enhetOrderV2DTO.getInnsynskravEpost()),
                        mailCaptor.capture() // mail content
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
    deleteInnsynskravFromBestilling(innsynskravBestillingDTO);

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
    awaitSideEffects();
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
            any(String.class));

    // Delete the InnsynskravBestilling
    var deleteResponse = deleteAdmin("/innsynskravBestilling/" + innsynskravBestillingDTO.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    innsynskravBestillingDTO =
        gson.fromJson(deleteResponse.getBody(), InnsynskravBestillingDTO.class);
    assertEquals(true, innsynskravBestillingDTO.getDeleted());
    deleteInnsynskravFromBestilling(innsynskravBestillingDTO);
  }

  @Test
  void testInnsynskravDocumentsAreSortedPerEnhetBySakAndDocumentNumber() throws Exception {
    var saksmappe1Json = getSaksmappeJSON();
    saksmappe1Json.put("sakssekvensnummer", 10);
    var saksmappe1Response =
        post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappe1Json);
    assertEquals(HttpStatus.CREATED, saksmappe1Response.getStatusCode());
    var saksmappe1 = gson.fromJson(saksmappe1Response.getBody(), SaksmappeDTO.class);

    var saksmappe2Json = getSaksmappeJSON();
    saksmappe2Json.put("sakssekvensnummer", 11);
    var saksmappe2Response =
        post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappe2Json);
    assertEquals(HttpStatus.CREATED, saksmappe2Response.getStatusCode());
    var saksmappe2 = gson.fromJson(saksmappe2Response.getBody(), SaksmappeDTO.class);

    var saksmappe3Json = getSaksmappeJSON();
    saksmappe3Json.put("sakssekvensnummer", 20);
    var saksmappe3Response =
        post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappe3Json, journalenhet2Key);
    assertEquals(HttpStatus.CREATED, saksmappe3Response.getStatusCode());
    var saksmappe3 = gson.fromJson(saksmappe3Response.getBody(), SaksmappeDTO.class);

    var saksmappe4Json = getSaksmappeJSON();
    saksmappe4Json.put("sakssekvensnummer", 21);
    var saksmappe4Response =
        post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappe4Json, journalenhet2Key);
    assertEquals(HttpStatus.CREATED, saksmappe4Response.getStatusCode());
    var saksmappe4 = gson.fromJson(saksmappe4Response.getBody(), SaksmappeDTO.class);

    var saksmappe5Json = getSaksmappeJSON();
    saksmappe5Json.put("saksaar", 2021);
    saksmappe5Json.put("sakssekvensnummer", 30);
    var saksmappe5Response =
        post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappe5Json);
    assertEquals(HttpStatus.CREATED, saksmappe5Response.getStatusCode());
    var saksmappe5 = gson.fromJson(saksmappe5Response.getBody(), SaksmappeDTO.class);

    var saksmappe6Json = getSaksmappeJSON();
    saksmappe6Json.put("saksaar", 2022);
    saksmappe6Json.put("sakssekvensnummer", 40);
    var saksmappe6Response =
        post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappe6Json, journalenhet2Key);
    assertEquals(HttpStatus.CREATED, saksmappe6Response.getStatusCode());
    var saksmappe6 = gson.fromJson(saksmappe6Response.getBody(), SaksmappeDTO.class);

    var jp11 = createJournalpost(saksmappe1.getId(), 3, journalenhetKey);
    var jp12 = createJournalpost(saksmappe1.getId(), 1, journalenhetKey);
    var jp13 = createJournalpost(saksmappe1.getId(), 2, journalenhetKey);
    var jp21 = createJournalpost(saksmappe2.getId(), 2, journalenhetKey);
    var jp22 = createJournalpost(saksmappe2.getId(), 1, journalenhetKey);

    var jp31 = createJournalpost(saksmappe3.getId(), 4, journalenhet2Key);
    var jp32 = createJournalpost(saksmappe3.getId(), 2, journalenhet2Key);
    var jp33 = createJournalpost(saksmappe3.getId(), 3, journalenhet2Key);
    var jp41 = createJournalpost(saksmappe4.getId(), 2, journalenhet2Key);
    var jp42 = createJournalpost(saksmappe4.getId(), 1, journalenhet2Key);

    var jp51 = createJournalpost(saksmappe5.getId(), 3, journalenhetKey);
    var jp52 = createJournalpost(saksmappe5.getId(), 1, journalenhetKey);
    var jp53 = createJournalpost(saksmappe5.getId(), 2, journalenhetKey);

    var jp61 = createJournalpost(saksmappe6.getId(), 3, journalenhet2Key);
    var jp62 = createJournalpost(saksmappe6.getId(), 1, journalenhet2Key);
    var jp63 = createJournalpost(saksmappe6.getId(), 2, journalenhet2Key);

    var innsynskravBestillingJson = getInnsynskravBestillingJSON();
    innsynskravBestillingJson.put(
        "innsynskrav",
        new JSONArray(
            List.of(
                getInnsynskravJSON().put("journalpost", jp41.getId()),
                getInnsynskravJSON().put("journalpost", jp12.getId()),
                getInnsynskravJSON().put("journalpost", jp31.getId()),
                getInnsynskravJSON().put("journalpost", jp22.getId()),
                getInnsynskravJSON().put("journalpost", jp13.getId()),
                getInnsynskravJSON().put("journalpost", jp42.getId()),
                getInnsynskravJSON().put("journalpost", jp11.getId()),
                getInnsynskravJSON().put("journalpost", jp32.getId()),
                getInnsynskravJSON().put("journalpost", jp21.getId()),
                getInnsynskravJSON().put("journalpost", jp33.getId()),
                getInnsynskravJSON().put("journalpost", jp61.getId()),
                getInnsynskravJSON().put("journalpost", jp52.getId()),
                getInnsynskravJSON().put("journalpost", jp63.getId()),
                getInnsynskravJSON().put("journalpost", jp51.getId()),
                getInnsynskravJSON().put("journalpost", jp62.getId()),
                getInnsynskravJSON().put("journalpost", jp53.getId()))));

    var response = post("/innsynskravBestilling", innsynskravBestillingJson);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    var verificationSecret =
        innsynskravTestService.getVerificationSecret(innsynskravBestillingDTO.getId());

    var orderCaptor = ArgumentCaptor.forClass(String.class);
    var mimeMessageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
    response =
        patch(
            "/innsynskravBestilling/"
                + innsynskravBestillingDTO.getId()
                + "/verify/"
                + verificationSecret,
            null);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    Awaitility.await()
        .untilAsserted(
            () ->
                verify(ipSender, times(2))
                    .sendInnsynskrav(
                        orderCaptor.capture(),
                        any(String.class),
                        any(String.class),
                        any(String.class),
                        any(String.class)));

    var actualOrders =
        orderCaptor.getAllValues().stream().map(this::extractOrderedDocuments).toList();
    assertTrue(
        actualOrders.contains(
            List.of(
                "2020/10-1",
                "2020/10-2",
                "2020/10-3",
                "2020/11-1",
                "2020/11-2",
                "2021/30-1",
                "2021/30-2",
                "2021/30-3")));
    assertTrue(
        actualOrders.contains(
            List.of(
                "2020/20-2",
                "2020/20-3",
                "2020/20-4",
                "2020/21-1",
                "2020/21-2",
                "2022/40-1",
                "2022/40-2",
                "2022/40-3")));

    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(2)).send(mimeMessageCaptor.capture()));

    var firstVirksomhet = gson.fromJson(get("/enhet/" + journalenhetId).getBody(), EnhetDTO.class);
    var secondVirksomhet =
        gson.fromJson(get("/enhet/" + journalenhet2Id).getBody(), EnhetDTO.class);

    // Find the correct mail
    var txtContent =
        normalizeLineEndings(findMailTextContaining(mimeMessageCaptor.getAllValues(), "Sakstittel:"));
    assertTrue(txtContent.contains("Sakstittel: testOffentligTittelSensitiv"));
    assertTrue(txtContent.contains("Journaltittel: JournalpostOffentligTittelSensitiv"));
    assertTrue(txtContent.contains("Virksomhet: " + firstVirksomhet.getNavn()));
    assertTrue(txtContent.contains("Virksomhet: " + secondVirksomhet.getNavn()));
    assertTrue(txtContent.contains("innsynskravepost@example.com"));
    // Check the order in that mail
    assertDocumentsInOrder(
        txtContent,
        List.of(
            firstVirksomhet.getNavn(),
            "Doknr: 1\nSaksnr: 2020/10",
            "Doknr: 2\nSaksnr: 2020/10",
            "Doknr: 3\nSaksnr: 2020/10",
            "Doknr: 1\nSaksnr: 2020/11",
            "Doknr: 2\nSaksnr: 2020/11",
            "Doknr: 1\nSaksnr: 2021/30",
            "Doknr: 2\nSaksnr: 2021/30",
            "Doknr: 3\nSaksnr: 2021/30",
            secondVirksomhet.getNavn(),
            "Doknr: 2\nSaksnr: 2020/20",
            "Doknr: 3\nSaksnr: 2020/20",
            "Doknr: 4\nSaksnr: 2020/20",
            "Doknr: 1\nSaksnr: 2020/21",
            "Doknr: 2\nSaksnr: 2020/21",
            "Doknr: 1\nSaksnr: 2022/40",
            "Doknr: 2\nSaksnr: 2022/40",
            "Doknr: 3\nSaksnr: 2022/40"));

    response = deleteAdmin("/innsynskravBestilling/" + innsynskravBestillingDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    deleteInnsynskravFromBestilling(innsynskravBestillingDTO);

    delete("/saksmappe/" + saksmappe1.getId());
    delete("/saksmappe/" + saksmappe2.getId());
    delete("/saksmappe/" + saksmappe5.getId());
    delete("/saksmappe/" + saksmappe3.getId(), journalenhet2Key);
    delete("/saksmappe/" + saksmappe4.getId(), journalenhet2Key);
    delete("/saksmappe/" + saksmappe6.getId(), journalenhet2Key);
    awaitSideEffects();
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

    // Check that InnsynskravSenderService didn't send anything to the integrasjonspunkt client
    verify(ipSender, times(0))
        .sendInnsynskrav(
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class));

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

    // Check that InnsynskravSenderService sent to the integrasjonspunkt client
    verify(ipSender, times(1))
        .sendInnsynskrav(
            any(String.class),
            any(String.class), // HandteresAv
            any(String.class), // Administrativ enhet
            eq("innsynskravepost@example.com"),
            any(String.class) // Email text. TODO: Verify that the journalpost titles are mentioned
            );

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

    // Verify that the innsynskravs are not deleted
    assertEquals(
        HttpStatus.OK,
        getAdmin("/innsynskrav/" + innsynskravBestillingDTO.getInnsynskrav().get(0).getId())
            .getStatusCode());

    // Verify that the InnsynskravBestilling is deleted
    assertEquals(
        HttpStatus.NOT_FOUND,
        get("/innsynskravBestilling/" + innsynskravBestillingId).getStatusCode());

    // Cleanup
    deleteInnsynskravFromBestilling(innsynskravBestillingDTO);
  }

  // Test sending an InnsynskravBestilling where a journalpost has been deleted before verifying the
  // InnsynskravBestilling
  @Test
  void testInnsynskravWithDeletedJournalpost() throws Exception {

    // Insert saksmappe with two journalposts, one will be deleted
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
    var innsynskravBestillingDTO1 =
        gson.fromJson(innsynskravResponse.getBody(), InnsynskravBestillingDTO.class);

    // Create an additional InnsynskravBestilling with only the Journalpost that will be deleted
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray().put(innsynskravToDeleteJSON));
    innsynskravResponse = post("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.CREATED, innsynskravResponse.getStatusCode());
    var innsynskravBestillingDTO2 =
        gson.fromJson(innsynskravResponse.getBody(), InnsynskravBestillingDTO.class);

    // Verify that InnsynskravBestillingService tried to send two emails
    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(2)).send(any(MimeMessage.class)));

    // Delete the journalpost that should be sent through eFormidling
    var deleteResponse = deleteAdmin("/journalpost/" + journalpostToDelete.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    var deletedJournalpost = gson.fromJson(deleteResponse.getBody(), JournalpostDTO.class);
    assertEquals(true, deletedJournalpost.getDeleted());

    // Verify that the journalpost is deleted
    var deletedJournalpostObject =
        journalpostRepository.findById(deletedJournalpost.getId()).orElse(null);
    assertNull(deletedJournalpostObject);

    // Verify first InnsynskravBestilling
    verifyAnonymousInnsynskravBestilling(innsynskravBestillingDTO1.getId());

    // Check that InnsynskravBestillingService tried to send another mail
    var mimeMessageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(3)).send(mimeMessageCaptor.capture()));

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

    // Check that InnsynskravBestillingSenderService tried to send through eFormidling
    verify(ipSender, times(1))
        .sendInnsynskrav(
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class));

    // Verify the second Innsynskravbestilling
    verifyAnonymousInnsynskravBestilling(innsynskravBestillingDTO2.getId());

    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(4)).send(mimeMessageCaptor.capture()));

    mimeMessage = mimeMessageCaptor.getValue();
    txtContent = getTxtContent(mimeMessage);
    htmlContent = getHtmlContent(mimeMessage);
    attachmentContent = getAttachment(mimeMessage);

    var language = innsynskravBestillingDTO2.getLanguage();
    var locale = Locale.forLanguageTag(language);
    var languageBundle = ResourceBundle.getBundle("mailtemplates/mailtemplates", locale);

    // Check mail contents
    assertNull(attachmentContent);
    assertTrue(txtContent.contains(languageBundle.getString("orderConfirmationToBrukerEmpty")));
    assertTrue(htmlContent.contains(languageBundle.getString("orderConfirmationToBrukerEmpty")));
    assertFalse(txtContent.contains("Sekvensnr"));
    assertFalse(htmlContent.contains("Sekvensnr"));

    // As the only document in the InnsynskravBestilling was deleted, nothing should be sent through
    // eFormidling
    verify(ipSender, times(1))
        .sendInnsynskrav(
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class));

    // Cleanup
    // Delete both InnsynskravBestilling
    deleteResponse = deleteAdmin("/innsynskravBestilling/" + innsynskravBestillingDTO1.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    innsynskravBestillingDTO1 =
        gson.fromJson(deleteResponse.getBody(), InnsynskravBestillingDTO.class);
    assertNotNull(innsynskravBestillingDTO1);
    assertEquals(true, innsynskravBestillingDTO1.getDeleted());
    deleteInnsynskravFromBestilling(innsynskravBestillingDTO1);

    deleteResponse = deleteAdmin("/innsynskravBestilling/" + innsynskravBestillingDTO2.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    innsynskravBestillingDTO2 =
        gson.fromJson(deleteResponse.getBody(), InnsynskravBestillingDTO.class);
    assertNotNull(innsynskravBestillingDTO2);
    assertEquals(true, innsynskravBestillingDTO2.getDeleted());
    deleteInnsynskravFromBestilling(innsynskravBestillingDTO2);

    // Delete the Saksmappe
    deleteResponse = delete("/saksmappe/" + saksmappe.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    saksmappe = gson.fromJson(deleteResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe);
    assertEquals(true, saksmappe.getDeleted());
  }

  void verifyAnonymousInnsynskravBestilling(String innsynskravBestillingId) throws Exception {
    var verificationSecret = innsynskravTestService.getVerificationSecret(innsynskravBestillingId);
    var innsynskravResponse =
        patch(
            "/innsynskravBestilling/" + innsynskravBestillingId + "/verify/" + verificationSecret,
            null);
    assertEquals(HttpStatus.OK, innsynskravResponse.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(innsynskravResponse.getBody(), InnsynskravBestillingDTO.class);
    assertNotNull(innsynskravBestillingDTO);
    assertEquals(true, innsynskravBestillingDTO.getVerified());
  }

  @Test
  void testInnsynskravOnNonexistingJournalpost() throws Exception {
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("journalpost", "jp_thisiddoesnotexist");
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray().put(innsynskravJSON));

    var response = post("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void testInnsynskravWithFailingEformidling() throws Exception {
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("journalpost", journalpostDTO.getId());
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray().put(innsynskravJSON));

    // Make the integrasjonspunkt client fail the first time, then succeed the second time
    when(ipSender.sendInnsynskrav(
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class)))
        .thenThrow(new NetworkException(""))
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
                        any(String.class)));

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
            any(String.class));

    // Check that the innsynskrav is sent
    innsynskravTestService.assertSent(innsynskravBestillingId);

    // Delete the InnsynskravBestilling
    response = deleteAdmin("/innsynskravBestilling/" + innsynskravBestillingId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravBestillingDTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    assertEquals(true, innsynskravBestillingDTO.getDeleted());
    deleteInnsynskravFromBestilling(innsynskravBestillingDTO);
  }

  // Test that InnsynskravSenderService falls back to email after 3 failed eFormidling calls
  @Test
  void testInnsynskravEmailFallback() throws Exception {
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("journalpost", journalpostDTO.getId());
    innsynskravBestillingJSON.put("innsynskrav", new JSONArray().put(innsynskravJSON));

    // Make the integrasjonspunkt client fail the first time, then succeed the second time
    when(ipSender.sendInnsynskrav(
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class),
            any(String.class)))
        .thenThrow(new NetworkException(""));

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
                      any(String.class));

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
                      any(String.class));
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
                      any(String.class));
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
                      any(String.class));
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
    deleteInnsynskravFromBestilling(innsynskravBestillingDTO);
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
    deleteInnsynskravFromBestilling(innsynskravBestillingDTO);
  }

  @Test
  void testInnsynskravWhenLoggedIn() throws Exception {
    // Create and activate Bruker
    var brukerJSON = getBrukerJSON();
    var response = post("/bruker", brukerJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var brukerDTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    var bruker = brukerService.find(brukerDTO.getId());
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
    deleteInnsynskravFromBestilling(innsynskravBestillingDTO);

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
    deleteInnsynskravFromBestilling(innsynskrav1DTO);
    deleteInnsynskravFromBestilling(innsynskrav2DTO);

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

    // Verify that the integrasjonspunkt client was called
    Awaitility.await()
        .untilAsserted(
            () ->
                verify(ipSender, times(1))
                    .sendInnsynskrav(
                        any(), // Order.xml
                        any(),
                        any(),
                        any(),
                        any(String.class) // mail content
                        ));

    // Verify that confirmation email was sent to user, and email to enhetNoEF
    Awaitility.await()
        .untilAsserted(() -> verify(javaMailSender, times(3)).send(any(MimeMessage.class)));

    // Delete the InnsynskravBestilling
    response = deleteAdmin("/innsynskravBestilling/" + innsynskravBestillingId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravBestillingDTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    assertEquals(true, innsynskravBestillingDTO.getDeleted());
    deleteInnsynskravFromBestilling(innsynskravBestillingDTO);

    // Delete journalpost
    response = deleteAdmin("/journalpost/" + avhendetJournalpost.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testInnsynskravKorrespondansepartWithoutSaksbehandler() throws Exception {
    // Setup data
    var saksmappeArkdelResponse =
        post(
            "/arkivdel/" + arkivdelDTO.getId() + "/saksmappe",
            getSaksmappeJSON(),
            enhetOrderv2SecretKey);
    assertEquals(HttpStatus.CREATED, saksmappeArkdelResponse.getStatusCode());
    var saksmappeArkdelOrderV2DTO =
        gson.fromJson(saksmappeArkdelResponse.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappeArkdelOrderV2DTO);
    // JP with legacy Saksbehandler
    var jp = getJournalpostJSON();
    // lag korrpart i rett retning - med berre admEnhet
    var kp1 = getKorrespondansepartJSON();
    kp1.put("administrativEnhet", "Den andre eininga for sakshandsaming");
    kp1.put("korrespondanseparttype", "mottaker");
    // lag korrpart i feil retning - med både sakshandsamar og admEnhet
    var kp2 = getKorrespondansepartJSON();
    kp2.put("saksbehandler", "Avsendars Sakshandsamar");
    kp2.put("administrativEnhet", "Ekstern Thingamajig");
    kp2.put("korrespondanseparttype", "avsender");
    var kp3 = getKorrespondansepartJSON();
    kp3.put("saksbehandler", "[Ufordelt]");
    kp3.put("administrativEnhet", "[Ufordelt]");
    var kp4 = getKorrespondansepartJSON();
    kp4.put("saksbehandler", "Knut Kopi");
    kp4.put("administrativEnhet", "Intern eining");
    kp4.put("korrespondanseparttype", "kopimottaker");
    jp.put("korrespondansepart", new JSONArray(List.of(kp1, kp2, kp3, kp4)));
    jp.put("journalpostnummer", 3);
    jp.put("journalsekvensnummer", 3);
    var journalpostResponse =
        post(
            "/saksmappe/" + saksmappeArkdelOrderV2DTO.getId() + "/journalpost",
            jp,
            enhetOrderv2SecretKey);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    var journalpostOrderV2WithLegacyKorrPartDTO =
        gson.fromJson(journalpostResponse.getBody(), JournalpostDTO.class);
    assertNotNull(journalpostOrderV2WithLegacyKorrPartDTO);

    // Create and activate Bruker
    var brukerJSON = getBrukerJSON();
    var response = post("/bruker", brukerJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var brukerDTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    assertNotNull(brukerDTO);
    var bruker = brukerService.find(brukerDTO.getId());
    assertNotNull(bruker);
    response = patch("/bruker/" + brukerDTO.getId() + "/activate/" + bruker.getSecret(), null);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Login
    var loginRequest = new JSONObject();
    loginRequest.put("username", brukerJSON.get("email"));
    loginRequest.put("password", brukerJSON.get("password"));
    response = post("/auth/token", loginRequest);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var tokenResponse = gson.fromJson(response.getBody(), TokenResponse.class);
    assertNotNull(tokenResponse);
    var token = tokenResponse.getToken();

    // Insert InnsynskravBestilling
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    innsynskravBestillingJSON.put("email", brukerDTO.getEmail());
    var innsynskravJSON = getInnsynskravJSON();
    innsynskravJSON.put("journalpost", journalpostOrderV2WithLegacyKorrPartDTO.getId());

    innsynskravBestillingJSON.put("innsynskrav", new JSONArray(List.of(innsynskravJSON)));
    response = post("/innsynskravBestilling", innsynskravBestillingJSON, token);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    assertNotNull(innsynskravBestillingDTO);
    var innsynskravBestillingId = innsynskravBestillingDTO.getId();
    assertEquals(brukerDTO.getEmail(), innsynskravBestillingDTO.getEmail());
    assertEquals(brukerDTO.getId(), innsynskravBestillingDTO.getBruker().getId());

    var orderCaptor = ArgumentCaptor.forClass(String.class);
    var mailCaptor = ArgumentCaptor.forClass(String.class);

    // Verify that the integrasjonspunkt client was called
    Awaitility.await()
        .untilAsserted(
            () ->
                verify(ipSender, times(1))
                    .sendInnsynskrav(
                        orderCaptor.capture(), // Order.xml
                        eq(enhetOrderV2DTO.getOrgnummer()), // handteresAv
                        eq(enhetOrderV2DTO.getOrgnummer()),
                        eq(enhetOrderV2DTO.getInnsynskravEpost()),
                        mailCaptor.capture() // mail content
                        ));

    // Verify contents of order.xml. Replace placeholders with runtime values.
    var actualXml = orderCaptor.getValue();
    assertTrue(actualXml.contains("<saksbehandler>[Ufordelt]</saksbehandler>"));
    assertTrue(actualXml.contains("<admEnhet>" + kp1.get("administrativEnhet") + "</admEnhet>"));

    // Verify contents of email
    var actualMail = mailCaptor.getValue();
    assertTrue(actualMail.contains("Saksbehandler: [Ufordelt]"));
    assertTrue(actualMail.contains("Enhet: " + kp1.get("administrativEnhet")));

    // Cleanup
    // Journalposts
    response = delete("/journalpost/" + journalpostOrderV2WithLegacyKorrPartDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Saksmappes
    response = delete("/saksmappe/" + saksmappeArkdelOrderV2DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Delete the InnsynskravBestilling
    response = deleteAdmin("/innsynskravBestilling/" + innsynskravBestillingId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    innsynskravBestillingDTO = gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    assertNotNull(innsynskravBestillingDTO);
    assertEquals(true, innsynskravBestillingDTO.getDeleted());
    deleteInnsynskravFromBestilling(innsynskravBestillingDTO);

    // Delete the Bruker
    response = deleteAdmin("/bruker/" + brukerDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    brukerDTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    assertNotNull(brukerDTO);
    assertEquals(true, brukerDTO.getDeleted());
  }

  @Test
  void testInnsynskravBestillingExceedsMaxInnsynskrav() throws Exception {
    // Create a request with one more innsynskrav than the configured limit.
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravArray = new JSONArray();
    for (var i = 0; i < maxInnsynskravPerInnsynskravBestilling + 1; i++) {
      var innsynskravJSON = getInnsynskravJSON();
      innsynskravJSON.put("journalpost", journalpostDTO.getId());
      innsynskravArray.put(innsynskravJSON);
    }
    innsynskravBestillingJSON.put("innsynskrav", innsynskravArray);

    var response = post("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertTrue(response.getBody().contains("Too many Innsynskrav"));
  }

  @Test
  void testInnsynskravBestillingAtMaxInnsynskravLimit() throws Exception {
    // Create a request with exactly the configured limit (should succeed).
    var innsynskravBestillingJSON = getInnsynskravBestillingJSON();
    var innsynskravArray = new JSONArray();
    for (var i = 0; i < maxInnsynskravPerInnsynskravBestilling; i++) {
      var innsynskravJSON = getInnsynskravJSON();
      innsynskravJSON.put("journalpost", journalpostDTO.getId());
      innsynskravArray.put(innsynskravJSON);
    }
    innsynskravBestillingJSON.put("innsynskrav", innsynskravArray);

    var response = post("/innsynskravBestilling", innsynskravBestillingJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var innsynskravBestillingDTO =
        gson.fromJson(response.getBody(), InnsynskravBestillingDTO.class);
    assertEquals(
        maxInnsynskravPerInnsynskravBestilling, innsynskravBestillingDTO.getInnsynskrav().size());

    // Cleanup
    var deleteResponse = deleteAdmin("/innsynskravBestilling/" + innsynskravBestillingDTO.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    deleteInnsynskravFromBestilling(innsynskravBestillingDTO);
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

  private JournalpostDTO createJournalpost(String saksmappeId, int journalpostnummer, String apiKey)
      throws Exception {
    var journalpostJson = getJournalpostJSON();
    journalpostJson.put("journalpostnummer", journalpostnummer);
    var response = post("/saksmappe/" + saksmappeId + "/journalpost", journalpostJson, apiKey);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    return gson.fromJson(response.getBody(), JournalpostDTO.class);
  }

  private List<String> extractOrderedDocuments(String xml) {
    var pattern =
        Pattern.compile(
            "<dokument>\\s*.*?<saksnr>(.*?)</saksnr>\\s*<dokumentnr>(.*?)</dokumentnr>",
            Pattern.DOTALL);
    var matcher = pattern.matcher(xml);
    var documents = new ArrayList<String>();
    while (matcher.find()) {
      documents.add(matcher.group(1) + "-" + matcher.group(2));
    }
    return documents;
  }

  private void assertDocumentsInOrder(String content, List<String> documents) {
    var previousIndex = -1;
    for (var document : documents) {
      var currentIndex = content.indexOf(document);
      assertTrue(currentIndex >= 0, "Expected content to contain: " + document);
      assertTrue(currentIndex > previousIndex, "Expected content order for: " + document);
      previousIndex = currentIndex;
    }
  }

  private String findMailTextContaining(List<MimeMessage> messages, String expectedText)
      throws Exception {
    for (var message : messages) {
      var textContent = normalizeLineEndings(getTxtContent(message));
      if (textContent.contains(expectedText)) {
        return textContent;
      }
    }
    throw new NoSuchElementException("Could not find mail containing: " + expectedText);
  }

  private String normalizeLineEndings(String text) {
    return text.replace("\r\n", "\n");
  }
}

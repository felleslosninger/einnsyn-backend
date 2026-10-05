package no.einnsyn.backend.auth.ansattporten;

import static no.einnsyn.backend.auth.ansattporten.AnsattportenTestKeys.TEST_KEY_ID;
import static no.einnsyn.backend.auth.ansattporten.AnsattportenTestKeys.TEST_KEY_PAIR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.gson.reflect.TypeToken;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.authinfo.models.AuthInfo;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.apikey.models.ApiKeyDTO;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.enhet.EnhetService;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import no.einnsyn.backend.utils.id.IdGenerator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AnsattportenAuthenticationTest extends EinnsynControllerTestBase {

  @Value("${application.ansattporten.issuerUri}")
  private String ansattportenIssuerUri;

  @Autowired private EnhetService enhetService;

  private static final String TEST_CLIENT_ID = "einnsyn-test-client";
  private static final String TEST_RESOURCE = "urn:altinn:resource:einnsyn-api";

  @Test
  void testAuthInfo() throws Exception {
    var jwt = generateMockAltinn3Jwt(journalenhetOrgnummer);
    var response = get("/me", jwt);
    var authInfo = gson.fromJson(response.getBody(), AuthInfo.class);
    assertEquals("Ansattporten", authInfo.getAuthType());
    assertEquals("Enhet", authInfo.getType());
    assertEquals(journalenhetId, authInfo.getId());
    assertEquals(journalenhetOrgnummer, authInfo.getOrgnummer());

    jwt = generateMockAltinn3Jwt(journalenhet2Orgnummer);
    response = get("/me", jwt);
    authInfo = gson.fromJson(response.getBody(), AuthInfo.class);
    assertEquals("Ansattporten", authInfo.getAuthType());
    assertEquals("Enhet", authInfo.getType());
    assertEquals(journalenhet2Id, authInfo.getId());
    assertEquals(journalenhet2Orgnummer, authInfo.getOrgnummer());

    jwt = generateMockAltinn3Jwt("123456789");
    response = get("/me", jwt);
    authInfo = gson.fromJson(response.getBody(), AuthInfo.class);
    assertEquals("Ansattporten", authInfo.getAuthType());
    assertEquals("Enhet", authInfo.getType());
    assertEquals("123456789", authInfo.getOrgnummer());
    assertNull(authInfo.getId());
  }

  @Test
  void testAuthorization() throws Exception {
    // Create arkiv / arkivdel / saksmappe as Journalenhet2
    var response = post("/arkiv", getArkivJSON(), journalenhet2Key);
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertEquals(journalenhet2Id, arkivDTO.getJournalenhet().getId());
    response =
        post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON(), journalenhet2Key);
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertEquals(journalenhet2Id, arkivdelDTO.getJournalenhet().getId());
    response =
        post(
            "/arkivdel/" + arkivdelDTO.getId() + "/saksmappe",
            getSaksmappeJSON(),
            journalenhet2Key);
    var saksmappeDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertEquals(journalenhet2Id, saksmappeDTO.getJournalenhet().getId());

    // Should not be able to update as Journalenhet1 using API key
    response = patch("/arkiv/" + arkivDTO.getId(), getArkivJSON(), journalenhetKey);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    response = patch("/arkivdel/" + arkivdelDTO.getId(), getArkivdelJSON(), journalenhetKey);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    response = patch("/saksmappe/" + saksmappeDTO.getId(), getSaksmappeJSON(), journalenhetKey);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Should be able to update as Journalenhet2 using Ansattporten Altinn 3 JWT
    var journalenhet2Jwt = generateMockAltinn3Jwt(journalenhet2Orgnummer);
    response = patch("/arkiv/" + arkivDTO.getId(), getArkivJSON(), journalenhet2Jwt);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertEquals(journalenhet2Id, arkivDTO.getJournalenhet().getId());
    response = patch("/arkivdel/" + arkivdelDTO.getId(), getArkivdelJSON(), journalenhet2Jwt);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Should not be able to update as Journalenhet1 using Ansattporten Altinn 3 JWT
    var journalenhet1Jwt = generateMockAltinn3Jwt(journalenhetOrgnummer);
    response = patch("/arkiv/" + arkivDTO.getId(), getArkivJSON(), journalenhet1Jwt);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    response = patch("/arkivdel/" + arkivdelDTO.getId(), getArkivdelJSON(), journalenhet1Jwt);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    response = patch("/saksmappe/" + saksmappeDTO.getId(), getSaksmappeJSON(), journalenhet1Jwt);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Should be able to delete as Journalenhet2 using Ansattporten Altinn 3 JWT
    response = delete("/saksmappe/" + saksmappeDTO.getId(), journalenhet2Jwt);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = delete("/arkivdel/" + arkivdelDTO.getId(), journalenhet2Jwt);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = delete("/arkiv/" + arkivDTO.getId(), journalenhet2Jwt);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void shouldAllowAddingEnhetWhenAuthenticatedOrgnummerMatchesBodyOrgnummer() throws Exception {
    var orgnummer = "123456789";
    var jwt = generateMockAltinn3Jwt(orgnummer);
    var enhetJSON = getEnhetJSON();
    enhetJSON.put("orgnummer", orgnummer);
    enhetJSON.put("parent", rootEnhetId);

    var response = post("/enhet", enhetJSON, jwt);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var enhetDTO = gson.fromJson(response.getBody(), EnhetDTO.class);
    assertEquals(orgnummer, enhetDTO.getOrgnummer());
    assertEquals(rootEnhetId, enhetDTO.getParent().getId());

    response = deleteAdmin("/enhet/" + enhetDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void selfRegisteredEnhetStaysUnverifiedUntilAdminVerifies() throws Exception {
    var orgnummer = "723456789";
    var jwt = generateMockAltinn3Jwt(orgnummer);
    var enhetJSON = getEnhetJSON();
    enhetJSON.put("orgnummer", orgnummer);
    enhetJSON.put("parent", rootEnhetId);

    var response = post("/enhet", enhetJSON, jwt);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var enhetDTO = gson.fromJson(response.getBody(), EnhetDTO.class);
    var enhetId = enhetDTO.getId();
    assertEquals(false, enhetDTO.getVerified());

    // /me reports the id of the Enhet, but the principal itself still carries only the orgnummer
    var authInfo = gson.fromJson(get("/me", jwt).getBody(), AuthInfo.class);
    assertEquals(enhetId, authInfo.getId());
    assertEquals(orgnummer, authInfo.getOrgnummer());

    // Cannot publish, create API keys or add underenhets
    assertEquals(HttpStatus.FORBIDDEN, post("/arkiv", getArkivJSON(), jwt).getStatusCode());
    assertEquals(
        HttpStatus.FORBIDDEN,
        post("/enhet/" + enhetId + "/apiKey", getApiKeyJSON(), jwt).getStatusCode());
    assertEquals(
        HttpStatus.FORBIDDEN,
        post("/enhet/" + enhetId + "/underenhet", getEnhetJSON(), jwt).getStatusCode());

    // Does not exist for anyone else
    assertEquals(HttpStatus.NOT_FOUND, getAnon("/enhet/" + enhetId).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/enhet/" + enhetId).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/enhet/" + orgnummer).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, patch("/enhet/" + enhetId, new JSONObject()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, delete("/enhet/" + enhetId).getStatusCode());
    assertTrue(listEnhetIds(getAnon("/enhet?orgnummer=" + orgnummer)).isEmpty());
    assertFalse(listEnhetIds(get("/enhet?ids=" + enhetId)).contains(enhetId));
    assertFalse(
        listEnhetIds(getAnon("/enhet/" + rootEnhetId + "/underenhet?limit=100")).contains(enhetId));
    var rootDTO = gson.fromJson(getAnon("/enhet/" + rootEnhetId).getBody(), EnhetDTO.class);
    assertFalse(
        rootDTO.getUnderenhet().stream().map(ExpandableField::getId).toList().contains(enhetId));
    for (var sub : List.of("/arkiv", "/apiKey", "/innsynskrav", "/underenhet")) {
      assertEquals(HttpStatus.NOT_FOUND, getAnon("/enhet/" + enhetId + sub).getStatusCode(), sub);
      assertEquals(HttpStatus.NOT_FOUND, get("/enhet/" + enhetId + sub).getStatusCode(), sub);
    }

    // Exists for itself and for admins, and both see verified
    response = get("/enhet/" + enhetId, jwt);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(false, gson.fromJson(response.getBody(), EnhetDTO.class).getVerified());
    assertEquals(HttpStatus.OK, get("/enhet/" + orgnummer, jwt).getStatusCode());
    assertEquals(HttpStatus.OK, get("/enhet/" + enhetId + "/arkiv", jwt).getStatusCode());
    assertEquals(HttpStatus.OK, getAdmin("/enhet/" + enhetId + "/arkiv").getStatusCode());
    response = getAdmin("/enhet/" + enhetId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(false, gson.fromJson(response.getBody(), EnhetDTO.class).getVerified());
    assertTrue(listEnhetIds(getAdmin("/enhet?orgnummer=" + orgnummer)).contains(enhetId));

    // May maintain its own info, but not change orgnummer, become a top node or verify itself
    response =
        patch(
            "/enhet/" + enhetId,
            new JSONObject().put("kontaktpunktEpost", "pending@example.com"),
            jwt);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(
        "pending@example.com",
        gson.fromJson(response.getBody(), EnhetDTO.class).getKontaktpunktEpost());
    for (var patchJSON :
        List.of(
            new JSONObject().put("orgnummer", "723456780"),
            new JSONObject().put("verified", true),
            new JSONObject().put("enhetstype", "DUMMYENHET"))) {
      assertEquals(
          HttpStatus.FORBIDDEN,
          patch("/enhet/" + enhetId, patchJSON, jwt).getStatusCode(),
          patchJSON.toString());
    }
    assertEquals(HttpStatus.FORBIDDEN, delete("/enhet/" + enhetId, jwt).getStatusCode());

    // Nor may it attach children through its own update
    var childJSON = getEnhetJSON();
    var childOrgnummer = childJSON.getString("orgnummer");
    response =
        patch(
            "/enhet/" + enhetId,
            new JSONObject().put("underenhet", new JSONArray().put(childJSON)),
            jwt);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertNull(enhetRepository.findByOrgnummer(childOrgnummer));

    // Admin verifies
    response = patchAdmin("/enhet/" + enhetId, new JSONObject().put("verified", true));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(true, gson.fromJson(response.getBody(), EnhetDTO.class).getVerified());

    // Now the token resolves to the Enhet, and the Enhet is public, but verified stays private
    authInfo = gson.fromJson(get("/me", jwt).getBody(), AuthInfo.class);
    assertEquals(enhetId, authInfo.getId());
    response = getAnon("/enhet/" + enhetId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNull(gson.fromJson(response.getBody(), EnhetDTO.class).getVerified());
    assertEquals(
        true, gson.fromJson(get("/enhet/" + enhetId, jwt).getBody(), EnhetDTO.class).getVerified());
    assertTrue(listEnhetIds(getAnon("/enhet?orgnummer=" + orgnummer)).contains(enhetId));
    response = post("/arkiv", getArkivJSON(), jwt);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertEquals(enhetId, arkivDTO.getJournalenhet().getId());

    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkivDTO.getId(), jwt).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/enhet/" + enhetId, jwt).getStatusCode());
  }

  @Test
  void selfRegistrationNotifiesVerifier() throws Exception {
    var orgnummer = "733456789";
    var jwt = generateMockAltinn3Jwt(orgnummer);
    var enhetJSON = getEnhetJSON();
    enhetJSON.put("orgnummer", orgnummer);
    enhetJSON.put("parent", rootEnhetId);

    var response = post("/enhet", enhetJSON, jwt);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var enhetDTO = gson.fromJson(response.getBody(), EnhetDTO.class);
    verify(javaMailSender, times(1)).send(any(MimeMessage.class));

    // Enhets added by a verified Enhet are verified at once, and nobody is notified
    resetMail();
    response = post("/enhet/" + journalenhetId + "/underenhet", getEnhetJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var underenhetDTO = gson.fromJson(response.getBody(), EnhetDTO.class);
    verify(javaMailSender, never()).send(any(MimeMessage.class));
    response = getAdmin("/enhet/" + underenhetDTO.getId());
    assertEquals(true, gson.fromJson(response.getBody(), EnhetDTO.class).getVerified());

    assertEquals(HttpStatus.OK, delete("/enhet/" + underenhetDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, deleteAdmin("/enhet/" + enhetDTO.getId()).getStatusCode());
  }

  @Test
  void apiKeyForUnverifiedEnhetIsRejected() throws Exception {
    var orgnummer = "743456789";
    var jwt = generateMockAltinn3Jwt(orgnummer);
    var enhetJSON = getEnhetJSON();
    enhetJSON.put("orgnummer", orgnummer);
    enhetJSON.put("parent", rootEnhetId);
    var response = post("/enhet", enhetJSON, jwt);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var enhetId = gson.fromJson(response.getBody(), EnhetDTO.class).getId();

    response = postAdmin("/enhet/" + enhetId + "/apiKey", getApiKeyJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var secretKey = gson.fromJson(response.getBody(), ApiKeyDTO.class).getSecretKey();
    assertEquals(
        HttpStatus.UNAUTHORIZED, post("/arkiv", getArkivJSON(), secretKey).getStatusCode());

    response = patchAdmin("/enhet/" + enhetId, new JSONObject().put("verified", true));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = post("/arkiv", getArkivJSON(), secretKey);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkivDTO.getId(), secretKey).getStatusCode());
    assertEquals(HttpStatus.OK, deleteAdmin("/enhet/" + enhetId).getStatusCode());
  }

  @Test
  void shouldRejectSelfRegistrationWithUnderenhet() throws Exception {
    var orgnummer = "753456789";
    var jwt = generateMockAltinn3Jwt(orgnummer);
    var enhetJSON = getEnhetJSON();
    enhetJSON.put("orgnummer", orgnummer);
    enhetJSON.put("parent", rootEnhetId);
    enhetJSON.put("underenhet", new JSONArray().put(getEnhetJSON()));

    var response = post("/enhet", enhetJSON, jwt);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertNull(enhetRepository.findByOrgnummer(orgnummer));
  }

  private List<String> listEnhetIds(ResponseEntity<String> response) {
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var type = new TypeToken<PaginatedList<EnhetDTO>>() {}.getType();
    PaginatedList<EnhetDTO> list = gson.fromJson(response.getBody(), type);
    return list.getItems().stream().map(EnhetDTO::getId).toList();
  }

  @Test
  void shouldRejectAddingEnhetWhenParentIsNotTopNode() throws Exception {
    var orgnummer = "133456789";
    var jwt = generateMockAltinn3Jwt(orgnummer);
    var enhetJSON = getEnhetJSON();
    enhetJSON.put("orgnummer", orgnummer);
    enhetJSON.put("parent", journalenhetId);

    var response = post("/enhet", enhetJSON, jwt);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertNull(enhetRepository.findByOrgnummer(orgnummer));
  }

  @Test
  void shouldRejectAddingEnhetWhenAuthenticatedOrgnummerDiffersFromBodyOrgnummer()
      throws Exception {
    var jwt = generateMockAltinn3Jwt("223456789");
    var enhetJSON = getEnhetJSON();
    enhetJSON.put("orgnummer", "323456789");
    enhetJSON.put("parent", rootEnhetId);

    var response = post("/enhet", enhetJSON, jwt);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertNull(enhetRepository.findByOrgnummer("323456789"));
  }

  @Test
  void shouldRejectAddingEnhetWhenMatchingOrgnummerButConflictingIdentifierExists()
      throws Exception {
    var orgnummer = "423456789";
    var jwt = generateMockAltinn3Jwt(orgnummer);
    var enhetJSON = getEnhetJSON();
    enhetJSON.put("orgnummer", orgnummer);
    enhetJSON.put("externalId", "root");

    var response = post("/enhet/" + rootEnhetId + "/underenhet", enhetJSON, jwt);
    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertNull(enhetRepository.findByOrgnummer(orgnummer));
  }

  @Test
  void shouldRejectAddingEnhetWhenBodyContainsIdOnAddEndpoint() throws Exception {
    var orgnummer = "523456789";
    var jwt = generateMockAltinn3Jwt(orgnummer);
    var enhetJSON = getEnhetJSON();
    enhetJSON.put("id", rootEnhetId);
    enhetJSON.put("orgnummer", orgnummer);
    enhetJSON.put("parent", new JSONObject().put("id", rootEnhetId));

    var response = post("/enhet", enhetJSON, jwt);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNull(enhetRepository.findByOrgnummer(orgnummer));
  }

  @Test
  void shouldRejectTokensForOtherClientsOrResources() throws Exception {
    assertRejected(
        generateMockAltinn3Jwt(
            journalenhetOrgnummer, ansattportenIssuerUri, "another-client", TEST_RESOURCE));
    assertRejected(
        generateMockAltinn3Jwt(
            journalenhetOrgnummer,
            ansattportenIssuerUri,
            TEST_CLIENT_ID,
            "urn:altinn:resource:unrelated"));
  }

  /** Flips a flag on the live service and restores it on close. Unwraps the AOP proxy itself. */
  private AutoCloseable withEnhetServiceFlag(String field, boolean value) {
    var original = (boolean) ReflectionTestUtils.getField(enhetService, field);
    ReflectionTestUtils.setField(enhetService, field, value);
    return () -> ReflectionTestUtils.setField(enhetService, field, original);
  }

  @Test
  void shouldRejectSelfAddWhenDisabled() throws Exception {
    var orgnummer = "623456789";
    var jwt = generateMockAltinn3Jwt(orgnummer);
    var enhetJSON = getEnhetJSON();
    enhetJSON.put("orgnummer", orgnummer);
    enhetJSON.put("parent", rootEnhetId);

    try (var _ = withEnhetServiceFlag("ansattportenAllowSelfRegistration", false)) {
      assertEquals(HttpStatus.FORBIDDEN, post("/enhet", enhetJSON, jwt).getStatusCode());
    }
    assertNull(enhetRepository.findByOrgnummer(orgnummer));
  }

  @Test
  void shouldVerifySelfRegisteredEnhetOnInsertWhenAutoVerifyIsEnabled() throws Exception {
    var orgnummer = "633456789";
    var jwt = generateMockAltinn3Jwt(orgnummer);
    var enhetJSON = getEnhetJSON();
    enhetJSON.put("orgnummer", orgnummer);
    enhetJSON.put("parent", rootEnhetId);

    ResponseEntity<String> response;
    try (var _ = withEnhetServiceFlag("ansattportenAutoVerifySelfRegistration", true)) {
      response = post("/enhet", enhetJSON, jwt);
    }
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var enhetId = gson.fromJson(response.getBody(), EnhetDTO.class).getId();

    // Verified at once, so nobody is asked to verify it
    verify(javaMailSender, never()).send(any(MimeMessage.class));
    response = getAdmin("/enhet/" + enhetId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(true, gson.fromJson(response.getBody(), EnhetDTO.class).getVerified());

    // The token resolves to the Enhet, which is public and may publish right away
    var authInfo = gson.fromJson(get("/me", jwt).getBody(), AuthInfo.class);
    assertEquals(enhetId, authInfo.getId());
    assertEquals(HttpStatus.OK, getAnon("/enhet/" + enhetId).getStatusCode());
    response = post("/arkiv", getArkivJSON(), jwt);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertEquals(enhetId, arkivDTO.getJournalenhet().getId());

    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkivDTO.getId(), jwt).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/enhet/" + enhetId, jwt).getStatusCode());
  }

  private void assertRejected(String jwt) throws Exception {
    assertEquals(HttpStatus.UNAUTHORIZED, get("/me", jwt).getStatusCode());
  }

  private String generateMockAltinn3Jwt(String orgnummer) throws Exception {
    if (orgnummer == null) {
      orgnummer = journalenhetOrgnummer;
    }
    return generateMockAltinn3Jwt(orgnummer, ansattportenIssuerUri);
  }

  static String generateMockAltinn3Jwt(String orgnummer, String issuerUri) throws Exception {
    return generateMockAltinn3Jwt(orgnummer, issuerUri, TEST_CLIENT_ID, TEST_RESOURCE);
  }

  private static String generateMockAltinn3Jwt(
      String orgnummer, String issuerUri, String clientId, String resource) throws Exception {
    var now = Instant.now();
    var expiryTimeSeconds = 3600L;

    var claimsSetBuilder =
        new JWTClaimsSet.Builder()
            // Ansattporten returns a random subject
            .subject(IdGenerator.generateId("subject"))
            .issuer(issuerUri)
            // Ansattporten sets no "aud" on access tokens, the client is in "client_id"
            .claim("client_id", clientId)
            .jwtID(UUID.randomUUID().toString())
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(expiryTimeSeconds)));

    // Add authorization_details, shaped like a real Ansattporten Altinn 3 access token. The
    // authorized parties carry no "actions", access is expressed by being listed for the resource.
    claimsSetBuilder.claim(
        "authorization_details",
        List.of(
            Map.of(
                "type",
                "ansattporten:altinn:resource",
                "resource",
                resource,
                "authorized_parties",
                List.of(
                    Map.of(
                        "orgno",
                        Map.of("authority", "iso6523-actorid-upis", "ID", "0192:" + orgnummer),
                        "resource",
                        "einnsyn-api",
                        "name",
                        "ANSTENDIG UNØYAKTIG TIGER AS",
                        "unit_type",
                        "AS")))));

    var signedJWT =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT)
                .keyID(TEST_KEY_ID)
                .build(),
            claimsSetBuilder.build());

    signedJWT.sign(new RSASSASigner(TEST_KEY_PAIR.getPrivate()));
    return signedJWT.serialize();
  }
}

package no.einnsyn.backend.auth.ansattporten;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.einnsyn.backend.EInnsynApplication;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.authinfo.models.AuthInfo;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import no.einnsyn.backend.utils.id.IdGenerator;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = {EInnsynApplication.class, AnsattportenTestJwtConfiguration.class})
@ActiveProfiles("test")
class AnsattportenAuthenticationTest extends EinnsynControllerTestBase {

  @Value("${application.ansattporten.issuerUri}")
  private String ansattportenIssuerUri;

  public static final KeyPair TEST_KEY_PAIR = generateTestRsaKeyPair();
  public static final String TEST_KEY_ID = "test-ansattporten-rsa-key-1";
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

    response = delete("/enhet/" + enhetDTO.getId(), jwt);
    assertEquals(HttpStatus.OK, response.getStatusCode());
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
  void shouldRejectTokensWithoutWriteAccessToEInnsyn() throws Exception {
    assertRejected(
        generateMockAltinn3Jwt(
            journalenhetOrgnummer,
            ansattportenIssuerUri,
            "another-client",
            TEST_RESOURCE,
            List.of("write")));
    assertRejected(
        generateMockAltinn3Jwt(
            journalenhetOrgnummer,
            ansattportenIssuerUri,
            TEST_CLIENT_ID,
            "urn:altinn:resource:unrelated",
            List.of("write")));
    assertRejected(
        generateMockAltinn3Jwt(
            journalenhetOrgnummer,
            ansattportenIssuerUri,
            TEST_CLIENT_ID,
            TEST_RESOURCE,
            List.of("read")));
  }

  private void assertRejected(String jwt) throws Exception {
    assertEquals(HttpStatus.UNAUTHORIZED, get("/me", jwt).getStatusCode());
  }

  private static KeyPair generateTestRsaKeyPair() {
    try {
      var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      return keyPairGenerator.generateKeyPair();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Failed to generate RSA key pair for tests", e);
    }
  }

  private String generateMockAltinn3Jwt(String orgnummer) throws Exception {
    if (orgnummer == null) {
      orgnummer = journalenhetOrgnummer;
    }
    return generateMockAltinn3Jwt(orgnummer, ansattportenIssuerUri);
  }

  static String generateMockAltinn3Jwt(String orgnummer, String issuerUri) throws Exception {
    return generateMockAltinn3Jwt(
        orgnummer, issuerUri, TEST_CLIENT_ID, TEST_RESOURCE, List.of("write"));
  }

  private static String generateMockAltinn3Jwt(
      String orgnummer, String issuerUri, String clientId, String resource, List<String> actions)
      throws Exception {
    var now = Instant.now();
    var expiryTimeSeconds = 3600L;

    var claimsSetBuilder =
        new JWTClaimsSet.Builder()
            // Ansattporten returns a random subject
            .subject(IdGenerator.generateId("subject"))
            .issuer(issuerUri)
            .audience(clientId)
            .jwtID(UUID.randomUUID().toString())
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(expiryTimeSeconds)));

    // Add authorization_details
    claimsSetBuilder.claim(
        "authorization_details",
        List.of(
            Map.of(
                "resource",
                resource,
                "type",
                "ansattporten:altinn:resource",
                "resource_name",
                "eInnsyn API resource",
                "authorized_parties",
                List.of(
                    Map.of(
                        "orgno",
                        Map.of("authority", "iso6523-actorid-upis", "ID", "0192:" + orgnummer),
                        "resource",
                        "einnsyn-api",
                        "actions",
                        actions,
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

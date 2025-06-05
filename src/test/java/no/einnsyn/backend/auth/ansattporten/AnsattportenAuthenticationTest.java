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
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.authinfo.models.AuthInfoResponse;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.utils.idgenerator.IdGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AnsattportenAuthenticationTest extends EinnsynControllerTestBase {

  @Value("${application.ansattportenIssuerUri}")
  private String ansattportenIssuerUri;

  public static final KeyPair TEST_KEY_PAIR = generateTestRsaKeyPair();
  public static final String TEST_KEY_ID = "test-ansattporten-rsa-key-1";

  @Test
  void testAuthInfo() throws Exception {
    var jwt = generateMockAnsattportenJwt(journalenhetOrgnummer);
    var response = get("/me", jwt);
    var authInfo = gson.fromJson(response.getBody(), AuthInfoResponse.class);
    assertEquals("Ansattporten", authInfo.getAuthType());
    assertEquals("Enhet", authInfo.getType());
    assertEquals(journalenhetId, authInfo.getId());
    assertEquals(journalenhetOrgnummer, authInfo.getOrgnummer());

    jwt = generateMockAnsattportenJwt(journalenhet2Orgnummer);
    response = get("/me", jwt);
    authInfo = gson.fromJson(response.getBody(), AuthInfoResponse.class);
    assertEquals("Ansattporten", authInfo.getAuthType());
    assertEquals("Enhet", authInfo.getType());
    assertEquals(journalenhet2Id, authInfo.getId());
    assertEquals(journalenhet2Orgnummer, authInfo.getOrgnummer());

    jwt = generateMockAnsattportenJwt("123456789");
    response = get("/me", jwt);
    authInfo = gson.fromJson(response.getBody(), AuthInfoResponse.class);
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

    // Should be able to update as Journalenhet2 using Ansattporten JWT
    var journalenhet2Jwt = generateMockAnsattportenJwt(journalenhet2Orgnummer);
    response = patch("/arkiv/" + arkivDTO.getId(), getArkivJSON(), journalenhet2Jwt);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertEquals(journalenhet2Id, arkivDTO.getJournalenhet().getId());
    response = patch("/arkivdel/" + arkivdelDTO.getId(), getArkivdelJSON(), journalenhet2Jwt);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Should not be able to update as Journalenhet1 using Ansattporten JWT
    var journalenhet1Jwt = generateMockAnsattportenJwt(journalenhetOrgnummer);
    response = patch("/arkiv/" + arkivDTO.getId(), getArkivJSON(), journalenhet1Jwt);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    response = patch("/arkivdel/" + arkivdelDTO.getId(), getArkivdelJSON(), journalenhet1Jwt);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    response = patch("/saksmappe/" + saksmappeDTO.getId(), getSaksmappeJSON(), journalenhet1Jwt);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Should be able to delete as Journalenhet2 using Ansattporten JWT
    response = delete("/saksmappe/" + saksmappeDTO.getId(), journalenhet2Jwt);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = delete("/arkivdel/" + arkivdelDTO.getId(), journalenhet2Jwt);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = delete("/arkiv/" + arkivDTO.getId(), journalenhet2Jwt);
    assertEquals(HttpStatus.OK, response.getStatusCode());
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

  private String generateMockAnsattportenJwt(String orgnummer) throws Exception {
    var now = Instant.now();
    var expiryTimeSeconds = 3600L;

    if (orgnummer == null) {
      orgnummer = "0192:" + journalenhetOrgnummer;
    }

    var claimsSetBuilder =
        new JWTClaimsSet.Builder()
            // Ansattporten returns a random subject
            .subject(IdGenerator.generateId("subject"))
            .issuer(ansattportenIssuerUri)
            .jwtID(UUID.randomUUID().toString())
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(expiryTimeSeconds)));

    // Add demo-authorization_details
    claimsSetBuilder.claim(
        "authorization_details",
        List.of(
            Map.of(
                "resource",
                "urn:altinn:resource:2480:40",
                "type",
                "ansattporten:altinn:service",
                "resource_name",
                "Demo Ansattporten Service",
                "reportees",
                List.of(
                    Map.of(
                        "Rights",
                        List.of("Read", "ArchiveDelete", "ArchiveRead"),
                        "Authority",
                        "iso6523-actorid-upis",
                        "ID",
                        "0192:" + orgnummer,
                        "Name",
                        "DEMO ORG")))));

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

  @TestConfiguration
  static class TestJwtConfiguration {

    @Bean("ansattportenJwtDecoder")
    @Primary
    public JwtDecoder ansattportenJwtDecoder(
        @Value("${application.ansattportenIssuerUri}") String issuerUri) {
      var publicKey = (RSAPublicKey) TEST_KEY_PAIR.getPublic();

      var jwtDecoder =
          NimbusJwtDecoder.withPublicKey(publicKey)
              .signatureAlgorithm(SignatureAlgorithm.RS256)
              .build();

      // Standard validators
      var issuerValidator = new JwtIssuerValidator(issuerUri);
      var timestampValidator = new JwtTimestampValidator();

      // Combine validators
      var combinedValidators =
          new DelegatingOAuth2TokenValidator<>(issuerValidator, timestampValidator);

      jwtDecoder.setJwtValidator(combinedValidators);
      return jwtDecoder;
    }
  }
}

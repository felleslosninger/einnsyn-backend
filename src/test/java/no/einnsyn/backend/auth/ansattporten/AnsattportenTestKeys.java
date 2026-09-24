package no.einnsyn.backend.auth.ansattporten;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/** Signing key for mock Ansattporten tokens, and the decoder EinnsynTestBase installs for it. */
public final class AnsattportenTestKeys {

  public static final KeyPair TEST_KEY_PAIR = generateTestRsaKeyPair();
  public static final String TEST_KEY_ID = "test-ansattporten-rsa-key-1";

  private AnsattportenTestKeys() {}

  /** Checks signature and timestamps only; the provider checks issuer and client itself. */
  public static JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withPublicKey((RSAPublicKey) TEST_KEY_PAIR.getPublic())
        .signatureAlgorithm(SignatureAlgorithm.RS256)
        .build();
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
}

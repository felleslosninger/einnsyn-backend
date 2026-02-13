package no.einnsyn.backend.authentication.bruker;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.time.Duration;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@Slf4j
public class EInnsynJwtConfiguration {

  public static final String EINNSYN_JWT_KEY_ID = "einnsyn-hs256-key-1";

  private final byte[] decodedSecretBytes;
  private final String issuerUri;

  public EInnsynJwtConfiguration(
      @Value("${application.jwt.encryption-secret}") String base64Secret,
      @Value("${application.jwt.issuerUri}") String issuerUri) {
    this.decodedSecretBytes = Base64.getDecoder().decode(base64Secret);
    this.issuerUri = issuerUri;
  }

  @Bean("eInnsynJwtDecoder")
  public JwtDecoder eInnsynJwtDecoder() {
    var secretKey = new SecretKeySpec(decodedSecretBytes, "HS256");

    // Don't use clock skew for JWT validation.
    var timestampValidator = new JwtTimestampValidator(Duration.ZERO);
    var issuerValidator = new JwtIssuerValidator(issuerUri);
    var tokenValidator = new DelegatingOAuth2TokenValidator<>(timestampValidator, issuerValidator);
    var decoder = NimbusJwtDecoder.withSecretKey(secretKey).build();
    decoder.setJwtValidator(tokenValidator);

    return decoder;
  }

  @Bean("eInnsynJwtEncoder")
  public JwtEncoder eInnsynBrukerJwtEncoder(
      @Qualifier("eInnsynJwkSource") JWKSource<SecurityContext> jwkSource) {
    return new NimbusJwtEncoder(jwkSource);
  }

  @Bean("eInnsynJwkSource")
  public JWKSource<SecurityContext> eInnsynJwkSource() {
    var jwk =
        new OctetSequenceKey.Builder(decodedSecretBytes)
            .keyID(EINNSYN_JWT_KEY_ID)
            .algorithm(JWSAlgorithm.HS256)
            .keyUse(KeyUse.SIGNATURE)
            .build();
    var jwkSet = new JWKSet(jwk);

    return new ImmutableJWKSet<>(jwkSet);
  }
}

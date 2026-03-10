package no.einnsyn.backend.auth.ansattporten;

import java.security.interfaces.RSAPublicKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@TestConfiguration
class AnsattportenTestJwtConfiguration {

  @Bean("ansattportenJwtDecoder")
  @Primary
  public JwtDecoder ansattportenJwtDecoder(
      @Value("${application.ansattporten.issuerUri}") String issuerUri) {
    var publicKey = (RSAPublicKey) AnsattportenAuthenticationTest.TEST_KEY_PAIR.getPublic();

    var jwtDecoder =
        NimbusJwtDecoder.withPublicKey(publicKey)
            .signatureAlgorithm(SignatureAlgorithm.RS256)
            .build();

    var issuerValidator = new JwtIssuerValidator(issuerUri);
    var timestampValidator = new JwtTimestampValidator();
    var combinedValidators =
        new DelegatingOAuth2TokenValidator<>(issuerValidator, timestampValidator);

    jwtDecoder.setJwtValidator(combinedValidators);
    return jwtDecoder;
  }
}

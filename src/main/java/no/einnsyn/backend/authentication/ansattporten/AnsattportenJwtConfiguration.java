package no.einnsyn.backend.authentication.ansattporten;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;

@Configuration
public class AnsattportenJwtConfiguration {

  private final String ansattportenIssuerUri;

  public AnsattportenJwtConfiguration(
      @Value("${application.ansattportenIssuerUri}") String ansattportenIssuerUri) {
    this.ansattportenIssuerUri = ansattportenIssuerUri;
  }

  @Bean("ansattportenJwtDecoder")
  public JwtDecoder ansattportenJwtDecoder() {
    return JwtDecoders.fromOidcIssuerLocation(ansattportenIssuerUri);
  }
}

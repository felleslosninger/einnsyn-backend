package no.einnsyn.backend.auth.bruker;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import no.einnsyn.backend.authentication.AuthenticationService;
import no.einnsyn.backend.authentication.EInnsynAuthentication;
import no.einnsyn.backend.authentication.bruker.BrukerAuthenticationProvider;
import no.einnsyn.backend.entities.bruker.BrukerService;
import no.einnsyn.backend.entities.bruker.models.Bruker;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * The eInnsyn JWT decoder already enforces an exact issuer match, but the provider does its own
 * issuer check as well. That check must be an equality check, like the one in
 * AnsattportenAuthenticationProvider, so that it does not accept issuers that merely contain the
 * expected issuer URI as a substring.
 */
class BrukerAuthenticationProviderIssuerTest {

  private static final String ISSUER_URI = "http://localhost:8080";

  private Jwt buildJwt(String issuer) {
    var now = Instant.now();
    return Jwt.withTokenValue("token")
        .header("alg", "HS256")
        .issuedAt(now)
        .expiresAt(now.plus(5, ChronoUnit.MINUTES))
        .subject("test@example.com")
        .claim("iss", issuer)
        .claim("id", "bruk_test")
        .claim("use", "access")
        .build();
  }

  private BrukerAuthenticationProvider buildProvider(Jwt jwt) {
    var jwtDecoder = mock(JwtDecoder.class);
    when(jwtDecoder.decode(anyString())).thenReturn(jwt);

    var bruker = new Bruker();
    bruker.setEmail("test@example.com");
    var brukerService = mock(BrukerService.class);
    when(brukerService.find(anyString())).thenReturn(bruker);

    var authenticationService = mock(AuthenticationService.class);
    when(authenticationService.getAuthoritiesFromBruker(anyList(), anyString()))
        .thenReturn(List.of());

    return new BrukerAuthenticationProvider(
        authenticationService, brukerService, jwtDecoder, ISSUER_URI);
  }

  @Test
  void testExactIssuerIsAccepted() {
    var provider = buildProvider(buildJwt(ISSUER_URI));
    assertNotNull(provider.authenticate(new EInnsynAuthentication("token")));
  }

  @Test
  void testIssuerContainingExpectedUriIsRejected() {
    // A substring match would accept this issuer
    var provider = buildProvider(buildJwt("https://evil.example.com/?x=" + ISSUER_URI));
    assertNull(provider.authenticate(new EInnsynAuthentication("token")));
  }

  @Test
  void testIssuerWithExtraPathIsRejected() {
    // A substring match would accept this issuer as well
    var provider = buildProvider(buildJwt(ISSUER_URI + "/somewhere/else"));
    assertNull(provider.authenticate(new EInnsynAuthentication("token")));
  }
}

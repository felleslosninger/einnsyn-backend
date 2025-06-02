package no.einnsyn.backend.authentication.bruker;

import java.util.List;
import no.einnsyn.backend.authentication.AuthenticationService;
import no.einnsyn.backend.authentication.EInnsynAuthentication;
import no.einnsyn.backend.authentication.EInnsynPrincipalBruker;
import no.einnsyn.backend.entities.bruker.BrukerService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class BrukerAuthenticationProvider implements AuthenticationProvider {

  private final AuthenticationService authenticationService;
  private final BrukerService brukerService;
  private final JwtDecoder jwtDecoder;
  private final String eInnsynIssuerUri;
  ;

  public BrukerAuthenticationProvider(
      AuthenticationService authenticationService,
      BrukerService brukerService,
      @Qualifier("eInnsynJwtDecoder") JwtDecoder jwtDecoder,
      @Value("${application.jwt.issuerUri}") String eInnsynIssuerUri) {
    this.authenticationService = authenticationService;
    this.brukerService = brukerService;
    this.jwtDecoder = jwtDecoder;
    this.eInnsynIssuerUri = eInnsynIssuerUri;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    var token = (String) authentication.getCredentials();

    Jwt jwt;
    try {
      jwt = jwtDecoder.decode(token);
    } catch (Exception e) {
      // If decoding fails, we assume it's not a valid Ansattporten token.
      return null;
    }

    var issuer = jwt.getIssuer().toString();
    // TODO: Add this to token
    if (!issuer.contains(eInnsynIssuerUri)) {
      // Not an eInnsyn token.
      return null;
    }

    // Check that this is an access token
    if (!"access".equals(jwt.getClaimAsString("use"))) {
      return null;
    }

    // Prefer ID from the token, for edge cases such as users changing their email address.
    var username = jwt.getClaimAsString("id");

    // The old API doesn't store IDs in the token, so we need a fallback.
    if (username == null) {
      username = jwt.getSubject();
    }

    if (username == null) {
      return null;
    }

    try {
      var bruker = brukerService.findById(username);
      var brukerPrincipal =
          new EInnsynPrincipalBruker(
              "JWT", bruker.getId(), bruker.getId(), bruker.getEmail(), false);
      var authorities = authenticationService.getAuthoritiesFromBruker(List.of(bruker), "Write");
      var authResult = new EInnsynAuthentication(brukerPrincipal, null, authorities);
      authResult.setAuthenticated(true);
      return authResult;
    } catch (Exception e) {
      throw new AuthenticationException("Failed to authenticate bruker", e) {};
    }
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return EInnsynAuthentication.class.isAssignableFrom(authentication);
  }
}

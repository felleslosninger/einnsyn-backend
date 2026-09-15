package no.einnsyn.backend.authentication.bruker;

import java.util.List;
import no.einnsyn.backend.authentication.AuthenticationService;
import no.einnsyn.backend.authentication.EInnsynAuthentication;
import no.einnsyn.backend.authentication.EInnsynPrincipalBruker;
import no.einnsyn.backend.entities.bruker.BrukerService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class BrukerAuthenticationProvider implements AuthenticationProvider {

  private final AuthenticationService authenticationService;
  private final BrukerService brukerService;
  private final EInnsynTokenService tokenService;

  public BrukerAuthenticationProvider(
      AuthenticationService authenticationService,
      BrukerService brukerService,
      EInnsynTokenService tokenService) {
    this.authenticationService = authenticationService;
    this.brukerService = brukerService;
    this.tokenService = tokenService;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    var token = (String) authentication.getCredentials();

    Jwt jwt;
    try {
      jwt = tokenService.decodeToken(token);
    } catch (Exception e) {
      // If decoding fails, we assume it's not a valid eInnsyn token. The decoder verifies the
      // signature and the issuer, so tokens from other issuers end up here.
      return null;
    }

    // Check that this is an access token
    if (!"access".equals(jwt.getClaimAsString("use"))) {
      return null;
    }

    var username = tokenService.getBrukerId(jwt);
    if (username == null) {
      return null;
    }

    try {
      var bruker = brukerService.find(username);
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

package no.einnsyn.backend.authentication.apikey;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.authentication.AuthenticationService;
import no.einnsyn.backend.authentication.EInnsynAuthentication;
import no.einnsyn.backend.authentication.EInnsynAuthority;
import no.einnsyn.backend.authentication.EInnsynPrincipal;
import no.einnsyn.backend.authentication.EInnsynPrincipalBruker;
import no.einnsyn.backend.authentication.EInnsynPrincipalEnhet;
import no.einnsyn.backend.entities.apikey.ApiKeyService;
import no.einnsyn.backend.entities.enhet.EnhetService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ApiKeyAuthenticationProvider implements AuthenticationProvider {

  private final AuthenticationService authenticationService;
  private final ApiKeyService apiKeyService;
  private final EnhetService enhetService;

  public ApiKeyAuthenticationProvider(
      AuthenticationService authenticationService,
      ApiKeyService apiKeyService,
      EnhetService enhetService) {
    this.authenticationService = authenticationService;
    this.apiKeyService = apiKeyService;
    this.enhetService = enhetService;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {

    var credentials = authentication.getCredentials();
    if (credentials instanceof ApiKeyCredentials apiKeyCredentials) {

      // Key could either be given in the API-KEY header or in the Authorization header
      var key = apiKeyCredentials.getApiKey();
      log.trace("ApiKey Auth, key: {}", key);

      // The request can be done on behalf of another Enhet, that is below the authenticated Enhet
      var actingAsId = apiKeyCredentials.getActingAs();
      if (actingAsId != null) {
        log.trace("Acting as Enhet: {}", actingAsId);
      }

      var apiKey = apiKeyService.findBySecretKey(key);
      if (apiKey == null) {
        throw new AuthenticationException("Invalid API key") {};
      }

      if (apiKey.getExpiresAt() != null
          && apiKey.getExpiresAt().isBefore(java.time.Instant.now())) {
        throw new AuthenticationException("API key has expired") {};
      }

      var enhet = apiKey.getEnhet();
      var bruker = apiKey.getBruker();
      var isAdmin = enhet != null && enhet.getParent() == null;

      // If we have Acting-As, check if we're allowed to act on behalf of the Enhet
      if (actingAsId != null) {
        if (enhet == null) {
          throw new AuthenticationException("API key is not associated with an Enhet") {};
        }
        if (!enhet.getId().equals(actingAsId)) {
          if (!enhetService.isAncestorOf(enhet.getId(), actingAsId)
              && !enhetService.isHandledBy(enhet.getId(), actingAsId)) {
            throw new AuthenticationException("Not allowed to act as " + actingAsId) {};
          }
          enhet = enhetService.findByIdOrThrow(actingAsId, AuthenticationException.class);
        }
      }

      EInnsynPrincipal principal;
      List<EInnsynAuthority> authorities = null;

      // If the API key is associated with an Enhet, use that Enhet's ID
      if (enhet != null) {
        principal =
            new EInnsynPrincipalEnhet(
                "ApiKey", apiKey.getId(), enhet.getId(), enhet.getOrgnummer(), isAdmin);
        authorities = authenticationService.getAuthoritiesFromEnhet(List.of(enhet), "Write");
      }

      // If the API key is associated with a Bruker, use the Bruker's ID
      else if (bruker != null) {
        principal =
            new EInnsynPrincipalBruker(
                "ApiKey", apiKey.getId(), bruker.getId(), bruker.getEmail(), false);
        authorities = authenticationService.getAuthoritiesFromBruker(List.of(bruker), "Write");
      }

      // This request is not associated with any Enhet or Bruker.
      else {
        throw new AuthenticationException("API key is not associated with an Enhet or Bruker") {};
      }

      var authResult = new EInnsynAuthentication(principal, null, authorities);
      authResult.setAuthenticated(true);

      return authResult;
    }

    return null;
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return EInnsynAuthentication.class.isAssignableFrom(authentication);
  }
}

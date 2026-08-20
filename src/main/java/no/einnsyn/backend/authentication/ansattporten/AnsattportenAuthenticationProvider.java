package no.einnsyn.backend.authentication.ansattporten;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.authentication.AuthenticationService;
import no.einnsyn.backend.authentication.EInnsynAuthentication;
import no.einnsyn.backend.authentication.EInnsynPrincipalEnhet;
import no.einnsyn.backend.entities.enhet.EnhetService;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AnsattportenAuthenticationProvider implements AuthenticationProvider {

  private static final String ALTINN_RESOURCE = "ansattporten:altinn:resource";

  private final AuthenticationService authenticationService;
  private final EnhetService enhetService;
  private final JwtDecoder jwtDecoder;
  private final String ansattportenIssuerUri;
  private final String clientId;
  private final String resource;

  public AnsattportenAuthenticationProvider(
      AuthenticationService authenticationService,
      EnhetService enhetService,
      @Qualifier("ansattportenJwtDecoder") JwtDecoder jwtDecoder,
      @Value("${application.ansattporten.issuerUri}") String ansattportenIssuerUri,
      @Value("${application.ansattporten.clientId}") String clientId,
      @Value("${application.ansattporten.resource}") String resource) {
    this.authenticationService = authenticationService;
    this.enhetService = enhetService;
    this.jwtDecoder = jwtDecoder;
    this.ansattportenIssuerUri = ansattportenIssuerUri;
    this.clientId = clientId;
    this.resource = resource;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    Jwt jwt;
    try {
      jwt = jwtDecoder.decode((String) authentication.getCredentials());
    } catch (Exception e) {
      log.debug("Not an Ansattporten token, could not decode: {}", e.getMessage());
      return null;
    }

    if (jwt.getIssuer() == null || !ansattportenIssuerUri.equals(jwt.getIssuer().toString())) {
      log.debug(
          "Rejecting token, expected issuer {}, got {}", ansattportenIssuerUri, jwt.getIssuer());
      return null;
    }

    // Ansattporten access tokens carry the requesting client in "client_id", not in "aud".
    var tokenClientId = jwt.getClaimAsString("client_id");
    if (!clientId.equals(tokenClientId)) {
      log.debug("Rejecting token, client_id {} does not match {}", tokenClientId, clientId);
      return null;
    }

    var orgnummers = getAuthorizedOrgnummers(jwt);
    if (orgnummers.isEmpty()) {
      log.debug("Rejecting token, no orgnummer is authorized for {}", resource);
      return null;
    }

    String representingId = null;
    String representingOrgnummer = orgnummers.getFirst();
    var enhetList = new ArrayList<Enhet>();
    for (var orgnummer : orgnummers) {
      var enhet = enhetService.find(orgnummer);
      if (enhet != null) {
        enhetList.add(enhet);
        if (representingId == null) {
          representingId = enhet.getId();
          representingOrgnummer = orgnummer;
        }
      }
    }

    var authorities = authenticationService.getAuthoritiesFromEnhet(enhetList, "Write");

    var principal =
        new EInnsynPrincipalEnhet(
            "Ansattporten", jwt.getSubject(), representingId, representingOrgnummer, false);
    var result = new EInnsynAuthentication(principal, null, authorities);
    result.setAuthenticated(true);
    return result;
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return EInnsynAuthentication.class.isAssignableFrom(authentication);
  }

  /**
   * Collects the organization numbers the token is authorized for. Altinn 3 expresses access by
   * listing a party under the requested resource, the parties carry no per-action claims.
   */
  private List<String> getAuthorizedOrgnummers(Jwt jwt) {
    var orgnummers = new LinkedHashSet<String>();
    var claim = jwt.getClaim("authorization_details");
    if (!(claim instanceof List<?> details)) {
      log.debug("authorization_details is missing or not a list: {}", claim);
      return List.of();
    }

    for (var value : details) {
      if (!(value instanceof Map<?, ?> detail)) {
        log.debug("Skipping authorization_details entry, not an object: {}", value);
        continue;
      }
      if (!ALTINN_RESOURCE.equals(detail.get("type")) || !resource.equals(detail.get("resource"))) {
        log.debug(
            "Skipping authorization_details entry, expected type {} and resource {}, got {}",
            ALTINN_RESOURCE,
            resource,
            detail);
        continue;
      }
      if (!(detail.get("authorized_parties") instanceof List<?> parties)) {
        log.debug(
            "Skipping authorization_details entry, authorized_parties is missing or not a list:"
                + " {}",
            detail.get("authorized_parties"));
        continue;
      }

      for (var partyValue : parties) {
        if (!(partyValue instanceof Map<?, ?> party)) {
          log.debug("Skipping authorized party, not an object: {}", partyValue);
          continue;
        }
        var orgnummer = getOrgNoFromClaim(party.get("orgno"));
        if (orgnummer == null) {
          log.debug(
              "Skipping authorized party, could not read orgnummer from {}", party.get("orgno"));
          continue;
        }
        orgnummers.add(orgnummer);
      }
    }

    return List.copyOf(orgnummers);
  }

  private String getOrgNoFromClaim(Object claim) {
    if (claim instanceof Map<?, ?> orgno
        && "iso6523-actorid-upis".equals(orgno.get("authority"))
        && orgno.get("ID") instanceof String id
        && id.startsWith("0192:")) {
      return id.substring(5);
    }
    return null;
  }
}

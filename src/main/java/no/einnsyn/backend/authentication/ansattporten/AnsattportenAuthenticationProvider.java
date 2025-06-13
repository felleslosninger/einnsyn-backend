package no.einnsyn.backend.authentication.ansattporten;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

@Component
public class AnsattportenAuthenticationProvider implements AuthenticationProvider {

  private final AuthenticationService authenticationService;
  private final EnhetService enhetService;
  private final JwtDecoder jwtDecoder;
  private final String ansattportenIssuerUri;

  public AnsattportenAuthenticationProvider(
      AuthenticationService authenticationService,
      EnhetService enhetService,
      @Qualifier("ansattportenJwtDecoder") JwtDecoder jwtDecoder,
      @Value("${application.ansattportenIssuerUri}") String ansattportenIssuerUri) {
    this.authenticationService = authenticationService;
    this.enhetService = enhetService;
    this.jwtDecoder = jwtDecoder;
    this.ansattportenIssuerUri = ansattportenIssuerUri;
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
    if (!issuer.equals(ansattportenIssuerUri)) {
      // Not an Ansattporten token.
      return null;
    }

    // Find orgnummers from the token
    String representingId = null;
    String representingOrgnummer = null;
    var orgnummers = getOrgnummersFromJWT(jwt);
    var enhetList = new ArrayList<Enhet>();
    for (var orgnummer : orgnummers) {
      if (representingOrgnummer == null) {
        representingOrgnummer = orgnummer;
      }
      var enhet = enhetService.findById(orgnummer);
      if (enhet != null) {
        if (representingId == null) {
          representingId = enhet.getId();
          representingOrgnummer = orgnummer;
        }
        enhetList.add(enhet);
      }
    }

    // Create a principal with the orgnummers and set it in the security context
    var principal =
        new EInnsynPrincipalEnhet(
            "Ansattporten", jwt.getSubject(), representingId, representingOrgnummer, false);

    var authorities = authenticationService.getAuthoritiesFromEnhet(enhetList, "Write");
    var authResult = new EInnsynAuthentication(principal, null, authorities);
    authResult.setAuthenticated(true);

    return authResult;
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return EInnsynAuthentication.class.isAssignableFrom(authentication);
  }

  /**
   * Extracts organization numbers from the JWT's "authorization_details" claim.
   *
   * @param jwt The decoded Ansattporten JWT token.
   * @return A list of organization numbers.
   */
  private List<String> getOrgnummersFromJWT(Jwt jwt) {
    var authDetailsClaim = jwt.getClaim("authorization_details");
    if (authDetailsClaim instanceof List<?> authDetailsList) {
      var organizationNumbers = new ArrayList<String>();

      for (var authDetail : authDetailsList) {
        if (authDetail instanceof Map<?, ?> authDetailMap) {
          var reporteesClaim = authDetailMap.get("reportees");
          if (reporteesClaim instanceof List reporteesClaimList) {
            for (var reportee : reporteesClaimList) {
              if (reportee instanceof Map<?, ?> reporteeMap) {
                var authority = reporteeMap.get("Authority");
                var id = reporteeMap.get("ID");

                // Norwegian orgnummers are prefixed with "0192:"
                if ("iso6523-actorid-upis".equals(authority)
                    && id instanceof String idString
                    && idString.startsWith("0192:")) {
                  organizationNumbers.add(idString.substring(5)); // Skip "0192:"
                }
              }
            }
          }
        }
      }

      return organizationNumbers;
    }

    return List.of();
  }
}

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
          var typeClaim = authDetailMap.get("type");

          // Altinn 3 resource
          if ("ansattporten:altinn:resource".equals(typeClaim)) {
            var authorizedPartiesClaim = authDetailMap.get("authorized_parties");
            if (authorizedPartiesClaim instanceof List authorizedPartiesClaimList) {
              for (var authorizedPartyClaim : authorizedPartiesClaimList) {
                if (authorizedPartyClaim instanceof Map<?, ?> authorizedPartyMap) {
                  var orgNoClaim = authorizedPartyMap.get("orgno");
                  var orgNo = getOrgNoFromClaim(orgNoClaim);
                  if (orgNo != null) {
                    organizationNumbers.add(orgNo);
                  }
                }
              }
            }
          }

          // Altinn 2 service
          else if ("ansattporten:altinn:service".equals(typeClaim)) {
            var reporteesClaim = authDetailMap.get("reportees");
            if (reporteesClaim instanceof List reporteesClaimList) {
              for (var reportee : reporteesClaimList) {
                if (reportee instanceof Map<?, ?> reporteeMap) {
                  var authority = reporteeMap.get("Authority");
                  var idClaim = reporteeMap.get("ID");

                  if ("iso6523-actorid-upis".equals(authority)
                      && idClaim instanceof String idString) {
                    var orgNo = getOrgNoFromISO6523(idString);
                    if (orgNo != null) {
                      organizationNumbers.add(orgNo);
                    }
                  }
                }
              }
            }
          }

          // Entra ID
          else if ("ansattporten:orgno".equals(typeClaim)) {
            var orgNoClaim = authDetailMap.get("orgno");
            var orgNo = getOrgNoFromClaim(orgNoClaim);
            if (orgNo != null) {
              organizationNumbers.add(orgNo);
            }
          }
        }
      }

      return organizationNumbers;
    }

    return List.of();
  }

  private String getOrgNoFromClaim(Object orgnoClaim) {
    if (orgnoClaim instanceof Map<?, ?> orgnoMap) {
      var authority = orgnoMap.get("authority");
      var id = orgnoMap.get("ID");

      if ("iso6523-actorid-upis".equals(authority) && id instanceof String idString) {
        return getOrgNoFromISO6523(idString);
      }
    }
    return null;
  }

  private String getOrgNoFromISO6523(String id) {
    // Norwegian orgnummers are prefixed with "0192:"
    if (id.startsWith("0192:")) {
      return id.substring(5); // Skip "0192:"
    }
    return null;
  }
}

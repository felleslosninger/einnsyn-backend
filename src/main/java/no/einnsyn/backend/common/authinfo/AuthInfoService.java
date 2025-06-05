package no.einnsyn.backend.common.authinfo;

import no.einnsyn.backend.authentication.EInnsynPrincipal;
import no.einnsyn.backend.authentication.EInnsynPrincipalBruker;
import no.einnsyn.backend.authentication.EInnsynPrincipalEnhet;
import no.einnsyn.backend.common.authinfo.models.AuthInfoResponse;
import no.einnsyn.backend.common.exceptions.models.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthInfoService {

  public AuthInfoResponse get() throws AuthenticationException {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    var authInfo = new AuthInfoResponse();
    var principal = authentication.getPrincipal();

    if (principal instanceof EInnsynPrincipal eInnsynPrincipal) {
      authInfo.setAuthType(eInnsynPrincipal.getAuthType());
      authInfo.setId(eInnsynPrincipal.getId());
    } else {
      throw new AuthenticationException("Not authenticated");
    }

    if (principal instanceof EInnsynPrincipalEnhet enhetPrincipal) {
      authInfo.setType(AuthInfoResponse.TypeEnum.ENHET.toString());
      authInfo.setOrgnummer(enhetPrincipal.getName());
    } else if (principal instanceof EInnsynPrincipalBruker brukerPrincipal) {
      authInfo.setType(AuthInfoResponse.TypeEnum.BRUKER.toString());
      authInfo.setEmail(brukerPrincipal.getName());
    }

    return authInfo;
  }
}

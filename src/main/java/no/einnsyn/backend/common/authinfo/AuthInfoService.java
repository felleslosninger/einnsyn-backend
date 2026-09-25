package no.einnsyn.backend.common.authinfo;

import no.einnsyn.backend.authentication.EInnsynPrincipal;
import no.einnsyn.backend.authentication.EInnsynPrincipalBruker;
import no.einnsyn.backend.authentication.EInnsynPrincipalEnhet;
import no.einnsyn.backend.common.authinfo.models.AuthInfo;
import no.einnsyn.backend.common.exceptions.models.AuthenticationException;
import no.einnsyn.backend.entities.enhet.EnhetRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthInfoService {

  private final EnhetRepository enhetRepository;

  public AuthInfoService(EnhetRepository enhetRepository) {
    this.enhetRepository = enhetRepository;
  }

  public AuthInfo get() throws AuthenticationException {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      throw new AuthenticationException("Not authenticated");
    }
    var authInfo = new AuthInfo();
    var principal = authentication.getPrincipal();

    if (principal instanceof EInnsynPrincipal eInnsynPrincipal) {
      authInfo.setAuthType(eInnsynPrincipal.getAuthType());
      authInfo.setId(eInnsynPrincipal.getId());
    } else {
      throw new AuthenticationException("Not authenticated");
    }

    if (principal instanceof EInnsynPrincipalEnhet enhetPrincipal) {
      authInfo.setType(AuthInfo.TypeEnum.ENHET.toString());
      authInfo.setOrgnummer(enhetPrincipal.getName());
      // An unverified Enhet is kept off the principal so no authorization check can match it,
      // but the caller still needs its id to look it up.
      if (authInfo.getId() == null && enhetPrincipal.getName() != null) {
        var enhet = enhetRepository.findByOrgnummer(enhetPrincipal.getName());
        if (enhet != null) {
          authInfo.setId(enhet.getId());
        }
      }
    } else if (principal instanceof EInnsynPrincipalBruker brukerPrincipal) {
      authInfo.setType(AuthInfo.TypeEnum.BRUKER.toString());
      authInfo.setEmail(brukerPrincipal.getName());
    }

    return authInfo;
  }
}

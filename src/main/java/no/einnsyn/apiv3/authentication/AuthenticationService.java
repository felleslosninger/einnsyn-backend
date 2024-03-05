package no.einnsyn.apiv3.authentication;

import no.einnsyn.apiv3.authentication.bruker.models.BrukerUserDetails;
import no.einnsyn.apiv3.authentication.hmac.HmacUserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

  public boolean isSelf(String checkId) {
    var selfId = getBrukerId();
    return selfId != null && selfId.equals(checkId);
  }

  /**
   * Get bruker from authentication
   *
   * @return Bruker
   */
  public String getBrukerId() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null) {
      return null;
    }

    var principal = authentication.getPrincipal();
    if (principal == null) {
      return null;
    }

    if (principal instanceof BrukerUserDetails brukerUserDetails) {
      return brukerUserDetails.getId();
    } else {
      return null;
    }
  }

  /**
   * Get Journalenhet ID from authentication
   *
   * @return Journalenhet ID
   */
  public String getJournalenhetId() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null) {
      return null;
    }

    var principal = authentication.getPrincipal();
    if (principal == null) {
      return null;
    }

    if (principal instanceof HmacUserDetails hmacUserDetails) {
      return hmacUserDetails.getEnhetId();
    }

    return null;
  }

  public boolean isAdmin() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null) {
      return false;
    }

    var principal = authentication.getPrincipal();
    if (principal == null) {
      return false;
    }

    if (principal instanceof HmacUserDetails hmacUserDetails) {
      return hmacUserDetails.getAuthorities().stream()
          .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    if (principal instanceof BrukerUserDetails brukerUserDetails) {
      return brukerUserDetails.getAuthorities().stream()
          .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    return false;
  }
}

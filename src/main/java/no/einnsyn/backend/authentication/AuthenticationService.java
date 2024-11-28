package no.einnsyn.backend.authentication;

import no.einnsyn.backend.authentication.apikey.models.ApiKeyUserDetails;
import no.einnsyn.backend.authentication.bruker.models.BrukerUserDetails;
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

    if (principal instanceof ApiKeyUserDetails apiKeyUserDetails) {
      return apiKeyUserDetails.getEnhetId();
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

    if (principal instanceof ApiKeyUserDetails apiKeyUserDetails) {
      return apiKeyUserDetails.getAuthorities().stream()
          .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    if (principal instanceof BrukerUserDetails brukerUserDetails) {
      return brukerUserDetails.getAuthorities().stream()
          .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    return false;
  }
}

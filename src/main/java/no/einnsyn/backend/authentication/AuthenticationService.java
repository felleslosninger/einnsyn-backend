package no.einnsyn.backend.authentication;

import java.util.List;
import no.einnsyn.backend.entities.bruker.models.Bruker;
import no.einnsyn.backend.entities.enhet.EnhetRepository;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

  private final EnhetRepository enhetRepository;

  public AuthenticationService(EnhetRepository enhetRepository) {
    this.enhetRepository = enhetRepository;
  }

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
    if (principal instanceof EInnsynPrincipalBruker brukerPrincipal) {
      // Only return brukerId if the principal is a user
      return brukerPrincipal.getId();
    }

    return null;
  }

  /**
   * Get the ID of the authenticated Enhet, if any.
   *
   * @return ID
   */
  public String getEnhetId() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return null;
    }

    var principal = authentication.getPrincipal();
    if (principal instanceof EInnsynPrincipalEnhet enhetPrincipal) {
      // Only return enhetId if the principal is an enhet
      return enhetPrincipal.getId();
    }

    return null;
  }

  /**
   * Get Journalenhet subtree list from authentication
   *
   * @return
   */
  public List<String> getEnhetSubtreeIdList() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof EInnsynAuthentication eInnsynAuthentication) {
      // Fetch all Enhets from authentication.authorities
      var authorities = eInnsynAuthentication.getAuthorities();
      return authorities.stream()
          .filter(a -> a instanceof EInnsynAuthority)
          .map(a -> (EInnsynAuthority) a)
          .filter(a -> "Enhet".equals(a.getEntity()))
          .map(EInnsynAuthority::getId)
          .filter(id -> id != null && !id.isBlank())
          .toList();
    }

    return List.of();
  }

  /** Check if the authenticated user is an admin */
  public boolean isAdmin() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return false;
    }

    var principal = authentication.getPrincipal();
    if (principal instanceof EInnsynPrincipal apiKeyPrincipal) {
      return apiKeyPrincipal.isAdmin();
    }

    return false;
  }

  /**
   * Get EInnsynCredentials from a list of Enhet objects.
   *
   * @param enhetList
   * @param access
   * @return
   */
  public List<EInnsynAuthority> getAuthoritiesFromEnhet(List<Enhet> enhetList, String access) {
    if (enhetList == null || enhetList.isEmpty()) {
      return List.of();
    }

    var enhetWithChildrenAuthorities =
        enhetList.stream()
            .map(enhet -> enhetRepository.getSubtreeIdList(enhet.getId()))
            .flatMap(List::stream)
            .distinct()
            .map(enhetId -> new EInnsynAuthority("Enhet", enhetId, access))
            .toList();

    return enhetWithChildrenAuthorities;
  }

  /**
   * Get EInnsynCredentials from a list of Bruker objects.
   *
   * @param brukerList
   * @param access
   * @return
   */
  public List<EInnsynAuthority> getAuthoritiesFromBruker(List<Bruker> brukerList, String access) {
    if (brukerList == null || brukerList.isEmpty()) {
      return List.of();
    }

    return brukerList.stream()
        .map(bruker -> new EInnsynAuthority("Bruker", bruker.getId(), access))
        .toList();
  }
}

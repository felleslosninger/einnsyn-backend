package no.einnsyn.backend.authentication.apikey;

import no.einnsyn.backend.authentication.apikey.models.ApiKeyUserDetails;
import no.einnsyn.backend.entities.apikey.ApiKeyService;
import no.einnsyn.backend.entities.enhet.EnhetService;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyUserDetailsService implements UserDetailsService {

  private final ApiKeyService apiKeyService;
  private final EnhetService enhetService;

  public ApiKeyUserDetailsService(ApiKeyService apiKeyService, EnhetService enhetService) {
    this.apiKeyService = apiKeyService;
    this.enhetService = enhetService;
  }

  /**
   * Load API key by ID, and set "enhet" to the actingAsId if it is a descendant of the
   * authenticated "enhet".
   *
   * @param id
   * @param actingAsId
   * @return
   */
  public UserDetails loadUserByUsernameAndActingAs(String id, String actingAsId) {
    var apiKey = apiKeyService.findById(id);
    if (apiKey == null) {
      throw new UsernameNotFoundException(id);
    }

    var authenticatedAsId = apiKey.getEnhet().getId();
    if (actingAsId == null) {
      actingAsId = authenticatedAsId;
    } else if (!enhetService.isAncestorOf(authenticatedAsId, actingAsId)) {
      throw new AuthenticationException("Not allowed to act as " + actingAsId) {};
    }

    var enhetId = actingAsId;
    var enhetSubtreeIdList = enhetService.getSubtreeIdList(enhetId);
    return new ApiKeyUserDetails(apiKey, enhetId, enhetSubtreeIdList);
  }

  @Override
  public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
    var apiKey = apiKeyService.findById(id);
    if (apiKey == null) {
      throw new UsernameNotFoundException(id);
    }
    var enhetId = apiKey.getEnhet().getId();
    var enhetSubtreeIdList = enhetService.getSubtreeIdList(enhetId);

    return new ApiKeyUserDetails(apiKey, enhetId, enhetSubtreeIdList);
  }
}

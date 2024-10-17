package no.einnsyn.apiv3.authentication.apikey;

import no.einnsyn.apiv3.authentication.apikey.models.ApiKeyUserDetails;
import no.einnsyn.apiv3.entities.apikey.ApiKeyService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyUserDetailsService implements UserDetailsService {

  private final ApiKeyService apiKeyService;

  public ApiKeyUserDetailsService(ApiKeyService apiKeyService) {
    this.apiKeyService = apiKeyService;
  }

  @Override
  public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
    var apiKey = apiKeyService.findById(id);
    if (apiKey == null) {
      throw new UsernameNotFoundException(id);
    }
    return new ApiKeyUserDetails(apiKey);
  }
}

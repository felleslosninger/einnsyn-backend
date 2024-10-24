package no.einnsyn.apiv3.auth;

import java.util.HashMap;
import no.einnsyn.apiv3.authentication.apikey.models.ApiKeyUserDetails;
import no.einnsyn.apiv3.authentication.bruker.models.BrukerUserDetails;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("test")
public class AuthenticationController {

  @GetMapping("/testauth")
  public ResponseEntity<Object> testEndpoint() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return null;
    }

    var principal = authentication.getPrincipal();
    if (principal == null) {
      return null;
    }

    var response = new HashMap<String, String>();

    if (principal instanceof UserDetails userDetails) {
      response.put("username", userDetails.getUsername());
      response.put("authorities", userDetails.getAuthorities().toString());
    }

    if (principal instanceof BrukerUserDetails brukerUserDetails) {
      response.put("id", brukerUserDetails.getId());
    }

    if (principal instanceof ApiKeyUserDetails apiKeyUserDetails) {
      response.put("id", apiKeyUserDetails.getId());
      response.put("enhetId", apiKeyUserDetails.getEnhetId());
    }

    return ResponseEntity.ok(response);
  }
}

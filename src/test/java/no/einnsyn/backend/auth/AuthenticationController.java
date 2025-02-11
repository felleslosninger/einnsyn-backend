package no.einnsyn.backend.auth;

import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.authentication.apikey.models.ApiKeyUserDetails;
import no.einnsyn.backend.authentication.bruker.models.BrukerUserDetails;
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
  public ResponseEntity<TestAuthResponse> testEndpoint() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return null;
    }

    var principal = authentication.getPrincipal();
    if (principal == null) {
      return null;
    }

    var response = new TestAuthResponse();

    if (principal instanceof UserDetails userDetails) {

      response.setUsername(userDetails.getUsername());
      response.setAuthorities(userDetails.getAuthorities().toString());
    }

    if (principal instanceof BrukerUserDetails brukerUserDetails) {
      response.setId(brukerUserDetails.getId());
    }

    if (principal instanceof ApiKeyUserDetails apiKeyUserDetails) {
      response.setId(apiKeyUserDetails.getId());
      response.setEnhetId(apiKeyUserDetails.getEnhetId());
    }

    return ResponseEntity.ok(response);
  }

  @Getter
  @Setter
  public class TestAuthResponse {
    public String username;
    public String authorities;
    public String id;
    public String enhetId;
  }
}

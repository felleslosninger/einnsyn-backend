package no.einnsyn.backend.auth;

import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.authentication.EInnsynPrincipal;
import no.einnsyn.backend.authentication.EInnsynPrincipalBruker;
import no.einnsyn.backend.authentication.EInnsynPrincipalEnhet;
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
    }
    if (principal instanceof EInnsynPrincipal eInnsynPrincipal) {
      response.setUsername(eInnsynPrincipal.getName());
    }

    if (principal instanceof EInnsynPrincipalBruker brukerPrincipal) {
      response.setId(brukerPrincipal.getId());
    } else if (principal instanceof EInnsynPrincipalEnhet enhetPrincipal) {
      response.setId(enhetPrincipal.getAuthId());
      response.setEnhetId(enhetPrincipal.getId());
    }

    return ResponseEntity.ok(response);
  }

  @Getter
  @Setter
  public static class TestAuthResponse {
    public String username;
    public String id;
    public String enhetId;
  }
}

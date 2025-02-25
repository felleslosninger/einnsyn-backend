package no.einnsyn.backend.authentication.bruker;

import jakarta.validation.Valid;
import no.einnsyn.backend.authentication.bruker.models.BrukerLoginRequestBody;
import no.einnsyn.backend.authentication.bruker.models.BrukerUserDetails;
import no.einnsyn.backend.authentication.bruker.models.TokenResponse;
import no.einnsyn.backend.common.exceptions.models.AuthenticationException;
import no.einnsyn.backend.entities.bruker.BrukerService;
import no.einnsyn.backend.entities.bruker.models.Bruker;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class BrukerAuthenticationController {
  private final JwtService jwtService;
  private final BrukerService brukerService;

  public BrukerAuthenticationController(JwtService jwtService, BrukerService brukerService) {
    this.jwtService = jwtService;
    this.brukerService = brukerService;
  }

  @PostMapping("/token")
  public ResponseEntity<TokenResponse> login(@Valid @RequestBody BrukerLoginRequestBody loginData)
      throws AuthenticationException {
    var username = loginData.getUsername();
    var password = loginData.getPassword();
    var refreshToken = loginData.getRefreshToken();
    Bruker bruker;

    // Authorize using refresh token
    if (refreshToken != null) {
      username = jwtService.validateAndReturnIdOrUsername(refreshToken, "refresh");
      if (username == null) {
        throw new AuthenticationException("Invalid refresh token");
      }
      bruker = brukerService.findById(username);
      if (bruker == null) {
        throw new AuthenticationException("Invalid refresh token");
      }
    }

    // Authorize using username / password
    else {
      bruker = brukerService.findById(username);
      if (bruker != null && !bruker.isActive()) {
        throw new AuthenticationException("User account is not activated");
      } else if (bruker == null || !brukerService.authenticate(bruker, password)) {
        throw new AuthenticationException("Invalid username or password");
      }
    }

    var brukerUserDetails = new BrukerUserDetails(bruker);
    var tokenResponse =
        new TokenResponse(
            jwtService.generateToken(brukerUserDetails),
            jwtService.generateRefreshToken(brukerUserDetails),
            jwtService.getExpiration());

    return ResponseEntity.ok(tokenResponse);
  }
}

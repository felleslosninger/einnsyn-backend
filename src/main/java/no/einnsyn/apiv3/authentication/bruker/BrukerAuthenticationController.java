package no.einnsyn.apiv3.authentication.bruker;

import jakarta.validation.Valid;
import no.einnsyn.apiv3.authentication.bruker.models.BrukerLoginRequestBody;
import no.einnsyn.apiv3.authentication.bruker.models.BrukerUserDetails;
import no.einnsyn.apiv3.authentication.bruker.models.TokenResponse;
import no.einnsyn.apiv3.common.exceptions.UnauthorizedException;
import no.einnsyn.apiv3.entities.bruker.BrukerService;
import no.einnsyn.apiv3.entities.bruker.models.Bruker;
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
      throws UnauthorizedException {
    var username = loginData.getUsername();
    var password = loginData.getPassword();
    var refreshToken = loginData.getRefreshToken();
    Bruker bruker;

    // Authorize using refresh token
    if (refreshToken != null) {
      username = jwtService.validateAndReturnUsername(refreshToken, "refresh");
      if (username == null) {
        throw new UnauthorizedException("Invalid refresh token");
      }
      bruker = brukerService.findById(username);
      if (bruker == null) {
        throw new UnauthorizedException("Invalid refresh token");
      }
    }

    // Authorize using username / password
    else {
      bruker = brukerService.findById(username);
      if (bruker != null && !bruker.isActive()) {
        throw new UnauthorizedException("User account is not activated");
      } else if (bruker == null || !brukerService.authenticate(bruker, password)) {
        throw new UnauthorizedException("Invalid username or password");
      }
    }

    // @formatter:off
    var brukerUserDetails = new BrukerUserDetails(bruker);
    var tokenResponse =
        new TokenResponse(
            jwtService.generateToken(brukerUserDetails),
            jwtService.generateRefreshToken(brukerUserDetails),
            jwtService.getExpiration());
    // @formatter:on

    return ResponseEntity.ok(tokenResponse);
  }
}

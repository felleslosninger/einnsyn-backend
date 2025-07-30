package no.einnsyn.backend.authentication.bruker;

import jakarta.validation.Valid;
import no.einnsyn.backend.authentication.bruker.models.BrukerLoginRequestBody;
import no.einnsyn.backend.authentication.bruker.models.TokenResponse;
import no.einnsyn.backend.common.exceptions.models.AuthenticationException;
import no.einnsyn.backend.entities.bruker.BrukerService;
import no.einnsyn.backend.entities.bruker.models.Bruker;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class BrukerAuthenticationController {
  private final EInnsynTokenService tokenService;
  private final BrukerService brukerService;

  public BrukerAuthenticationController(
      EInnsynTokenService jwtService, BrukerService brukerService) {
    this.tokenService = jwtService;
    this.brukerService = brukerService;
  }

  @PostMapping("/token")
  public ResponseEntity<TokenResponse> login(@Valid @RequestBody BrukerLoginRequestBody loginData)
      throws AuthenticationException {
    var username = loginData.getUsername() != null ? loginData.getUsername().toLowerCase() : null;
    var password = loginData.getPassword();
    var refreshToken = loginData.getRefreshToken();
    Bruker bruker;

    // Authorize using refresh token
    if (refreshToken != null) {
      Jwt jwt;
      try {
        jwt = tokenService.decodeToken(refreshToken);
      } catch (JwtException e) {
        throw new AuthenticationException("Invalid refresh token: " + e.getMessage());
      }
      username = jwt.getSubject();
      if (username == null) {
        throw new AuthenticationException("Invalid refresh token");
      }
      bruker = brukerService.findByIdOrThrow(username.toLowerCase(), AuthenticationException.class);
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

    var tokenResponse =
        new TokenResponse(
            tokenService.generateToken(bruker),
            tokenService.generateRefreshToken(bruker),
            tokenService.getExpiration());

    return ResponseEntity.ok(tokenResponse);
  }
}

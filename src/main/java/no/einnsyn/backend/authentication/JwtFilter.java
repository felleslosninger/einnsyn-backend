package no.einnsyn.backend.authentication;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import no.einnsyn.backend.authentication.ansattporten.AnsattportenAuthenticationProvider;
import no.einnsyn.backend.authentication.bruker.BrukerAuthenticationProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtFilter extends OncePerRequestFilter {
  private final AuthenticationManager authenticationManager;

  public JwtFilter(
      BrukerAuthenticationProvider brukerProvider,
      AnsattportenAuthenticationProvider ansattportenProvider) {
    this.authenticationManager = new ProviderManager(brukerProvider, ansattportenProvider);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    // Extract the bearer token from the Authorization header
    var token =
        Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION))
            .filter(header -> header.toUpperCase().startsWith("BEARER "))
            .map(header -> header.substring(7))
            .orElse(null);

    if (token != null) {
      var authenticationRequest = new EInnsynAuthentication(null, token, null);

      try {
        var authenticationResult = authenticationManager.authenticate(authenticationRequest);
        SecurityContextHolder.getContext().setAuthentication(authenticationResult);
      } catch (AuthenticationException e) {
        SecurityContextHolder.clearContext();
        logger.debug("Authentication request for token failed: " + e.getMessage());
      }
    } else {
      logger.trace("No bearer token found in request.");
    }

    filterChain.doFilter(request, response);
  }
}

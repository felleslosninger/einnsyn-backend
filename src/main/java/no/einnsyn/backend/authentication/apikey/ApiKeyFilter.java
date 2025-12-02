package no.einnsyn.backend.authentication.apikey;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.authentication.AuthenticationConfiguration; // For header names
import no.einnsyn.backend.authentication.EInnsynAuthentication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
public class ApiKeyFilter extends OncePerRequestFilter {

  private final AuthenticationManager apiKeyAuthenticationManager;

  public ApiKeyFilter(ApiKeyAuthenticationProvider apiKeyAuthenticationProvider) {
    this.apiKeyAuthenticationManager = new ProviderManager(apiKeyAuthenticationProvider);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    // This is invoked after checking that the API_KEY_HEADER exists
    var apiKeyHeaderValue = request.getHeader(AuthenticationConfiguration.API_KEY_HEADER);

    // Return early if the API Key header is not present or is empty
    if (apiKeyHeaderValue == null || apiKeyHeaderValue.isBlank()) {
      log.trace("No API Key header found in request.");
      filterChain.doFilter(request, response);
      return;
    }

    var actingAsHeaderValue = request.getHeader(AuthenticationConfiguration.ACTING_AS_HEADER);
    var credentials =
        new ApiKeyCredentials(
            apiKeyHeaderValue.trim(),
            actingAsHeaderValue != null ? actingAsHeaderValue.trim() : null);

    // Create an unauthenticated token with ApiKeyCredentials
    var authenticationRequest = new EInnsynAuthentication(credentials);

    try {
      var authenticationResult = apiKeyAuthenticationManager.authenticate(authenticationRequest);
      SecurityContextHolder.getContext().setAuthentication(authenticationResult);
      log.trace("API Key authentication successful for: {}", authenticationResult.getName());
    } catch (AuthenticationException e) {
      SecurityContextHolder.clearContext();
      log.debug("API Key authentication failed: {}", e.getMessage());
    }

    filterChain.doFilter(request, response);
  }
}

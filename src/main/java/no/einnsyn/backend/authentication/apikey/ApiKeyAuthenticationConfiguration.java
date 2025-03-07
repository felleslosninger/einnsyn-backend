package no.einnsyn.backend.authentication.apikey;

import com.google.gson.Gson;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.entities.apikey.ApiKeyService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@Slf4j
public class ApiKeyAuthenticationConfiguration {

  private final ApiKeyService apiKeyService;
  private final ApiKeyUserDetailsService apiKeyUserDetailsService;
  private final Gson gson;

  public ApiKeyAuthenticationConfiguration(
      ApiKeyService apiKeyService, ApiKeyUserDetailsService apiKeyUserDetailsService, Gson gson) {
    this.apiKeyService = apiKeyService;
    this.apiKeyUserDetailsService = apiKeyUserDetailsService;
    this.gson = gson;
  }

  /**
   * This filter chain is used for ApiKey authentication.
   *
   * @param http
   * @return
   * @throws Exception
   */
  @Bean
  @Order(1)
  SecurityFilterChain apiKeyAuthFilterChain(HttpSecurity http) throws Exception {
    var apiKeyAuthenticationFilter = new ApiKeyAuthenticationFilter();

    http.securityMatcher(
            request ->
                request.getHeader("X-EIN-API-KEY") != null || request.getHeader("API-KEY") != null)
        .cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /** A custom authentication filter for ApiKey authentication. */
  private class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

      // Key could either be given in the API-KEY header or in the Authorization header
      var key = request.getHeader("API-KEY");
      // Fall back to X-EIN-API-KEY header
      // TODO: Remove when the java client is updated
      if (key == null) {
        key = request.getHeader("X-EIN-API-KEY");
      }
      log.trace("ApiKey Auth, key: {}", key);

      // The request can be done on behalf of another Enhet, that is below the authenticated Enhet
      var actingAsId = request.getHeader("ACTING-AS");
      if (actingAsId != null) {
        log.trace("Acting as Enhet: {}", actingAsId);
      }

      try {
        var apiKey = apiKeyService.findBySecretKey(key);
        if (apiKey == null) {
          throw new AuthenticationException("Invalid API key") {};
        }

        // Auth was successful, set the UserDetails in the security context.
        var userDetails =
            apiKeyUserDetailsService.loadUserByUsernameAndActingAs(apiKey.getId(), actingAsId);
        var authToken =
            new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
      } catch (AuthenticationException e) {
        log.warn("Failed login attempt: {}", e.getMessage());
        var exception =
            new no.einnsyn.backend.common.exceptions.models.AuthenticationException(
                "Failed to authenticate", e);
        var responseBody = gson.toJson(exception.toClientResponse());
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, responseBody);
      }
    }
  }
}

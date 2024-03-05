package no.einnsyn.apiv3.authentication.hmac;

import com.google.gson.Gson;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.entities.apikey.ApiKeyService;
import no.einnsyn.apiv3.error.responses.ErrorResponse;
import no.einnsyn.apiv3.utils.hmac.Hmac;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
public class HmacAuthenticationConfiguration {

  private final HmacUserDetailsService hmacUserDetailsService;
  private final ApiKeyService apiKeyService;
  private final Gson gson;

  @Value("${application.baseUrl}")
  private String baseUrl;

  public HmacAuthenticationConfiguration(
      HmacUserDetailsService hmacUserDetailsService, ApiKeyService apiKeyService, Gson gson) {
    this.hmacUserDetailsService = hmacUserDetailsService;
    this.apiKeyService = apiKeyService;
    this.gson = gson;
  }

  /**
   * This filter chain is used for HMAC authentication.
   *
   * @param http
   * @return
   * @throws Exception
   */
  @Bean
  @Order(1)
  SecurityFilterChain hmacAuthFilterChain(HttpSecurity http) throws Exception {
    var hmacAuthenticationFilter = new HmacAuthenticationFilter();

    http.securityMatcher(
            request ->
                Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION))
                    .map(h -> h.toUpperCase().startsWith("HMAC-SHA256 "))
                    .orElse(false))
        .cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(hmacAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /** A custom authentication filter for HMAC authentication. */
  private class HmacAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

      // This is what valid HMAC Authorization headers might look like:
      // Authorization: "HMAC-SHA256 <hmac-sha256-signature>"

      // We know the request has a valid header, since it passed the security matcher.
      var tokenHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
      var clientHmac = tokenHeader.substring(12);
      var method = request.getMethod();
      var path = request.getRequestURI();
      var timestamp = request.getHeader("x-ein-timestamp");

      log.trace(
          "HMAC Auth, Method: {} Path: {} Timestamp: {} Token: {}",
          method,
          path,
          timestamp,
          clientHmac);

      // TODO: Check if the key has already been used (Redis?)

      try {
        if (timestamp == null) {
          throw new AuthenticationException("Missing timestamp") {};
        }

        var keyId = request.getHeader("x-ein-api-key");
        var apiKey = apiKeyService.findById(keyId);
        if (apiKey == null) {
          throw new AuthenticationException("Invalid API key '" + keyId + "'") {};
        }

        var secret = apiKey.getSecretKey();
        var serverHmac = Hmac.generateHmac(method, path, timestamp, secret);
        if (!serverHmac.equals(clientHmac)) {
          throw new AuthenticationException("Invalid HMAC signature") {};
        }

        // Auth was successful, set the UserDetails in the security context.
        var userDetails = hmacUserDetailsService.loadUserByUsername(keyId);
        var authToken =
            new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
        filterChain.doFilter(request, response);
      } catch (AuthenticationException e) {
        log.warn("Failed login attempt: {}", e.getMessage());
        var errorResponse = new ErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage(), null, null);
        var responseBody = gson.toJson(errorResponse);
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, responseBody);
      }
    }
  }
}

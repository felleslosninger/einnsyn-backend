package no.einnsyn.backend.authentication;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import no.einnsyn.backend.authentication.apikey.ApiKeyAuthenticationProvider;
import no.einnsyn.backend.authentication.apikey.ApiKeyFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;

@Configuration
public class AuthenticationConfiguration {

  public static final String API_KEY_HEADER = "API-KEY";
  public static final String ACTING_AS_HEADER = "ACTING-AS";

  private final ApiKeyFilter apiKeyFilter;
  private final EInnsynAuthenticationEntryPoint eInnsynAuthenticationEntryPoint;
  private final JwtFilter jwtFilter;

  public AuthenticationConfiguration(
      ApiKeyFilter apiKeyFilter,
      EInnsynAuthenticationEntryPoint eInnsynAuthenticationEntryPoint,
      JwtFilter jwtFilter) {
    this.apiKeyFilter = apiKeyFilter;
    this.eInnsynAuthenticationEntryPoint = eInnsynAuthenticationEntryPoint;
    this.jwtFilter = jwtFilter;
  }

  /**
   * Security Filter Chain for API Key Authentication. This chain is attempted first for requests
   * containing the API-KEY header.
   */
  @Bean
  @Order(1)
  public SecurityFilterChain apiKeyFilterChain(
      HttpSecurity http, ApiKeyAuthenticationProvider apiKeyAuthenticationProvider)
      throws Exception {

    var apiKeyRequestMatcher = new RequestHeaderRequestMatcher(API_KEY_HEADER);

    http.securityMatcher(apiKeyRequestMatcher)
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(apiKeyFilter, BasicAuthenticationFilter.class)
        .authorizeHttpRequests(authz -> authz.anyRequest().authenticated())
        .anonymous(AbstractHttpConfigurer::disable)
        .exceptionHandling(
            exceptions -> exceptions.authenticationEntryPoint(eInnsynAuthenticationEntryPoint));

    return http.build();
  }

  /**
   * Security Filter Chain for JWT Bearer Token Authentication. This chain is attempted if the API
   * Key chain did not match or did not authenticate. It specifically looks for the Authorization:
   * Bearer header.
   */
  @Bean
  @Order(2)
  public SecurityFilterChain jwtAuthentication(HttpSecurity http) throws Exception {

    http.securityMatcher(
            (HttpServletRequest request) ->
                Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION))
                    .map(h -> h.toUpperCase().startsWith("BEARER "))
                    .orElse(false))
        .csrf(csrf -> csrf.disable())
        .sessionManagement(
            management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(jwtFilter, BasicAuthenticationFilter.class)
        .authorizeHttpRequests(authz -> authz.anyRequest().authenticated())
        .anonymous(AbstractHttpConfigurer::disable)
        .exceptionHandling(
            exceptions -> exceptions.authenticationEntryPoint(eInnsynAuthenticationEntryPoint));

    return http.build();
  }

  /**
   * This filter chain allows all requests, without setting any authentication. Authorization will
   * be handled by the controllers.
   */
  @Bean
  @Order(Ordered.LOWEST_PRECEDENCE)
  SecurityFilterChain allowAll(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
        .exceptionHandling(
            exceptions -> exceptions.authenticationEntryPoint(eInnsynAuthenticationEntryPoint));

    return http.build();
  }
}

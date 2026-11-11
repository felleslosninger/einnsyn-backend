package no.einnsyn.backend.authentication;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import no.einnsyn.backend.authentication.apikey.ApiKeyFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
  private final boolean requireApiKey;

  public AuthenticationConfiguration(
      ApiKeyFilter apiKeyFilter,
      EInnsynAuthenticationEntryPoint eInnsynAuthenticationEntryPoint,
      JwtFilter jwtFilter,
      @Value("${application.requireApiKey:false}") boolean requireApiKey) {
    this.apiKeyFilter = apiKeyFilter;
    this.eInnsynAuthenticationEntryPoint = eInnsynAuthenticationEntryPoint;
    this.jwtFilter = jwtFilter;
    this.requireApiKey = requireApiKey;
  }

  /**
   * Security Filter Chain for API Key Authentication. This chain is attempted first for requests
   * containing the API-KEY header.
   */
  @Bean
  @Order(1)
  public SecurityFilterChain apiKeyFilterChain(HttpSecurity http) throws Exception {

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
        .csrf(AbstractHttpConfigurer::disable)
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
   * be handled by the controllers. When `requireApiKey` is enabled, GET requests are rejected,
   * since requests reaching this chain have neither an API key nor a Bearer token. Downloads are
   * still allowed, since they are opened directly in the browser.
   */
  @Bean
  @Order(Ordered.LOWEST_PRECEDENCE)
  SecurityFilterChain allowAll(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authorize -> {
              if (requireApiKey) {
                authorize
                    .requestMatchers(HttpMethod.GET, "/dokumentobjekt/*/download")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET)
                    .authenticated();
              }
              authorize.anyRequest().permitAll();
            })
        .exceptionHandling(
            exceptions -> exceptions.authenticationEntryPoint(eInnsynAuthenticationEntryPoint));

    return http.build();
  }
}

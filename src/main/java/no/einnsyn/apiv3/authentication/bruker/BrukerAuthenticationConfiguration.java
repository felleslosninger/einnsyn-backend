package no.einnsyn.apiv3.authentication.bruker;

import java.io.IOException;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class BrukerAuthenticationConfiguration {

  private final BrukerUserDetailsService brukerUserDetailsService;
  private final JwtService jwtService;

  public BrukerAuthenticationConfiguration(BrukerUserDetailsService brukerUserDetailsService,
      JwtService jwtService) {
    this.brukerUserDetailsService = brukerUserDetailsService;
    this.jwtService = jwtService;
  }


  @Bean
  @Order(2) // Check this after HMAC (api key) authentication.
  SecurityFilterChain brukerAuthentication(HttpSecurity http) throws Exception {
    var brukerAuthenticationFilter = new BrukerAuthenticationFilter();
    var authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(brukerUserDetailsService);
    authProvider.setPasswordEncoder(new BCryptPasswordEncoder());

    // @formatter:off
    http
      .securityMatcher((HttpServletRequest request) -> 
        Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION)).map(h -> 
          h.toUpperCase().startsWith("BEARER ")
        ).orElse(false)
      )
      .cors(Customizer.withDefaults())
      .csrf(csrf -> csrf.disable())
      .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authenticationProvider(authProvider)
      .addFilterBefore(brukerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    // @formatter:on

    return http.build();
  }


  /**
   * Custom authentication filter for JWT tokens.
   */
  private class BrukerAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {

      // We know the request has a valid header, since it passed the security matcher.
      var header = request.getHeader("Authorization");
      var token = header.substring(7);
      var username = jwtService.validateAndReturnUsername(token, "access");
      if (username == null) {
        // Do we want to log this? No username given, or expired token.
        // Since Oauth2 also uses Bearer tokens, the login may be handled in another chain.
        filterChain.doFilter(request, response);
        return;
      }

      try {
        var userDetails = brukerUserDetailsService.loadUserByUsername(username);
        var authToken = new UsernamePasswordAuthenticationToken(userDetails, null,
            userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
      } catch (Exception e) {
        // Should we log this? Again, it might be valid in another chain.
      }

      filterChain.doFilter(request, response);
    }
  }



}

package no.einnsyn.apiv3.authentication.hmac;

import java.io.IOException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class HmacAuthenticationConfiguration {

  @Value("${application.baseUrl}")
  private String baseUrl;


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

    // @formatter:off
    http
      .securityMatcher((HttpServletRequest request) -> 
        Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION)).map(h -> 
          h.toUpperCase().startsWith("HMAC-SHA256 ")
        ).orElse(false)
      )
      .cors(Customizer.withDefaults())
      .csrf(AbstractHttpConfigurer::disable)
      .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .addFilterBefore(hmacAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    // @formatter:on

    return http.build();
  }


  /**
   * A custom authentication filter for HMAC authentication.
   */
  private class HmacAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {

      // This is what a HMAC Authorization header might look like:
      // Authorization: "HMAC-SHA256
      // SignedHeaders=x-date;host;x-content-sha256&Signature=<hmac-sha256-signature>"

      // We know the request has a valid header, since it passed the security matcher.
      var header = request.getHeader(HttpHeaders.AUTHORIZATION);
      var token = header.substring(12);

      // TODO: Check if the key has already been used (Redis?)

      try {
        // TODO: Implement
      } catch (Exception e) {
        // Here we might want to throw, there won't be other filters authenticating HMAC, and it
        // will ease debugging if we give a proper error response.

        // handlerExceptionResolver.resolveException(request, response, null, e);
      }

      filterChain.doFilter(request, response);
    }
  }

}

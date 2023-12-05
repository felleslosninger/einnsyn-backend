package no.einnsyn.apiv3.authentication;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class AllowAllAuthenticationConfiguration {

  /**
   * This filter chain allows all requests, without setting any authentication. Authorization will
   * be handled by the controllers.
   * 
   * @param http
   * @return
   * @throws Exception
   */
  @Bean
  @Order(Ordered.LOWEST_PRECEDENCE)
  SecurityFilterChain allowAll(HttpSecurity http) throws Exception {
    // @formatter:off
    http
      .cors(Customizer.withDefaults())
      .csrf(csrf -> csrf.disable())
      .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
    // @formatter:on

    return http.build();
  }

}

package no.einnsyn.apiv3.authentication;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class EinnsynCorsConfiguration {

  @Bean
  CorsFilter corsFilter() {
    var configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of(CorsConfiguration.ALL));
    configuration.setAllowedMethods(List.of(CorsConfiguration.ALL));
    configuration.setAllowedHeaders(List.of(CorsConfiguration.ALL));

    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return new CorsFilter(source);
  }

}

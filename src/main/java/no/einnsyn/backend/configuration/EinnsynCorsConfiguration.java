package no.einnsyn.backend.configuration;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/** Allows CORS requests from our frontend application. */
@Configuration
public class EinnsynCorsConfiguration {

  @Bean
  CorsFilter corsFilter(@Value("${application.baseUrl}") String baseUrl) {
    var configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(
        List.of(baseUrl, "http://localhost:3000", "https://*.einnsyn.no", "https://*.einnsyn.dev"));
    configuration.setAllowedMethods(List.of(CorsConfiguration.ALL));
    configuration.setAllowedHeaders(List.of(CorsConfiguration.ALL));
    configuration.setAllowCredentials(true);

    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return new CorsFilter(source);
  }
}

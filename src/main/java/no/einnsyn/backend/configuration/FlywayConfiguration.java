package no.einnsyn.backend.configuration;

import java.util.HashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

@Configuration
public class FlywayConfiguration {

  /**
   * Validate that the fallback API key is set and does not contain a single quote, and add it to
   * the Flyway placeholders so that it can be used in the baseline migration.
   */
  @Bean
  FlywayConfigurationCustomizer rootApiKeyPlaceholder(
      @Value("${application.apikey.root-key}") String rootApiKey) {
    Assert.hasText(rootApiKey, "application.apikey.root-key (ROOT_API_KEY) must not be empty");
    Assert.doesNotContain(
        rootApiKey,
        "'",
        "application.apikey.root-key (ROOT_API_KEY) must not contain a single quote, as it is"
            + " interpolated into a quoted SQL literal in the baseline migration");
    return configuration -> {
      // Merge rather than replace: Spring Boot has already applied spring.flyway.placeholders by
      // the time customizers run, and placeholders(Map) overwrites the whole map.
      var placeholders = new HashMap<>(configuration.getPlaceholders());
      placeholders.put("apikey-root-key", rootApiKey);
      configuration.placeholders(placeholders);
    };
  }
}

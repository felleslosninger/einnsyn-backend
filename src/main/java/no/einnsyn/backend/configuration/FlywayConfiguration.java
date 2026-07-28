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
   * Supplies the root API key secret to the baseline migration, which seeds it as the secret of the
   * root API key. The root Enhet has no parent, so that key authenticates as an administrator. Only
   * the baseline migration reads this value, so it has no effect against an already migrated
   * database.
   *
   * <p>This is deliberately not wired through {@code spring.flyway.placeholders} in
   * application.yml: the configuration property binder leaves placeholders it cannot resolve as
   * literal text, so with {@code ROOT_API_KEY} unset, the yml route seeds {@code
   * sha256("${ROOT_API_KEY}")} — a working administrator credential that anyone reading this
   * repository could present. A {@code @Value} parameter resolves strictly instead, and fails while
   * the Flyway bean is being built, before any migration runs.
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

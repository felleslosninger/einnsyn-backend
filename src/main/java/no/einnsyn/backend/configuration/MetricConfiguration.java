package no.einnsyn.backend.configuration;

import io.micrometer.core.instrument.config.MeterFilter;
import no.einnsyn.backend.utils.idgenerator.IdValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricConfiguration {

  /**
   * To avoid reaching the maximum limit of URIs for "http.client.requests", we need to normalize
   * URIs containing variables.
   */
  @Bean
  public MeterFilter uriNormalizer() {

    return MeterFilter.replaceTagValues(
        "uri",
        uri -> {
          // Replace valid eInnsyn IDs
          uri = uri.replaceAll(IdValidator.ID_PATTERN, "{id}");

          // Replace email-addresses
          uri = uri.replaceAll("[^/]+@[^/]+", "{email}");

          // Replace UUIDs (Noark system-id)
          uri =
              uri.replaceAll(
                  "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}",
                  "{uuid}");

          return uri;
        });
  }
}

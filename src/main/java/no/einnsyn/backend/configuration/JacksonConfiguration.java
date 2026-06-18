package no.einnsyn.backend.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class JacksonConfiguration {

  @Bean
  ObjectMapper objectMapper() {
    return JsonMapper.builder()
        .findAndAddModules()
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build();
  }
}

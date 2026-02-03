package no.einnsyn.backend.configuration;

import java.io.IOException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfiguration {

  RestTemplateBuilder restTemplateBuilder;

  public RestTemplateConfiguration(RestTemplateBuilder restTemplateBuilder) {
    this.restTemplateBuilder = restTemplateBuilder;
  }

  @Bean
  RestTemplate restTemplate() {
    var restTemplate =
        restTemplateBuilder
            .errorHandler(new RestTemplateResponseErrorHandler())
            .requestFactory(HttpComponentsClientHttpRequestFactory.class)
            .build();

    return restTemplate;
  }

  public static class RestTemplateResponseErrorHandler extends DefaultResponseErrorHandler {
    @Override
    public void handleError(ClientHttpResponse response) throws IOException {}

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
      return false;
    }
  }
}

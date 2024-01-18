package no.einnsyn.apiv3.configuration;

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
    // @formatter:off
    var restTemplate =
        restTemplateBuilder
            .errorHandler(new RestTemplateResponseErrorHandler())
            .requestFactory(HttpComponentsClientHttpRequestFactory.class)
            .build();
    // @formatter:on

    // restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());

    return restTemplate;
  }

  public class RestTemplateResponseErrorHandler extends DefaultResponseErrorHandler {
    @Override
    public void handleError(ClientHttpResponse response) throws IOException {}

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
      return false;
    }
  }
}

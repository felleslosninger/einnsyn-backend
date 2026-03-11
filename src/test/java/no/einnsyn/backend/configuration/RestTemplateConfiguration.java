package no.einnsyn.backend.configuration;

import java.io.IOException;
import java.util.List;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
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
    var httpClient = HttpClients.custom().disableRedirectHandling().build();
    var restTemplate =
        restTemplateBuilder
            .errorHandler(new RestTemplateResponseErrorHandler())
            .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(httpClient))
            .build();

    // Allow StringHttpMessageConverter to read any content type (e.g. application/pdf)
    for (var converter : restTemplate.getMessageConverters()) {
      if (converter instanceof StringHttpMessageConverter stringConverter) {
        stringConverter.setSupportedMediaTypes(List.of(MediaType.TEXT_PLAIN, MediaType.ALL));
        break;
      }
    }

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

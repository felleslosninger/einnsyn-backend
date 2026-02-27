package no.einnsyn.backend.configuration;

import jakarta.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfiguration {

  @Bean
  RestTemplate restTemplate() {
    var restTemplate = new RestTemplate();
    restTemplate.setErrorHandler(new RestTemplateResponseErrorHandler());
    restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
    return restTemplate;
  }

  public static class RestTemplateResponseErrorHandler extends DefaultResponseErrorHandler {
    @Override
    public void handleError(
        ClientHttpResponse response,
        HttpStatusCode statusCode,
        @Nullable URI url,
        @Nullable HttpMethod method)
        throws IOException {}

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
      return false;
    }
  }
}

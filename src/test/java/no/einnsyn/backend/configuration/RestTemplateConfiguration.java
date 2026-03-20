package no.einnsyn.backend.configuration;

import jakarta.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfiguration {

  @Bean
  RestTemplate restTemplate() {
    var restTemplate = new RestTemplate();
    restTemplate.setErrorHandler(new RestTemplateResponseErrorHandler());
    restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());

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

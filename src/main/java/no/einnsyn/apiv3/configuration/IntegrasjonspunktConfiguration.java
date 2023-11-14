package no.einnsyn.apiv3.configuration;

import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import no.einnsyn.clients.ip.client.ApiClient;

@Configuration
public class IntegrasjonspunktConfiguration {

  @URL
  @Value("${application.integrasjonspunkt.moveUrl}")
  private String moveUrl;

  private ApiClient apiClient;

  @Bean
  ApiClient apiClient() {
    if (apiClient == null) {
      apiClient = new ApiClient();
      apiClient.setBasePath(moveUrl);
    }
    return apiClient;
  }
}

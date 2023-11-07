package no.einnsyn.apiv3.configuration;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

@Configuration
public class ElasticsearchConfiguration {


  @Value("${application.elasticsearchUri}")
  private String elasticsearchUri;

  @Bean
  public ElasticsearchClient client() {
    var restClient = RestClient.builder(HttpHost.create(elasticsearchUri)).build();

    ElasticsearchTransport transport =
        new RestClientTransport(restClient, new JacksonJsonpMapper());
    return new ElasticsearchClient(transport);
  }
}

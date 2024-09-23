package no.einnsyn.apiv3.configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import no.einnsyn.apiv3.tasks.elasticsearch.ElasticsearchHandlerInterceptor;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ElasticsearchConfiguration implements WebMvcConfigurer {

  @Value("${application.elasticsearch.uri}")
  private String elasticsearchUri;

  private ElasticsearchHandlerInterceptor esInterceptor;

  public ElasticsearchConfiguration(ElasticsearchHandlerInterceptor esInterceptor) {
    this.esInterceptor = esInterceptor;
  }

  @Bean
  RestClient restClient() {
    return RestClient.builder(HttpHost.create(elasticsearchUri)).build();
  }

  @Bean
  ElasticsearchClient client() {
    var restClient = restClient();

    ElasticsearchTransport transport =
        new RestClientTransport(restClient, new JacksonJsonpMapper());
    return new ElasticsearchClient(transport);
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(esInterceptor);
  }
}

package no.einnsyn.backend.configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import jakarta.annotation.PostConstruct;
import no.einnsyn.backend.tasks.handlers.index.ElasticsearchHandlerInterceptor;
import no.einnsyn.backend.utils.ElasticsearchIndexCreator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ElasticsearchConfiguration implements WebMvcConfigurer {

  private final ElasticsearchHandlerInterceptor esInterceptor;
  private final ElasticsearchClient elasticsearchClient;
  private final String elasticsearchIndex;
  private final String percolatorIndex;

  public ElasticsearchConfiguration(
      ElasticsearchHandlerInterceptor esInterceptor,
      ElasticsearchClient elasticsearchClient,
      @Value("${application.elasticsearch.index}") String elasticsearchIndex,
      @Value("${application.elasticsearch.percolatorIndex}") String percolatorIndex) {
    this.esInterceptor = esInterceptor;
    this.elasticsearchClient = elasticsearchClient;
    this.elasticsearchIndex = elasticsearchIndex;
    this.percolatorIndex = percolatorIndex;
  }

  @PostConstruct
  public void initIndices() {
    // Initialize indices with mappings and settings
    ElasticsearchIndexCreator.maybeCreateIndex(elasticsearchClient, elasticsearchIndex);
    ElasticsearchIndexCreator.maybeCreateIndex(elasticsearchClient, percolatorIndex);
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(esInterceptor);
  }
}

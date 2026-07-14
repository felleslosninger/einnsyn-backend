package no.einnsyn.backend.configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import no.einnsyn.backend.tasks.handlers.index.ElasticsearchHandlerInterceptor;
import no.einnsyn.backend.utils.ElasticsearchIndexCreator;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchConnectionDetails;
import org.springframework.boot.elasticsearch.autoconfigure.Rest5ClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
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

  /**
   * Send credentials preemptively instead of waiting for a 401 challenge.
   *
   * <p>Static because this class injects the ElasticsearchClient, which must be built after this
   * customizer.
   */
  @Bean
  static Rest5ClientBuilderCustomizer preemptiveAuthCustomizer(
      ElasticsearchConnectionDetails connectionDetails) {
    // Only set the auth header if we have credentials to use
    if (StringUtils.hasText(connectionDetails.getUsername())) {
      var token =
          Base64.getEncoder()
              .encodeToString(
                  (connectionDetails.getUsername() + ":" + connectionDetails.getPassword())
                      .getBytes(StandardCharsets.UTF_8));
      var authHeader = new BasicHeader(HttpHeaders.AUTHORIZATION, "Basic " + token);
      return builder -> builder.setDefaultHeaders(new Header[] {authHeader});
    }
    return _ -> {};
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

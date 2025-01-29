package no.einnsyn.backend.configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import no.einnsyn.backend.utils.ElasticsearchIndexCreator;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@TestConfiguration
@AutoConfigureBefore(ElasticsearchConfiguration.class)
public class ElasticsearchTestConfiguration {

  private static ElasticsearchContainer container;

  @Value("${application.elasticsearch.index}")
  private String elasticsearchIndex;

  @Value("${application.elasticsearch.percolatorIndex}")
  private String percolatorIndex;

  @SuppressWarnings("resource")
  @Bean
  ElasticsearchContainer elasticsearchContainer() {
    if (container != null && container.isRunning()) {
      return container;
    }

    container =
        new ElasticsearchContainer(
                DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.16.0"))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node")
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("elasticsearch/elasticsearch-plugins.yml"),
                "/usr/share/elasticsearch/config/elasticsearch-plugins.yml")
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("elasticsearch/synonym.txt"),
                "/usr/share/elasticsearch/config/analysis/synonym.txt");

    container.start();
    container.waitingFor(
        Wait.forHttp("/_cluster/health")
            .forPort(container.getFirstMappedPort())
            .forStatusCode(200)
            .withReadTimeout(Duration.ofSeconds(30)));

    return container;
  }

  @Bean
  @Primary
  ElasticsearchClient client(ElasticsearchContainer container) {
    var restClient =
        RestClient.builder(new HttpHost(container.getHost(), container.getFirstMappedPort()))
            .build();
    var transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
    var client = new ElasticsearchClient(transport);

    // Initialize indices with mappings and settings
    ElasticsearchIndexCreator.maybeCreateIndex(client, elasticsearchIndex);
    ElasticsearchIndexCreator.maybeCreateIndex(client, percolatorIndex);

    return client;
  }

  @PreDestroy
  public void tearDown() {
    if (container != null && container.isRunning()) {
      container.stop();
    }
  }
}

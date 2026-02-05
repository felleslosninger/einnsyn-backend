package no.einnsyn.backend.utils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

@Slf4j
public class ElasticsearchIndexCreator {
  /**
   * If an index or alias with the given name doesn't exist, create a new index with a timestamped
   * name, and create a new alias with the given name that points to it.
   *
   * <p>Mappings and settings for the index are read from the classpath resources:
   * <li>elasticsearch/indexSettings.json
   * <li>elasticsearch/indexSettings.json
   *
   * @param esClient the Elasticsearch client
   * @param aliasName the name of the alias to create
   */
  public static void maybeCreateIndex(ElasticsearchClient esClient, String aliasName) {
    try {
      var settingsString =
          new String(
              new ClassPathResource("elasticsearch/indexSettings.json")
                  .getInputStream()
                  .readAllBytes(),
              StandardCharsets.UTF_8);
      var mapper = new JacksonJsonpMapper();
      var settingsParser = mapper.jsonProvider().createParser(new StringReader(settingsString));
      var settings = IndexSettings._DESERIALIZER.deserialize(settingsParser, mapper);

      var mappingsString =
          new String(
              new ClassPathResource("elasticsearch/indexMappings.json")
                  .getInputStream()
                  .readAllBytes(),
              StandardCharsets.UTF_8);
      var mappingsParser = mapper.jsonProvider().createParser(new StringReader(mappingsString));
      var mappings = TypeMapping._DESERIALIZER.deserialize(mappingsParser, mapper);

      // Check if index or alias exists
      if (esClient.indices().existsAlias(b -> b.name(aliasName)).value()) {
        log.info("Alias {} already exists.", aliasName);
      } else if (esClient.indices().exists(b -> b.index(aliasName)).value()) {
        log.info("Index {} already exists.", aliasName);
      } else {
        // Create new index
        var indexName =
            aliasName + LocalDateTime.now().format(DateTimeFormatter.ofPattern("_yyyyMMddHHmm"));
        log.info("Creating index: {}", indexName);
        esClient.indices().create(b -> b.index(indexName).settings(settings).mappings(mappings));

        // Create alias
        log.info("Creating alias: {}", aliasName);
        esClient
            .indices()
            .updateAliases(
                b -> b.actions(a -> a.add((add -> add.alias(aliasName).index(indexName)))));
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to create index: " + aliasName, e);
    }
  }
}

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
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

@Slf4j
public class ElasticsearchIndexCreator {

  private ElasticsearchIndexCreator() {}

  /**
   * If an index or alias with the given name doesn't exist, create a new index with a timestamped
   * name, and create a new alias with the given name that points to it.
   *
   * <p>Mappings and settings for the index are read from the classpath resources:
   * <li>elasticsearch/indexSettings.json
   * <li>elasticsearch/indexMappings.json
   *
   * @param esClient the Elasticsearch client
   * @param aliasName the name of the alias to create
   */
  public static void maybeCreateIndex(ElasticsearchClient esClient, String aliasName) {
    try {
      var settings = readSettings();
      var mappings = readMappings();

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

  /**
   * Apply the mappings from elasticsearch/indexMappings.json to an existing index or alias.
   *
   * <p>{@link #maybeCreateIndex} only reads the mappings when it creates a new index, and the
   * mappings are {@code dynamic: false}, so a field added to the mappings file would otherwise be
   * silently ignored by every existing index: the value ends up in {@code _source} but is neither
   * searchable nor aggregatable. Putting the mappings on startup closes that gap.
   *
   * <p>This is an additive-only update path, because that is all a mapping update can be:
   *
   * <ul>
   *   <li>New fields, new children of a join field, and unchanged existing fields are applied.
   *   <li>Changing a field's type or analyzer, or referencing an analyzer the index settings lack,
   *       is rejected by Elasticsearch and fails startup. That is deliberate: such a change needs a
   *       new index and a reindex (see scripts/elasticsearch/updateIndices.sh for settings
   *       changes), and code assuming a mapping the index does not have is exactly the silent
   *       failure this method exists to prevent.
   *   <li>Fields removed from or renamed in the mappings file are <em>not</em> removed from the
   *       index; a mapping update never drops fields. The index keeps the old field and startup
   *       succeeds, so such drift is logged as a warning instead.
   * </ul>
   *
   * @param esClient the Elasticsearch client
   * @param aliasName the index or alias to update
   */
  public static void updateMappings(ElasticsearchClient esClient, String aliasName) {
    try {
      var mappings = readMappings();
      log.info("Updating mappings for {}", aliasName);
      esClient
          .indices()
          .putMapping(
              b ->
                  b.index(aliasName).dynamic(mappings.dynamic()).properties(mappings.properties()));
      warnAboutUndeclaredFields(esClient, aliasName, mappings);
    } catch (IOException e) {
      throw new RuntimeException("Failed to update mappings for: " + aliasName, e);
    }
  }

  /**
   * Log the fields an index has that the mappings file no longer declares. A mapping update cannot
   * remove them, so this is the only visibility a removed or renamed field gets.
   */
  private static void warnAboutUndeclaredFields(
      ElasticsearchClient esClient, String aliasName, TypeMapping mappings) throws IOException {
    var response = esClient.indices().getMapping(g -> g.index(aliasName));
    for (var entry : response.mappings().entrySet()) {
      var undeclared = new TreeSet<>(entry.getValue().mappings().properties().keySet());
      undeclared.removeAll(mappings.properties().keySet());
      if (!undeclared.isEmpty()) {
        log.warn(
            "Index {} has fields not declared in indexMappings.json, which a mapping update cannot"
                + " remove: {}",
            entry.getKey(),
            undeclared);
      }
    }
  }

  /** The index settings from elasticsearch/indexSettings.json. */
  public static IndexSettings readSettings() throws IOException {
    var mapper = new JacksonJsonpMapper();
    var parser =
        mapper.jsonProvider().createParser(new StringReader(readResource("indexSettings.json")));
    return IndexSettings._DESERIALIZER.deserialize(parser, mapper);
  }

  /** The index mappings from elasticsearch/indexMappings.json. */
  public static TypeMapping readMappings() throws IOException {
    var mapper = new JacksonJsonpMapper();
    var parser =
        mapper.jsonProvider().createParser(new StringReader(readResource("indexMappings.json")));
    return TypeMapping._DESERIALIZER.deserialize(parser, mapper);
  }

  private static String readResource(String name) throws IOException {
    return new String(
        new ClassPathResource("elasticsearch/" + name).getInputStream().readAllBytes(),
        StandardCharsets.UTF_8);
  }
}

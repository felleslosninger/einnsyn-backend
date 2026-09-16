package no.einnsyn.backend.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import no.einnsyn.backend.EinnsynTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

/**
 * Upgrade path for the Elasticsearch mappings. The test containers always start from a fresh index,
 * so the situation these tests set up by hand is a production index created before a field was
 * added to indexMappings.json.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ElasticsearchIndexCreatorTest extends EinnsynTestBase {

  private String indexName;
  private String aliasName;

  @AfterEach
  void deleteIndex() throws Exception {
    if (indexName != null) {
      esClient.indices().delete(d -> d.index(indexName));
    }
  }

  /** Create an index behind an alias, using the real settings and the given mappings. */
  private void createIndex(TypeMapping mappings) throws Exception {
    var suffix = UUID.randomUUID().toString().substring(0, 8);
    indexName = "mapping-upgrade-test_" + suffix;
    aliasName = "mapping-upgrade-test-alias_" + suffix;
    var settings = ElasticsearchIndexCreator.readSettings();
    esClient
        .indices()
        .create(
            c ->
                c.index(indexName)
                    .settings(settings)
                    .mappings(mappings)
                    .aliases(aliasName, a -> a));
  }

  /** The current mappings, with the "count" field replaced by the given property or removed. */
  private static TypeMapping mappingsWithCount(Property count) throws Exception {
    var current = ElasticsearchIndexCreator.readMappings();
    var properties = new HashMap<>(current.properties());
    if (count == null) {
      properties.remove("count");
    } else {
      properties.put("count", count);
    }
    return TypeMapping.of(m -> m.dynamic(current.dynamic()).properties(properties));
  }

  private Map<String, Property> indexedProperties() throws Exception {
    return esClient
        .indices()
        .getMapping(g -> g.index(aliasName))
        .get(indexName)
        .mappings()
        .properties();
  }

  @Test
  void updateMappingsShouldAddFieldsMissingFromAnExistingIndex() throws Exception {
    createIndex(mappingsWithCount(null));
    assertFalse(indexedProperties().containsKey("count"), "precondition: count is unmapped");

    ElasticsearchIndexCreator.updateMappings(esClient, aliasName);

    // Without the mapping, dynamic: false would leave `count` unindexed and every sum aggregation
    // over it at zero, even though the documents carry the value.
    var count = indexedProperties().get("count");
    assertTrue(count != null && count.isInteger(), "count should be mapped as integer");
    assertEquals(
        ElasticsearchIndexCreator.readMappings().properties().keySet(),
        indexedProperties().keySet());
  }

  @Test
  void updateMappingsShouldBeIdempotent() throws Exception {
    createIndex(ElasticsearchIndexCreator.readMappings());

    // Every instance runs this on startup, against an index that is usually already up to date
    ElasticsearchIndexCreator.updateMappings(esClient, aliasName);
    ElasticsearchIndexCreator.updateMappings(esClient, aliasName);

    assertEquals(
        ElasticsearchIndexCreator.readMappings().properties().keySet(),
        indexedProperties().keySet());
  }

  @Test
  void updateMappingsShouldKeepFieldsTheFileNoLongerDeclares() throws Exception {
    var current = ElasticsearchIndexCreator.readMappings();
    var properties = new HashMap<>(current.properties());
    properties.put("legacyField", Property.of(p -> p.keyword(k -> k)));
    createIndex(TypeMapping.of(m -> m.dynamic(current.dynamic()).properties(properties)));

    // A mapping update is additive: it neither removes the field nor fails. This is why the update
    // path is documented as additive-only, and why such drift is only warned about.
    ElasticsearchIndexCreator.updateMappings(esClient, aliasName);

    assertTrue(indexedProperties().containsKey("legacyField"));
    assertTrue(indexedProperties().containsKey("count"));
  }

  @Test
  void updateMappingsShouldFailOnIncompatibleChanges() throws Exception {
    createIndex(mappingsWithCount(Property.of(p -> p.keyword(k -> k))));

    // A type change cannot be applied in place; failing loudly is the point, since the index would
    // otherwise silently disagree with what the code expects.
    assertThrows(
        RuntimeException.class,
        () -> ElasticsearchIndexCreator.updateMappings(esClient, aliasName));
  }
}

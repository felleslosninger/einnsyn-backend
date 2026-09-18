package no.einnsyn.backend.utils.id;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IdUtils {

  // entity name -> prefix map, spanning both API-spec entities and internal ones
  private static final Map<String, String> entityMap = mergePrefixMaps();

  // prefix -> entity name map
  private static final Map<String, String> prefixMap =
      entityMap.entrySet().stream()
          .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

  private IdUtils() {}

  /**
   * Merge the generated {@link IdPrefix} map with the hand-written {@link IdPrefixInternal} map.
   * Both an entity registered twice and a prefix shared by two entities would make {@link
   * #resolveEntity} ambiguous, so either is rejected at class-load time.
   */
  private static Map<String, String> mergePrefixMaps() {
    var merged = new HashMap<String, String>();
    Stream.concat(IdPrefix.map.entrySet().stream(), IdPrefixInternal.map.entrySet().stream())
        .forEach(
            entry -> {
              var previous = merged.putIfAbsent(entry.getKey(), entry.getValue());
              if (previous != null) {
                throw new IllegalStateException(
                    "Entity " + entry.getKey() + " is registered in both IdPrefix maps");
              }
            });

    // Make sure no entities share the same prefix.
    var prefixes = merged.values().stream().distinct().count();
    if (prefixes != merged.size()) {
      throw new IllegalStateException("Duplicate ID prefix across IdPrefix maps: " + merged);
    }
    return Map.copyOf(merged);
  }

  /**
   * Get the prefix for an entity, given an entity name.
   *
   * @param entity the entity name
   * @return the prefix for the entity
   */
  public static String getPrefix(String entity) {
    return entityMap.get(entity);
  }

  /**
   * Get the prefix for an entity, given an entity name, or a default prefix if the entity is not
   * found.
   *
   * @param entity the entity name
   * @param defaultPrefix the default prefix to return if entity not found
   * @return the prefix for the entity or the default prefix
   */
  public static String getPrefixOrDefault(String entity, String defaultPrefix) {
    return entityMap.getOrDefault(entity, defaultPrefix);
  }

  /**
   * Get the entity name for a prefix.
   *
   * @param prefix the prefix to look up
   * @return the entity name for the prefix
   */
  public static String getEntityFromPrefix(String prefix) {
    return prefixMap.get(prefix);
  }

  /**
   * Resolve an entity name from an id.
   *
   * @param id the ID to resolve
   * @return the entity name
   */
  public static String resolveEntity(String id) {
    var index = id.indexOf("_");
    if (index == -1) {
      return null;
    }
    var prefix = id.substring(0, index);
    return getEntityFromPrefix(prefix);
  }
}

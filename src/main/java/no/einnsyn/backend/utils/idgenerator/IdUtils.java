package no.einnsyn.backend.utils.idgenerator;

import java.util.Map;
import java.util.stream.Collectors;

public class IdUtils {

  // prefix -> entity name map
  private static final Map<String, String> prefixMap =
      IdPrefix.map.entrySet().stream()
          .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

  private IdUtils() {}

  /**
   * Get the prefix for an entity, given an entity name.
   *
   * @param entity
   * @return
   */
  public static String getPrefix(String entity) {
    return IdPrefix.map.get(entity);
  }

  /**
   * Get the prefix for an entity, given an entity name, or a default prefix if the entity is not
   *
   * @param entity
   * @param defaultPrefix
   * @return
   */
  public static String getPrefixOrDefault(String entity, String defaultPrefix) {
    return IdPrefix.map.getOrDefault(entity, defaultPrefix);
  }

  /**
   * Get the entity name for a prefix.
   *
   * @param prefix
   * @return
   */
  public static String getEntityFromPrefix(String prefix) {
    return prefixMap.get(prefix);
  }

  /**
   * Resolve an entity name from an id.
   *
   * @param id
   * @return
   */
  public static String resolveEntity(String id) {
    var index = id.indexOf("_");
    if (index == -1) {
      return null;
    }
    var prefix = id.substring(0, id.indexOf("_"));
    return getEntityFromPrefix(prefix);
  }
}

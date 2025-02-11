package no.einnsyn.backend.utils.idgenerator;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

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
    return IdPrefix.map.get(entity.toLowerCase());
  }

  /**
   * Get the prefix for an entity, given an entity name, or a default prefix if the entity is not
   *
   * @param entity
   * @param defaultPrefix
   * @return
   */
  public static String getPrefixOrDefault(String entity, String defaultPrefix) {
    return IdPrefix.map.getOrDefault(entity.toLowerCase(), defaultPrefix);
  }

  /**
   * Get the entity name for a prefix.
   *
   * @param prefix
   * @return
   */
  public static String getEntity(String prefix) {
    var entity = prefixMap.get(prefix);
    if (entity != null) {
      return StringUtils.capitalize(entity);
    }
    return null;
  }

  /**
   * Resolve an entity name from an id.
   *
   * @param id
   * @return
   */
  public static String resolveEntity(String id) {
    var prefix = id.substring(0, id.indexOf("_"));
    return getEntity(prefix);
  }
}

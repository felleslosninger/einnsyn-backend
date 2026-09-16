package no.einnsyn.backend.utils.id;

import java.util.Map;

/**
 * ID prefixes for entities that are internal to the backend and therefore not part of the API
 * specification.
 *
 * <p>{@link IdPrefix} is generated from the API spec and must not be edited by hand. Entities that
 * never cross the API boundary but still need a stable prefix, e.g. because they are routed through
 * the Elasticsearch indexing pipeline by {@link IdUtils#resolveEntity}, are registered here
 * instead. {@link IdUtils} merges both maps and fails fast on collisions.
 */
public class IdPrefixInternal {
  public static final Map<String, String> map = Map.ofEntries(Map.entry("DownloadCount", "dc"));

  private IdPrefixInternal() {}
}

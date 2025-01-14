package no.einnsyn.backend.common.search;

import java.util.Map;

public class SortByMapper {
  public static final Map<String, String> map =
      Map.ofEntries(
          Map.entry("score", "_score"),
          Map.entry("publisertDato", "publisertDato"),
          Map.entry("oppdatertDato", "oppdatertDato"),
          Map.entry("moetedato", "moetedato"),
          Map.entry("fulltekst", "fulltekst"),
          Map.entry("type", "sorteringstype"));

  public static String resolve(String sortBy) {
    return map.get(sortBy);
  }
}

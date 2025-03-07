package no.einnsyn.backend.common.search;

import co.elastic.clients.elasticsearch._types.FieldValue;

public class SortByMapper {

  public static String resolve(String sortBy) {
    return switch (sortBy) {
      case "score" -> "_score";
      case "id" -> "id";
      case "entity" -> "type";
      case "publisertDato" -> "publisertDato";
      case "oppdatertDato" -> "oppdatertDato";
      case "moetedato" -> "moetedato";
      case "fulltekst" -> "fulltekst";
      case "type" -> "sorteringstype";
      default -> null;
    };
  }

  public static FieldValue toFieldValue(String sortBy, String value) {
    return switch (sortBy) {
      case "score" -> FieldValue.of(Double.parseDouble(value));
      case "id" -> FieldValue.of(value);
      case "entity" -> FieldValue.of(value);
      case "publisertDato" -> FieldValue.of(Long.parseLong(value));
      case "oppdatertDato" -> FieldValue.of(Long.parseLong(value));
      case "moetedato" -> FieldValue.of(Long.parseLong(value));
      case "fulltekst" -> FieldValue.of(Boolean.parseBoolean(value));
      case "type" -> FieldValue.of(value);
      default -> null;
    };
  }
}

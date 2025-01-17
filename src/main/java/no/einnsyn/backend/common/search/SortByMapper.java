package no.einnsyn.backend.common.search;

import co.elastic.clients.elasticsearch._types.FieldValue;
import java.time.Instant;

public class SortByMapper {

  public static String resolve(String sortBy) {
    return switch (sortBy) {
      case "id" -> "id";
      case "score" -> "_score";
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
      case "id" -> FieldValue.of(value);
      case "score" -> FieldValue.of(Double.parseDouble(value));
      case "publisertDato" -> FieldValue.of(Instant.parse(value));
      case "oppdatertDato" -> FieldValue.of(Instant.parse(value));
      case "moetedato" -> FieldValue.of(Instant.parse(value));
      case "fulltekst" -> FieldValue.of(Boolean.parseBoolean(value));
      case "type" -> FieldValue.of(value);
      default -> null;
    };
  }
}

package no.einnsyn.apiv3.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;

public class TimeConverter {

  private TimeConverter() {}

  public static Instant timestampToInstant(String timestamp) {
    var parsed = DateTimeFormatter.ISO_DATE_TIME.parse(timestamp);
    if (parsed.isSupported(ChronoField.OFFSET_SECONDS)) {
      return ZonedDateTime.parse(timestamp).toInstant();
    } else {
      var localDateTime = LocalDateTime.from(parsed);
      return localDateTime.atZone(ZoneId.of("Europe/Oslo")).toInstant();
    }
  }

  /**
   * Generates a value for the field "standardDato" in the legacy Elasticsearch document models.
   *
   * @param candidates
   * @return
   */
  public static String generateStandardDato(Object... candidates) {
    for (var untypedCandidate : candidates) {
      if (untypedCandidate == null) {
        continue;
      }
      LocalDate candidate = null;
      if (untypedCandidate instanceof LocalDate localdateCandidate) {
        candidate = localdateCandidate;
      }
      if (untypedCandidate instanceof Instant instantCandidate) {
        candidate = instantCandidate.atZone(ZoneId.systemDefault()).toLocalDate();
      }
      if (candidate != null) {
        return candidate.toString() + "T00:00:00";
      }
    }
    return null;
  }
}

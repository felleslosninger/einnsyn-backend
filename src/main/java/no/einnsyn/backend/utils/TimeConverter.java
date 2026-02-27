package no.einnsyn.backend.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;

/**
 * Utility class for handling conversions between various date and time formats. Provides methods to
 * convert timestamp strings to {@link Instant}, convert {@link Instant} to {@link ZonedDateTime},
 * and generate a standard date format.
 */
public class TimeConverter {

  private static final ZoneId NORWEGIAN_ZONE = ZoneId.of("Europe/Oslo");

  private TimeConverter() {}

  /**
   * Converts a timestamp string to an {@link Instant}.
   *
   * @param timestamp the timestamp in ISO-8601 format. If the timestamp contains an offset, it will
   *     be parsed directly; otherwise, it is treated as local time in the "Europe/Oslo" time zone.
   * @return the corresponding {@link Instant} based on the provided timestamp.
   */
  public static Instant timestampToInstant(String timestamp) {
    var parsed = DateTimeFormatter.ISO_DATE_TIME.parse(timestamp);
    if (parsed.isSupported(ChronoField.OFFSET_SECONDS)) {
      return ZonedDateTime.parse(timestamp).toInstant();
    } else {
      var localDateTime = LocalDateTime.from(parsed);
      return localDateTime.atZone(NORWEGIAN_ZONE).toInstant();
    }
  }

  /**
   * Converts an {@link Instant} to a {@link ZonedDateTime} in the "Europe/Oslo" time zone.
   *
   * @param instant the {@link Instant} to convert.
   * @return a {@link ZonedDateTime} representing the given {@link Instant} in the "Europe/Oslo"
   *     zone.
   */
  public static ZonedDateTime instantToZonedDateTime(Instant instant) {
    return instant.atZone(NORWEGIAN_ZONE);
  }

  /**
   * Converts an {@link Instant} to a timestamp string in ISO-8601 format, with the time zone
   * offset.
   *
   * @param instant the Instant to convert
   * @return the formatted timestamp string
   */
  public static String instantToTimestamp(Instant instant) {
    return instantToZonedDateTime(instant).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
  }

  /**
   * Generates a standard date format string, used for the "standardDato" field in legacy
   * Elasticsearch document models. Attempts to format the first valid date from the provided
   * candidates.
   *
   * @param candidates a vararg of possible date/time candidates of types {@link LocalDate} or
   *     {@link Instant}. If a {@link LocalDate} is provided, it will be directly formatted. If an
   *     {@link Instant} is provided, it will be converted to local date in the "Europe/Oslo" time
   *     zone. The method returns the date in the "yyyy-MM-ddT00:00:00" format.
   * @return a formatted date string for the first valid candidate, or {@code null} if no valid date
   *     is found.
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
        candidate = instantCandidate.atZone(NORWEGIAN_ZONE).toLocalDate();
      }
      if (candidate != null) {
        return candidate.toString() + "T00:00:00";
      }
    }
    return null;
  }
}

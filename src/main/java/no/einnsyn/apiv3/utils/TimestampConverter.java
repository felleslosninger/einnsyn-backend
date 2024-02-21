package no.einnsyn.apiv3.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;

public class TimestampConverter {

  private TimestampConverter() {}

  public static Instant timestampToInstant(String timestamp) {
    var parsed = DateTimeFormatter.ISO_DATE_TIME.parse(timestamp);
    if (parsed.isSupported(ChronoField.OFFSET_SECONDS)) {
      return ZonedDateTime.parse(timestamp).toInstant();
    } else {
      var localDateTime = LocalDateTime.from(parsed);
      return localDateTime.atZone(ZoneId.of("Europe/Oslo")).toInstant();
    }
  }
}

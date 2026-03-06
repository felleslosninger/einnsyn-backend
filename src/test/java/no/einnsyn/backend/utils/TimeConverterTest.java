package no.einnsyn.backend.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;

class TimeConverterTest {

  @Test
  void timestampToInstantShouldRespectExplicitOffset() {
    assertEquals(
        Instant.parse("2026-03-15T10:34:56Z"),
        TimeConverter.timestampToInstant("2026-03-15T12:34:56+02:00"));
  }

  @Test
  void timestampToInstantShouldAssumeEuropeOsloWhenOffsetIsMissing() {
    assertEquals(
        Instant.parse("2026-01-01T11:00:00Z"),
        TimeConverter.timestampToInstant("2026-01-01T12:00:00"));
  }

  @Test
  void instantToZonedDateTimeShouldReturnEuropeOsloZone() {
    var zoned = TimeConverter.instantToZonedDateTime(Instant.parse("2026-01-01T11:00:00Z"));
    assertEquals(ZoneId.of("Europe/Oslo"), zoned.getZone());
    assertEquals(12, zoned.getHour());
  }

  @Test
  void instantToTimestampShouldUseEuropeOsloOffset() {
    assertEquals(
        "2026-01-01T12:00:00+01:00",
        TimeConverter.instantToTimestamp(Instant.parse("2026-01-01T11:00:00Z")));
  }

  @Test
  void generateStandardDatoShouldUseEuropeOsloForInstants() {
    var original = TimeZone.getDefault();
    try {
      // Use a non-Oslo JVM default timezone to prove output is deterministic.
      TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
      var instant = Instant.parse("2026-01-01T23:30:00Z");
      assertEquals("2026-01-02T00:00:00", TimeConverter.generateStandardDato(instant));
    } finally {
      TimeZone.setDefault(original);
    }
  }

  @Test
  void generateStandardDatoShouldKeepLocalDateAsIs() {
    assertEquals(
        "2026-02-25T00:00:00", TimeConverter.generateStandardDato(LocalDate.of(2026, 2, 25)));
  }

  @Test
  void generateStandardDatoShouldUseFirstValidCandidate() {
    assertEquals(
        "2026-01-02T00:00:00",
        TimeConverter.generateStandardDato(
            null, "not-a-date", Instant.parse("2026-01-01T23:30:00Z"), LocalDate.of(2026, 1, 3)));
  }

  @Test
  void generateStandardDatoShouldReturnNullWhenNoValidCandidates() {
    assertNull(TimeConverter.generateStandardDato(null, "not-a-date", 123));
  }
}

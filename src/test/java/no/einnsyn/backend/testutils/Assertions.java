package no.einnsyn.backend.testutils;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;

public class Assertions {
  /**
   * Asserts that two instants are equal, with a margin of 1 millisecond.
   *
   * @param expected The expected instant
   * @param actual The actual instant
   * @throws AssertionError If the instants are not equal
   */
  public static void assertEqualInstants(String expected, String actual) {
    if (expected == null || actual == null) {
      assertNull(actual);
      assertNull(expected);
      return;
    }

    var expectedInstant = ZonedDateTime.parse(expected).toInstant().toEpochMilli();
    var actualInstant = ZonedDateTime.parse(actual).toInstant().toEpochMilli();

    // Account for rounding errors
    var diff = Math.abs(expectedInstant - actualInstant);
    assertTrue(diff <= 1, "Expected: " + expected + " but was: " + actual);
  }
}

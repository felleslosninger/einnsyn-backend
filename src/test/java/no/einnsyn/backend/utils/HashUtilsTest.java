package no.einnsyn.backend.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HashUtilsTest {

  @Test
  void secretEqualsMatchesIdenticalSecrets() {
    assertTrue(HashUtils.secretEquals("issec_abc123", "issec_abc123"));
    assertTrue(HashUtils.secretEquals("", ""));
    assertTrue(HashUtils.secretEquals("æøå", "æøå"));
  }

  @Test
  void secretEqualsRejectsDifferentSecrets() {
    assertFalse(HashUtils.secretEquals("issec_abc123", "issec_abc124"));
    assertFalse(HashUtils.secretEquals("issec_abc123", "issec_abc123 "));
    assertFalse(HashUtils.secretEquals("issec_abc123", ""));
  }

  /** A cleared (null) secret must never match, instead of throwing a NullPointerException. */
  @Test
  void secretEqualsRejectsNull() {
    assertFalse(HashUtils.secretEquals(null, "issec_abc123"));
    assertFalse(HashUtils.secretEquals("issec_abc123", null));
    assertFalse(HashUtils.secretEquals(null, null));
  }
}

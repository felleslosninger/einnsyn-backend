package no.einnsyn.backend.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SecretUtilsTest {

  @Test
  void secretEqualsMatchesIdenticalSecrets() {
    assertTrue(SecretUtils.secretEquals("issec_abc123", "issec_abc123"));
    assertTrue(SecretUtils.secretEquals("æøå", "æøå"));
  }

  @Test
  void secretEqualsRejectsDifferentSecrets() {
    assertFalse(SecretUtils.secretEquals("issec_abc123", "issec_abc124"));
    assertFalse(SecretUtils.secretEquals("issec_abc123", "issec_abc123 "));
    assertFalse(SecretUtils.secretEquals("issec_abc123", ""));
  }

  /** An object with no secret must never match, and must not throw a NullPointerException. */
  @Test
  void secretEqualsRejectsMissingStoredSecret() {
    assertFalse(SecretUtils.secretEquals(null, "issec_abc123"));
    assertFalse(SecretUtils.secretEquals("issec_abc123", null));
    assertFalse(SecretUtils.secretEquals(null, null));
  }

  /** An empty stored secret must not be matchable by an empty input. */
  @Test
  void secretEqualsRejectsEmptyStoredSecret() {
    assertFalse(SecretUtils.secretEquals("", ""));
    assertFalse(SecretUtils.secretEquals("", "issec_abc123"));
  }
}

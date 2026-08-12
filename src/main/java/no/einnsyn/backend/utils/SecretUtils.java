package no.einnsyn.backend.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class SecretUtils {

  private SecretUtils() {}

  /**
   * Compare a stored secret with a user supplied one, without leaking the position of the first
   * differing byte through timing. Only a non-empty stored secret can match, so an object with no
   * secret set rejects every input.
   *
   * @param expected the stored secret, or null if the object has no secret
   * @param provided the secret supplied by the caller
   * @return true if the stored secret is non-empty and the two are equal
   */
  public static boolean secretEquals(String expected, String provided) {
    if (expected == null || expected.isEmpty() || provided == null) {
      return false;
    }
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8));
  }
}

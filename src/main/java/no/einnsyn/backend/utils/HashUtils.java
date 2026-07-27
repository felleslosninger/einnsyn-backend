package no.einnsyn.backend.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class HashUtils {

  private static final String SHA_256 = "SHA-256";
  private static final HexFormat HEX_FORMAT = HexFormat.of();

  private HashUtils() {}

  public static String sha256Hex(String value) {
    try {
      var digest = MessageDigest.getInstance(SHA_256);
      return HEX_FORMAT.formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }

  /**
   * Compare a stored secret with a user supplied one, without leaking the position of the first
   * differing byte through timing. A null stored or supplied secret never matches.
   *
   * @param expected the stored secret, or null if there is no secret to match
   * @param provided the secret supplied by the caller
   * @return true if both are non-null and equal
   */
  public static boolean secretEquals(String expected, String provided) {
    if (expected == null || provided == null) {
      return false;
    }
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8));
  }
}

package no.einnsyn.backend.utils;

import java.text.Normalizer;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * Utility class for generating URL-friendly slugs from text strings. Converts titles and other text
 * into clean, lowercase, hyphenated slugs suitable for use in URLs.
 */
public class SlugGenerator {

  private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}+");
  private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^a-z0-9]+");
  private static final Pattern LEADING_TRAILING_HYPHENS_PATTERN = Pattern.compile("^-+|-+$");
  private static final int MAX_SLUG_LENGTH = 75;

  private SlugGenerator() {}

  /**
   * Generates a URL-friendly slug from the given text.
   *
   * <p>This method converts input text into a clean, lowercase, hyphenated slug by:
   *
   * <ul>
   *   <li>Converting to lowercase
   *   <li>Replacing Scandinavian characters (æ, ø, å) with ASCII equivalents
   *   <li>Normalizing Unicode characters and removing diacritics (é→e, è→e, ä→a, ö→o, etc.)
   *   <li>Replacing non-alphanumeric characters with hyphens
   *   <li>Removing leading and trailing hyphens
   * </ul>
   *
   * @param text the input text (e.g., a web page title) to convert to a slug
   * @return a lowercase, hyphenated slug suitable for URLs, or null if input is null or blank
   */
  public static String generate(String text) {
    return generate(text, false);
  }

  /**
   * Generates a URL-friendly slug from the given text with an optional random suffix.
   *
   * @param text the input text (e.g., a web page title) to convert to a slug
   * @param randomSuffix whether to append a random alphanumeric suffix (e.g., "-a1b2c3")
   * @return a lowercase, hyphenated slug suitable for URLs, or null if input is null or blank
   */
  public static String generate(String text, boolean randomSuffix) {
    if (text == null || text.isBlank()) {
      return null;
    }

    var slug = text.toLowerCase();

    // Replace Scandinavian characters not handled by Normalizer
    slug = transliterate(slug);

    // Normalize and remove diacritics
    slug = Normalizer.normalize(slug, Normalizer.Form.NFD);
    slug = DIACRITICS_PATTERN.matcher(slug).replaceAll("");

    // Remove non-alphanumeric characters and replace with hyphens
    slug = NON_ALPHANUMERIC_PATTERN.matcher(slug).replaceAll("-");
    slug = LEADING_TRAILING_HYPHENS_PATTERN.matcher(slug).replaceAll("");

    // Clean up multiple consecutive hyphens and leading/trailing hyphens
    slug = slug.replaceAll("-+", "-");
    slug = LEADING_TRAILING_HYPHENS_PATTERN.matcher(slug).replaceAll("");

    if (slug.isEmpty()) {
      return null;
    }

    // Truncate if exceeds max length
    slug = truncateToMaxLength(slug);

    // Add random suffix if applicable
    if (randomSuffix) {
      slug = slug + "-" + generateRandomSuffix();
    }

    return slug;
  }

  /**
   * Truncates the slug to the maximum allowed length, ensuring truncation happens at word
   * boundaries (hyphens) to avoid cutting words in the middle.
   *
   * @param slug the slug to truncate
   * @return the truncated slug, or the original if it's within the limit
   */
  private static String truncateToMaxLength(String slug) {
    if (slug.length() <= MAX_SLUG_LENGTH) {
      return slug;
    }

    // Truncate at the last hyphen before the max length
    var truncated = slug.substring(0, MAX_SLUG_LENGTH);
    var lastHyphen = truncated.lastIndexOf('-');

    if (lastHyphen > 0) {
      return truncated.substring(0, lastHyphen);
    }

    // If no hyphen found, just truncate at max length
    return truncated;
  }

  /**
   * Generates a random alphanumeric suffix for slug uniqueness.
   *
   * @return a random 6-character lowercase alphanumeric string
   */
  private static String generateRandomSuffix() {
    var chars = "abcdefghijklmnopqrstuvwxyz0123456789";
    var random = new Random();
    var suffix = new StringBuilder(6);

    for (int i = 0; i < 6; i++) {
      suffix.append(chars.charAt(random.nextInt(chars.length())));
    }

    return suffix.toString();
  }

  /**
   * Transliterates Scandinavian and Germanic characters to their ASCII equivalents. Handles
   * characters that cannot be decomposed by Unicode normalization.
   *
   * @param text the input text
   * @return text with Scandinavian/Germanic characters replaced (æ→ae, ø→o, å→aa, ä→ae, ö→o)
   */
  private static String transliterate(String text) {
    return text.replace("æ", "ae")
        .replace("ø", "o")
        .replace("å", "aa")
        .replace("ä", "a")
        .replace("ö", "o");
  }
}

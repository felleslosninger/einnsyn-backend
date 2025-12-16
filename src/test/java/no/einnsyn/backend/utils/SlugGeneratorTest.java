package no.einnsyn.backend.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SlugGeneratorTest {

  @Test
  void testBasicSlugGeneration() {
    assertEquals("hello-world", SlugGenerator.generate("Hello World"));
    assertEquals("my-page-title", SlugGenerator.generate("My Page Title"));
    assertEquals("simple", SlugGenerator.generate("simple"));
  }

  @Test
  void testScandinavianCharacters() {
    assertEquals("soknad-om-innsyn", SlugGenerator.generate("Søknad om innsyn"));
    assertEquals("aerlig-talt", SlugGenerator.generate("Ærlig talt"));
    assertEquals("baat-og-hav", SlugGenerator.generate("Båt og hav"));
    assertEquals("norsk-aeaao", SlugGenerator.generate("Norsk ÆÅØ"));
    assertEquals("svensk-aaao", SlugGenerator.generate("Svensk ÄÅÖ"));
  }

  @Test
  void testDiacriticalMarks() {
    assertEquals("cafe-resume", SlugGenerator.generate("Café Résumé"));
    assertEquals("nino-espanol", SlugGenerator.generate("Niño Español"));
  }

  @Test
  void testSpecialCharacters() {
    assertEquals("hello-world", SlugGenerator.generate("Hello, World!"));
    assertEquals("test-123", SlugGenerator.generate("Test #123"));
    assertEquals("price-50", SlugGenerator.generate("Price: $50"));
    assertEquals("q-a", SlugGenerator.generate("Q & A"));
  }

  @Test
  void testConsecutiveSpecialCharacters() {
    assertEquals("hello-world", SlugGenerator.generate("Hello---World"));
    assertEquals("spaced-out", SlugGenerator.generate("  Spaced   Out  "));
    assertEquals("mixed-chars", SlugGenerator.generate("Mixed!@#$%Chars"));
  }

  @Test
  void testLeadingAndTrailingHyphens() {
    assertEquals("clean", SlugGenerator.generate("---clean---"));
    assertEquals("no-hyphens", SlugGenerator.generate("!!!no-hyphens!!!"));
  }

  @Test
  void testWithRandomSuffix() {
    var slug1 = SlugGenerator.generate("My Title", true);
    var slug2 = SlugGenerator.generate("My Title", true);

    // Both should start with "my-title-"
    assertTrue(slug1.startsWith("my-title-"));
    assertTrue(slug2.startsWith("my-title-"));

    // Should have 6 character suffix
    assertEquals("my-title-".length() + 6, slug1.length());
    assertEquals("my-title-".length() + 6, slug2.length());

    // Suffixes should be different (with very high probability)
    assertNotEquals(slug1, slug2);
  }

  @Test
  void testRandomSuffixFalse() {
    assertEquals("my-title", SlugGenerator.generate("My Title", false));
    assertEquals("my-title", SlugGenerator.generate("My Title"));
  }

  @Test
  void testNullAndEmptyInput() {
    assertNull(SlugGenerator.generate(null));
    assertNull(SlugGenerator.generate(""));
    assertNull(SlugGenerator.generate("   "));
    assertNull(SlugGenerator.generate("-"));
  }

  @Test
  void testNumbersPreserved() {
    assertEquals("version-2-0-1", SlugGenerator.generate("Version 2.0.1"));
    assertEquals("2024-annual-report", SlugGenerator.generate("2024 Annual Report"));
  }
}

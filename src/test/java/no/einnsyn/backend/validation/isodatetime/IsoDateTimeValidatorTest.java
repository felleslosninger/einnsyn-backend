package no.einnsyn.backend.validation.isodatetime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IsoDateTimeValidatorTest {

  private IsoDateTimeValidator validator;
  private ConstraintValidatorContext context;

  @BeforeEach
  void setUp() {
    validator = new IsoDateTimeValidator();
    context = mock(ConstraintValidatorContext.class);
  }

  @Test
  void testEmptyValueIsValid() {
    var annotation = createAnnotation(IsoDateTime.Format.ISO_DATE, false);
    validator.initialize(annotation);

    assertTrue(validator.isValid("", context));
    assertTrue(validator.isValid(null, context));
  }

  @Test
  void testValidIsoDate() {
    var annotation = createAnnotation(IsoDateTime.Format.ISO_DATE, false);
    validator.initialize(annotation);

    assertTrue(validator.isValid("2024-01-15", context));
    assertTrue(validator.isValid("2024-12-31", context));
  }

  @Test
  void testInvalidIsoDate() {
    var annotation = createAnnotation(IsoDateTime.Format.ISO_DATE, false);
    validator.initialize(annotation);

    assertFalse(validator.isValid("2024-13-01", context));
    assertFalse(validator.isValid("2024-02-30", context));
    assertFalse(validator.isValid("not-a-date", context));
  }

  @Test
  void testValidIsoDateTime() {
    var annotation = createAnnotation(IsoDateTime.Format.ISO_DATE_TIME, false);
    validator.initialize(annotation);

    assertTrue(validator.isValid("2024-01-15T10:30:00Z", context));
    assertTrue(validator.isValid("2024-12-31T23:59:59+01:00", context));
  }

  @Test
  void testInvalidIsoDateTime() {
    var annotation = createAnnotation(IsoDateTime.Format.ISO_DATE_TIME, false);
    validator.initialize(annotation);

    assertFalse(validator.isValid("2024-01-15", context)); // Date only
    assertFalse(validator.isValid("2024-01-15T25:00:00Z", context)); // Invalid hour
    assertFalse(validator.isValid("not-a-datetime", context));
  }

  @Test
  void testValidIsoDateOrDateTime() {
    var annotation = createAnnotation(IsoDateTime.Format.ISO_DATE_OR_DATE_TIME, false);
    validator.initialize(annotation);

    assertTrue(validator.isValid("2024-01-15", context));
    assertTrue(validator.isValid("2024-01-15T10:30:00Z", context));
    assertTrue(validator.isValid("2024-12-31T23:59:59+01:00", context));
  }

  @Test
  void testRelativeDateNotAllowedByDefault() {
    var annotation = createAnnotation(IsoDateTime.Format.ISO_DATE, false);
    validator.initialize(annotation);

    assertFalse(validator.isValid("now", context));
    assertFalse(validator.isValid("now-1d", context));
  }

  @Test
  void testRelativeDateNow() {
    var annotation = createAnnotation(IsoDateTime.Format.ISO_DATE, true);
    validator.initialize(annotation);

    assertTrue(validator.isValid("now", context));
  }

  @Test
  void testRelativeDateWithOffset() {
    var annotation = createAnnotation(IsoDateTime.Format.ISO_DATE, true);
    validator.initialize(annotation);

    // Single offsets
    assertTrue(validator.isValid("now-1d", context));
    assertTrue(validator.isValid("now+5h", context));
    assertTrue(validator.isValid("now-30m", context));
    assertTrue(validator.isValid("now+10s", context));
    assertTrue(validator.isValid("now-1y", context));
    assertTrue(validator.isValid("now+2M", context));
    assertTrue(validator.isValid("now-3w", context));

    // Multiple offsets
    assertTrue(validator.isValid("now-1d-5h", context));
    assertTrue(validator.isValid("now+1M-15d", context));
    assertTrue(validator.isValid("now-1y+6M", context));
  }

  @Test
  void testRelativeDateWithRounding() {
    var annotation = createAnnotation(IsoDateTime.Format.ISO_DATE, true);
    validator.initialize(annotation);

    assertTrue(validator.isValid("now/d", context));
    assertTrue(validator.isValid("now/M", context));
    assertTrue(validator.isValid("now/y", context));
    assertTrue(validator.isValid("now/h", context));
    assertTrue(validator.isValid("now/m", context));
    assertTrue(validator.isValid("now/s", context));
    assertTrue(validator.isValid("now/w", context));
  }

  @Test
  void testRelativeDateWithOffsetAndRounding() {
    var annotation = createAnnotation(IsoDateTime.Format.ISO_DATE, true);
    validator.initialize(annotation);

    assertTrue(validator.isValid("now-1d/d", context));
    assertTrue(validator.isValid("now-1M/M", context));
    assertTrue(validator.isValid("now+5h/h", context));
    assertTrue(validator.isValid("now-1y+6M/M", context));
  }

  @Test
  void testInvalidRelativeDateFormats() {
    var annotation = createAnnotation(IsoDateTime.Format.ISO_DATE, true);
    validator.initialize(annotation);

    // Invalid: doesn't start with "now"
    assertFalse(validator.isValid("today", context));
    assertFalse(validator.isValid("yesterday", context));

    // Invalid: missing number
    assertFalse(validator.isValid("now-d", context));
    assertFalse(validator.isValid("now+h", context));

    // Invalid: invalid unit
    assertFalse(validator.isValid("now-1x", context));
    assertFalse(validator.isValid("now+5z", context));

    // Invalid: multiple roundings
    assertFalse(validator.isValid("now/d/M", context));

    // Invalid: spaces
    assertFalse(validator.isValid("now - 1d", context));
    assertFalse(validator.isValid("now -1d", context));
  }

  @Test
  void testRelativeDateWithIsoDateTimeFormat() {
    var annotation = createAnnotation(IsoDateTime.Format.ISO_DATE_TIME, true);
    validator.initialize(annotation);

    // Relative dates should work with any format when allowRelative is true
    assertTrue(validator.isValid("now", context));
    assertTrue(validator.isValid("now-1h", context));
    assertTrue(validator.isValid("now/h", context));

    // ISO date times should still work
    assertTrue(validator.isValid("2024-01-15T10:30:00Z", context));
  }

  @Test
  void testRelativeDateWithIsoDateOrDateTimeFormat() {
    var annotation = createAnnotation(IsoDateTime.Format.ISO_DATE_OR_DATE_TIME, true);
    validator.initialize(annotation);

    // All three formats should work
    assertTrue(validator.isValid("now-1d", context));
    assertTrue(validator.isValid("2024-01-15", context));
    assertTrue(validator.isValid("2024-01-15T10:30:00Z", context));
  }

  @Test
  void testComplexRelativeDateExpressions() {
    var annotation = createAnnotation(IsoDateTime.Format.ISO_DATE, true);
    validator.initialize(annotation);

    // Complex Elasticsearch-style expressions
    assertTrue(validator.isValid("now-15m", context));
    assertTrue(validator.isValid("now-1h", context));
    assertTrue(validator.isValid("now-1h-15m", context));
    assertTrue(validator.isValid("now/d", context));
    assertTrue(validator.isValid("now-1d/d", context));
    assertTrue(validator.isValid("now-7d/d", context));
    assertTrue(validator.isValid("now-1M/M", context));
    assertTrue(validator.isValid("now-1y/y", context));
  }

  @Test
  void testRelativeDateWithRoundingBeforeOffset() {
    var annotation = createAnnotation(IsoDateTime.Format.ISO_DATE, true);
    validator.initialize(annotation);

    // Rounding before offset (operations evaluated left to right)
    assertTrue(validator.isValid("now/d-1y", context));
    assertTrue(validator.isValid("now/d-1h", context));
    assertTrue(validator.isValid("now/M-15d", context));
    assertTrue(validator.isValid("now/y+6M", context));
  }

  @Test
  void testRelativeDateWithMixedOperations() {
    var annotation = createAnnotation(IsoDateTime.Format.ISO_DATE, true);
    validator.initialize(annotation);

    // Mixed offsets and roundings (left to right evaluation)
    assertTrue(validator.isValid("now-1y/d+5h", context));
    assertTrue(validator.isValid("now/d-1M/M", context));
    assertTrue(validator.isValid("now-5d/d+12h/h", context));
    assertTrue(validator.isValid("now/M-1w+3d", context));
  }

  @Test
  void testRelativeDateWithMilliseconds() {
    var annotation = createAnnotation(IsoDateTime.Format.ISO_DATE, true);
    validator.initialize(annotation);

    // Milliseconds support
    assertTrue(validator.isValid("now-500ms", context));
    assertTrue(validator.isValid("now+1000ms", context));
    assertTrue(validator.isValid("now/ms", context));
    assertTrue(validator.isValid("now-1s+500ms", context));
  }

  @Test
  void testRelativeDateWithCapitalH() {
    var annotation = createAnnotation(IsoDateTime.Format.ISO_DATE, true);
    validator.initialize(annotation);

    // Capital H for hours (same as lowercase h)
    assertTrue(validator.isValid("now-5H", context));
    assertTrue(validator.isValid("now+12H", context));
    assertTrue(validator.isValid("now/H", context));
    assertTrue(validator.isValid("now-1d+12H/H", context));
  }

  private IsoDateTime createAnnotation(IsoDateTime.Format format, boolean allowRelative) {
    var annotation = mock(IsoDateTime.class);
    when(annotation.format()).thenReturn(format);
    when(annotation.allowRelative()).thenReturn(allowRelative);
    return annotation;
  }
}

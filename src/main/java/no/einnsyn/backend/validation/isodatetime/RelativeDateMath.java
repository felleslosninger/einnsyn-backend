package no.einnsyn.backend.validation.isodatetime;

import java.util.regex.Pattern;

public final class RelativeDateMath {

  private static final int MAX_EXPRESSION_LENGTH = 100;
  private static final String TIME_UNITS = "(?:y|M|w|d|h|H|m|s)";
  private static final String OFFSET_OP = "[+\\-]\\d+" + TIME_UNITS;
  private static final String ROUNDING_OP = "/" + TIME_UNITS + "(?!/" + TIME_UNITS + ")";

  @SuppressWarnings("java:S5998") // We limit input length before applying this regex
  private static final String OPERATIONS = "(?:" + OFFSET_OP + "|" + ROUNDING_OP + ")*";

  private static final Pattern RELATIVE_DATE_PATTERN = Pattern.compile("^now" + OPERATIONS + "$");

  private RelativeDateMath() {}

  public static boolean isValid(String value) {
    if (value == null || value.isEmpty() || value.length() > MAX_EXPRESSION_LENGTH) {
      return false;
    }
    return RELATIVE_DATE_PATTERN.matcher(value).matches();
  }
}

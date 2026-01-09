package no.einnsyn.backend.validation.isodatetime;

import jakarta.validation.ConstraintValidator;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public class IsoDateTimeValidator implements ConstraintValidator<IsoDateTime, String> {

  // Relative date format: "now" followed by optional offset and rounding operations
  // Examples: "now", "now+1d", "now-5h/d", "now+1y+2M-3d/M"

  private static final String TIME_UNITS = "(?:ms|y|M|w|d|h|H|m|s)";

  // Offset operations: +1d, -5h, +10m, etc.
  private static final String OFFSET_OP = "[+\\-]\\d+" + TIME_UNITS;

  // Rounding operations: /d, /M, /h, etc. (but prevent consecutive roundings like /d/M)
  private static final String ROUNDING_OP = "/" + TIME_UNITS + "(?!/" + TIME_UNITS + ")";

  // Zero or more operations (offsets or roundings) in any order
  @SuppressWarnings("java:S5998") // We limit input length before applying this regex
  private static final String OPERATIONS = "(?:" + OFFSET_OP + "|" + ROUNDING_OP + ")*";

  private static final Pattern RELATIVE_DATE_PATTERN = Pattern.compile("^now" + OPERATIONS + "$");

  private IsoDateTime.Format format;
  private boolean allowRelative;

  @Override
  public void initialize(IsoDateTime constraintAnnotation) {
    this.format = constraintAnnotation.format();
    this.allowRelative = constraintAnnotation.allowRelative();
  }

  @Override
  public boolean isValid(String value, jakarta.validation.ConstraintValidatorContext context) {
    if (StringUtils.isEmpty(value)) {
      return true;
    }

    // Check if it's a relative date
    if (allowRelative && isValidRelativeDate(value)) {
      return true;
    }

    // Otherwise, validate as ISO date/datetime
    if (format == IsoDateTime.Format.ISO_DATE_OR_DATE_TIME) {
      try {
        IsoDateTime.Format.ISO_DATE_TIME.getFormatter().parse(value);
        return true;
      } catch (Exception e) {
        try {
          IsoDateTime.Format.ISO_DATE.getFormatter().parse(value);
          return true;
        } catch (Exception e2) {
          return false;
        }
      }
    }

    try {
      format.getFormatter().parse(value);
    } catch (Exception e) {
      return false;
    }

    return true;
  }

  private boolean isValidRelativeDate(String value) {
    // Reject unreasonably long inputs to prevent potential regex DoS
    if (value.length() > 100) {
      return false;
    }
    return RELATIVE_DATE_PATTERN.matcher(value).matches();
  }
}

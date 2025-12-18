package no.einnsyn.backend.validation.isodatetime;

import jakarta.validation.ConstraintValidator;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public class IsoDateTimeValidator implements ConstraintValidator<IsoDateTime, String> {

  private static final Pattern RELATIVE_DATE_PATTERN =
      Pattern.compile(
          "^now"
              + "(?:[+\\-]\\d+(?:ms|y|M|w|d|h|H|m|s))*" // Zero or more offsets like +1d, -5h, +30m
              + "(?:/(?:ms|y|M|w|d|h|H|m|s))?" // Optional: rounding like /d, /M
              + "$");

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
    return RELATIVE_DATE_PATTERN.matcher(value).matches();
  }
}

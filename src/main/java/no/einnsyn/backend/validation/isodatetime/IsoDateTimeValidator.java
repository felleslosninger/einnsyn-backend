package no.einnsyn.backend.validation.isodatetime;

import jakarta.validation.ConstraintValidator;
import org.apache.commons.lang3.StringUtils;

public class IsoDateTimeValidator implements ConstraintValidator<IsoDateTime, String> {

  private IsoDateTime.Format format;

  @Override
  public void initialize(IsoDateTime constraintAnnotation) {
    this.format = constraintAnnotation.format();
  }

  @Override
  public boolean isValid(String value, jakarta.validation.ConstraintValidatorContext context) {
    if (StringUtils.isEmpty(value)) {
      return true;
    }

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
}

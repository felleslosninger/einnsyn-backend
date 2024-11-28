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

    try {
      format.getFormatter().parse(value);
    } catch (Exception e) {
      return false;
    }

    return true;
  }
}

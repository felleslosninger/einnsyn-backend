package no.einnsyn.backend.validation.validenum;

import jakarta.validation.ConstraintValidator;

public class ValidEnumValidator implements ConstraintValidator<ValidEnum, Object> {

  private Class<? extends java.lang.Enum<?>> enumClass;

  @Override
  public void initialize(ValidEnum constraintAnnotation) {
    this.enumClass = constraintAnnotation.enumClass();
  }

  @Override
  public boolean isValid(Object value, jakarta.validation.ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }

    if (enumClass.isInstance(value)) {
      return true;
    }

    if (value instanceof String string) {
      for (var enumValue : enumClass.getEnumConstants()) {
        if (enumValue.toString().equals(string)) {
          return true;
        }
      }
    }

    if (value instanceof Number number) {
      for (var enumValue : enumClass.getEnumConstants()) {
        if (enumValue.ordinal() == number.intValue()) {
          return true;
        }
      }
    }

    return false;
  }
}

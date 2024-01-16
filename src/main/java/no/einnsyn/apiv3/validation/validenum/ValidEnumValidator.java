package no.einnsyn.apiv3.validation.validenum;

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

    if (value instanceof String) {
      for (java.lang.Enum<?> enumValue : enumClass.getEnumConstants()) {
        if (enumValue.name().equals(value)) {
          return true;
        }
      }
    }

    if (value instanceof Number) {
      for (java.lang.Enum<?> enumValue : enumClass.getEnumConstants()) {
        if (enumValue.ordinal() == ((Number) value).intValue()) {
          return true;
        }
      }
    }

    return false;
  }
}

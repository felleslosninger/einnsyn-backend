package no.einnsyn.apiv3.validation.validenum;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ValidEnumValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEnum {
  String message() default "The given value is not one of the supported values.";

  Class<?>[] groups() default {};

  @SuppressWarnings("java:S1452")
  Class<? extends java.lang.Enum<?>> enumClass();

  Class<? extends Payload>[] payload() default {};
}

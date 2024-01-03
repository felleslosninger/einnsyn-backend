package no.einnsyn.apiv3.features.validation.NoSSN;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = NoSSNValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoSSN {
  String message() default "Possible SSN detected";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}

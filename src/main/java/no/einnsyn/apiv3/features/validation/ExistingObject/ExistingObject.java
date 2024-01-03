package no.einnsyn.apiv3.features.validation.ExistingObject;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ExistingObjectValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExistingObject {
  String message() default "The requested object was not found.";

  Class<?>[] groups() default {};

  Class<? extends Object> type();

  Class<? extends Payload>[] payload() default {};
}

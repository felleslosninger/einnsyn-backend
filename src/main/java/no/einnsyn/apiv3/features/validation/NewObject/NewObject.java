package no.einnsyn.apiv3.features.validation.NewObject;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = NewObjectValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NewObject {
  String message() default "Existing objects cannot be used for this property";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}

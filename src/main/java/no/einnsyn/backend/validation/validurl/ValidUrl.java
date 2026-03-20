package no.einnsyn.backend.validation.validurl;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a string is a valid URL. Unlike Hibernate's {@code @URL}, this accepts URLs with
 * un-encoded characters (spaces, non-ASCII) that users may type, as long as they are parseable by
 * {@link java.net.URL}.
 */
@Documented
@Constraint(validatedBy = ValidUrlValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUrl {
  String message() default "must be a valid URL";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}

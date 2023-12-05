package no.einnsyn.apiv3.features.validation.password;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Password {
  String message() default "Password does not meet requirements: At least one lowercase, one uppercase, one number or special character and minimum length of 8";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}

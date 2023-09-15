package no.einnsyn.apiv3.features.validation.ExistingObject;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObject;

@Documented
@Constraint(validatedBy = ExistingObjectValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExistingObject {
  String message() default "The given object could not be found";

  Class<?>[] groups() default {};

  Class<? extends EinnsynObject> type();

  Class<? extends Payload>[] payload() default {};
}

package no.einnsyn.apiv3.validation.idornewobject;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import no.einnsyn.apiv3.entities.base.BaseService;
import no.einnsyn.apiv3.entities.base.models.Base;
import no.einnsyn.apiv3.entities.base.models.BaseDTO;

@Documented
@Constraint(validatedBy = IdOrNewObjectValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface IdOrNewObject {
  String message() default
      "This property requires either an ID or a new object, not an object with existing unique"
          + " identifiers.";

  Class<?>[] groups() default {};

  @SuppressWarnings("java:S1452")
  Class<? extends BaseService<? extends Base, ? extends BaseDTO>> service();

  Class<? extends Payload>[] payload() default {};
}

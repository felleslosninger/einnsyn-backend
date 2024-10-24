package no.einnsyn.apiv3.validation.expandableobject;

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
@Constraint(validatedBy = ExpandableObjectValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExpandableObject {
  String message() default "Invalid value.";

  Class<?>[] groups() default {};

  @SuppressWarnings("java:S1452")
  Class<? extends BaseService<? extends Base, ? extends BaseDTO>> service();

  boolean mustExist() default false;

  boolean mustNotExist() default false;

  Class<? extends Payload>[] payload() default {};
}

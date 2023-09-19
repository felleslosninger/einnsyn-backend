package no.einnsyn.apiv3.features.validation.NewObject;

import java.util.List;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;

public class NewObjectValidator implements ConstraintValidator<NewObject, Object> {

  @Override
  public void initialize(NewObject constraint) {}

  @Override
  public boolean isValid(Object field, ConstraintValidatorContext cxt) {
    // Empty fields are valid
    if (field == null) {
      return true;
    }

    if (field instanceof List) {
      for (Object o: (List<?>) field) {
        if (!isValid(o, cxt)) {
          return false;
        }
      }
      return true;
    }

    // If no ID is given, this is a new object.
    if (field instanceof ExpandableField<?>) {
      if (((ExpandableField<?>)field).getId() == null) {
        return true;
      }
    }

    return false;
  }
}

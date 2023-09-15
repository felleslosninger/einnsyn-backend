package no.einnsyn.apiv3.features.validation.NewObject;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;

public class NewObjectValidator implements ConstraintValidator<NewObject, ExpandableField<?>> {

  @Override
  public void initialize(NewObject constraint) {}

  @Override
  public boolean isValid(ExpandableField<?> field, ConstraintValidatorContext cxt) {
    // Empty fields are valid
    if (field == null) {
      return true;
    }

    // If no ID is given, this is a new object.
    if (field.getId() == null) {
      return true;
    }

    return false;
  }
}

package no.einnsyn.apiv3.features.validation.NewObject;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;

public class NewObjectValidator implements ConstraintValidator<NewObject, Object> {

  @Override
  public boolean isValid(Object field, ConstraintValidatorContext cxt) {
    // Empty fields are valid
    if (field == null) {
      return true;
    }

    if (field instanceof List) {
      for (Object o : (List<?>) field) {
        if (!isValid(o, cxt)) {
          return false;
        }
      }
      return true;
    }

    // If no ID is given, this is a new object.
    if (field instanceof ExpandableField<?>) {
      if (((ExpandableField<?>) field).getId() == null) {
        return true;
      }
    }

    // The given object is an EinnsynObjectJSON
    if (field instanceof EinnsynObjectJSON) {
      if (((EinnsynObjectJSON) field).getId() == null) {
        return true;
      }
    }

    return false;
  }
}

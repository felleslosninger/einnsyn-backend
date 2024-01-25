package no.einnsyn.apiv3.validation.newobject;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.base.models.BaseDTO;

public class NewObjectValidator implements ConstraintValidator<NewObject, Object> {

  @Override
  public boolean isValid(Object field, ConstraintValidatorContext cxt) {
    // Empty fields are valid
    if (field == null) {
      return true;
    }

    if (field instanceof List<?> listField) {
      for (Object o : listField) {
        if (!isValid(o, cxt)) {
          return false;
        }
      }
      return true;
    }

    // If no ID is given, this is a new object.
    if (field instanceof ExpandableField<?> expandableField && expandableField.getId() == null) {
      return true;
    }

    // The given object is an BaseDTO
    if (field instanceof BaseDTO baseDtoField && baseDtoField.getId() == null) {
      return true;
    }

    return false;
  }
}

package no.einnsyn.apiv3.validation.expandableobject;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.base.BaseService;
import no.einnsyn.apiv3.entities.base.models.Base;
import no.einnsyn.apiv3.entities.base.models.BaseDTO;
import org.springframework.context.ApplicationContext;

public class ExpandableObjectValidator implements ConstraintValidator<ExpandableObject, Object> {

  private BaseService<? extends Base, ? extends BaseDTO> service;
  private final ApplicationContext applicationContext;
  private boolean mustExist;
  private boolean mustNotExist;

  public ExpandableObjectValidator(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Override
  public void initialize(ExpandableObject constraint) {
    var serviceClass = constraint.service();
    service = applicationContext.getBean(serviceClass);
    mustExist = constraint.mustExist();
    mustNotExist = constraint.mustNotExist();
  }

  /** Checks if a given ID exists in the repository for the given class. */
  @Override
  public boolean isValid(Object unknownObject, ConstraintValidatorContext context) {
    // If no value is given, we regard it as valid.
    if (unknownObject == null) {
      return true;
    }

    // If we have a list, we check if all elements are valid
    if (unknownObject instanceof List<?> listObject) {
      for (Object o : listObject) {
        if (!isValid(o, context)) {
          return false;
        }
      }
      return true;
    }

    // We have a String (id) or ExpandableField
    String id = null;
    BaseDTO expandedObject = null;
    if (unknownObject instanceof ExpandableField<?> expandableField) {
      id = expandableField.getId();
      var unknownExpandedObject = expandableField.getExpandedObject();
      if (unknownExpandedObject instanceof BaseDTO dto) {
        expandedObject = dto;
      }
    } else if (unknownObject instanceof String stringObject) {
      id = stringObject;
    }

    // If both mustExist and mustNotExist are set, the constraint is invalid
    if (mustExist && mustNotExist) {
      context.disableDefaultConstraintViolation();
      context
          .buildConstraintViolationWithTemplate(
              "Both `mustExist` and `mustNotExist` was specified.")
          .addConstraintViolation();
      return false;
    }

    // Object must exist in database
    if (mustExist && id == null) {
      context.disableDefaultConstraintViolation();
      context
          .buildConstraintViolationWithTemplate(
              "No ID was given, but an existing object was expected.")
          .addConstraintViolation();
      return false;
    }

    // Object must *not* exist in database
    if (mustNotExist) {
      if (id != null) {
        context.disableDefaultConstraintViolation();
        context
            .buildConstraintViolationWithTemplate("An ID was given, but a new object was expected.")
            .addConstraintViolation();
        return false;
      }
      if (expandedObject != null && service.findByDTO(expandedObject) != null) {
        context.disableDefaultConstraintViolation();
        context
            .buildConstraintViolationWithTemplate(
                "The given object had conflicting unique properties with an existing object.")
            .addConstraintViolation();
        return false;
      }
    }

    // If an id is given, check that it exists
    if (id != null) {
      var exists = service.findById(id) != null;
      if (!exists) {
        context.disableDefaultConstraintViolation();
        context
            .buildConstraintViolationWithTemplate("The given ID was not found.")
            .addConstraintViolation();
        return false;
      }
    }

    return true;
  }
}

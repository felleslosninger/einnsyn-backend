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
  public boolean isValid(Object unknownObject, ConstraintValidatorContext cxt) {
    // If no value is given, we regard it as valid.
    if (unknownObject == null) {
      return true;
    }

    // If we have a list, we check if all elements are valid
    if (unknownObject instanceof List<?> listObject) {
      for (Object o : listObject) {
        if (!isValid(o, cxt)) {
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
      return false;
    }

    // Object must exist in database
    if (mustExist && id == null) {
      return false;
    }

    // Object must *not* exist in database
    if (mustNotExist) {
      if (id != null) {
        return false;
      }
      if (expandedObject != null && service.findByDTO(expandedObject) != null) {
        return false;
      }
    }

    // If an id is given, check that it exists
    if (id != null) {
      var exists = service.findById(id) != null;
      if (!exists) {
        return false;
      }
    }

    return true;
  }
}

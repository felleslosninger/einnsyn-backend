package no.einnsyn.apiv3.validation.existingobject;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.base.BaseService;
import no.einnsyn.apiv3.entities.base.models.Base;
import no.einnsyn.apiv3.entities.base.models.BaseDTO;
import org.springframework.context.ApplicationContext;

public class ExistingObjectValidator implements ConstraintValidator<ExistingObject, Object> {

  private BaseService<? extends Base, ? extends BaseDTO> service;
  private final ApplicationContext applicationContext;

  public ExistingObjectValidator(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Override
  public void initialize(ExistingObject constraint) {
    var serviceClass = constraint.service();
    this.service = applicationContext.getBean(serviceClass);
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
    if (unknownObject instanceof ExpandableField<?> expandableFieldObject) {
      id = expandableFieldObject.getId();
    } else if (unknownObject instanceof String stringObject) {
      id = stringObject;
    }

    if (id != null) {
      return service.findById(id) != null;
    }

    return false;
  }
}

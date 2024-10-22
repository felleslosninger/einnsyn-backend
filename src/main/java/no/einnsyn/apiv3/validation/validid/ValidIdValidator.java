package no.einnsyn.apiv3.validation.validid;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.base.BaseService;
import no.einnsyn.apiv3.entities.base.models.Base;
import no.einnsyn.apiv3.entities.base.models.BaseDTO;
import org.springframework.context.ApplicationContext;

public class ValidIdValidator implements ConstraintValidator<ValidId, Object> {

  private BaseService<? extends Base, ? extends BaseDTO> service;
  private final ApplicationContext applicationContext;

  public ValidIdValidator(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Override
  public void initialize(ValidId constraint) {
    var serviceClass = constraint.service();
    service = applicationContext.getBean(serviceClass);
  }

  @Override
  public boolean isValid(Object unknownObject, ConstraintValidatorContext cxt) {

    // Empty fields are valid
    if (unknownObject == null) {
      return true;
    }

    // If we have a list, we check if all elements are valid
    if (unknownObject instanceof List<?> listField) {
      for (Object o : listField) {
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

    // If we don't have an ID, this is valid
    return true;
  }
}

package no.einnsyn.apiv3.validation.idornewobject;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.base.BaseService;
import no.einnsyn.apiv3.entities.base.models.Base;
import no.einnsyn.apiv3.entities.base.models.BaseDTO;
import org.springframework.context.ApplicationContext;

public class IdOrNewObjectValidator implements ConstraintValidator<IdOrNewObject, Object> {

  private BaseService<? extends Base, ? extends BaseDTO> service;
  private final ApplicationContext applicationContext;

  public IdOrNewObjectValidator(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Override
  public void initialize(IdOrNewObject constraint) {
    var serviceClass = constraint.service();
    service = applicationContext.getBean(serviceClass);
  }

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
    if (field instanceof ExpandableField<?> expandableField) {
      if (expandableField.getId() != null) {
        return false;
      }
      return isValid(expandableField.getExpandedObject(), cxt);
    }

    // Check if the object exists, by DTO
    if (field instanceof BaseDTO dtoField) {
      return service.findByDTO(dtoField) == null;
    }

    return false;
  }
}

package no.einnsyn.apiv3.features.validation.NoSSN;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NoSSNValidator implements ConstraintValidator<NoSSN, String> {

  @Override
  public void initialize(NoSSN text) {}

  @Override
  public boolean isValid(String text, ConstraintValidatorContext cxt) {
    if (text == null) {
      return true;
    }

    // TODO: Implement real SSN detector
    return !text.matches(".*\\d{11}.*");
  }

}

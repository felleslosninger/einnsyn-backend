package no.einnsyn.apiv3.validation.password;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class PasswordValidator implements ConstraintValidator<Password, String> {

  // Require lowercase, uppercase and number or special character
  private Pattern pattern = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9\\W]).{8,}");

  @Override
  public boolean isValid(String password, ConstraintValidatorContext cxt) {
    if (password == null) {
      return true;
    }

    // Check if password matches regex
    return pattern.matcher(password).matches();
  }
}

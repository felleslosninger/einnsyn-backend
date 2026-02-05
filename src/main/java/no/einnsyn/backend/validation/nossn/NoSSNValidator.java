package no.einnsyn.backend.validation.nossn;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.regex.Pattern;
import no.bekk.bekkopen.person.FodselsnummerValidator;

public class NoSSNValidator implements ConstraintValidator<NoSSN, Object> {

  // Pre-compile the pattern
  private final Pattern pattern =
      Pattern.compile(
          ""
              + "\\b" // Cannot have a preceding digit
              + "(\\d{11}|\\d{6}\\s\\d{5}|\\d{4}[\\.\s]\\d{2}[\\.\s]\\d{5})" // SSN
              + "\\b"); // Cannot have a following digit

  /**
   * Check if the given input contains any SSNs.
   *
   * @param input the input object to validate
   * @param cxt the constraint validator context
   * @return true if the input is valid (no SSNs found), false otherwise
   */
  @Override
  public boolean isValid(Object input, ConstraintValidatorContext cxt) {
    if (input == null) {
      return true;
    }

    // Check list of strings
    if (input instanceof List<?> list) {
      for (Object text : list) {
        if (!isValid(text, cxt)) {
          return false;
        }
      }
      return true;
    }

    // Check single string
    if (input instanceof String text) {
      // Check all matches
      var matcher = pattern.matcher(text);
      while (matcher.find()) {
        var possibleSSN = matcher.group(1);
        possibleSSN = possibleSSN.replaceAll("[^\\d]", "");
        if (FodselsnummerValidator.isValid(possibleSSN)) {
          return false;
        }
      }

      return true;
    }

    return false;
  }
}

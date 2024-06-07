package no.einnsyn.apiv3.validation.nossn;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import no.bekk.bekkopen.person.FodselsnummerValidator;

public class NoSSNValidator implements ConstraintValidator<NoSSN, String> {

  // Pre-compile the pattern
  private final Pattern pattern =
      Pattern.compile(
          "(^|[^\\d])(\\d{11}|\\d{6}\\s\\d{5}|\\d{4}\\.\\d{2}\\.\\d{5}|\\d{4}\\s\\d{2}\\s\\d{5})($|[^\\d])");

  @Override
  public boolean isValid(String text, ConstraintValidatorContext cxt) {
    if (text == null) {
      return true;
    }

    // Match 11 digits, without a preceding or following digit
    Matcher matcher = pattern.matcher(text);

    // Check all matches
    while (matcher.find()) {
      String possibleSSN = matcher.group(2);
      possibleSSN = possibleSSN.replaceAll("[^\\d]", "");
      if (FodselsnummerValidator.isValid(possibleSSN)) {
        System.err.println("SSN found: " + possibleSSN);
        return false;
      }
      System.err.println("No SSN found: " + possibleSSN);
    }

    return true;
  }
}

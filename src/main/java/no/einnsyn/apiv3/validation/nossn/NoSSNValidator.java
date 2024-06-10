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
          ""
              + "(^|[^\\d])" // Cannot have a preceding digit
              + "(\\d{11}|\\d{6}\\s\\d{5}|\\d{4}[\\.\s]\\d{2}[\\.\s]\\d{5})" // SSN
              + "($|[^\\d])"); // Cannot have a following digit

  /**
   * Check if the given text contains any SSNs.
   *
   * @param text
   * @param cxt
   */
  @Override
  public boolean isValid(String text, ConstraintValidatorContext cxt) {
    if (text == null) {
      return true;
    }

    // Check all matches
    Matcher matcher = pattern.matcher(text);
    while (matcher.find()) {
      String possibleSSN = matcher.group(2);
      possibleSSN = possibleSSN.replaceAll("[^\\d]", "");
      if (FodselsnummerValidator.isValid(possibleSSN) && !isInUUID(possibleSSN, text)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Check if the given SSN is part of a UUID. If it is, we can assume it's not a real SSN.
   *
   * @param ssn
   * @param text
   * @return
   */
  private boolean isInUUID(String ssn, String text) {
    final String uuidPatternString =
        "("
            + "[a-fA-F\\d]{8}"
            + "-[a-fA-F\\d]{4}"
            + "-[a-fA-F\\d]{4}"
            + "-[a-fA-F\\d]{4}"
            + "-("
            + "[a-fA-F\\d]"
            + ssn
            + "|"
            + ssn
            + "[a-fA-F\\d]"
            + ")"
            + ")";
    final Pattern uuidPattern = Pattern.compile(uuidPatternString);
    Matcher matcher = uuidPattern.matcher(text);
    while (matcher.find()) {
      String maybeUUID = matcher.group(1);
      try {
        java.util.UUID.fromString(maybeUUID);
        return true;
      } catch (IllegalArgumentException e) {
        return false;
      }
    }
    return false;
  }
}

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
        var possibleSSN = matcher.group(2);
        possibleSSN = possibleSSN.replaceAll("[^\\d]", "");
        if (FodselsnummerValidator.isValid(possibleSSN) && !isInUUID(possibleSSN, text)) {
          return false;
        }
      }

      return true;
    }

    return false;
  }

  /**
   * Check if the given SSN is part of a UUID. If it is, we can assume it's not a real SSN.
   *
   * @param ssn
   * @param text
   * @return
   */
  private boolean isInUUID(String ssn, String text) {
    var uuidPatternString =
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
    var uuidPattern = Pattern.compile(uuidPatternString);
    var matcher = uuidPattern.matcher(text);
    var allOccurrencesAreInUUIDs = false;
    while (matcher.find()) {
      var maybeUUID = matcher.group(1);
      try {
        java.util.UUID.fromString(maybeUUID);
        allOccurrencesAreInUUIDs = true;
      } catch (IllegalArgumentException e) {
        return false;
      }
    }
    return allOccurrencesAreInUUIDs;
  }
}

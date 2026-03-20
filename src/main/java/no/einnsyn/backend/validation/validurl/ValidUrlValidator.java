package no.einnsyn.backend.validation.validurl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Validates that a string can be parsed as a URL, accepting un-encoded characters (spaces,
 * non-ASCII) that real users may provide. Mirrors the encoding fallback in the service layer.
 */
public class ValidUrlValidator implements ConstraintValidator<ValidUrl, CharSequence> {

  @Override
  public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
    if (value == null || value.isEmpty()) {
      return true;
    }

    try {
      UriComponentsBuilder.fromUriString(value.toString()).encode().build().toUri().toURL();
      return true;
    } catch (Exception _) {
      return false;
    }
  }
}

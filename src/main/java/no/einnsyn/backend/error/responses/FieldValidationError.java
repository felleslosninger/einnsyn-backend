package no.einnsyn.backend.error.responses;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FieldValidationError {

  private final String fieldName;
  private final String value;
  private final String message;

  public FieldValidationError(String fieldName, String value, String message) {
    // Remove ExpandedField wrapper for better debugging
    fieldName = fieldName.replace("expandedObject.", "");

    this.fieldName = fieldName;
    this.value = value;
    this.message = message;
  }
}

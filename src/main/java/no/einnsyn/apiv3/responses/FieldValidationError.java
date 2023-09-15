package no.einnsyn.apiv3.responses;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FieldValidationError {

  private final String fieldName;
  private final String value;
  private final String message;

  public FieldValidationError(String fieldName, String value, String message) {
    this.fieldName = fieldName;
    this.value = value;
    this.message = message;
  }
}

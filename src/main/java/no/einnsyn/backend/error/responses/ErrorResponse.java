package no.einnsyn.backend.error.responses;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class ErrorResponse {

  private HttpStatus status;
  private String message;
  private List<String> errors;
  private List<FieldValidationError> fieldErrors;

  public ErrorResponse(final HttpStatus status) {
    super();
    this.status = status;
    this.message = status.getReasonPhrase();
  }

  public ErrorResponse(final HttpStatus status, final String message) {
    super();
    this.status = status;
    this.message = message;
  }

  public ErrorResponse(
      final HttpStatus status,
      final List<String> errors,
      final List<FieldValidationError> fieldErrors) {
    super();
    this.status = status;
    this.message = status.getReasonPhrase();
    this.errors = errors;
    this.fieldErrors = fieldErrors;
  }

  public ErrorResponse(
      final HttpStatus status,
      final String message,
      final List<String> errors,
      final List<FieldValidationError> fieldErrors) {
    super();
    this.status = status;
    this.message = message;
    this.errors = errors;
    this.fieldErrors = fieldErrors;
  }
}

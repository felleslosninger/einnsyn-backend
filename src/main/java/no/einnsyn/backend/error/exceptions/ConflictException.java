package no.einnsyn.backend.error.exceptions;

public class ConflictException extends EInnsynException {

  public ConflictException(String message) {
    super(message);
  }

  public ConflictException(String message, Exception cause) {
    super(message, cause);
  }
}

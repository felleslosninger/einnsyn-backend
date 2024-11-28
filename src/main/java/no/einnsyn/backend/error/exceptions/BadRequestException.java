package no.einnsyn.backend.error.exceptions;

public class BadRequestException extends EInnsynException {

  public BadRequestException(String message) {
    super(message);
  }

  public BadRequestException(String message, Exception cause) {
    super(message, cause);
  }
}

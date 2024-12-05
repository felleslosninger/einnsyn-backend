package no.einnsyn.backend.error.exceptions;

public class InternalServerErrorException extends EInnsynException {
  public InternalServerErrorException(String message) {
    super(message);
  }

  public InternalServerErrorException(String message, Exception cause) {
    super(message, cause);
  }
}

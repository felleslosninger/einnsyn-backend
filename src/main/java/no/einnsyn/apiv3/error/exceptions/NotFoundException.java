package no.einnsyn.apiv3.error.exceptions;

public class NotFoundException extends EInnsynException {
  public NotFoundException(String message) {
    super(message);
  }

  public NotFoundException(String message, Exception cause) {
    super(message, cause);
  }
}

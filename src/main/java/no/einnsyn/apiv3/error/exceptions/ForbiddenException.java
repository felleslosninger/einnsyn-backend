package no.einnsyn.apiv3.error.exceptions;

public class ForbiddenException extends EInnsynException {
  public ForbiddenException(String message) {
    super(message);
  }

  public ForbiddenException(String message, Exception cause) {
    super(message, cause);
  }
}

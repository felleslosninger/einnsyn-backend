package no.einnsyn.apiv3.error.exceptions;

public class EInnsynDataIntegrityViolationException extends InternalServerErrorException {
  public EInnsynDataIntegrityViolationException(String message) {
    super(message);
  }

  public EInnsynDataIntegrityViolationException(String message, Exception cause) {
    super(message, cause);
  }
}

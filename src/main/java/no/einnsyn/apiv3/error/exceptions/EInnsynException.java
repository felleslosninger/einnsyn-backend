package no.einnsyn.apiv3.error.exceptions;

public class EInnsynException extends Exception {

  public EInnsynException(String message) {
    super(message);
  }

  public EInnsynException(String message, Exception cause) {
    super(message, cause);
  }
}

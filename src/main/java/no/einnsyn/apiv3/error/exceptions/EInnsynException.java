package no.einnsyn.apiv3.error.exceptions;

public class EInnsynException extends Exception {

  final Exception e;

  public EInnsynException(String message) {
    super(message);
    this.e = null;
  }

  public EInnsynException(String message, Exception originalException) {
    super(message);
    this.e = originalException;
  }
}

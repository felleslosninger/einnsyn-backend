package no.einnsyn.backend.error.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;

@Getter
public class UnauthorizedException extends EInnsynException {

  private final String method;
  private final String path;
  private final String queryString;
  private final String timestamp;

  public UnauthorizedException(String message) {
    super(message);
    this.method = null;
    this.path = null;
    this.queryString = null;
    this.timestamp = null;
  }

  public UnauthorizedException(String message, HttpServletRequest request) {
    super(message);
    this.method = request.getMethod();
    this.path = request.getRequestURI();
    this.queryString = request.getQueryString();
    this.timestamp = request.getHeader("ein-x-timestamp");
  }
}

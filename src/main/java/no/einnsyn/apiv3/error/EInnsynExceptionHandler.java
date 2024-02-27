package no.einnsyn.apiv3.error;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.error.exceptions.ForbiddenException;
import no.einnsyn.apiv3.error.exceptions.UnauthorizedException;
import no.einnsyn.apiv3.error.responses.ErrorResponse;
import no.einnsyn.apiv3.error.responses.FieldValidationError;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
@Slf4j
public class EInnsynExceptionHandler extends ResponseEntityExceptionHandler {

  MeterRegistry meterRegistry;

  public EInnsynExceptionHandler(MeterRegistry meterRegistry) {
    super();
    this.meterRegistry = meterRegistry;
  }

  private void logAndCountError(Exception ex) {
    log.error(ex.getMessage(), ex);
    meterRegistry.counter("ein_error", "error", ex.getClass().getSimpleName()).increment();
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> exception(Exception ex) {
    logAndCountError(ex);
    var status = HttpStatus.INTERNAL_SERVER_ERROR;
    var apiError = new ErrorResponse(status, ex.getMessage(), null, null);
    return new ResponseEntity<>(apiError, null, status);
  }

  /**
   * 401 Unauthorized
   *
   * @param ex
   * @return
   */
  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ErrorResponse> unauthorizedException(UnauthorizedException ex) {
    logAndCountError(ex);
    var status = HttpStatus.UNAUTHORIZED;
    var apiError = new ErrorResponse(status, ex.getMessage(), null, null);
    return new ResponseEntity<>(apiError, null, status);
  }

  /**
   * 403 Forbidden
   *
   * @param ex
   * @return
   */
  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<ErrorResponse> forbiddenException(ForbiddenException ex) {
    logAndCountError(ex);
    var status = HttpStatus.FORBIDDEN;
    var apiError = new ErrorResponse(status, ex.getMessage(), null, null);
    return new ResponseEntity<>(apiError, null, status);
  }

  /**
   * 400 Bad Request
   *
   * @param ex
   * @return
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> illegalArgumentException(IllegalArgumentException ex) {
    logAndCountError(ex);
    var status = HttpStatus.BAD_REQUEST;
    var apiError = new ErrorResponse(status, ex.getMessage(), null, null);
    return new ResponseEntity<>(apiError, null, status);
  }

  /**
   * Input validation errors, @Valid and @Validated on JSON fields.
   *
   * @param ex
   * @param headers
   * @param status
   * @param request
   */
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    logAndCountError(ex);

    var fieldErrors =
        ex.getFieldErrors().stream()
            .map(
                e ->
                    new FieldValidationError(
                        e.getField(),
                        e.getRejectedValue() == null ? null : e.getRejectedValue().toString(),
                        e.getDefaultMessage()))
            .toList();

    var apiError = new ErrorResponse(HttpStatus.BAD_REQUEST, null, null, fieldErrors);
    return handleExceptionInternal(ex, apiError, headers, apiError.getStatus(), request);
  }

  /**
   * Path-variable validation errors. Most likely non-existent IDs.
   *
   * @param ex
   * @param headers
   * @param status
   * @param request
   */
  @Override
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    logAndCountError(ex);

    var notFound = false;
    for (var error : ex.getAllErrors()) {
      var defaultMessage = error.getDefaultMessage();
      if (defaultMessage != null && defaultMessage.contains("not found")) {
        notFound = true;
      }
    }

    ErrorResponse apiError;
    if (notFound) {
      apiError =
          new ErrorResponse(
              HttpStatus.NOT_FOUND, "The requested resource was not found.", null, null);
    } else {
      apiError = new ErrorResponse(HttpStatus.BAD_REQUEST, null, null, null);
    }

    return handleExceptionInternal(ex, apiError, headers, apiError.getStatus(), request);
  }

  /**
   * JSON parse errors
   *
   * @param ex
   * @param headers
   * @param status
   * @param request
   */
  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    logAndCountError(ex);

    final ErrorResponse apiError =
        new ErrorResponse(HttpStatus.BAD_REQUEST, "Could not parse the request body.", null, null);

    return handleExceptionInternal(ex, apiError, headers, apiError.getStatus(), request);
  }
}

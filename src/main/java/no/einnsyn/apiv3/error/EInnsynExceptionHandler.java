package no.einnsyn.apiv3.error;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.error.exceptions.BadRequestException;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import no.einnsyn.apiv3.error.exceptions.ForbiddenException;
import no.einnsyn.apiv3.error.exceptions.NotFoundException;
import no.einnsyn.apiv3.error.exceptions.UnauthorizedException;
import no.einnsyn.apiv3.error.responses.ErrorResponse;
import no.einnsyn.apiv3.error.responses.FieldValidationError;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
@Slf4j
public class EInnsynExceptionHandler extends ResponseEntityExceptionHandler {

  private final MeterRegistry meterRegistry;

  public EInnsynExceptionHandler(MeterRegistry meterRegistry) {
    super();
    this.meterRegistry = meterRegistry;
  }

  private void logAndCountWarning(Exception ex) {
    log.warn(ex.getMessage());
    meterRegistry.counter("ein_error", "warning", ex.getClass().getSimpleName()).increment();
  }

  private void logAndCountError(Exception ex) {
    log.error(ex.getMessage(), ex);
    meterRegistry.counter("ein_error", "error", ex.getClass().getSimpleName()).increment();
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception ex) {
    logAndCountError(ex);
    var status = HttpStatus.INTERNAL_SERVER_ERROR;
    var apiError = new ErrorResponse(status, ex.getMessage(), null, null);
    return new ResponseEntity<>(apiError, null, status);
  }

  @ExceptionHandler(TransactionSystemException.class)
  public ResponseEntity<ErrorResponse> handleException(TransactionSystemException ex) {
    if (ex.getRootCause() instanceof EInnsynException eInnsynException) {
      return handleException(eInnsynException);
    }

    logAndCountError(ex);
    var status = HttpStatus.BAD_REQUEST;
    var apiError = new ErrorResponse(status, ex.getMessage(), null, null);
    return new ResponseEntity<>(apiError, null, status);
  }

  /**
   * 400 Bad Request
   *
   * @param ex The exception
   * @return The response entity
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleException(IllegalArgumentException ex) {
    logAndCountWarning(ex);
    var status = HttpStatus.BAD_REQUEST;
    var apiError = new ErrorResponse(status, ex.getMessage(), null, null);
    return new ResponseEntity<>(apiError, null, status);
  }

  /**
   * 400 Bad Request
   *
   * @param ex The exception
   * @return The response entity
   */
  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ErrorResponse> handleException(BadRequestException ex) {
    logAndCountWarning(ex);
    var status = HttpStatus.BAD_REQUEST;
    var apiError = new ErrorResponse(status, ex.getMessage(), null, null);
    return new ResponseEntity<>(apiError, null, status);
  }

  /**
   * 401 Unauthorized
   *
   * @param ex The exception
   * @return The response entity
   */
  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ErrorResponse> handleException(UnauthorizedException ex) {
    logAndCountWarning(ex);
    var status = HttpStatus.UNAUTHORIZED;
    var apiError = new ErrorResponse(status, ex.getMessage(), null, null);
    return new ResponseEntity<>(apiError, null, status);
  }

  /**
   * 403 Forbidden
   *
   * @param ex The exception
   * @return The response entity
   */
  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<ErrorResponse> handleException(ForbiddenException ex) {
    logAndCountWarning(ex);
    var status = HttpStatus.FORBIDDEN;
    var apiError = new ErrorResponse(status, ex.getMessage(), null, null);
    return new ResponseEntity<>(apiError, null, status);
  }

  /*
   * 404 Not Found
   *
   *
   */
  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorResponse> handleException(NotFoundException ex) {
    logAndCountWarning(ex);
    var status = HttpStatus.NOT_FOUND;
    var apiError = new ErrorResponse(status, ex.getMessage(), null, null);
    return new ResponseEntity<>(apiError, null, status);
  }

  /**
   * Conflict
   *
   * @param ex The exception
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleException(DataIntegrityViolationException ex) {
    logAndCountWarning(ex);
    var status = HttpStatus.CONFLICT;
    var apiError = new ErrorResponse(status, ex.getMessage(), null, null);
    return new ResponseEntity<>(apiError, null, status);
  }

  /**
   * Input validation errors, @Valid and @Validated on JSON fields.
   *
   * @param ex The exception
   * @param headers The headers
   * @param status The status
   * @param request The request
   */
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      @NotNull MethodArgumentNotValidException ex,
      @NotNull HttpHeaders headers,
      @NotNull HttpStatusCode status,
      @NotNull WebRequest request) {
    logAndCountWarning(ex);

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
   * @param ex The exception
   * @param headers The headers
   * @param status The status
   * @param request The request
   */
  @Override
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      @NotNull HandlerMethodValidationException ex,
      @NotNull HttpHeaders headers,
      @NotNull HttpStatusCode status,
      @NotNull WebRequest request) {
    logAndCountWarning(ex);

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
   * @param ex The exception
   * @param headers The headers
   * @param status The status
   * @param request The request
   */
  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      @NotNull HttpMessageNotReadableException ex,
      @NotNull HttpHeaders headers,
      @NotNull HttpStatusCode status,
      @NotNull WebRequest request) {
    logAndCountWarning(ex);

    final ErrorResponse apiError =
        new ErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Could not parse the request body.",
            List.of(ex.getMessage()),
            null);

    return handleExceptionInternal(ex, apiError, headers, apiError.getStatus(), request);
  }
}

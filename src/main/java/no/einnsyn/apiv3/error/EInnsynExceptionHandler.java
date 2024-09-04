package no.einnsyn.apiv3.error;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import no.einnsyn.apiv3.error.exceptions.BadRequestException;
import no.einnsyn.apiv3.error.exceptions.ConflictException;
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

  private void logAndCountWarning(EInnsynException ex, HttpStatusCode statusCode) {
    var exceptionName = ex.getClass().getSimpleName();
    var cause = ex.getCause();
    System.err.println("WARNING");
    log.warn(
        ex.getMessage(),
        ex,
        StructuredArguments.value("causeMessage", cause != null ? cause.getMessage() : null),
        StructuredArguments.value("exception", exceptionName),
        StructuredArguments.value("responseStatus", String.valueOf(statusCode)));
    meterRegistry.counter("ein_error", "warning", exceptionName).increment();
  }

  private void logAndCountError(EInnsynException ex, HttpStatusCode statusCode) {
    var exceptionName = ex.getClass().getSimpleName();
    var cause = ex.getCause();
    System.err.println("ERROR");
    System.err.println(cause);
    log.error(
        ex.getMessage(),
        ex,
        StructuredArguments.value("causeMessage", cause != null ? cause.getMessage() : null),
        StructuredArguments.value("causeStackTrace", getStackTrace(ex.getCause())),
        StructuredArguments.value("exception", exceptionName),
        StructuredArguments.value("responseStatus", String.valueOf(statusCode)));
    meterRegistry.counter("ein_error", "error", exceptionName).increment();
  }

  private String getStackTrace(Throwable ex) {
    return ex == null
        ? null
        : Arrays.stream(ex.getStackTrace())
            .map(StackTraceElement::toString)
            .collect(Collectors.joining("\n"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception ex) {
    var httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    var internalServerErrorException = new EInnsynException("Internal server error", ex);
    logAndCountError(internalServerErrorException, httpStatus);
    var apiError = new ErrorResponse(httpStatus);
    return new ResponseEntity<>(apiError, null, httpStatus);
  }

  @ExceptionHandler(TransactionSystemException.class)
  public ResponseEntity<ErrorResponse> handleException(TransactionSystemException ex) {
    if (ex.getRootCause() instanceof EInnsynException eInnsynException) {
      return handleException(eInnsynException);
    }

    var httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    var internalServerErrorException = new EInnsynException("Transaction system exception", ex);
    logAndCountError(internalServerErrorException, httpStatus);
    var apiError = new ErrorResponse(httpStatus);
    return new ResponseEntity<>(apiError, null, httpStatus);
  }

  /**
   * 400 Bad Request
   *
   * @param ex The exception
   * @return The response entity
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleException(IllegalArgumentException ex) {
    var httpStatus = HttpStatus.BAD_REQUEST;
    var badRequestException = new BadRequestException(ex.getMessage(), ex);
    logAndCountWarning(badRequestException, httpStatus);
    var apiError = new ErrorResponse(httpStatus);
    return new ResponseEntity<>(apiError, null, httpStatus);
  }

  /**
   * 400 Bad Request
   *
   * @param ex The exception
   * @return The response entity
   */
  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ErrorResponse> handleException(BadRequestException ex) {
    var httpStatus = HttpStatus.BAD_REQUEST;
    logAndCountWarning(ex, httpStatus);
    var apiError = new ErrorResponse(httpStatus);
    return new ResponseEntity<>(apiError, null, httpStatus);
  }

  /**
   * 401 Unauthorized
   *
   * @param ex The exception
   * @return The response entity
   */
  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ErrorResponse> handleException(UnauthorizedException ex) {
    var httpStatus = HttpStatus.UNAUTHORIZED;
    logAndCountWarning(ex, httpStatus);
    var apiError = new ErrorResponse(httpStatus);
    return new ResponseEntity<>(apiError, null, httpStatus);
  }

  /**
   * 403 Forbidden
   *
   * @param ex The exception
   * @return The response entity
   */
  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<ErrorResponse> handleException(ForbiddenException ex) {
    var httpStatus = HttpStatus.FORBIDDEN;
    logAndCountWarning(ex, httpStatus);
    var apiError = new ErrorResponse(httpStatus);
    return new ResponseEntity<>(apiError, null, httpStatus);
  }

  /*
   * 404 Not Found
   *
   *
   */
  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorResponse> handleException(NotFoundException ex) {
    var httpStatus = HttpStatus.NOT_FOUND;
    logAndCountWarning(ex, httpStatus);
    var apiError = new ErrorResponse(httpStatus);
    return new ResponseEntity<>(apiError, null, httpStatus);
  }

  /**
   * DataIntegrityViolation
   *
   * <p>Likely a unique constraint violation, foreign key constraint violation, not-null constraint
   * or similar.
   *
   * @param ex The exception
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleConflictException(Exception ex) {
    var httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    var internalServerErrorException = new EInnsynException("Data integrity violation", ex);
    logAndCountError(internalServerErrorException, httpStatus);
    var apiError = new ErrorResponse(httpStatus);
    return new ResponseEntity<>(apiError, null, httpStatus);
  }

  /**
   * Conflict
   *
   * @param ex The exception
   */
  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ErrorResponse> handleConflictException(ConflictException ex) {
    var httpStatus = HttpStatus.CONFLICT;
    logAndCountWarning(ex, httpStatus);
    var apiError = new ErrorResponse(httpStatus);
    return new ResponseEntity<>(apiError, null, httpStatus);
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

    var fieldErrors =
        ex.getFieldErrors().stream()
            .map(
                e ->
                    new FieldValidationError(
                        e.getField(),
                        e.getRejectedValue() == null ? null : e.getRejectedValue().toString(),
                        e.getDefaultMessage()))
            .toList();

    var httpStatus = HttpStatus.BAD_REQUEST;
    var badRequestException =
        new BadRequestException("Field validation error: " + request.getDescription(false), ex);
    var fieldNames = fieldErrors.stream().map(f -> f.getFieldName()).toList();
    var errorMessage = "Field validation error on fields: " + String.join(", ", fieldNames);
    var apiError = new ErrorResponse(httpStatus, errorMessage, null, fieldErrors);

    logAndCountWarning(badRequestException, httpStatus);
    return handleExceptionInternal(badRequestException, apiError, headers, httpStatus, request);
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

    var notFound = false;
    for (var error : ex.getAllErrors()) {
      var defaultMessage = error.getDefaultMessage();
      if (defaultMessage != null && defaultMessage.contains("not found")) {
        notFound = true;
        break;
      }
    }

    // 404
    if (notFound) {
      var httpStatus = HttpStatus.NOT_FOUND;
      var notFoundException =
          new NotFoundException("Not found: " + request.getDescription(false), ex);
      logAndCountWarning(notFoundException, httpStatus);
      var apiError = new ErrorResponse(httpStatus);
      return handleExceptionInternal(notFoundException, apiError, headers, httpStatus, request);
    }

    // Bad request
    else {
      var httpStatus = HttpStatus.BAD_REQUEST;
      var badRequestException =
          new BadRequestException("Bad request: " + request.getDescription(false), ex);
      logAndCountWarning(badRequestException, httpStatus);
      var apiError = new ErrorResponse(httpStatus);
      return handleExceptionInternal(badRequestException, apiError, headers, httpStatus, request);
    }
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

    var httpStatus = HttpStatus.BAD_REQUEST;
    var badRequestException = new BadRequestException(ex.getMessage(), ex);
    logAndCountWarning(badRequestException, httpStatus);
    // Don't send standard message, HttpMessageNotReadableException may contain valuable info:
    var apiError = new ErrorResponse(httpStatus, ex.getMessage());
    return handleExceptionInternal(badRequestException, apiError, headers, httpStatus, request);
  }
}

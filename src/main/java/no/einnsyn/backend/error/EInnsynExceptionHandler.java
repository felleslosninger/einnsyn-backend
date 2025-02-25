package no.einnsyn.backend.error;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import no.einnsyn.backend.common.exceptions.models.AuthenticationException;
import no.einnsyn.backend.common.exceptions.models.AuthorizationException;
import no.einnsyn.backend.common.exceptions.models.BadRequestException;
import no.einnsyn.backend.common.exceptions.models.ConflictException;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.exceptions.models.InternalServerErrorException;
import no.einnsyn.backend.common.exceptions.models.MethodNotAllowedException;
import no.einnsyn.backend.common.exceptions.models.NotFoundException;
import no.einnsyn.backend.common.exceptions.models.ValidationException;
import no.einnsyn.backend.common.exceptions.models.ValidationException.FieldError;
import no.einnsyn.backend.common.responses.models.ErrorResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
    log.warn(
        ex.getMessage(), StructuredArguments.value("responseStatus", String.valueOf(statusCode)));
    meterRegistry
        .counter("ein_exception", "level", "warning", "exception", exceptionName)
        .increment();
  }

  private void logAndCountError(EInnsynException ex, HttpStatusCode statusCode) {
    var exceptionName = ex.getClass().getSimpleName();
    log.error(
        ex.getMessage(),
        StructuredArguments.value("responseStatus", String.valueOf(statusCode)),
        ex);
    meterRegistry
        .counter("ein_exception", "level", "error", "exception", exceptionName)
        .increment();
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception ex) {
    var httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    var internalServerErrorException =
        new InternalServerErrorException("Internal server error", ex);
    logAndCountError(internalServerErrorException, httpStatus);
    return new ResponseEntity<>(internalServerErrorException.toClientResponse(), null, httpStatus);
  }

  @ExceptionHandler(TransactionSystemException.class)
  public ResponseEntity<ErrorResponse> handleException(TransactionSystemException ex) {
    if (ex.getRootCause() instanceof EInnsynException eInnsynException) {
      return handleException(eInnsynException);
    }

    var httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    var internalServerErrorException =
        new InternalServerErrorException("Transaction system exception", ex);
    logAndCountError(internalServerErrorException, httpStatus);
    var clientResponse = internalServerErrorException.toClientResponse();
    return new ResponseEntity<>(clientResponse, null, httpStatus);
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
    var clientResponse = badRequestException.toClientResponse();
    return new ResponseEntity<>(clientResponse, null, httpStatus);
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
    var clientResponse = ex.toClientResponse();
    return new ResponseEntity<>(clientResponse, null, httpStatus);
  }

  /**
   * 403 Forbidden
   *
   * @param ex The exception
   * @return The response entity
   */
  @ExceptionHandler(AuthorizationException.class)
  public ResponseEntity<ErrorResponse> handleException(AuthorizationException ex) {
    var httpStatus = HttpStatus.FORBIDDEN;
    logAndCountWarning(ex, httpStatus);
    var clientResponse = ex.toClientResponse();
    return new ResponseEntity<>(clientResponse, null, httpStatus);
  }

  /**
   * 401 Unauthorized
   *
   * @param ex The exception
   * @return The response entity
   */
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleException(AuthenticationException ex) {
    var httpStatus = HttpStatus.UNAUTHORIZED;
    logAndCountWarning(ex, httpStatus);
    var clientResponse = ex.toClientResponse();
    return new ResponseEntity<>(clientResponse, null, httpStatus);
  }

  /*
   * 404 Not Found
   *
   * @param ex The exception
   * @return The response entity
   */
  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorResponse> handleException(NotFoundException ex) {
    var httpStatus = HttpStatus.NOT_FOUND;
    logAndCountWarning(ex, httpStatus);
    var clientResponse = ex.toClientResponse();
    return new ResponseEntity<>(clientResponse, null, httpStatus);
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
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(Exception ex) {
    var httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    var internalServerErrorException =
        new InternalServerErrorException("Data integrity violation", ex);
    logAndCountError(internalServerErrorException, httpStatus);
    var clientResponse = internalServerErrorException.toClientResponse();
    return new ResponseEntity<>(clientResponse, null, httpStatus);
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
    var clientResponse = ex.toClientResponse();
    return new ResponseEntity<>(clientResponse, null, httpStatus);
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
                    new FieldError(
                        e.getField(),
                        e.getRejectedValue() == null ? null : e.getRejectedValue().toString(),
                        e.getDefaultMessage()))
            .toList();

    var httpStatus = HttpStatus.BAD_REQUEST;
    var fieldNames = fieldErrors.stream().map(f -> f.getFieldName()).toList();
    var validationException =
        new ValidationException(
            "Field validation error on fields: " + String.join(", ", fieldNames), ex, fieldErrors);

    logAndCountWarning(validationException, httpStatus);
    var clientResponse = validationException.toClientResponse();
    return handleExceptionInternal(
        validationException, clientResponse, headers, httpStatus, request);
  }

  /**
   * 404
   *
   * <p>When no handler is found for the request.
   *
   * @param ex The exception
   * @param headers The headers
   * @param status The status
   * @param request The request
   */
  @Override
  protected ResponseEntity<Object> handleNoHandlerFoundException(
      @NotNull NoHandlerFoundException ex,
      @NotNull HttpHeaders headers,
      @NotNull HttpStatusCode status,
      @NotNull WebRequest request) {
    var httpStatus = HttpStatus.NOT_FOUND;
    var uri =
        (request instanceof ServletWebRequest servletWebRequest)
            ? servletWebRequest.getRequest().getRequestURI()
            : request.getDescription(false);
    var notFoundException = new NotFoundException("No handler found: " + uri, ex);
    logAndCountWarning(notFoundException, httpStatus);
    var clientResponse = notFoundException.toClientResponse();
    return handleExceptionInternal(notFoundException, clientResponse, headers, httpStatus, request);
  }

  /**
   * 404
   *
   * <p>When a resource is not found.
   *
   * @param ex The exception
   * @param headers The headers
   * @param status The status
   * @param request The request
   */
  @Override
  protected ResponseEntity<Object> handleNoResourceFoundException(
      @NotNull NoResourceFoundException ex,
      @NotNull HttpHeaders headers,
      @NotNull HttpStatusCode status,
      @NotNull WebRequest request) {
    var httpStatus = HttpStatus.NOT_FOUND;
    var uri =
        (request instanceof ServletWebRequest servletWebRequest)
            ? servletWebRequest.getRequest().getRequestURI()
            : request.getDescription(false);
    var notFoundException = new NotFoundException("Resource not found: " + uri, ex);
    logAndCountWarning(notFoundException, httpStatus);
    var clientResponse = notFoundException.toClientResponse();
    return handleExceptionInternal(notFoundException, clientResponse, headers, httpStatus, request);
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
      var clientResponse = notFoundException.toClientResponse();
      return handleExceptionInternal(
          notFoundException, clientResponse, headers, httpStatus, request);
    }

    // Bad request
    else {
      // TODO: System.err.println(ex.getAllErrors());
      var httpStatus = HttpStatus.BAD_REQUEST;
      var badRequestException =
          new BadRequestException("Bad request: " + request.getDescription(false), ex);
      logAndCountWarning(badRequestException, httpStatus);
      var clientResponse = badRequestException.toClientResponse();
      return handleExceptionInternal(
          badRequestException, clientResponse, headers, httpStatus, request);
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
    var censoredBadRequestException = new BadRequestException("Failed to parse the request.", ex);
    var clientResponse = censoredBadRequestException.toClientResponse();
    return handleExceptionInternal(
        badRequestException, clientResponse, headers, httpStatus, request);
  }

  @Override
  protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
      HttpRequestMethodNotSupportedException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {

    var supportedMethods = ex.getSupportedHttpMethods();
    var methodNotAllowedException =
        new MethodNotAllowedException(
            "Method "
                + ex.getMethod()
                + " is not supported for this endpoint. Supported methods are: "
                + supportedMethods);

    // Add Allow header with supported methods
    if (supportedMethods != null) {
      headers.setAllow(supportedMethods);
    }

    var errorResponse = methodNotAllowedException.toClientResponse();
    return handleExceptionInternal(
        methodNotAllowedException, errorResponse, headers, status, request);
  }
}

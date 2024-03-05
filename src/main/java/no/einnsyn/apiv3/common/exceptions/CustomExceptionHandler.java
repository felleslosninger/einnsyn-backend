package no.einnsyn.apiv3.common.exceptions;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
@Slf4j
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {

  MeterRegistry meterRegistry;

  public CustomExceptionHandler(MeterRegistry meterRegistry) {
    super();
    this.meterRegistry = meterRegistry;
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> exception(Exception ex) {
    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

    if (ex instanceof IllegalArgumentException) {
      status = HttpStatus.BAD_REQUEST;
    } else if (ex instanceof UnauthorizedException) {
      status = HttpStatus.UNAUTHORIZED;
    } else if (ex instanceof AccessDeniedException) {
      status = HttpStatus.FORBIDDEN;
    } else {
      log.error(ex.getMessage(), ex);
    }

    // Count error
    meterRegistry.counter("ein_error", "error", ex.getClass().getSimpleName()).increment();

    final BadRequestResponse apiError = new BadRequestResponse(status, ex.getMessage(), null, null);
    return new ResponseEntity<>(apiError, null, status);
  }

  /** Input field validation errors. */
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {

    List<FieldValidationError> fieldErrors =
        ex.getFieldErrors().stream()
            .map(
                e ->
                    new FieldValidationError(
                        e.getField(),
                        e.getRejectedValue() == null ? null : e.getRejectedValue().toString(),
                        e.getDefaultMessage()))
            .toList();

    // Count error
    meterRegistry.counter("ein_error", "error", ex.getClass().getSimpleName()).increment();

    final BadRequestResponse apiError =
        new BadRequestResponse(HttpStatus.BAD_REQUEST, null, null, fieldErrors);

    return handleExceptionInternal(ex, apiError, headers, apiError.getStatus(), request);
  }

  /** Handle path-variable validation errors. Most likely non-existent IDs. */
  @Override
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {

    boolean notFound = false;
    for (MessageSourceResolvable error : ex.getAllErrors()) {
      var defaultMessage = error.getDefaultMessage();
      if (defaultMessage != null && defaultMessage.contains("not found")) {
        notFound = true;
      }
    }

    // Count error
    meterRegistry.counter("ein_error", "error", ex.getClass().getSimpleName()).increment();

    BadRequestResponse apiError;
    if (notFound) {
      apiError =
          new BadRequestResponse(
              HttpStatus.NOT_FOUND, "The requested resource was not found.", null, null);
    } else {
      apiError = new BadRequestResponse(HttpStatus.BAD_REQUEST, null, null, null);
    }

    return handleExceptionInternal(ex, apiError, headers, apiError.getStatus(), request);
  }

  /** JSON parse errors */
  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {

    // Count error
    meterRegistry.counter("ein_error", "error", ex.getClass().getSimpleName()).increment();

    List<String> errors = List.of(ex.getLocalizedMessage()); // TODO: We don't want to expose error
    // messages to the client.
    final BadRequestResponse apiError =
        new BadRequestResponse(
            HttpStatus.BAD_REQUEST, "Could not parse the request body.", errors, null);

    return handleExceptionInternal(ex, apiError, headers, apiError.getStatus(), request);
  }
}

package no.einnsyn.apiv3.responses;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {

  /**
   * Input field validation errors.
   */
  protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
      HttpHeaders headers, HttpStatusCode status, WebRequest request) {

    List<FieldValidationError> fieldErrors = ex.getFieldErrors().stream().map(e -> {
      return new FieldValidationError(e.getField(),
          e.getRejectedValue() == null ? null : e.getRejectedValue().toString(),
          e.getDefaultMessage());
    }).collect(Collectors.toList());

    final ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, null, null, fieldErrors);

    return handleExceptionInternal(ex, apiError, headers, apiError.getStatus(), request);
  }


  /**
   * Handle path-variable validation errors. Most likely non-existent IDs.
   */
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status,
      WebRequest request) {

    boolean notFound = false;
    for (MessageSourceResolvable error : ex.getAllErrors()) {
      if (error.getDefaultMessage().contains("not found")) {
        notFound = true;
      }
    }

    ApiError apiError;
    if (notFound) {
      apiError =
          new ApiError(HttpStatus.NOT_FOUND, "The requested resource was not found.", null, null);
    } else {
      apiError = new ApiError(HttpStatus.BAD_REQUEST, null, null, null);
    }

    return handleExceptionInternal(ex, apiError, headers, apiError.getStatus(), request);
  }


  /**
   * JSON parse errors
   */
  protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
      HttpHeaders headers, HttpStatusCode status, WebRequest request) {

    // List<String> errors = List.of(ex.getLocalizedMessage()); // We don't want to expose error
    // messages to the client.
    final ApiError apiError =
        new ApiError(HttpStatus.BAD_REQUEST, "Could not parse the request body.", null, null);

    return handleExceptionInternal(ex, apiError, headers, apiError.getStatus(), request);
  }

}

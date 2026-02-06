package no.einnsyn.backend.common.exceptions;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import no.einnsyn.backend.common.exceptions.models.BadRequestException;
import no.einnsyn.backend.common.exceptions.models.NotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Test controller to trigger HandlerMethodValidationException bad request scenarios. This
 * controller has direct validation constraints on method parameters (not bean validation on DTOs),
 * which triggers HandlerMethodValidationException when validation fails in Spring Boot 3.
 */
@RestController
@Profile("test")
public class ErrorResponseTestController {

  /**
   * Endpoint with @Min constraint on path variable. When a value less than 1 is passed, validation
   * fails with "must be greater than or equal to 1" - which doesn't contain "not found".
   */
  @GetMapping("/validationTest/minValue/{value}")
  public ResponseEntity<String> testMinValue(@PathVariable @Min(1) Integer value) {
    return ResponseEntity.ok("Value: " + value);
  }

  /**
   * Endpoint with @Pattern constraint on path variable. When an invalid pattern is passed,
   * validation fails with "must match ..." - which doesn't contain "not found".
   */
  @GetMapping("/validationTest/pattern/{value}")
  public ResponseEntity<String> testPattern(
      @PathVariable @Pattern(regexp = "^[a-z]+$", message = "Must contain only lowercase letters")
          String value) {
    return ResponseEntity.ok("Value: " + value);
  }

  /**
   * Endpoint with @Min constraint on request param. When a value less than 1 is passed, validation
   * fails with "must be greater than or equal to 1" - which doesn't contain "not found".
   */
  @GetMapping("/validationTest/minParam")
  public ResponseEntity<String> testMinParam(@RequestParam @Min(1) Integer value) {
    return ResponseEntity.ok("Value: " + value);
  }

  /** Endpoint with multiple validation constraints. Both can fail without "not found" messages. */
  @GetMapping("/validationTest/multiple/{id}")
  public ResponseEntity<String> testMultiple(
      @PathVariable @Min(1) Integer id, @RequestParam @Min(0) Integer count) {
    return ResponseEntity.ok("Id: " + id + ", Count: " + count);
  }

  /** Endpoint that throws a RuntimeException to trigger the generic Exception handler. */
  @GetMapping("/validationTest/internalError")
  public ResponseEntity<String> testInternalError() {
    throw new RuntimeException("Simulated internal error");
  }

  /** Endpoint that throws a TransactionSystemException to trigger the transaction error handler. */
  @GetMapping("/validationTest/transactionError")
  public ResponseEntity<String> testTransactionError() {
    throw new TransactionSystemException("Simulated transaction error");
  }

  /**
   * Endpoint that throws a TransactionSystemException with an EInnsynException root cause to
   * trigger the if-branch in the transaction error handler.
   */
  @GetMapping("/validationTest/transactionErrorWithCause")
  public ResponseEntity<String> testTransactionErrorWithCause() {
    throw new TransactionSystemException(
        "Simulated transaction error", new BadRequestException("Wrapped bad request"));
  }

  /** Endpoint that throws an IllegalArgumentException. */
  @GetMapping("/validationTest/illegalArgument")
  public ResponseEntity<String> testIllegalArgument() {
    throw new IllegalArgumentException("Simulated illegal argument");
  }

  /** Endpoint that throws a NotFoundException to trigger the NotFoundException handler. */
  @GetMapping("/validationTest/notFound")
  public ResponseEntity<String> testNotFound() throws NotFoundException {
    throw new NotFoundException("Simulated not found");
  }

  /** Endpoint that throws a DataIntegrityViolationException. */
  @GetMapping("/validationTest/dataIntegrityViolation")
  public ResponseEntity<String> testDataIntegrityViolation() {
    throw new DataIntegrityViolationException("Simulated data integrity violation");
  }

  /** Endpoint that throws a NoHandlerFoundException. */
  @GetMapping("/validationTest/noHandlerFound")
  public ResponseEntity<String> testNoHandlerFound() throws NoHandlerFoundException {
    throw new NoHandlerFoundException("GET", "/nonexistent", new HttpHeaders());
  }

  /**
   * Endpoint with @Min constraint with a blank message. When validation fails, the error has a
   * blank defaultMessage, triggering the codes fallback in resolveValidationMessage.
   */
  @GetMapping("/validationTest/blankMessage/{value}")
  public ResponseEntity<String> testBlankMessage(@PathVariable @Min(value = 1, message = "") Integer value) {
    return ResponseEntity.ok("Value: " + value);
  }
}

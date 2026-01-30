package no.einnsyn.backend.common.exceptions;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test controller to trigger HandlerMethodValidationException bad request scenarios. This
 * controller has direct validation constraints on method parameters (not bean validation on DTOs),
 * which triggers HandlerMethodValidationException when validation fails in Spring Boot 3.
 */
@RestController
@Profile("test")
public class ErrorResponseController {

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
}

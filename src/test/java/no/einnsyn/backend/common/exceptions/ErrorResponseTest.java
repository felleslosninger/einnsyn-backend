package no.einnsyn.backend.common.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.exceptions.models.AuthenticationException;
import no.einnsyn.backend.common.exceptions.models.AuthorizationException;
import no.einnsyn.backend.common.exceptions.models.BadRequestException;
import no.einnsyn.backend.common.exceptions.models.ConflictException;
import no.einnsyn.backend.common.exceptions.models.InternalServerErrorException;
import no.einnsyn.backend.common.exceptions.models.MethodNotAllowedException;
import no.einnsyn.backend.common.exceptions.models.NotFoundException;
import no.einnsyn.backend.common.exceptions.models.ValidationException;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ErrorResponseTest extends EinnsynControllerTestBase {

  @Test
  void testNotFound() throws Exception {
    var response = get("/notfound");
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    var errorResponse = gson.fromJson(response.getBody(), NotFoundException.ClientResponse.class);
    assertEquals("notFound", errorResponse.getType());
    assertNotNull(errorResponse.getMessage());

    response = get("/journalpost/id_foo");
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    errorResponse = gson.fromJson(response.getBody(), NotFoundException.ClientResponse.class);
    assertEquals("notFound", errorResponse.getType());
    assertNotNull(errorResponse.getMessage());

    response = patch("/journalpost/id_bar", getJournalpostJSON());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    errorResponse = gson.fromJson(response.getBody(), NotFoundException.ClientResponse.class);
    assertEquals("notFound", errorResponse.getType());
    assertNotNull(errorResponse.getMessage());
  }

  @Test
  void testMethodNotAllowedException() throws Exception {
    var journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("foo", "bar");
    var response = post("/journalpost", journalpostJSON);
    assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
    assertTrue(response.getBody().contains("methodNotAllowed"));
    var errorResponse =
        gson.fromJson(response.getBody(), MethodNotAllowedException.ClientResponse.class);
    assertEquals("methodNotAllowed", errorResponse.getType());
    assertNotNull(errorResponse.getMessage());
  }

  @Test
  void testValidationException() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(arkivDTO.getId());

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    assertNotNull(arkivdelDTO.getId());

    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("saksaar", "1200");
    saksmappeJSON.put("sakssekvensnummer", -1);
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertTrue(response.getBody().contains("validationError"));
    var errorResponse = gson.fromJson(response.getBody(), ValidationException.ClientResponse.class);
    assertEquals("validationError", errorResponse.getType());
    assertNotNull(errorResponse.getMessage());
    assertNotNull(errorResponse.getFieldError());
    assertEquals(2, errorResponse.getFieldError().size());

    response = delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testAuthenticationException() throws Exception {
    var response = post("/arkiv", getArkivJSON(), "secret_invalidApiKey");
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertTrue(response.getBody().contains("authenticationError"));
    var errorResponse =
        gson.fromJson(response.getBody(), AuthenticationException.ClientResponse.class);
    assertEquals("authenticationError", errorResponse.getType());
    assertNotNull(errorResponse.getMessage());
  }

  @Test
  void testAuthorizationException() throws Exception {
    var response = postAnon("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertTrue(response.getBody().contains("authorizationError"));
    var errorResponse =
        gson.fromJson(response.getBody(), AuthorizationException.ClientResponse.class);
    assertEquals("authorizationError", errorResponse.getType());
    assertNotNull(errorResponse.getMessage());
  }

  @Test
  void testConflictException() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(arkivDTO.getId());

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    assertNotNull(arkivdelDTO.getId());

    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("externalId", "foo");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(saksmappeDTO.getId());

    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertTrue(response.getBody().contains("conflict"));
    var errorResponse = gson.fromJson(response.getBody(), ConflictException.ClientResponse.class);
    assertEquals("conflict", errorResponse.getType());
    assertNotNull(errorResponse.getMessage());

    response = delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  /**
   * Tests validation errors on query parameters. Query parameter validation on beans goes through
   * handleMethodArgumentNotValid and returns validationError responses.
   */
  @Test
  void testQueryParameterValidationError() throws Exception {
    // Test with limit below minimum (violates @Min(1))
    var response = get("/arkiv?limit=0");
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    var errorResponse = gson.fromJson(response.getBody(), ValidationException.ClientResponse.class);
    assertEquals("validationError", errorResponse.getType());
    assertEquals("Field validation error on fields: \"limit\": \"0\"", errorResponse.getMessage());
    assertNotNull(errorResponse.getFieldError());
    assertTrue(
        errorResponse.getFieldError().stream().anyMatch(e -> "limit".equals(e.getFieldName())));

    // Test with limit above maximum (violates @Max(100))
    response = get("/arkiv?limit=101");
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    errorResponse = gson.fromJson(response.getBody(), ValidationException.ClientResponse.class);
    assertEquals("validationError", errorResponse.getType());
    assertEquals(
        "Field validation error on fields: \"limit\": \"101\"", errorResponse.getMessage());
    assertNotNull(errorResponse.getFieldError());
    assertTrue(
        errorResponse.getFieldError().stream().anyMatch(e -> "limit".equals(e.getFieldName())));

    // Test with multiple invalid parameters
    response = get("/arkiv?limit=0&sortOrder=invalid");
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    errorResponse = gson.fromJson(response.getBody(), ValidationException.ClientResponse.class);
    assertEquals("validationError", errorResponse.getType());
    assertNotNull(errorResponse.getMessage());
    assertTrue(errorResponse.getMessage().contains("Field validation error on fields:"));
    assertNotNull(errorResponse.getMessage());
    assertNotNull(errorResponse.getFieldError());
    assertEquals(2, errorResponse.getFieldError().size());
    assertTrue(
        errorResponse.getFieldError().stream().anyMatch(e -> "limit".equals(e.getFieldName())));
    assertTrue(
        errorResponse.getFieldError().stream().anyMatch(e -> "sortOrder".equals(e.getFieldName())));
  }

  /**
   * Tests Bad Request responses from HandlerMethodValidationException. This tests the
   * buildValidationMessages and summarizeValidationMessages functionality in
   * EInnsynExceptionHandler when validation fails on method parameters (path variables, request
   * params) with errors that don't contain "not found".
   */
  @Test
  void testBadRequestFromMethodValidation() throws Exception {
    // Test with @Min constraint violation on path variable (value < 1)
    var response = get("/validationTest/minValue/0");
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    var errorResponse = gson.fromJson(response.getBody(), BadRequestException.ClientResponse.class);
    assertEquals("badRequest", errorResponse.getType());
    assertNotNull(errorResponse.getMessage());
    assertEquals(
        "Validation failed: must be greater than or equal to 1", errorResponse.getMessage());

    // Test with @Min constraint violation on path variable (negative value)
    response = get("/validationTest/minValue/-5");
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    errorResponse = gson.fromJson(response.getBody(), BadRequestException.ClientResponse.class);
    assertEquals("badRequest", errorResponse.getType());
    assertNotNull(errorResponse.getMessage());
    assertEquals(
        "Validation failed: must be greater than or equal to 1", errorResponse.getMessage());

    // Test with @Pattern constraint violation on path variable
    response = get("/validationTest/pattern/ABC123");
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    errorResponse = gson.fromJson(response.getBody(), BadRequestException.ClientResponse.class);
    assertEquals("badRequest", errorResponse.getType());
    assertNotNull(errorResponse.getMessage());
    assertEquals(
        "Validation failed: Must contain only lowercase letters", errorResponse.getMessage());

    // Test with @Min constraint violation on request param
    response = get("/validationTest/minParam?value=0");
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    errorResponse = gson.fromJson(response.getBody(), BadRequestException.ClientResponse.class);
    assertEquals("badRequest", errorResponse.getType());
    assertNotNull(errorResponse.getMessage());
    assertEquals(
        "Validation failed: must be greater than or equal to 1", errorResponse.getMessage());

    // Test with multiple validation errors on different parameters
    response = get("/validationTest/multiple/0?count=-1");
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    errorResponse = gson.fromJson(response.getBody(), BadRequestException.ClientResponse.class);
    assertEquals("badRequest", errorResponse.getType());
    assertNotNull(errorResponse.getMessage());
    assertTrue(errorResponse.getMessage().contains("Validation failed"));
    assertTrue(errorResponse.getMessage().contains("must be greater than or equal to 1"));
    assertTrue(errorResponse.getMessage().contains("must be greater than or equal to 0"));
  }

  @Test
  void testInternalServerError() throws Exception {
    var response = get("/validationTest/internalError");
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    var errorResponse =
        gson.fromJson(response.getBody(), InternalServerErrorException.ClientResponse.class);
    assertEquals("internalServerError", errorResponse.getType());
    assertNotNull(errorResponse.getMessage());
  }

  @Test
  void testTransactionSystemException() throws Exception {
    // TransactionSystemException without EInnsynException root cause (else-branch)
    var response = get("/validationTest/transactionError");
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    var errorResponse =
        gson.fromJson(response.getBody(), InternalServerErrorException.ClientResponse.class);
    assertEquals("internalServerError", errorResponse.getType());
    assertNotNull(errorResponse.getMessage());

    // TransactionSystemException with EInnsynException root cause (if-branch)
    response = get("/validationTest/transactionErrorWithCause");
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    errorResponse =
        gson.fromJson(response.getBody(), InternalServerErrorException.ClientResponse.class);
    assertEquals("internalServerError", errorResponse.getType());
    assertNotNull(errorResponse.getMessage());
  }

  @Test
  void testIllegalArgumentException() throws Exception {
    var response = get("/validationTest/illegalArgument");
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    var errorResponse = gson.fromJson(response.getBody(), BadRequestException.ClientResponse.class);
    assertEquals("badRequest", errorResponse.getType());
    assertNotNull(errorResponse.getMessage());
  }

  @Test
  void testNotFoundFromMethodValidation() throws Exception {
    var response = get("/validationTest/notFound");
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    var errorResponse = gson.fromJson(response.getBody(), NotFoundException.ClientResponse.class);
    assertEquals("notFound", errorResponse.getType());
    assertNotNull(errorResponse.getMessage());
  }

  @Test
  void testDataIntegrityViolationException() throws Exception {
    var response = get("/validationTest/dataIntegrityViolation");
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    var errorResponse =
        gson.fromJson(response.getBody(), InternalServerErrorException.ClientResponse.class);
    assertEquals("internalServerError", errorResponse.getType());
    assertNotNull(errorResponse.getMessage());
  }

  @Test
  void testNoHandlerFoundException() throws Exception {
    var response = get("/validationTest/noHandlerFound");
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    var errorResponse = gson.fromJson(response.getBody(), NotFoundException.ClientResponse.class);
    assertEquals("notFound", errorResponse.getType());
    assertNotNull(errorResponse.getMessage());
  }

  @Test
  void testBlankValidationMessage() throws Exception {
    var response = get("/validationTest/blankMessage/0");
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    var errorResponse = gson.fromJson(response.getBody(), BadRequestException.ClientResponse.class);
    assertEquals("badRequest", errorResponse.getType());
    assertNotNull(errorResponse.getMessage());
    assertTrue(errorResponse.getMessage().contains("Validation failed"));
  }

  @Test
  void testManyValidationErrors() throws Exception {
    var response = get("/validationTest/manyErrors?a=0&b=0&c=0&d=0&e=0&f=0");
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    var errorResponse = gson.fromJson(response.getBody(), BadRequestException.ClientResponse.class);
    assertEquals("badRequest", errorResponse.getType());
    assertNotNull(errorResponse.getMessage());
    assertTrue(errorResponse.getMessage().contains("Validation failed"));
    assertTrue(errorResponse.getMessage().contains("and 1 more"));
  }
}

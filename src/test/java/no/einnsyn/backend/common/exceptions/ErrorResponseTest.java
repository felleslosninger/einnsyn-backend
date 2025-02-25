package no.einnsyn.backend.common.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.exceptions.models.AuthenticationException;
import no.einnsyn.backend.common.exceptions.models.AuthorizationException;
import no.einnsyn.backend.common.exceptions.models.ConflictException;
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
}

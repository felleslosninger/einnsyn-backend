package no.einnsyn.backend.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.exceptions.models.ValidationException;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UnknownQueryParametersControllerTest extends EinnsynControllerTestBase {

  @Test
  void testUnknownQueryParameterOnList() throws Exception {
    var response = get("/arkiv?foo=bar");
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    var error = gson.fromJson(response.getBody(), ValidationException.ClientResponse.class);
    assertEquals("validationError", error.getType());
    assertEquals("Unknown query parameter: foo", error.getMessage());
    assertEquals(1, error.getFieldError().size());
    var fieldError = error.getFieldError().getFirst();
    assertEquals("foo", fieldError.getFieldName());
    assertEquals("bar", fieldError.getValue());
    assertEquals("Unknown query parameter", fieldError.getMessage());
  }

  @Test
  void testMultipleUnknownQueryParametersAreAllReported() throws Exception {
    var response = get("/arkiv?foo=1&limit=5&bar=2");
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    var error = gson.fromJson(response.getBody(), ValidationException.ClientResponse.class);
    assertEquals("validationError", error.getType());
    assertEquals("Unknown query parameters: foo, bar", error.getMessage());
    assertEquals(2, error.getFieldError().size());
    assertTrue(error.getFieldError().stream().anyMatch(e -> "foo".equals(e.getFieldName())));
    assertTrue(error.getFieldError().stream().anyMatch(e -> "bar".equals(e.getFieldName())));
  }

  @Test
  void testKnownQueryParametersAreAccepted() throws Exception {
    var response = get("/arkiv?limit=5&sortOrder=asc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testPathVariablesDoNotCountAsQueryParameters() throws Exception {
    var response = get("/enhet/" + journalenhetId + "/arkiv?limit=5");
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testUnknownQueryParameterOnGet() throws Exception {
    var response = get("/enhet/" + journalenhetId + "?foo=bar");
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    var error = gson.fromJson(response.getBody(), ValidationException.ClientResponse.class);
    assertEquals("Unknown query parameter: foo", error.getMessage());
  }

  @Test
  void testUnknownQueryParameterOnSearchIsRejectedBeforeSearching() throws Exception {
    var response = getAnon("/search?query=foo&bar=baz");
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    var error = gson.fromJson(response.getBody(), ValidationException.ClientResponse.class);
    assertEquals("Unknown query parameter: bar", error.getMessage());
  }

  @Test
  void testQueryStringOnEndpointWithoutQueryParametersIsRejected() throws Exception {
    var arkivJSON = getArkivJSON();
    var response = post("/arkiv?foo=bar", arkivJSON);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    var error = gson.fromJson(response.getBody(), ValidationException.ClientResponse.class);
    assertEquals("Unknown query parameter: foo", error.getMessage());

    response = post("/arkiv", arkivJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    delete("/arkiv/" + arkivDTO.getId());
  }
}

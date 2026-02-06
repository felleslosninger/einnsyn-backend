package no.einnsyn.backend.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EnumValidationControllerTest extends EinnsynControllerTestBase {

  @Test
  void testEnhetEnum() throws Exception {
    var enhetJSON = getEnhetJSON();
    enhetJSON.put("enhetstype", "INVALID");
    var response = post("/enhet/" + journalenhetId + "/underenhet", enhetJSON);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    for (var enumValue : EnhetDTO.EnhetstypeEnum.values()) {
      enhetJSON.put("enhetstype", enumValue.name());
      response = post("/enhet/" + journalenhetId + "/underenhet", enhetJSON);
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      var responseDTO = gson.fromJson(response.getBody(), EnhetDTO.class);
      delete("/enhet/" + responseDTO.getId());
    }
  }

  @Test
  void testInvalidItemInListIsRejectedByController() throws Exception {
    var response = get("/search?entity=INVALID&entity=Journalpost");
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void testValidEnumNumberIsValidatedByController() throws Exception {
    var body = new JSONObject().put("value", 0);
    var response = post("/validation-tests/valid-enum/number", body);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    body.put("value", 999);
    response = post("/validation-tests/valid-enum/number", body);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void testValidEnumInstanceIsValidatedByController() throws Exception {
    var body = new JSONObject().put("value", EnhetDTO.EnhetstypeEnum.ADMINISTRATIVENHET.name());
    var response = post("/validation-tests/valid-enum/instance", body);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}

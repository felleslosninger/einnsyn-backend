package no.einnsyn.apiv3.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.enhet.models.EnhetDTO;
import no.einnsyn.apiv3.entities.enhet.models.EnhetstypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class EnumValidationControllerTest extends EinnsynControllerTestBase {

  @Test
  void testEnhetEnum() throws Exception {
    var enhetJSON = getEnhetJSON();
    enhetJSON.put("enhetstype", "INVALID");
    var response = post("/enhet/" + journalenhetId + "/underenhet", enhetJSON);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    for (var enumValue : EnhetstypeEnum.values()) {
      enhetJSON.put("enhetstype", enumValue.name());
      response = post("/enhet/" + journalenhetId + "/underenhet", enhetJSON);
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      var responseDTO = gson.fromJson(response.getBody(), EnhetDTO.class);
      delete("/enhet/" + responseDTO.getId());
    }
  }
}

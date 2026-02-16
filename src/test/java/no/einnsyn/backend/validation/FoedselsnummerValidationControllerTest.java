package no.einnsyn.backend.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FoedselsnummerValidationControllerTest extends EinnsynControllerTestBase {

  private ArkivDTO arkivDTO;
  ArkivdelDTO arkivdelDTO;

  List<String> validFoedselsnummers = FoedselsnummerTestData.validFoedselsnummers();
  List<String> invalidFoedselsnummers = FoedselsnummerTestData.invalidFoedselsnummers();

  @BeforeAll
  void setUp() throws Exception {
    var arkivJSON = getArkivJSON();
    var response = post("/arkiv", arkivJSON);
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
  }

  @AfterAll
  void tearDown() throws Exception {
    delete("/arkiv/" + arkivDTO.getId());
  }

  @Test
  void checkValidFoedselsnummers() throws Exception {
    for (var fnr : validFoedselsnummers) {
      var saksmappeJSON = getSaksmappeJSON();
      saksmappeJSON.put("offentligTittel", "foo " + fnr + " bar");
      var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), fnr + " should match as SSN");
    }
  }

  @Test
  void checkInvalidFoedselsnummers() throws Exception {
    for (var fnr : invalidFoedselsnummers) {
      var saksmappeJSON = getSaksmappeJSON();
      saksmappeJSON.put("offentligTittel", "foo " + fnr + " bar");
      var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
      assertEquals(HttpStatus.CREATED, response.getStatusCode(), fnr + " should not match as SSN");
      var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
      delete("/saksmappe/" + saksmappeDTO.getId());
    }
  }

  @Test
  void checkNoSSNListValidation() throws Exception {
    var body =
        new JSONObject()
            .put(
                "values",
                new JSONArray(List.of("foo", FoedselsnummerTestData.REFERENCE_FOEDSELSNUMMER)));
    var response = post("/validation-tests/nossn/list", body);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }
}

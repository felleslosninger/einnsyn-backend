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

  List<String> validFoedselsnummers =
      List.of(
          "05063826601",
          "31085314494",
          "25038738758",
          "03042535418",
          "04041870708",
          "08061939521",
          "28077848004",
          "14051802811",
          "02120818212",
          "10046038385",
          "050638 26601",
          "0506.38.26601",
          "0506 38 26601");

  List<String> invalidFoedselsnummers =
      List.of(
          "050638266010", // Extra integer
          "3108531a449", // Contains non-integer character
          "32038738758", // 32 days in month
          "03042535428", // Wrong k1
          "04041870709", // Wrong k2
          "08131939521", // 13 months
          "2807784800", // Too few integers
          "15051802111", // Invalid checksum
          "02120818202", // Invalid checksum
          "10046038375", // Invalid checksum
          "20060810012", // Invalid checksum
          "005063826601", // Valid, with leading number
          "050638266010", // Valid, with trailing number
          "13da68dd-6c0c-591f-a183-05063826601a", // Valid, but part of an UUID
          "13da68dd-6c0c-591f-a183-a05063826601", // Valid, but part of an UUID
          "D1A11529277DADC9BF7EACEBC12072480617B4E29C2F82BFB5C9D701A4E8C11B");

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
    var body = new JSONObject().put("values", new JSONArray(List.of("foo", "05063826601")));
    var response = post("/validation-tests/nossn/list", body);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }
}

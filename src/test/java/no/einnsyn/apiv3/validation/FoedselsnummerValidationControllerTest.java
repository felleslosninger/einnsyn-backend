package no.einnsyn.apiv3.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class FoedselsnummerValidationControllerTest extends EinnsynControllerTestBase {

  private ArkivDTO arkivDTO;

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
          "10046038385");

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
          "20060810012" // Invalid checksum
          );

  @BeforeAll
  void setUp() throws Exception {
    var arkivJSON = getArkivJSON();
    var response = post("/arkiv", arkivJSON);
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
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
      var response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
  }

  @Test
  void checkInvalidFoedselsnummers() throws Exception {
    for (var fnr : invalidFoedselsnummers) {
      var saksmappeJSON = getSaksmappeJSON();
      saksmappeJSON.put("offentligTittel", "foo " + fnr + " bar");
      var response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", saksmappeJSON);
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
      delete("/saksmappe/" + saksmappeDTO.getId());
    }
  }
}

package no.einnsyn.backend.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DateValidationControllerTest extends EinnsynControllerTestBase {

  @Test
  void testDate() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    var dateMap =
        new HashMap<String, Boolean>() {
          {
            put("2024-02-20", true);
            put("2024-02-29", true);
            put("2023-02-29", false);
            put("2024-02-30", false);
            put("2024-29-02", false);
            put("2024-05-31", true);
            put("2024-06-31", false);
          }
        };

    for (var testEntry : dateMap.entrySet()) {
      var saksmappeJSON = getSaksmappeJSON();
      saksmappeJSON.put("saksdato", testEntry.getKey());
      response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
      if (testEntry.getValue()) {
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
      } else {
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      }
    }

    delete("/arkiv/" + arkivDTO.getId());
  }

  @Test
  void testDateTime() throws Exception {
    var arkivJSON = getArkivJSON();
    var response = post("/arkiv", arkivJSON);
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    var tests =
        new HashMap<String, String>() {
          {
            put("2024-02-20T15:45:00Z", "2024-02-20T15:45:00Z"); // UTC
            put("2024-02-20T16:45:00+01:00", "2024-02-20T15:45:00Z"); // UTC+1
            put("2024-02-20T14:45:00-01:00", "2024-02-20T15:45:00Z"); // UTC-1
            put("2024-02-20T23:59:00-02:00", "2024-02-21T01:59:00Z"); // Day change due to UTC-2
            put("2024-02-20T00:01:00+02:00", "2024-02-19T22:01:00Z"); // Previous day due to UTC+2
            put(
                "2024-12-31T23:45:00-14:00",
                "2025-01-01T13:45:00Z"); // New Year's change due to UTC-14
            put("2024-01-01T00:15:00+14:00", "2023-12-31T10:15:00Z"); // Previous year due to UTC+14
            put("2024-02-29T12:00:00Z", "2024-02-29T12:00:00Z"); // Leap day
            put("2024-06-30T23:30:00+05:30", "2024-06-30T18:00:00Z"); // UTC+5:30
            put("2024-01-01T00:00:00-10:30", "2024-01-01T10:30:00Z"); // UTC-10:30
          }
        };

    // Moetemappe
    for (var testEntry : tests.entrySet()) {
      var moetemappeJSON = getMoetemappeJSON();
      moetemappeJSON.put("moetedato", testEntry.getKey());
      response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      var mmDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      assertEquals(testEntry.getValue(), mmDTO.getMoetedato());
    }

    delete("/arkiv/" + arkivDTO.getId());
  }
}

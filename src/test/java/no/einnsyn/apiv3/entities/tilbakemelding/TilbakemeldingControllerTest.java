package no.einnsyn.apiv3.entities.tilbakemelding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.tilbakemelding.models.TilbakemeldingDTO;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TilbakemeldingControllerTest extends EinnsynControllerTestBase {

  @Test
  void insertTilbakemelding() throws Exception {
    JSONObject tilbakemeldingJSON = getTilbakemeldingJSON();
    ResponseEntity<String> tilbakemeldingResponse = post("/tilbakemelding", tilbakemeldingJSON);
    assertEquals(HttpStatus.CREATED, tilbakemeldingResponse.getStatusCode());
    TilbakemeldingDTO insertedTilbakemeldingDTO =
        gson.fromJson(tilbakemeldingResponse.getBody(), TilbakemeldingDTO.class);
    assertEquals(
        tilbakemeldingJSON.get("messageFromUser"), insertedTilbakemeldingDTO.getMessageFromUser());
    assertEquals(tilbakemeldingJSON.get("path"), insertedTilbakemeldingDTO.getPath());
    assertEquals(tilbakemeldingJSON.get("referer"), insertedTilbakemeldingDTO.getReferer());
    assertEquals(tilbakemeldingJSON.get("userAgent"), insertedTilbakemeldingDTO.getUserAgent());
    assertEquals(
        tilbakemeldingJSON.get("screenHeight"), insertedTilbakemeldingDTO.getScreenHeight());
    assertEquals(tilbakemeldingJSON.get("screenWidth"), insertedTilbakemeldingDTO.getScreenWidth());
    assertEquals(tilbakemeldingJSON.get("docHeight"), insertedTilbakemeldingDTO.getDocHeight());
    assertEquals(tilbakemeldingJSON.get("docWidth"), insertedTilbakemeldingDTO.getDocWidth());
    assertEquals(tilbakemeldingJSON.get("winHeight"), insertedTilbakemeldingDTO.getWinHeight());
    assertEquals(tilbakemeldingJSON.get("winWidth"), insertedTilbakemeldingDTO.getWinWidth());
    assertEquals(tilbakemeldingJSON.get("scrollX"), insertedTilbakemeldingDTO.getScrollX());
    assertEquals(tilbakemeldingJSON.get("scrollY"), insertedTilbakemeldingDTO.getScrollY());
    assertEquals(
        tilbakemeldingJSON.get("userSatisfied"), insertedTilbakemeldingDTO.getUserSatisfied());
    assertEquals(
        tilbakemeldingJSON.get("handledByAdmin"), insertedTilbakemeldingDTO.getHandledByAdmin());
    assertEquals(
        tilbakemeldingJSON.get("adminComment"), insertedTilbakemeldingDTO.getAdminComment());
    String tilbakemeldingId = insertedTilbakemeldingDTO.getId();

    // Check that normal users can't get the new Tilbakemelding
    tilbakemeldingResponse = get("/tilbakemelding/" + tilbakemeldingId);
    assertEquals(HttpStatus.FORBIDDEN, tilbakemeldingResponse.getStatusCode());

    // Check that admins can get the new Tilbakemelding from the API
    tilbakemeldingResponse = getAdmin("/tilbakemelding/" + tilbakemeldingId);
    assertEquals(HttpStatus.OK, tilbakemeldingResponse.getStatusCode());

    // Check that normal users can't update the Tilbakemelding
    tilbakemeldingJSON.put("messageFromUser", "Not so happy");
    tilbakemeldingResponse = patch("/tilbakemelding/" + tilbakemeldingId, tilbakemeldingJSON);
    assertEquals(HttpStatus.FORBIDDEN, tilbakemeldingResponse.getStatusCode());

    // Check that admins can update the Tilbakemelding
    tilbakemeldingResponse = patchAdmin("/tilbakemelding/" + tilbakemeldingId, tilbakemeldingJSON);
    assertEquals(HttpStatus.OK, tilbakemeldingResponse.getStatusCode());
    insertedTilbakemeldingDTO =
        gson.fromJson(tilbakemeldingResponse.getBody(), TilbakemeldingDTO.class);
    assertEquals(
        tilbakemeldingJSON.get("messageFromUser"), insertedTilbakemeldingDTO.getMessageFromUser());

    // Check that normal users can't delete the Tilbakemelding
    tilbakemeldingResponse = delete("/tilbakemelding/" + tilbakemeldingId);
    assertEquals(HttpStatus.FORBIDDEN, tilbakemeldingResponse.getStatusCode());

    // Check that admins can delete the Tilbakemelding
    tilbakemeldingResponse = deleteAdmin("/tilbakemelding/" + tilbakemeldingId);
    assertEquals(HttpStatus.OK, tilbakemeldingResponse.getStatusCode());

    // Check that the Tilbakemelding is deleted
    tilbakemeldingResponse = getAdmin("/tilbakemelding/" + tilbakemeldingId);
    assertEquals(HttpStatus.NOT_FOUND, tilbakemeldingResponse.getStatusCode());
  }
}

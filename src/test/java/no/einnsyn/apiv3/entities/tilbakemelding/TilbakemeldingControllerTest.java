package no.einnsyn.apiv3.entities.tilbakemelding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import no.einnsyn.apiv3.entities.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.tilbakemelding.models.TilbakemeldingJSON;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class TilbakemeldingControllerTest extends EinnsynControllerTestBase {

  @Test
  void insertTilbakemelding() throws Exception {
    JSONObject tilbakemeldingJSON = getTilbakemeldingJSON();
    ResponseEntity<String> tilbakemeldingResponse = post("/tilbakemelding", tilbakemeldingJSON);
    assertEquals(HttpStatus.CREATED, tilbakemeldingResponse.getStatusCode());
    TilbakemeldingJSON insertedTilbakemeldingJSON =
        gson.fromJson(tilbakemeldingResponse.getBody(), TilbakemeldingJSON.class);
    assertEquals(
        tilbakemeldingJSON.get("messageFromUser"), insertedTilbakemeldingJSON.getMessageFromUser());
    assertEquals(tilbakemeldingJSON.get("path"), insertedTilbakemeldingJSON.getPath());
    assertEquals(tilbakemeldingJSON.get("referer"), insertedTilbakemeldingJSON.getReferer());
    assertEquals(tilbakemeldingJSON.get("userAgent"), insertedTilbakemeldingJSON.getUserAgent());
    assertEquals(
        tilbakemeldingJSON.get("screenHeight"), insertedTilbakemeldingJSON.getScreenHeight());
    assertEquals(
        tilbakemeldingJSON.get("screenWidth"), insertedTilbakemeldingJSON.getScreenWidth());
    assertEquals(tilbakemeldingJSON.get("docHeight"), insertedTilbakemeldingJSON.getDocHeight());
    assertEquals(tilbakemeldingJSON.get("docWidth"), insertedTilbakemeldingJSON.getDocWidth());
    assertEquals(tilbakemeldingJSON.get("winHeight"), insertedTilbakemeldingJSON.getWinHeight());
    assertEquals(tilbakemeldingJSON.get("winWidth"), insertedTilbakemeldingJSON.getWinWidth());
    assertEquals(tilbakemeldingJSON.get("scrollX"), insertedTilbakemeldingJSON.getScrollX());
    assertEquals(tilbakemeldingJSON.get("scrollY"), insertedTilbakemeldingJSON.getScrollY());
    assertEquals(
        tilbakemeldingJSON.get("userSatisfied"), insertedTilbakemeldingJSON.getUserSatisfied());
    assertEquals(
        tilbakemeldingJSON.get("handledByAdmin"), insertedTilbakemeldingJSON.getHandledByAdmin());
    assertEquals(
        tilbakemeldingJSON.get("adminComment"), insertedTilbakemeldingJSON.getAdminComment());
    String tilbakemeldingId = insertedTilbakemeldingJSON.getId();

    // Check that we can get the new tilbakemelding from the API
    tilbakemeldingResponse = get("/tilbakemelding/" + tilbakemeldingId);
    assertEquals(HttpStatus.OK, tilbakemeldingResponse.getStatusCode());

    // Check that we can update the tilbakemelding
    tilbakemeldingJSON.put("messageFromUser", "Not so happy");
    tilbakemeldingResponse = put("/tilbakemelding/" + tilbakemeldingId, tilbakemeldingJSON);
    assertEquals(HttpStatus.OK, tilbakemeldingResponse.getStatusCode());
    insertedTilbakemeldingJSON =
        gson.fromJson(tilbakemeldingResponse.getBody(), TilbakemeldingJSON.class);
    assertEquals(
        tilbakemeldingJSON.get("messageFromUser"), insertedTilbakemeldingJSON.getMessageFromUser());

    // Check that we can delete the tilbakemelding
    tilbakemeldingResponse = delete("/tilbakemelding/" + tilbakemeldingId);
    assertEquals(HttpStatus.OK, tilbakemeldingResponse.getStatusCode());

    // Check that the tilbakemelding is deleted
    tilbakemeldingResponse = get("/tilbakemelding/" + tilbakemeldingId);
    assertEquals(HttpStatus.NOT_FOUND, tilbakemeldingResponse.getStatusCode());
  }
}

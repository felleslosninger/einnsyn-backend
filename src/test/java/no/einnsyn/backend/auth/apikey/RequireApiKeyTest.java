package no.einnsyn.backend.auth.apikey;

import static org.junit.jupiter.api.Assertions.assertEquals;

import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.tilbakemelding.models.TilbakemeldingDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {"application.requireApiKey=true"})
@ActiveProfiles("test")
class RequireApiKeyTest extends EinnsynControllerTestBase {

  @Test
  void anonymousGetShouldBeRejected() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    assertEquals(HttpStatus.UNAUTHORIZED, getAnon("/arkiv/" + arkivDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.UNAUTHORIZED, getAnon("/arkiv").getStatusCode());
    assertEquals(HttpStatus.UNAUTHORIZED, getAnon("/search").getStatusCode());

    // Requests with an API key are not affected
    assertEquals(HttpStatus.OK, get("/arkiv/" + arkivDTO.getId()).getStatusCode());

    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkivDTO.getId()).getStatusCode());
  }

  @Test
  void anonymousDownloadShouldBeAllowed() throws Exception {
    // Not rejected by the API key requirement, so the request reaches the controller
    var response = getAnon("/dokumentobjekt/do_nonexistent/download");
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void anonymousPostShouldBeAllowed() throws Exception {
    var response = postAnon("/tilbakemelding", getTilbakemeldingJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var tilbakemeldingDTO = gson.fromJson(response.getBody(), TilbakemeldingDTO.class);

    assertEquals(
        HttpStatus.OK, deleteAdmin("/tilbakemelding/" + tilbakemeldingDTO.getId()).getStatusCode());
  }
}

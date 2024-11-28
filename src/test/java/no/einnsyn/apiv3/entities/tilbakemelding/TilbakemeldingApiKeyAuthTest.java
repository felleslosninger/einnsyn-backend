package no.einnsyn.backend.entities.tilbakemelding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.entities.tilbakemelding.models.TilbakemeldingDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TilbakemeldingApiKeyAuthTest extends EinnsynControllerTestBase {

  // Anyone should be able to add
  @Test
  void testAddUpdateListDeleteTilbakemelding() throws Exception {
    var response = postAnon("/tilbakemelding", getTilbakemeldingJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    var tilbakemelding = gson.fromJson(response.getBody(), TilbakemeldingDTO.class);
    var id = tilbakemelding.getId();

    // Anon not allowed to update / list / delete
    response = patchAnon("/tilbakemelding/" + id, getTilbakemeldingJSON());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    response = getAnon("/tilbakemelding");
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    response = deleteAnon("/tilbakemelding/" + id);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Normal user not allowed to update / list / delete
    response = patch("/tilbakemelding/" + id, getTilbakemeldingJSON());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    response = get("/tilbakemelding");
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    response = delete("/tilbakemelding/" + id);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Admin allowed to update / list / delete
    response = patchAdmin("/tilbakemelding/" + id, getTilbakemeldingJSON());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = getAdmin("/tilbakemelding");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = deleteAdmin("/tilbakemelding/" + id);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}

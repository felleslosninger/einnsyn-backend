package no.einnsyn.apiv3.entities.tilbakemelding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.tilbakemelding.models.TilbakemeldingDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TilbakemeldingApiKeyAuthTest extends EinnsynControllerTestBase {

  // Anyone should be able to add
  @Test
  void testAddUpdateListDeleteTilbakemelding() throws Exception {
    var response = postAnon("/tilbakemelding", getTilbakemeldingJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    var tilbakemelding = gson.fromJson(response.getBody(), TilbakemeldingDTO.class);
    var id = tilbakemelding.getId();

    // Anon not allowed to update / list / delete
    response = putAnon("/tilbakemelding/" + id, getTilbakemeldingJSON());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    response = getAnon("/tilbakemelding");
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    response = deleteAnon("/tilbakemelding/" + id);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Normal user not allowed to update / list / delete
    response = put("/tilbakemelding/" + id, getTilbakemeldingJSON());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    response = get("/tilbakemelding");
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    response = delete("/tilbakemelding/" + id);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Admin allowed to update / list / delete
    response = putAdmin("/tilbakemelding/" + id, getTilbakemeldingJSON());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = getAdmin("/tilbakemelding");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = deleteAdmin("/tilbakemelding/" + id);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}

package no.einnsyn.backend.entities.enhet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EnhetApiKeyAuthTest extends EinnsynControllerTestBase {

  @Test
  void testListEnhet() throws Exception {
    // Add two Enhets
    var response = post("/enhet/" + journalenhetId + "/underenhet", getEnhetJSON());
    var enhetDTO = gson.fromJson(response.getBody(), EnhetDTO.class);
    response = post("/enhet/" + journalenhetId + "/underenhet", getEnhetJSON());
    var enhetDTO2 = gson.fromJson(response.getBody(), EnhetDTO.class);

    // Everybody are allowed to list Enhet
    response = getAnon("/enhet");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    assertEquals(HttpStatus.OK, delete("/enhet/" + enhetDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/enhet/" + enhetDTO2.getId()).getStatusCode());
  }

  @Test
  void testGetEnhet() throws Exception {
    // Add Enhet
    var response = post("/enhet/" + journalenhetId + "/underenhet", getEnhetJSON());
    var enhetDTO = gson.fromJson(response.getBody(), EnhetDTO.class);

    // Unauthorized users are allowed to get Enhet
    response = getAnon("/enhet/" + enhetDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Other Enhets are allowed to get this Enhet
    response = get("/enhet/" + enhetDTO.getId(), journalenhet2Key);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Authorized are allowed to get
    response = get("/enhet/" + enhetDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Clean up
    assertEquals(HttpStatus.OK, delete("/enhet/" + enhetDTO.getId()).getStatusCode());
  }

  @Test
  void testInsertUpdateDeleteEnhet() throws Exception {
    // Add Enhet
    var response = post("/enhet/" + journalenhetId + "/underenhet", getEnhetJSON());
    var enhetDTO = gson.fromJson(response.getBody(), EnhetDTO.class);

    // Unauthorized are not allowed to insert
    response = postAnon("/enhet/" + journalenhetId + "/underenhet", getEnhetJSON());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Unauthorized are not allowed to update
    response = patchAnon("/enhet/" + enhetDTO.getId(), getEnhetJSON());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Unauthorized are not allowed to delete
    response = deleteAnon("/enhet/" + enhetDTO.getId());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Other Enhets are not allowed to insert
    response = post("/enhet/" + journalenhetId + "/underenhet", getEnhetJSON(), journalenhet2Key);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Other Enhets are not allowed to update
    response = patch("/enhet/" + enhetDTO.getId(), getEnhetJSON(), journalenhet2Key);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Other Enhets are not allowed to delete
    response = delete("/enhet/" + enhetDTO.getId(), journalenhet2Key);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Authorized are allowed to insert
    response = post("/enhet/" + journalenhetId + "/underenhet", getEnhetJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var enhetDTO2 = gson.fromJson(response.getBody(), EnhetDTO.class);

    // Authorized are allowed to update
    response = patch("/enhet/" + enhetDTO2.getId(), getEnhetJSON());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Authorized are allowed to delete
    response = delete("/enhet/" + enhetDTO2.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Clean up
    assertEquals(HttpStatus.OK, delete("/enhet/" + enhetDTO.getId()).getStatusCode());
  }
}

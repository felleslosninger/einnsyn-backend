package no.einnsyn.apiv3.entities.enhet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.reflect.TypeToken;
import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.enhet.models.EnhetDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class EnhetControllerAuthTest extends EinnsynControllerTestBase {

  @Test
  void testListEnhet() throws Exception {
    // Add two Enhets
    var response = post("/enhet/" + journalenhetId + "/underenhet", getEnhetJSON());
    var enhetDTO = gson.fromJson(response.getBody(), EnhetDTO.class);
    response = post("/enhet/" + journalenhetId + "/underenhet", getEnhetJSON());
    var enhetDTO2 = gson.fromJson(response.getBody(), EnhetDTO.class);

    // Unauthorized are not allowed to list Enhet
    response = getAnon("/enhet");
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Other Enhets are not allowed to list
    response = get("/enhet", journalenhet2Key, journalenhet2Secret);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Authorized are allowed to list underenhets
    response = get("/enhet/" + journalenhetId + "/underenhet");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var enhetListType = new TypeToken<ResultList<EnhetDTO>>() {}.getType();
    ResultList<EnhetDTO> enhetResultList = gson.fromJson(response.getBody(), enhetListType);
    assertNotNull(enhetResultList);
    assertNotNull(enhetResultList.getItems());

    // Only admins are allowed to list all Enhets
    response = get("/enhet");
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    response = getAdmin("/enhet");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    assertEquals(HttpStatus.OK, delete("/enhet/" + enhetDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/enhet/" + enhetDTO2.getId()).getStatusCode());
  }

  @Test
  void testGetEnhet() throws Exception {
    // Add Enhet
    var response = post("/enhet/" + journalenhetId + "/underenhet", getEnhetJSON());
    var enhetDTO = gson.fromJson(response.getBody(), EnhetDTO.class);

    // Unauthorized are not allowed to get Enhet
    response = getAnon("/enhet/" + enhetDTO.getId());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Other Enhets are not allowed to get
    response = get("/enhet/" + enhetDTO.getId(), journalenhet2Key, journalenhet2Secret);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

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
    response = putAnon("/enhet/" + enhetDTO.getId(), getEnhetJSON());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Unauthorized are not allowed to delete
    response = deleteAnon("/enhet/" + enhetDTO.getId());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Other Enhets are not allowed to insert
    response =
        post(
            "/enhet/" + journalenhetId + "/underenhet",
            getEnhetJSON(),
            journalenhet2Key,
            journalenhet2Secret);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Other Enhets are not allowed to update
    response =
        put("/enhet/" + enhetDTO.getId(), getEnhetJSON(), journalenhet2Key, journalenhet2Secret);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Other Enhets are not allowed to delete
    response = delete("/enhet/" + enhetDTO.getId(), journalenhet2Key, journalenhet2Secret);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Authorized are allowed to insert
    response = post("/enhet/" + journalenhetId + "/underenhet", getEnhetJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var enhetDTO2 = gson.fromJson(response.getBody(), EnhetDTO.class);

    // Authorized are allowed to update
    response = put("/enhet/" + enhetDTO2.getId(), getEnhetJSON());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Authorized are allowed to delete
    response = delete("/enhet/" + enhetDTO2.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Clean up
    assertEquals(HttpStatus.OK, delete("/enhet/" + enhetDTO.getId()).getStatusCode());
  }
}

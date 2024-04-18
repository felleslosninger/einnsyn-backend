package no.einnsyn.apiv3.auth.apikey;

import static org.junit.jupiter.api.Assertions.assertEquals;

import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ApiKeyAuthorizationTest extends EinnsynControllerTestBase {

  @Test
  void testInsertAuthorization() throws Exception {
    // Unauthorized are not allowed to insert Arkiv
    var result = postAnon("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());

    // Authorized are allowed to insert
    result = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, result.getStatusCode());
    var arkivDTO = gson.fromJson(result.getBody(), ArkivDTO.class);

    delete("/arkiv/" + arkivDTO.getId());
  }

  @Test
  void testUpdateAuthorization() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", getSaksmappeJSON());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Unauthorized are not allowed to update Arkiv
    var updateJSON = getSaksmappeJSON();
    updateJSON.put("offentligTittel", "UpdatedTittel");
    response = putAnon("/saksmappe/" + saksmappeDTO.getId(), updateJSON);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Another user can not update arkiv
    response =
        put(
            "/saksmappe/" + saksmappeDTO.getId(),
            updateJSON,
            journalenhet2Key,
            journalenhet2Secret);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Authorized are allowed to update
    response = put("/saksmappe/" + saksmappeDTO.getId(), updateJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var updatedSaksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertEquals("UpdatedTittel", updatedSaksmappeDTO.getOffentligTittel());

    // Unauthorized are not allowed to delete Arkiv
    response = deleteAnon("/saksmappe/" + saksmappeDTO.getId());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Other users are not allowed to delete
    response = delete("/saksmappe/" + saksmappeDTO.getId(), journalenhet2Key, journalenhet2Secret);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Authorized are allowed to delete
    response = delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}

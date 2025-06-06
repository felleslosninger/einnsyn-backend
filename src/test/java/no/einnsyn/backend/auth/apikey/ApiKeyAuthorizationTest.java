package no.einnsyn.backend.auth.apikey;

import static org.junit.jupiter.api.Assertions.assertEquals;

import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
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

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Unauthorized are not allowed to update Arkiv
    var updateJSON = getSaksmappeJSON();
    updateJSON.put("offentligTittel", "UpdatedTittel");
    response = patchAnon("/saksmappe/" + saksmappeDTO.getId(), updateJSON);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Another user can not update arkiv
    response = patch("/saksmappe/" + saksmappeDTO.getId(), updateJSON, journalenhet2Key);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Authorized are allowed to update
    response = patch("/saksmappe/" + saksmappeDTO.getId(), updateJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var updatedSaksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertEquals("UpdatedTittel", updatedSaksmappeDTO.getOffentligTittel());

    // Unauthorized are not allowed to delete Arkiv
    response = deleteAnon("/saksmappe/" + saksmappeDTO.getId());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Other users are not allowed to delete
    response = delete("/saksmappe/" + saksmappeDTO.getId(), journalenhet2Key);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Authorized are allowed to delete
    response = delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}

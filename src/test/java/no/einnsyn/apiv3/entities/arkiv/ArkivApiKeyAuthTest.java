package no.einnsyn.apiv3.entities.arkiv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.reflect.TypeToken;
import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ArkivApiKeyAuthTest extends EinnsynControllerTestBase {

  @Test
  void testListArkiv() throws Exception {
    // Add two Arkivs
    var response = post("/arkiv", getArkivJSON());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    response = post("/arkiv", getArkivJSON());
    var arkivDTO2 = gson.fromJson(response.getBody(), ArkivDTO.class);

    // Unauthorized are allowed to list Arkiv
    response = getAnon("/enhet/" + journalenhetId + "/arkiv");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var arkivListType = new TypeToken<ResultList<ArkivDTO>>() {}.getType();
    ResultList<ArkivDTO> arkivResultList = gson.fromJson(response.getBody(), arkivListType);
    assertNotNull(arkivResultList);
    assertNotNull(arkivResultList.getItems());
    assertEquals(2, arkivResultList.getItems().size());

    // Authorized are allowed to list
    response = get("/enhet/" + journalenhetId + "/arkiv");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    arkivListType = new TypeToken<ResultList<ArkivDTO>>() {}.getType();
    arkivResultList = gson.fromJson(response.getBody(), arkivListType);
    assertNotNull(arkivResultList);
    assertNotNull(arkivResultList.getItems());
    assertEquals(2, arkivResultList.getItems().size());

    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkivDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkivDTO2.getId()).getStatusCode());
  }

  @Test
  void testGetArkiv() throws Exception {
    // Add Arkiv
    var response = post("/arkiv", getArkivJSON());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    // Unauthorized are allowed to get Arkiv
    response = getAnon("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var arkiv = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(arkiv);

    // Authorized are allowed to get
    response = get("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    arkiv = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(arkiv);

    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkivDTO.getId()).getStatusCode());
  }

  @Test
  void testInsertUpdateDeleteArkiv() throws Exception {
    // Add Arkiv
    var response = post("/arkiv", getArkivJSON());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    // Unauthorized are not allowed to insert
    response = postAnon("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Unauthorized are not allowed to update
    response = patchAnon("/arkiv/" + arkivDTO.getId(), getArkivJSON());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Unauthorized are not allowed to delete
    response = deleteAnon("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Other users are not allowed to update
    response = patch("/arkiv/" + arkivDTO.getId(), getArkivJSON(), journalenhet2Key);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Other users are not allowed to delete
    response = delete("/arkiv/" + arkivDTO.getId(), journalenhet2Key);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Authorized are allowed to insert
    response = post("/arkiv", getArkivJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivDTO2 = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(arkivDTO2);

    // Authorized are allowed to update
    response = patch("/arkiv/" + arkivDTO2.getId(), getArkivJSON());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var updatedArkiv = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(updatedArkiv);

    // Authorized are allowed to delete
    response = delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = delete("/arkiv/" + arkivDTO2.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}

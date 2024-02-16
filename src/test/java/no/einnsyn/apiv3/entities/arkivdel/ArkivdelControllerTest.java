package no.einnsyn.apiv3.entities.arkivdel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import no.einnsyn.apiv3.entities.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.apiv3.entities.klasse.models.KlasseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ArkivdelControllerTest extends EinnsynControllerTestBase {

  @Test
  void testArkivdelLifecycle() throws Exception {
    var arkiv = getArkivJSON();
    arkiv.put("tittel", "ParentArkiv");
    var response = post("/arkiv", arkiv);
    var arkivDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    assertNotNull(arkivDTO.getId());

    var arkivdelJSON = getArkivdelJSON();
    arkivdelJSON.put("tittel", "Arkivdel1");
    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", arkivdelJSON);
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    assertNotNull(arkivdelDTO.getId());

    var subKlasse = getKlasseJSON();
    subKlasse.put("tittel", "SubKlasse1");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/klasse", subKlasse);
    var subKlasseDTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(subKlasseDTO.getId());
    assertNotNull(subKlasseDTO.getParent().getId());

    var subKlasse2 = getKlasseJSON();
    subKlasse2.put("tittel", "SubKlasse2");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/klasse", subKlasse2);
    var subKlasse2DTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(subKlasse2DTO.getId());
    assertNotNull(subKlasse2DTO.getParent().getId());

    var subSaksmappe = getSaksmappeJSON();
    subSaksmappe.put("tittel", "SubSaksmappe1");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", subSaksmappe);
    var subSaksmappeDTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(subSaksmappeDTO.getId());
    assertNotNull(subSaksmappeDTO.getParent().getId());

    var subSaksmappe2 = getSaksmappeJSON();
    subSaksmappe2.put("tittel", "SubSaksmappe2");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", subSaksmappe2);
    var subSaksmappe2DTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(subSaksmappe2DTO.getId());
    assertNotNull(subSaksmappe2DTO.getParent().getId());

    // Delete
    response = delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkiv/" + arkivDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkivdel/" + arkivdelDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/klasse/" + subKlasseDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/klasse/" + subKlasse2DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/saksmappe/" + subSaksmappeDTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/saksmappe/" + subSaksmappe2DTO.getId()).getStatusCode());
  }

  // Make sure we delete the correct arkivdel when a parent is deleted
  @Test
  void testDeleteParent() throws Exception {
    var arkiv1 = getArkivJSON();
    var response = post("/arkiv", arkiv1);
    var arkiv1DTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    var arkiv2 = getArkivJSON();
    response = post("/arkiv", arkiv2);
    var arkiv2DTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    var arkivdel1 = getArkivdelJSON();
    response = post("/arkiv/" + arkiv1DTO.getId() + "/arkivdel", arkivdel1);
    var arkivdel1DTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    var arkivdel2 = getArkivdelJSON();
    response = post("/arkiv/" + arkiv2DTO.getId() + "/arkivdel", arkivdel2);
    var arkivdel2DTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    // Delete arkiv1
    response = delete("/arkiv/" + arkiv1DTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkiv/" + arkiv1DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkivdel/" + arkivdel1DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, get("/arkiv/" + arkiv2DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, get("/arkivdel/" + arkivdel2DTO.getId()).getStatusCode());

    // Delete arkiv2
    delete("/arkiv/" + arkiv2DTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkiv/" + arkiv2DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkivdel/" + arkivdel2DTO.getId()).getStatusCode());
  }

  // Make sure we cannot POST to /arkivdel
  @Test
  void testPostToArkivdel() throws Exception {
    var response = post("/arkivdel", getArkivdelJSON());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }
}

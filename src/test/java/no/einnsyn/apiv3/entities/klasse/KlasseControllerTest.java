package no.einnsyn.apiv3.entities.klasse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.reflect.TypeToken;
import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.apiv3.entities.klasse.models.KlasseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class KlasseControllerTest extends EinnsynControllerTestBase {

  @Test
  void testKlasseParents() throws Exception {
    var arkivJSON = getArkivJSON();
    var response = post("/arkiv", arkivJSON);
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(arkivDTO.getId());

    var arkivdelJSON = getArkivdelJSON();
    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", arkivdelJSON);
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    assertNotNull(arkivdelDTO.getId());
    assertEquals(arkivdelDTO.getParent().getId(), arkivDTO.getId());

    var klassifikasjonssystemJSON = getKlassifikasjonssystemJSON();
    response =
        post(
            "/arkivdel/" + arkivdelDTO.getId() + "/klassifikasjonssystem",
            klassifikasjonssystemJSON);
    var klassifikasjonssystemDTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(klassifikasjonssystemDTO.getId());
    assertEquals(klassifikasjonssystemDTO.getParent().getId(), arkivdelDTO.getId());

    var klasseJSON = getKlasseJSON();
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/klasse", klasseJSON);
    var klasseDTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(klasseDTO.getId());
    assertEquals(klasseDTO.getParent().getId(), arkivdelDTO.getId());

    var subklasseJSON = getKlasseJSON();
    subklasseJSON.put("tittel", "Subklasse");
    response = post("/klasse/" + klasseDTO.getId() + "/klasse", subklasseJSON);
    var subklasseDTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(subklasseDTO.getId());

    var subklasse2JSON = getKlasseJSON();
    subklasse2JSON.put("tittel", "Subklasse2");
    response =
        post(
            "/klassifikasjonssystem/" + klassifikasjonssystemDTO.getId() + "/klasse",
            subklasse2JSON);
    var subklasse2DTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(subklasse2DTO.getId());

    response = get("/klasse/" + klasseDTO.getId() + "?expand=parent");
    var getKlasseDTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(getKlasseDTO.getParent());
    assertEquals(getKlasseDTO.getParent().getId(), arkivdelDTO.getId());
    assertTrue(getKlasseDTO.getParent().isArkivdel());

    response = get("/klasse/" + subklasseDTO.getId() + "?expand=parent");
    var getSubklasseDTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(getSubklasseDTO.getParent());
    assertEquals(getSubklasseDTO.getParent().getId(), klasseDTO.getId());
    assertTrue(getSubklasseDTO.getParent().isKlasse());

    response = get("/klasse/" + subklasse2DTO.getId() + "?expand=parent");
    var getSubklasse2DTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(getSubklasse2DTO.getParent());
    assertEquals(getSubklasse2DTO.getParent().getId(), klassifikasjonssystemDTO.getId());
    assertTrue(getSubklasse2DTO.getParent().isKlassifikasjonssystem());

    // Clean up
    delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkiv/" + arkivDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkivdel/" + arkivdelDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/klasse/" + klasseDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/klasse/" + subklasseDTO.getId()).getStatusCode());
  }

  @Test
  void testSubLists() throws Exception {

    var arkivJSON = getArkivJSON();
    var response = post("/arkiv", arkivJSON);
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(arkivDTO.getId());

    var arkivdelJSON = getArkivdelJSON();
    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", arkivdelJSON);
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    assertNotNull(arkivdelDTO.getId());
    assertEquals(arkivdelDTO.getParent().getId(), arkivDTO.getId());

    var klassifikasjonssystemJSON = getKlassifikasjonssystemJSON();
    response =
        post(
            "/arkivdel/" + arkivdelDTO.getId() + "/klassifikasjonssystem",
            klassifikasjonssystemJSON);
    var klassifikasjonssystemDTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(klassifikasjonssystemDTO.getId());
    assertEquals(klassifikasjonssystemDTO.getParent().getId(), arkivdelDTO.getId());

    var klasseJSON = getKlasseJSON();
    response =
        post("/klassifikasjonssystem/" + klassifikasjonssystemDTO.getId() + "/klasse", klasseJSON);
    var klasseDTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(klasseDTO.getId());
    assertEquals(klasseDTO.getParent().getId(), klassifikasjonssystemDTO.getId());

    var subklasseJSON = getKlasseJSON();
    subklasseJSON.put("tittel", "Subklasse");
    response = post("/klasse/" + klasseDTO.getId() + "/klasse", subklasseJSON);
    var subklasseDTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(subklasseDTO.getId());

    var subklasse2JSON = getKlasseJSON();
    subklasse2JSON.put("tittel", "Subklasse2");
    response = post("/klasse/" + klasseDTO.getId() + "/klasse", subklasse2JSON);
    var subklasse2DTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(subklasse2DTO.getId());

    response = get("/klasse/" + klasseDTO.getId() + "/klasse");
    var type = new TypeToken<ResultList<KlasseDTO>>() {}.getType();
    ResultList<KlasseDTO> klasseDTOList = gson.fromJson(response.getBody(), type);
    assertEquals(2, klasseDTOList.getItems().size());
    assertEquals(subklasseDTO.getId(), klasseDTOList.getItems().get(1).getId());
    assertEquals(subklasse2DTO.getId(), klasseDTOList.getItems().get(0).getId());

    // Reverse
    response = get("/klasse/" + klasseDTO.getId() + "/klasse?sortOrder=asc");
    klasseDTOList = gson.fromJson(response.getBody(), type);
    assertEquals(2, klasseDTOList.getItems().size());
    assertEquals(subklasseDTO.getId(), klasseDTOList.getItems().get(0).getId());
    assertEquals(subklasse2DTO.getId(), klasseDTOList.getItems().get(1).getId());

    // Clean up
    delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkiv/" + arkivDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkivdel/" + arkivdelDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/klasse/" + klasseDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/klasse/" + subklasseDTO.getId()).getStatusCode());
  }

  // Make sure we cannot POST to /klasse
  @Test
  void testPostToKlasse() throws Exception {
    var response = post("/klasse", getKlasseJSON());
    assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
  }
}

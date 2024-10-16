package no.einnsyn.apiv3.entities.klassifikasjonssystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
class KlassifikasjonssystemControllerTest extends EinnsynControllerTestBase {

  @Test
  void testKlassifikasjonssystemLifecycle() throws Exception {
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

    var klasse1JSON = getKlasseJSON();
    response =
        post("/klassifikasjonssystem/" + klassifikasjonssystemDTO.getId() + "/klasse", klasse1JSON);
    var klasse1DTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(klasse1DTO.getId());
    assertEquals(klasse1DTO.getParent().getId(), klassifikasjonssystemDTO.getId());

    var klasse2JSON = getKlasseJSON();
    response =
        post("/klassifikasjonssystem/" + klassifikasjonssystemDTO.getId() + "/klasse", klasse2JSON);
    var klasse2DTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(klasse2DTO.getId());
    assertEquals(klasse2DTO.getParent().getId(), klassifikasjonssystemDTO.getId());

    response = get("/klassifikasjonssystem/" + klassifikasjonssystemDTO.getId() + "/klasse");
    var klasseListType = new TypeToken<ResultList<KlasseDTO>>() {}.getType();
    ResultList<KlasseDTO> klasseListDTO = gson.fromJson(response.getBody(), klasseListType);
    assertEquals(2, klasseListDTO.getItems().size());
    assertEquals(klasse1DTO.getId(), klasseListDTO.getItems().get(1).getId());
    assertEquals(klasse2DTO.getId(), klasseListDTO.getItems().get(0).getId());

    // Asc
    response =
        get("/klassifikasjonssystem/" + klassifikasjonssystemDTO.getId() + "/klasse?sortOrder=asc");
    klasseListDTO = gson.fromJson(response.getBody(), klasseListType);
    assertEquals(2, klasseListDTO.getItems().size());
    assertEquals(klasse1DTO.getId(), klasseListDTO.getItems().get(0).getId());
    assertEquals(klasse2DTO.getId(), klasseListDTO.getItems().get(1).getId());

    // Desc
    response =
        get(
            "/klassifikasjonssystem/"
                + klassifikasjonssystemDTO.getId()
                + "/klasse?sortOrder=desc");
    klasseListDTO = gson.fromJson(response.getBody(), klasseListType);
    assertEquals(2, klasseListDTO.getItems().size());
    assertEquals(klasse1DTO.getId(), klasseListDTO.getItems().get(1).getId());
    assertEquals(klasse2DTO.getId(), klasseListDTO.getItems().get(0).getId());

    // Clean up
    delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkiv/" + arkivDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkivdel/" + arkivdelDTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND,
        get("/klassifikasjonssystem/" + klassifikasjonssystemDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/klasse/" + klasse1DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/klasse/" + klasse2DTO.getId()).getStatusCode());
  }

  // Make sure we delete the correct children recursively
  @Test
  void testDeletion() throws Exception {
    var arkivJSON = getArkivJSON();
    var response = post("/arkiv", arkivJSON);
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(arkivDTO.getId());

    var arkivdelJSON = getArkivdelJSON();
    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", arkivdelJSON);
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    assertNotNull(arkivdelDTO.getId());

    var klassifikasjonssystem1JSON = getKlassifikasjonssystemJSON();
    response =
        post(
            "/arkivdel/" + arkivdelDTO.getId() + "/klassifikasjonssystem",
            klassifikasjonssystem1JSON);
    var klassifikasjonssystem1DTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(klassifikasjonssystem1DTO.getId());
    assertEquals(klassifikasjonssystem1DTO.getParent().getId(), arkivdelDTO.getId());

    var klassifikasjonssystem2JSON = getKlassifikasjonssystemJSON();
    response =
        post(
            "/arkivdel/" + arkivdelDTO.getId() + "/klassifikasjonssystem",
            klassifikasjonssystem2JSON);
    var klassifikasjonssystem2DTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(klassifikasjonssystem2DTO.getId());
    assertEquals(klassifikasjonssystem2DTO.getParent().getId(), arkivdelDTO.getId());

    var klasse1JSON = getKlasseJSON();
    response =
        post(
            "/klassifikasjonssystem/" + klassifikasjonssystem1DTO.getId() + "/klasse", klasse1JSON);
    var klasse1DTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(klasse1DTO.getId());
    assertEquals(klasse1DTO.getParent().getId(), klassifikasjonssystem1DTO.getId());

    var klasse2JSON = getKlasseJSON();
    response =
        post(
            "/klassifikasjonssystem/" + klassifikasjonssystem2DTO.getId() + "/klasse", klasse2JSON);
    var klasse2DTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(klasse2DTO.getId());
    assertEquals(klasse2DTO.getParent().getId(), klassifikasjonssystem2DTO.getId());

    // Delete the first Klassifikasjonssystem
    delete("/klassifikasjonssystem/" + klassifikasjonssystem1DTO.getId());
    assertEquals(
        HttpStatus.NOT_FOUND,
        get("/klassifikasjonssystem/" + klassifikasjonssystem1DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/klasse/" + klasse1DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.OK,
        get("/klassifikasjonssystem/" + klassifikasjonssystem2DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, get("/klasse/" + klasse2DTO.getId()).getStatusCode());

    // Clean up
    delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkiv/" + arkivDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkivdel/" + arkivdelDTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND,
        get("/klassifikasjonssystem/" + klassifikasjonssystem2DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/klasse/" + klasse2DTO.getId()).getStatusCode());
  }

  // Make sure we delete the correct Klassifikasjonssystem when a parent is deleted
  @Test
  void testDeleteParent() throws Exception {
    var arkivJSON = getArkivJSON();
    var response = post("/arkiv", arkivJSON);
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(arkivDTO.getId());

    var arkivdel1JSON = getArkivdelJSON();
    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", arkivdel1JSON);
    var arkivdel1DTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    assertNotNull(arkivdel1DTO.getId());

    var arkivdel2JSON = getArkivdelJSON();
    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", arkivdel2JSON);
    var arkivdel2DTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    assertNotNull(arkivdel2DTO.getId());

    var klassifikasjonssystem1JSON = getKlassifikasjonssystemJSON();
    response =
        post(
            "/arkivdel/" + arkivdel1DTO.getId() + "/klassifikasjonssystem",
            klassifikasjonssystem1JSON);
    var klassifikasjonssystem1DTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(klassifikasjonssystem1DTO.getId());
    assertEquals(klassifikasjonssystem1DTO.getParent().getId(), arkivdel1DTO.getId());

    var klassifikasjonssystem2JSON = getKlassifikasjonssystemJSON();
    response =
        post(
            "/arkivdel/" + arkivdel2DTO.getId() + "/klassifikasjonssystem",
            klassifikasjonssystem2JSON);
    var klassifikasjonssystem2DTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(klassifikasjonssystem2DTO.getId());
    assertEquals(klassifikasjonssystem2DTO.getParent().getId(), arkivdel2DTO.getId());

    // Delete the first arkivdel
    delete("/arkivdel/" + arkivdel1DTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkivdel/" + arkivdel1DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND,
        get("/klassifikasjonssystem/" + klassifikasjonssystem1DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, get("/arkivdel/" + arkivdel2DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.OK,
        get("/klassifikasjonssystem/" + klassifikasjonssystem2DTO.getId()).getStatusCode());

    // Clean up
    delete("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkiv/" + arkivDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkivdel/" + arkivdel2DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND,
        get("/klassifikasjonssystem/" + klassifikasjonssystem2DTO.getId()).getStatusCode());
  }

  // Make sure we cannot POST to /klassifikasjonssystem/
  @Test
  void testPostToKlassifikasjonssystem() throws Exception {
    var response = post("/klassifikasjonssystem", getKlassifikasjonssystemJSON());
    assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
  }
}

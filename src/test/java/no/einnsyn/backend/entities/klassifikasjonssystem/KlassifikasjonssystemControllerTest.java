package no.einnsyn.backend.entities.klassifikasjonssystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.reflect.TypeToken;
import java.util.List;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.klasse.models.KlasseDTO;
import no.einnsyn.backend.entities.klassifikasjonssystem.models.KlassifikasjonssystemDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
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
    assertEquals(arkivdelDTO.getArkiv().getId(), arkivDTO.getId());

    var klassifikasjonssystemJSON = getKlassifikasjonssystemJSON();
    response =
        post(
            "/arkivdel/" + arkivdelDTO.getId() + "/klassifikasjonssystem",
            klassifikasjonssystemJSON);
    var klassifikasjonssystemDTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(klassifikasjonssystemDTO.getId());
    assertEquals(klassifikasjonssystemDTO.getArkivdel().getId(), arkivdelDTO.getId());

    var klasse1JSON = getKlasseJSON();
    response =
        post("/klassifikasjonssystem/" + klassifikasjonssystemDTO.getId() + "/klasse", klasse1JSON);
    var klasse1DTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(klasse1DTO.getId());
    assertEquals(klasse1DTO.getKlassifikasjonssystem().getId(), klassifikasjonssystemDTO.getId());

    var klasse2JSON = getKlasseJSON();
    response =
        post("/klassifikasjonssystem/" + klassifikasjonssystemDTO.getId() + "/klasse", klasse2JSON);
    var klasse2DTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(klasse2DTO.getId());
    assertEquals(klasse2DTO.getKlassifikasjonssystem().getId(), klassifikasjonssystemDTO.getId());

    response = get("/klassifikasjonssystem/" + klassifikasjonssystemDTO.getId() + "/klasse");
    var klasseListType = new TypeToken<PaginatedList<KlasseDTO>>() {}.getType();
    PaginatedList<KlasseDTO> klasseListDTO = gson.fromJson(response.getBody(), klasseListType);
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
    assertEquals(klassifikasjonssystem1DTO.getArkivdel().getId(), arkivdelDTO.getId());

    var klassifikasjonssystem2JSON = getKlassifikasjonssystemJSON();
    response =
        post(
            "/arkivdel/" + arkivdelDTO.getId() + "/klassifikasjonssystem",
            klassifikasjonssystem2JSON);
    var klassifikasjonssystem2DTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(klassifikasjonssystem2DTO.getId());
    assertEquals(klassifikasjonssystem2DTO.getArkivdel().getId(), arkivdelDTO.getId());

    var klasse1JSON = getKlasseJSON();
    response =
        post(
            "/klassifikasjonssystem/" + klassifikasjonssystem1DTO.getId() + "/klasse", klasse1JSON);
    var klasse1DTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(klasse1DTO.getId());
    assertEquals(klasse1DTO.getKlassifikasjonssystem().getId(), klassifikasjonssystem1DTO.getId());

    var klasse2JSON = getKlasseJSON();
    response =
        post(
            "/klassifikasjonssystem/" + klassifikasjonssystem2DTO.getId() + "/klasse", klasse2JSON);
    var klasse2DTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(klasse2DTO.getId());
    assertEquals(klasse2DTO.getKlassifikasjonssystem().getId(), klassifikasjonssystem2DTO.getId());

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
    assertEquals(klassifikasjonssystem1DTO.getArkivdel().getId(), arkivdel1DTO.getId());

    var klassifikasjonssystem2JSON = getKlassifikasjonssystemJSON();
    response =
        post(
            "/arkivdel/" + arkivdel2DTO.getId() + "/klassifikasjonssystem",
            klassifikasjonssystem2JSON);
    var klassifikasjonssystem2DTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(klassifikasjonssystem2DTO.getId());
    assertEquals(klassifikasjonssystem2DTO.getArkivdel().getId(), arkivdel2DTO.getId());

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

  @Test
  void testListKlasseIsScopedToKlassifikasjonssystem() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    response =
        post(
            "/arkivdel/" + arkivdelDTO.getId() + "/klassifikasjonssystem",
            getKlassifikasjonssystemJSON());
    var ksysDTO = gson.fromJson(response.getBody(), KlassifikasjonssystemDTO.class);

    // One Klasse inside the Klassifikasjonssystem, and one directly under the Arkivdel that must
    // not show up when listing the Klasse of the Klassifikasjonssystem.
    response = post("/klassifikasjonssystem/" + ksysDTO.getId() + "/klasse", getKlasseJSON());
    var insideDTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/klasse", getKlasseJSON());
    var outsideDTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(outsideDTO.getId());

    response = get("/klassifikasjonssystem/" + ksysDTO.getId() + "/klasse");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var klasseListType = new TypeToken<PaginatedList<KlasseDTO>>() {}.getType();
    PaginatedList<KlasseDTO> list = gson.fromJson(response.getBody(), klasseListType);
    var ids = list.getItems().stream().map(KlasseDTO::getId).toList();
    assertEquals(List.of(insideDTO.getId()), ids);

    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkivDTO.getId()).getStatusCode());
  }

  @Test
  void testListKlassifikasjonssystemIsScopedToArkivdel() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    var arkivdel1DTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    var arkivdel2DTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    // One Klassifikasjonssystem in each Arkivdel. Listing one Arkivdel's must not return the
    // other's.
    response =
        post(
            "/arkivdel/" + arkivdel1DTO.getId() + "/klassifikasjonssystem",
            getKlassifikasjonssystemJSON());
    var ksys1DTO = gson.fromJson(response.getBody(), KlassifikasjonssystemDTO.class);
    response =
        post(
            "/arkivdel/" + arkivdel2DTO.getId() + "/klassifikasjonssystem",
            getKlassifikasjonssystemJSON());
    var ksys2DTO = gson.fromJson(response.getBody(), KlassifikasjonssystemDTO.class);
    assertNotNull(ksys2DTO.getId());

    response = get("/arkivdel/" + arkivdel1DTO.getId() + "/klassifikasjonssystem");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var listType = new TypeToken<PaginatedList<KlassifikasjonssystemDTO>>() {}.getType();
    PaginatedList<KlassifikasjonssystemDTO> list = gson.fromJson(response.getBody(), listType);
    var ids = list.getItems().stream().map(KlassifikasjonssystemDTO::getId).toList();
    assertEquals(List.of(ksys1DTO.getId()), ids);

    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkivDTO.getId()).getStatusCode());
  }

  @Test
  void testChildrenAddedThroughTheParentEndpointAreListed() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    // Adding through the nested endpoints must set the relation the list queries filter on.
    response =
        post(
            "/arkivdel/" + arkivdelDTO.getId() + "/klassifikasjonssystem",
            getKlassifikasjonssystemJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var ksysDTO = gson.fromJson(response.getBody(), KlassifikasjonssystemDTO.class);

    response = post("/klassifikasjonssystem/" + ksysDTO.getId() + "/klasse", getKlasseJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var klasseDTO = gson.fromJson(response.getBody(), KlasseDTO.class);

    response = get("/arkivdel/" + arkivdelDTO.getId() + "/klassifikasjonssystem");
    var ksysListType = new TypeToken<PaginatedList<KlassifikasjonssystemDTO>>() {}.getType();
    PaginatedList<KlassifikasjonssystemDTO> ksysList =
        gson.fromJson(response.getBody(), ksysListType);
    assertEquals(
        List.of(ksysDTO.getId()),
        ksysList.getItems().stream().map(KlassifikasjonssystemDTO::getId).toList());

    response = get("/klassifikasjonssystem/" + ksysDTO.getId() + "/klasse");
    var klasseListType = new TypeToken<PaginatedList<KlasseDTO>>() {}.getType();
    PaginatedList<KlasseDTO> klasseList = gson.fromJson(response.getBody(), klasseListType);
    assertEquals(
        List.of(klasseDTO.getId()), klasseList.getItems().stream().map(KlasseDTO::getId).toList());

    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkivDTO.getId()).getStatusCode());
  }
}

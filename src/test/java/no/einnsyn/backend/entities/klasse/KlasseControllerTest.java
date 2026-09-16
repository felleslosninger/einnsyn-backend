package no.einnsyn.backend.entities.klasse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.reflect.TypeToken;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.klasse.models.KlasseDTO;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
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
    assertEquals(arkivdelDTO.getArkiv().getId(), arkivDTO.getId());

    var klassifikasjonssystemJSON = getKlassifikasjonssystemJSON();
    response =
        post(
            "/arkivdel/" + arkivdelDTO.getId() + "/klassifikasjonssystem",
            klassifikasjonssystemJSON);
    var klassifikasjonssystemDTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(klassifikasjonssystemDTO.getId());
    assertEquals(klassifikasjonssystemDTO.getArkivdel().getId(), arkivdelDTO.getId());

    var klasseJSON = getKlasseJSON();
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/klasse", klasseJSON);
    var klasseDTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(klasseDTO.getId());
    assertEquals(klasseDTO.getArkivdel().getId(), arkivdelDTO.getId());

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
    assertNotNull(getKlasseDTO.getArkivdel());
    assertNull(getKlasseDTO.getKlasse());
    assertNull(getKlasseDTO.getKlassifikasjonssystem());
    assertEquals(getKlasseDTO.getArkivdel().getId(), arkivdelDTO.getId());

    response = get("/klasse/" + subklasseDTO.getId() + "?expand=parent");
    var getSubklasseDTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(getSubklasseDTO.getKlasse());
    assertNull(getSubklasseDTO.getArkivdel());
    assertNull(getSubklasseDTO.getKlassifikasjonssystem());
    assertEquals(getSubklasseDTO.getKlasse().getId(), klasseDTO.getId());

    response = get("/klasse/" + subklasse2DTO.getId() + "?expand=parent");
    var getSubklasse2DTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(getSubklasse2DTO.getKlassifikasjonssystem());
    assertNull(getSubklasse2DTO.getKlasse());
    assertNull(getSubklasse2DTO.getArkivdel());
    assertEquals(
        getSubklasse2DTO.getKlassifikasjonssystem().getId(), klassifikasjonssystemDTO.getId());

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
    assertEquals(arkivdelDTO.getArkiv().getId(), arkivDTO.getId());

    var klassifikasjonssystemJSON = getKlassifikasjonssystemJSON();
    response =
        post(
            "/arkivdel/" + arkivdelDTO.getId() + "/klassifikasjonssystem",
            klassifikasjonssystemJSON);
    var klassifikasjonssystemDTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(klassifikasjonssystemDTO.getId());
    assertEquals(klassifikasjonssystemDTO.getArkivdel().getId(), arkivdelDTO.getId());

    var klasseJSON = getKlasseJSON();
    response =
        post("/klassifikasjonssystem/" + klassifikasjonssystemDTO.getId() + "/klasse", klasseJSON);
    var klasseDTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(klasseDTO.getId());
    assertEquals(klasseDTO.getKlassifikasjonssystem().getId(), klassifikasjonssystemDTO.getId());

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
    var type = new TypeToken<PaginatedList<KlasseDTO>>() {}.getType();
    PaginatedList<KlasseDTO> klasseDTOList = gson.fromJson(response.getBody(), type);
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

  @Test
  void failToInsertDuplicateExternalIdAndJournalenhet() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    var klasse1JSON = getKlasseJSON();
    var klasse2JSON = getKlasseJSON();
    klasse1JSON.put("externalId", "externalId");
    klasse2JSON.put("externalId", "externalId");

    response = post("/arkivdel/" + arkivdelDTO.getId() + "/klasse", klasse1JSON);
    var klasse1DTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(klasse1DTO.getId());

    response = post("/arkivdel/" + arkivdelDTO.getId() + "/klasse", klasse2JSON);
    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

    delete("/arkiv/" + arkivDTO.getId());
  }

  @Test
  void testKlasseListByExternalIdAndJournalenhet() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    var klasse1JSON = getKlasseJSON();
    var klasse2JSON = getKlasseJSON();
    klasse1JSON.put("externalId", "externalId");
    klasse2JSON.put("externalId", "externalId");
    klasse2JSON.put("journalenhet", underenhetId);

    response = post("/arkivdel/" + arkivdelDTO.getId() + "/klasse", klasse1JSON);
    var klasse1DTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(klasse1DTO.getId());

    response = post("/arkivdel/" + arkivdelDTO.getId() + "/klasse", klasse2JSON);
    var klasse2DTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(klasse2DTO.getId());

    response = get("/klasse?externalId=externalId");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var resultListType = new TypeToken<PaginatedList<KlasseDTO>>() {}.getType();
    PaginatedList<KlasseDTO> klasseResultList = gson.fromJson(response.getBody(), resultListType);
    assertEquals(2, klasseResultList.getItems().size());
    assertEquals(klasse1DTO.getId(), klasseResultList.getItems().get(1).getId());
    assertEquals(klasse2DTO.getId(), klasseResultList.getItems().get(0).getId());

    response = get("/klasse?externalId=externalId&journalenhet=" + underenhetId);
    klasseResultList = gson.fromJson(response.getBody(), resultListType);
    assertEquals(1, klasseResultList.getItems().size());
    assertEquals(klasse2DTO.getId(), klasseResultList.getItems().get(0).getId());

    delete("/arkiv/" + arkivDTO.getId());
  }

  @Test
  void testListSaksmappeMoetemappeAndKlasseByKlasse() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    response = post("/arkivdel/" + arkivdelDTO.getId() + "/klasse", getKlasseJSON());
    var klasseDTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(klasseDTO.getId());

    // Add two Saksmappe, two Moetemappe and two child Klasse to the Klasse
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("klasse", klasseDTO.getId());
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappe1DTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappe2DTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    var moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.put("klasse", klasseDTO.getId());
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappe1DTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappe2DTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    response = post("/klasse/" + klasseDTO.getId() + "/klasse", getKlasseJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var subklasse1DTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    response = post("/klasse/" + klasseDTO.getId() + "/klasse", getKlasseJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var subklasse2DTO = gson.fromJson(response.getBody(), KlasseDTO.class);

    // List Saksmappe by Klasse, newest first by default, oldest first ascending
    var saksmappeListType = new TypeToken<PaginatedList<SaksmappeDTO>>() {}.getType();
    response = get("/klasse/" + klasseDTO.getId() + "/saksmappe");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<SaksmappeDTO> saksmappeList =
        gson.fromJson(response.getBody(), saksmappeListType);
    assertEquals(2, saksmappeList.getItems().size());
    assertEquals(saksmappe2DTO.getId(), saksmappeList.getItems().get(0).getId());
    assertEquals(saksmappe1DTO.getId(), saksmappeList.getItems().get(1).getId());

    response = get("/klasse/" + klasseDTO.getId() + "/saksmappe?sortOrder=asc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    saksmappeList = gson.fromJson(response.getBody(), saksmappeListType);
    assertEquals(2, saksmappeList.getItems().size());
    assertEquals(saksmappe1DTO.getId(), saksmappeList.getItems().get(0).getId());
    assertEquals(saksmappe2DTO.getId(), saksmappeList.getItems().get(1).getId());

    // List Moetemappe by Klasse, newest first by default, oldest first ascending
    var moetemappeListType = new TypeToken<PaginatedList<MoetemappeDTO>>() {}.getType();
    response = get("/klasse/" + klasseDTO.getId() + "/moetemappe");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<MoetemappeDTO> moetemappeList =
        gson.fromJson(response.getBody(), moetemappeListType);
    assertEquals(2, moetemappeList.getItems().size());
    assertEquals(moetemappe2DTO.getId(), moetemappeList.getItems().get(0).getId());
    assertEquals(moetemappe1DTO.getId(), moetemappeList.getItems().get(1).getId());

    response = get("/klasse/" + klasseDTO.getId() + "/moetemappe?sortOrder=asc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    moetemappeList = gson.fromJson(response.getBody(), moetemappeListType);
    assertEquals(2, moetemappeList.getItems().size());
    assertEquals(moetemappe1DTO.getId(), moetemappeList.getItems().get(0).getId());
    assertEquals(moetemappe2DTO.getId(), moetemappeList.getItems().get(1).getId());

    // List child Klasse by Klasse, newest first by default, oldest first ascending
    var klasseListType = new TypeToken<PaginatedList<KlasseDTO>>() {}.getType();
    response = get("/klasse/" + klasseDTO.getId() + "/klasse");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<KlasseDTO> klasseList = gson.fromJson(response.getBody(), klasseListType);
    assertEquals(2, klasseList.getItems().size());
    assertEquals(subklasse2DTO.getId(), klasseList.getItems().get(0).getId());
    assertEquals(subklasse1DTO.getId(), klasseList.getItems().get(1).getId());

    response = get("/klasse/" + klasseDTO.getId() + "/klasse?sortOrder=asc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    klasseList = gson.fromJson(response.getBody(), klasseListType);
    assertEquals(2, klasseList.getItems().size());
    assertEquals(subklasse1DTO.getId(), klasseList.getItems().get(0).getId());
    assertEquals(subklasse2DTO.getId(), klasseList.getItems().get(1).getId());

    delete("/arkiv/" + arkivDTO.getId());
  }
}

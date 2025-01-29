package no.einnsyn.backend.entities.arkivdel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.reflect.TypeToken;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.responses.models.ListResponseBody;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.klasse.models.KlasseDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
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
    assertNotNull(subKlasseDTO.getArkivdel().getId());

    var subKlasse2 = getKlasseJSON();
    subKlasse2.put("tittel", "SubKlasse2");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/klasse", subKlasse2);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var subKlasse2DTO = gson.fromJson(response.getBody(), KlasseDTO.class);
    assertNotNull(subKlasse2DTO.getId());
    assertNotNull(subKlasse2DTO.getArkivdel().getId());

    var subSaksmappe = getSaksmappeJSON();
    subSaksmappe.put("offentligTittel", "SubSaksmappe1");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", subSaksmappe);
    var subSaksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(subSaksmappeDTO.getId());
    assertNotNull(subSaksmappeDTO.getArkivdel().getId());
    assertEquals("SubSaksmappe1", subSaksmappeDTO.getOffentligTittel());

    var subSaksmappe2 = getSaksmappeJSON();
    subSaksmappe2.put("offentligTittel", "SubSaksmappe2");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", subSaksmappe2);
    var subSaksmappe2DTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(subSaksmappe2DTO.getId());
    assertNotNull(subSaksmappe2DTO.getArkivdel().getId());

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
    assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
  }

  @Test
  void failToInsertDuplicateExternalIdAndJournalenhet() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    var arkivdel1JSON = getArkivdelJSON();
    var arkivdel2JSON = getArkivdelJSON();
    arkivdel1JSON.put("externalId", "externalId");
    arkivdel2JSON.put("externalId", "externalId");

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", arkivdel1JSON);
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    assertNotNull(arkivdelDTO.getId());

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", arkivdel2JSON);
    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

    delete("/arkiv/" + arkivDTO.getId());
  }

  @Test
  void testArkivdelListByExternalIdAndJournalenhet() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    var arkivdel1JSON = getArkivdelJSON();
    var arkivdel2JSON = getArkivdelJSON();
    arkivdel1JSON.put("externalId", "externalId");
    arkivdel2JSON.put("externalId", "externalId");
    arkivdel2JSON.put("journalenhet", underenhetId);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", arkivdel1JSON);
    var arkivdel1DTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    assertNotNull(arkivdel1DTO.getId());

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", arkivdel2JSON);
    var arkivdel2DTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    assertNotNull(arkivdel2DTO.getId());

    response = get("/arkivdel?externalId=externalId");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var resultListType = new TypeToken<ListResponseBody<ArkivdelDTO>>() {}.getType();
    ListResponseBody<ArkivdelDTO> arkivdelResultList =
        gson.fromJson(response.getBody(), resultListType);
    assertEquals(2, arkivdelResultList.getItems().size());
    assertEquals(arkivdel1DTO.getId(), arkivdelResultList.getItems().get(1).getId());
    assertEquals(arkivdel2DTO.getId(), arkivdelResultList.getItems().get(0).getId());

    response = get("/arkivdel?externalId=externalId&journalenhet=" + underenhetId);
    arkivdelResultList = gson.fromJson(response.getBody(), resultListType);
    assertEquals(1, arkivdelResultList.getItems().size());
    assertEquals(arkivdel2DTO.getId(), arkivdelResultList.getItems().get(0).getId());

    delete("/arkiv/" + arkivDTO.getId());
  }
}

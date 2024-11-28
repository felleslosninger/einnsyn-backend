package no.einnsyn.backend.entities.arkiv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.reflect.TypeToken;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.resultlist.ResultList;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ArkivControllerTest extends EinnsynControllerTestBase {

  // Test Arkiv lifecycle
  @Test
  void testArkivLifecycle() throws Exception {
    var arkivJSON = getArkivJSON();
    arkivJSON.put("tittel", "ParentArkiv");
    var response = post("/arkiv", arkivJSON);
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(arkivDTO.getId());

    var arkivdelJSON = getArkivdelJSON();
    arkivdelJSON.put("tittel", "Arkivdel1");
    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", arkivdelJSON);
    var arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    assertNotNull(arkivdelDTO.getId());
    assertEquals(arkivdelDTO.getParent().getId(), arkivDTO.getId());

    var arkivdel2JSON = getArkivdelJSON();
    arkivdel2JSON.put("tittel", "Arkivdel2");
    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", arkivdel2JSON);
    var arkivdel2DTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    assertNotNull(arkivdel2DTO.getId());
    assertEquals(arkivdel2DTO.getParent().getId(), arkivDTO.getId());

    var subArkivJSON = getArkivJSON();
    subArkivJSON.put("tittel", "SubArkiv1");
    response = post("/arkiv/" + arkivDTO.getId() + "/arkiv", subArkivJSON);
    var subArkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(subArkivDTO.getId());
    assertEquals(subArkivDTO.getParent().getId(), arkivDTO.getId());

    var subArkiv2 = getArkivJSON();
    subArkiv2.put("tittel", "SubArkiv2");
    response = post("/arkiv/" + arkivDTO.getId() + "/arkiv", subArkiv2);
    var subArkiv2DTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(subArkiv2DTO.getId());
    assertEquals(subArkiv2DTO.getParent().getId(), arkivDTO.getId());

    // Get list of subArkiv
    response = get("/arkiv/" + arkivDTO.getId() + "/arkiv");
    var resultListType = new TypeToken<ResultList<ArkivDTO>>() {}.getType();
    ResultList<ArkivDTO> arkivResultList = gson.fromJson(response.getBody(), resultListType);
    assertEquals(2, arkivResultList.getItems().size());
    assertEquals(subArkivDTO.getId(), arkivResultList.getItems().get(1).getId());
    assertEquals(subArkiv2DTO.getId(), arkivResultList.getItems().get(0).getId());

    // Get list of Arkivdel
    response = get("/arkiv/" + arkivDTO.getId() + "/arkivdel");
    resultListType = new TypeToken<ResultList<ArkivdelDTO>>() {}.getType();
    ResultList<ArkivdelDTO> arkivdelResultList = gson.fromJson(response.getBody(), resultListType);
    assertEquals(2, arkivdelResultList.getItems().size());
    assertEquals(arkivdelDTO.getId(), arkivdelResultList.getItems().get(1).getId());
    assertEquals(arkivdel2DTO.getId(), arkivdelResultList.getItems().get(0).getId());

    // Reverse order
    response = get("/arkiv/" + arkivDTO.getId() + "/arkivdel?sortOrder=asc");
    resultListType = new TypeToken<ResultList<ArkivdelDTO>>() {}.getType();
    arkivdelResultList = gson.fromJson(response.getBody(), resultListType);
    assertEquals(arkivdel2DTO.getId(), arkivdelResultList.getItems().get(1).getId());
    assertEquals(arkivdelDTO.getId(), arkivdelResultList.getItems().get(0).getId());

    response = get("/arkiv/" + arkivDTO.getId() + "/arkiv?sortOrder=asc");
    resultListType = new TypeToken<ResultList<ArkivDTO>>() {}.getType();
    arkivResultList = gson.fromJson(response.getBody(), resultListType);
    assertEquals(subArkiv2DTO.getId(), arkivResultList.getItems().get(1).getId());
    assertEquals(subArkivDTO.getId(), arkivResultList.getItems().get(0).getId());

    // Delete arkiv
    response = delete("/arkiv/" + arkivDTO.getId());
    var deletedArkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertEquals(Boolean.TRUE, deletedArkivDTO.getDeleted());

    // Make sure everything is deleted
    get("/arkiv/" + arkivDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkiv/" + arkivDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkiv/" + subArkivDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkiv/" + subArkiv2DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkivdel/" + arkivdelDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkivdel/" + arkivdel2DTO.getId()).getStatusCode());
  }

  @Test
  void failToInsertDuplicateExternalIdAndJournalenhet() throws Exception {
    var arkiv1JSON = getArkivJSON();
    var arkiv2JSON = getArkivJSON();
    arkiv1JSON.put("externalId", "externalId");
    arkiv2JSON.put("externalId", "externalId");

    var response = post("/arkiv", arkiv1JSON);
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(arkivDTO.getId());

    response = post("/arkiv", arkiv2JSON);
    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

    delete("/arkiv/" + arkivDTO.getId());
  }

  @Test
  void testArkivListByExternalIdAndJournalenhet() throws Exception {
    var arkiv1JSON = getArkivJSON();
    var arkiv2JSON = getArkivJSON();
    var arkiv3JSON = getArkivJSON();
    arkiv1JSON.put("externalId", "externalIdValue");
    arkiv2JSON.put("externalId", "externalIdValue");
    arkiv2JSON.put("journalenhet", underenhetId);

    var response = post("/arkiv", arkiv1JSON);
    var arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(arkivDTO.getId());

    response = post("/arkiv", arkiv2JSON);
    var arkiv2DTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(arkiv2DTO.getId());

    response = post("/arkiv", arkiv3JSON);
    var arkiv3DTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(arkiv3DTO.getId());

    response = get("/arkiv?externalIds=externalIdValue");
    var resultListType = new TypeToken<ResultList<ArkivDTO>>() {}.getType();
    ResultList<ArkivDTO> arkivResultList = gson.fromJson(response.getBody(), resultListType);
    assertEquals(2, arkivResultList.getItems().size());
    var resultListIds = arkivResultList.getItems().stream().map(ArkivDTO::getId).toList();
    assertTrue(resultListIds.contains(arkivDTO.getId()));
    assertTrue(resultListIds.contains(arkiv2DTO.getId()));

    response = get("/arkiv?externalIds=externalIdValue&journalenhet=" + underenhetId);
    arkivResultList = gson.fromJson(response.getBody(), resultListType);
    assertEquals(1, arkivResultList.getItems().size());
    assertEquals(arkiv2DTO.getId(), arkivResultList.getItems().get(0).getId());

    delete("/arkiv/" + arkivDTO.getId());
    delete("/arkiv/" + arkiv2DTO.getId());
    delete("/arkiv/" + arkiv3DTO.getId());
  }
}

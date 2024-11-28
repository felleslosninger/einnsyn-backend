package no.einnsyn.backend.entities.identifikator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.identifikator.models.IdentifikatorDTO;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.moetesak.models.MoetesakDTO;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class IdentifikatorControllerTest extends EinnsynControllerTestBase {

  ArkivDTO arkivDTO;
  MoetemappeDTO moetemappeDTO;
  MoetesakDTO moetesakDTO;

  @BeforeEach
  void setUp() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(arkivDTO.getId());
    response = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", getMoetemappeJSON());
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    assertNotNull(moetemappeDTO.getId());
    response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", getMoetesakJSON());
    moetesakDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);
  }

  @AfterEach
  void tearDown() throws Exception {
    // Delete arkiv
    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkivDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkiv/" + arkivDTO.getId()).getStatusCode());
  }

  @Test
  void testIdentifikatorLifecycle() throws Exception {
    var identifikatorId =
        moetesakDTO
            .getVedtak()
            .getExpandedObject()
            .getVotering()
            .get(0)
            .getExpandedObject()
            .getRepresenterer()
            .getId();
    assertNotNull(identifikatorId);

    // GET
    var response = get("/identifikator/" + identifikatorId);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // PUT
    var updateJSON = new JSONObject();
    updateJSON.put("navn", "New navn");
    updateJSON.put("identifikator", "New identifikator");
    updateJSON.put("initialer", "New initialer");
    updateJSON.put("epostadresse", "New epostadresse");
    response = patch("/identifikator/" + identifikatorId, updateJSON);
    var initialerDTO = gson.fromJson(response.getBody(), IdentifikatorDTO.class);
    assertEquals("New navn", initialerDTO.getNavn());
    assertEquals("New identifikator", initialerDTO.getIdentifikator());
    assertEquals("New initialer", initialerDTO.getInitialer());
    assertEquals("New epostadresse", initialerDTO.getEpostadresse());

    // GET
    response = get("/identifikator/" + identifikatorId);
    var getInitialerDTO = gson.fromJson(response.getBody(), IdentifikatorDTO.class);
    assertEquals("New navn", getInitialerDTO.getNavn());
    assertEquals("New identifikator", getInitialerDTO.getIdentifikator());
    assertEquals("New initialer", getInitialerDTO.getInitialer());
    assertEquals("New epostadresse", getInitialerDTO.getEpostadresse());

    // Delete
    assertEquals(HttpStatus.OK, delete("/identifikator/" + identifikatorId).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/identifikator/" + identifikatorId).getStatusCode());
  }
}

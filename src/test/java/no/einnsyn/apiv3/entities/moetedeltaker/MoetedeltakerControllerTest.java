package no.einnsyn.apiv3.entities.moetedeltaker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.moetedeltaker.models.MoetedeltakerDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
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
class MoetedeltakerControllerTest extends EinnsynControllerTestBase {

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
  void testMoetedeltakerLifecycle() throws Exception {
    var moetedeltakerId =
        moetesakDTO
            .getVedtak()
            .getExpandedObject()
            .getVotering()
            .get(0)
            .getExpandedObject()
            .getMoetedeltaker()
            .getId();
    assertNotNull(moetedeltakerId);

    // GET
    var response = get("/moetedeltaker/" + moetedeltakerId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var moetedeltakerDTO = gson.fromJson(response.getBody(), MoetedeltakerDTO.class);
    assertNotNull(moetedeltakerDTO);
    assertNotNull(moetedeltakerDTO.getId());

    // Update moetedeltaker
    var updateJSON = new JSONObject();
    updateJSON.put("moetedeltakerNavn", "New navn");
    updateJSON.put("moetedeltakerFunksjon", "New funksjon");
    response = patch("/moetedeltaker/" + moetedeltakerId, updateJSON);
    var updatedMoetedeltakerDTO = gson.fromJson(response.getBody(), MoetedeltakerDTO.class);
    assertEquals("New navn", updatedMoetedeltakerDTO.getMoetedeltakerNavn());
    assertEquals("New funksjon", updatedMoetedeltakerDTO.getMoetedeltakerFunksjon());

    // GET
    response = get("/moetedeltaker/" + moetedeltakerId);
    var getMoetedeltakerDTO = gson.fromJson(response.getBody(), MoetedeltakerDTO.class);
    assertEquals("New navn", getMoetedeltakerDTO.getMoetedeltakerNavn());
    assertEquals("New funksjon", getMoetedeltakerDTO.getMoetedeltakerFunksjon());

    // Delete moetedeltaker
    assertEquals(HttpStatus.OK, delete("/moetedeltaker/" + moetedeltakerId).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/moetedeltaker/" + moetedeltakerId).getStatusCode());
  }
}

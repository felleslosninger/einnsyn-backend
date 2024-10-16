package no.einnsyn.apiv3.entities.votering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.apiv3.entities.votering.models.VoteringDTO;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class VoteringControllerTest extends EinnsynControllerTestBase {

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
    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkivDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkiv/" + arkivDTO.getId()).getStatusCode());
  }

  @Test
  void testVoteringLifecycle() throws Exception {
    var voteringId = moetesakDTO.getVedtak().getExpandedObject().getVotering().get(0).getId();
    assertNotNull(voteringId);

    // Update stemme
    var voteringJSON = new JSONObject();
    voteringJSON.put("stemme", "Ja");
    var result = put("/votering/" + voteringId, voteringJSON);
    var updatedVoteringDTO = gson.fromJson(result.getBody(), VoteringDTO.class);
    assertEquals("Ja", updatedVoteringDTO.getStemme());

    // Update stemme (again)
    voteringJSON = new JSONObject();
    voteringJSON.put("stemme", "Nei");
    put("/votering/" + voteringId, voteringJSON);
    updatedVoteringDTO = gson.fromJson(get("/votering/" + voteringId).getBody(), VoteringDTO.class);
    assertEquals("Nei", updatedVoteringDTO.getStemme());

    // Update Moetedeltaker
    var oldMoetedeltakerId = updatedVoteringDTO.getMoetedeltaker().getId();
    voteringJSON = new JSONObject();
    voteringJSON.put("moetedeltaker", getMoetedeltakerJSON());
    put("/votering/" + voteringId, voteringJSON);
    updatedVoteringDTO = gson.fromJson(get("/votering/" + voteringId).getBody(), VoteringDTO.class);
    assertNotEquals(oldMoetedeltakerId, updatedVoteringDTO.getMoetedeltaker().getId());

    // The old moetedeltaker should be deleted (orphaned)
    assertEquals(HttpStatus.NOT_FOUND, get("/moetedeltaker/" + oldMoetedeltakerId).getStatusCode());

    // The new moetedeltaker should be found
    assertNotNull(updatedVoteringDTO.getMoetedeltaker().getId());

    // Updated "representerer"
    var oldRepresentererId = updatedVoteringDTO.getRepresenterer().getId();
    voteringJSON = new JSONObject();
    voteringJSON.put("representerer", getIdentifikatorJSON());
    put("/votering/" + voteringId, voteringJSON);
    updatedVoteringDTO = gson.fromJson(get("/votering/" + voteringId).getBody(), VoteringDTO.class);
    assertNotEquals(oldRepresentererId, updatedVoteringDTO.getRepresenterer().getId());

    // The old representerer should be deleted (orphaned)
    assertEquals(HttpStatus.NOT_FOUND, get("/moetedeltaker/" + oldRepresentererId).getStatusCode());

    // The new representerer should be found
    assertNotNull(updatedVoteringDTO.getRepresenterer().getId());

    // Delete votering
    assertEquals(HttpStatus.OK, delete("/votering/" + voteringId).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/votering/" + voteringId).getStatusCode());
  }

  @Test
  void testMoetedeltakerOrphanRemoval() throws Exception {
    var votering1DTO =
        moetesakDTO.getVedtak().getExpandedObject().getVotering().get(0).getExpandedObject();
    var moetedeltaker = votering1DTO.getMoetedeltaker().getExpandedObject();

    // Create a new Moetesak with a new Votering that has the same Moetedeltaker
    var moetesakJSON = getMoetesakJSON();
    var votering2JSON = getVoteringJSON();
    var vedtakJSON = getVedtakJSON();
    votering2JSON.put("moetedeltaker", moetedeltaker.getId());
    vedtakJSON.put("votering", new JSONArray(List.of(votering2JSON)));
    moetesakJSON.put("vedtak", vedtakJSON);
    var response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", moetesakJSON);
    var moetesak2DTO = gson.fromJson(response.getBody(), MoetesakDTO.class);

    // Verify that the moetedeltaker exists
    response = get("/moetedeltaker/" + moetedeltaker.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Delete the first votering
    assertEquals(HttpStatus.OK, delete("/votering/" + votering1DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/votering/" + votering1DTO.getId()).getStatusCode());

    // Verify that the moetedeltaker still exists
    response = get("/moetedeltaker/" + moetedeltaker.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Delete the second votering
    var vedtakDTO = moetesak2DTO.getVedtak().getExpandedObject();
    assertNotNull(vedtakDTO);
    var votering2Id = vedtakDTO.getVotering().get(0).getId();
    assertEquals(HttpStatus.OK, delete("/votering/" + votering2Id).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/votering/" + votering2Id).getStatusCode());

    // Verify that the moetedeltaker is deleted
    response = get("/moetedeltaker/" + moetedeltaker.getId());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    // Clean up
    assertEquals(HttpStatus.OK, delete("/moetesak/" + moetesak2DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/moetesak/" + moetesak2DTO.getId()).getStatusCode());
  }

  @Test
  void testRepresentererOrphanRemoval() throws Exception {
    var votering1DTO =
        moetesakDTO.getVedtak().getExpandedObject().getVotering().get(0).getExpandedObject();
    var representerer = votering1DTO.getRepresenterer().getExpandedObject();

    // Create a new Moetesak with a new Votering that has the same Representerer
    var moetesakJSON = getMoetesakJSON();
    var votering2JSON = getVoteringJSON();
    var vedtakJSON = getVedtakJSON();
    votering2JSON.put("representerer", representerer.getId());
    vedtakJSON.put("votering", new JSONArray(List.of(votering2JSON)));
    moetesakJSON.put("vedtak", vedtakJSON);
    var response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", moetesakJSON);
    var moetesak2DTO = gson.fromJson(response.getBody(), MoetesakDTO.class);

    // Verify that the Identifikator exists
    response = get("/identifikator/" + representerer.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Delete the first votering
    assertEquals(HttpStatus.OK, delete("/votering/" + votering1DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/votering/" + votering1DTO.getId()).getStatusCode());

    // Verify that the Identifikator still exists
    response = get("/identifikator/" + representerer.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Delete the second votering
    var vedtakDTO = moetesak2DTO.getVedtak().getExpandedObject();
    assertNotNull(vedtakDTO);
    var votering2Id = vedtakDTO.getVotering().get(0).getId();
    assertEquals(HttpStatus.OK, delete("/votering/" + votering2Id).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/votering/" + votering2Id).getStatusCode());

    // Verify that the Identifikator is deleted
    response = get("/identifikator/" + representerer.getId());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    // Clean up
    assertEquals(HttpStatus.OK, delete("/moetesak/" + moetesak2DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/moetesak/" + moetesak2DTO.getId()).getStatusCode());
  }
}

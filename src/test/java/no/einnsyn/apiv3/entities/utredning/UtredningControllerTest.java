package no.einnsyn.apiv3.entities.utredning;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import no.einnsyn.apiv3.entities.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.apiv3.entities.utredning.models.UtredningDTO;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class UtredningControllerTest extends EinnsynControllerTestBase {

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
    // Delete moetesak
    assertEquals(HttpStatus.OK, delete("/moetesak/" + moetesakDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/moetesak/" + moetesakDTO.getId()).getStatusCode());

    assertEquals(HttpStatus.OK, delete("/moetemappe/" + moetemappeDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/moetemappe/" + moetemappeDTO.getId()).getStatusCode());

    // Delete arkiv
    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkivDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkiv/" + arkivDTO.getId()).getStatusCode());
  }

  @Test
  void testUtredningLifecycle() throws Exception {
    var moetesakJSON = getMoetesakJSON();
    var utredningJSON = moetesakJSON.getJSONObject("utredning");
    var response = put("/moetesak/" + moetesakDTO.getId(), moetesakJSON);
    var moetesakDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);
    var utredningDTO = moetesakDTO.getUtredning().getExpandedObject();
    assertNotNull(utredningDTO.getId());
    assertEquals(
        utredningJSON.getJSONObject("saksbeskrivelse").getString("tekstInnhold"),
        utredningDTO.getSaksbeskrivelse().getExpandedObject().getTekstInnhold());
    assertEquals(
        utredningJSON.getJSONObject("saksbeskrivelse").getString("tekstFormat"),
        utredningDTO.getSaksbeskrivelse().getExpandedObject().getTekstFormat());
    assertEquals(
        utredningJSON.getJSONObject("innstilling").getString("tekstInnhold"),
        utredningDTO.getInnstilling().getExpandedObject().getTekstInnhold());
    assertEquals(
        utredningJSON.getJSONObject("innstilling").getString("tekstFormat"),
        utredningDTO.getInnstilling().getExpandedObject().getTekstFormat());

    // Update saksbeskrivelse
    var updateJSON = new JSONObject();
    var saksbeskrivelseJSON = getMoetesaksbeskrivelseJSON();
    saksbeskrivelseJSON.put("tekstInnhold", "UPDATED TEKSTINNHOLD");
    saksbeskrivelseJSON.put("tekstFormat", "UPDATED TEKSTFORMAT");
    updateJSON.put("saksbeskrivelse", saksbeskrivelseJSON);
    response = put("/utredning/" + utredningDTO.getId(), updateJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    utredningDTO = gson.fromJson(response.getBody(), UtredningDTO.class);
    assertEquals(
        "UPDATED TEKSTINNHOLD",
        utredningDTO.getSaksbeskrivelse().getExpandedObject().getTekstInnhold());
    assertEquals(
        "UPDATED TEKSTFORMAT",
        utredningDTO.getSaksbeskrivelse().getExpandedObject().getTekstFormat());

    // Update innstilling
    updateJSON = new JSONObject();
    var innstillingJSON = getMoetesaksbeskrivelseJSON();
    innstillingJSON.put("tekstInnhold", "UPDATED INNSTILLINGTEKSTINNHOLD");
    innstillingJSON.put("tekstFormat", "UPDATED INNSTILLINGTEKSTFORMAT");
    updateJSON.put("innstilling", innstillingJSON);
    response = put("/utredning/" + utredningDTO.getId(), updateJSON);
    utredningDTO = gson.fromJson(response.getBody(), UtredningDTO.class);
    assertEquals(
        "UPDATED INNSTILLINGTEKSTINNHOLD",
        utredningDTO.getInnstilling().getExpandedObject().getTekstInnhold());
    assertEquals(
        "UPDATED INNSTILLINGTEKSTFORMAT",
        utredningDTO.getInnstilling().getExpandedObject().getTekstFormat());

    // Add utredningsdokument
    var oldDocCount = utredningJSON.getJSONArray("utredningsdokument").length();
    updateJSON = new JSONObject();
    updateJSON.put("utredningsdokument", new JSONArray(List.of(getDokumentbeskrivelseJSON())));
    response = put("/utredning/" + utredningDTO.getId(), updateJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    utredningDTO = gson.fromJson(response.getBody(), UtredningDTO.class);
    assertEquals(oldDocCount + 1, utredningDTO.getUtredningsdokument().size());

    updateJSON = new JSONObject();
    updateJSON.put(
        "utredningsdokument",
        new JSONArray(List.of(getDokumentbeskrivelseJSON(), getDokumentbeskrivelseJSON())));
    response = put("/utredning/" + utredningDTO.getId(), updateJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    utredningDTO = gson.fromJson(response.getBody(), UtredningDTO.class);
    assertEquals(oldDocCount + 3, utredningDTO.getUtredningsdokument().size());

    // Delete
    response = delete("/utredning/" + utredningDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Check the updated Moetesak
    response = get("/moetesak/" + moetesakDTO.getId());
    moetesakDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);
    assertNull(moetesakDTO.getUtredning());
    assertEquals(HttpStatus.NOT_FOUND, get("/utredning/" + utredningDTO.getId()).getStatusCode());
  }
}

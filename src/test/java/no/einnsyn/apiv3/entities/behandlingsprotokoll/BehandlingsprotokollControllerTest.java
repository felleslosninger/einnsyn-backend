package no.einnsyn.apiv3.entities.behandlingsprotokoll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.behandlingsprotokoll.models.BehandlingsprotokollDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class BehandlingsprotokollControllerTest extends EinnsynControllerTestBase {

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
  void testBehandlingsprotokollLifecycle() throws Exception {
    var behandlingsprotokollDTO =
        moetesakDTO.getVedtak().getExpandedObject().getBehandlingsprotokoll().getExpandedObject();
    var updateJSON = new JSONObject();
    updateJSON.put("tekstInnhold", "New tekstInnhold");
    updateJSON.put("tekstFormat", "New tekstFormat");
    var response = put("/behandlingsprotokoll/" + behandlingsprotokollDTO.getId(), updateJSON);
    var updatedBehandlingsprotokollDTO =
        gson.fromJson(response.getBody(), BehandlingsprotokollDTO.class);
    assertEquals("New tekstInnhold", updatedBehandlingsprotokollDTO.getTekstInnhold());
    assertEquals("New tekstFormat", updatedBehandlingsprotokollDTO.getTekstFormat());

    // GET
    response = get("/behandlingsprotokoll/" + behandlingsprotokollDTO.getId());
    var getBehandlingsprotokollDTO =
        gson.fromJson(response.getBody(), BehandlingsprotokollDTO.class);
    assertEquals("New tekstInnhold", getBehandlingsprotokollDTO.getTekstInnhold());
    assertEquals("New tekstFormat", getBehandlingsprotokollDTO.getTekstFormat());

    // Delete behandlingsprotokoll
    assertEquals(
        HttpStatus.OK,
        delete("/behandlingsprotokoll/" + behandlingsprotokollDTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND,
        get("/behandlingsprotokoll/" + behandlingsprotokollDTO.getId()).getStatusCode());
  }
}

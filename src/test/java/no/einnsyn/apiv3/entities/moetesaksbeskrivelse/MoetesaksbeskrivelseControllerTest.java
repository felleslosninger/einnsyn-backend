package no.einnsyn.apiv3.entities.moetesaksbeskrivelse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.apiv3.entities.moetesaksbeskrivelse.models.MoetesaksbeskrivelseDTO;
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
class MoetesaksbeskrivelseControllerTest extends EinnsynControllerTestBase {

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
  void cleanUp() throws Exception {
    assertEquals(HttpStatus.OK, delete("/moetesak/" + moetesakDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/moetesak/" + moetesakDTO.getId()).getStatusCode());

    assertEquals(HttpStatus.OK, delete("/moetemappe/" + moetemappeDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/moetemappe/" + moetemappeDTO.getId()).getStatusCode());

    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkivDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkiv/" + arkivDTO.getId()).getStatusCode());
  }

  @Test
  void testMoetesaksbeskrivelseLifecycle() throws Exception {
    var moetesakJSON = new JSONObject();
    moetesakJSON.put("innstilling", getMoetesaksbeskrivelseJSON());
    var response = put("/moetesak/" + moetesakDTO.getId(), moetesakJSON);
    var moetesakDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);
    var innstillingDTO = moetesakDTO.getInnstilling().getExpandedObject();
    assertNotNull(innstillingDTO);
    assertNotNull(innstillingDTO.getId());
    assertEquals("tekstInnhold", innstillingDTO.getTekstInnhold());
    assertEquals("tekstFormat", innstillingDTO.getTekstFormat());

    // Update moetesaksbeskrivelse
    var updateJSON = new JSONObject();
    updateJSON.put("tekstInnhold", "A");
    updateJSON.put("tekstFormat", "B");
    response = put("/moetesaksbeskrivelse/" + innstillingDTO.getId(), updateJSON);
    var updatedInnstillingDTO = gson.fromJson(response.getBody(), MoetesaksbeskrivelseDTO.class);
    assertEquals("A", updatedInnstillingDTO.getTekstInnhold());
    assertEquals("B", updatedInnstillingDTO.getTekstFormat());

    // Delete moetesaksbeskrivelse
    delete("/moetesaksbeskrivelse/" + updatedInnstillingDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Verify that the moetesaksbeskrivelse is deleted
    response = get("/moetesaksbeskrivelse/" + updatedInnstillingDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    // Verify that the moetesak is updated
    response = get("/moetesak/" + moetesakDTO.getId());
    moetesakDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);
    assertNull(moetesakDTO.getInnstilling());
  }
}

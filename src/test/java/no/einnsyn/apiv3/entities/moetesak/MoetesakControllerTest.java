package no.einnsyn.apiv3.entities.moetesak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.reflect.TypeToken;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class MoetesakControllerTest extends EinnsynControllerTestBase {

  MoetemappeDTO moetemappeDTO;

  @BeforeAll
  public void setUp() throws Exception {
    var result = post("/arkiv", getArkivJSON());
    var arkivDTO = gson.fromJson(result.getBody(), ArkivDTO.class);
    assertNotNull(arkivDTO.getId());
    result = post("/arkiv/" + arkivDTO.getId() + "/moetemappe", getMoetemappeJSON());
    moetemappeDTO = gson.fromJson(result.getBody(), MoetemappeDTO.class);
    assertNotNull(moetemappeDTO.getId());
  }

  @AfterAll
  public void tearDown() throws Exception {
    var result = delete("/moetemappe/" + moetemappeDTO.getId());
    assertEquals(HttpStatus.OK, result.getStatusCode());
  }

  @Test
  void testCreateMoetesak() throws Exception {
    var moetesakJSON = getMoetesakJSON();
    var result = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", moetesakJSON);
    var moetesakDTO = gson.fromJson(result.getBody(), MoetesakDTO.class);
    var moetesakId = moetesakDTO.getId();
    assertNotNull(moetesakId);

    result = get("/moetesak/" + moetesakDTO.getId());
    moetesakDTO = gson.fromJson(result.getBody(), MoetesakDTO.class);
    assertEquals(moetesakId, moetesakDTO.getId());

    result = delete("/moetesak/" + moetesakDTO.getId());
    assertEquals(HttpStatus.OK, result.getStatusCode());

    result = get("/moetesak/" + moetesakDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
  }

  @Test
  void testMoetesakWithChildren() throws Exception {
    var utredning = getUtredningJSON();
    var vedtak = getVedtakJSON();
    var moetesakJSON = getMoetesakJSON();
    moetesakJSON.put("utredning", utredning);
    moetesakJSON.put("vedtak", vedtak);
    moetesakJSON.put("innstilling", getMoetesaksbeskrivelseJSON());

    var result = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", moetesakJSON);
    System.err.println(result.getBody());
    var moetesakDTO = gson.fromJson(result.getBody(), MoetesakDTO.class);
    var moetesakId = moetesakDTO.getId();
    assertNotNull(moetesakId);

    var utredningDTO = moetesakDTO.getUtredning().getExpandedObject();
    assertNotNull(utredningDTO.getId());
    assertEquals("Utredning", utredningDTO.getEntity());

    var utredningSaksbeskrivelseDTO = utredningDTO.getSaksbeskrivelse().getExpandedObject();
    assertNotNull(utredningSaksbeskrivelseDTO.getId());
    assertEquals("Moetesaksbeskrivelse", utredningSaksbeskrivelseDTO.getEntity());

    var utredningInnstilling = utredningDTO.getInnstilling().getExpandedObject();
    assertNotNull(utredningInnstilling.getId());
    assertEquals("Moetesaksbeskrivelse", utredningInnstilling.getEntity());

    var utredningsdokument = utredningDTO.getUtredningsdokument();
    assertEquals(2, utredningsdokument.size());
    for (var utredningsdokumentField : utredningsdokument) {
      var utredningsdokumentDTO = utredningsdokumentField.getExpandedObject();
      assertNotNull(utredningsdokumentDTO.getId());
      assertEquals("Dokumentbeskrivelse", utredningsdokumentDTO.getEntity());
    }

    var innstilling = moetesakDTO.getInnstilling().getExpandedObject();
    assertNotNull(innstilling.getId());
    assertEquals("Moetesaksbeskrivelse", innstilling.getEntity());

    var vedtakDTO = moetesakDTO.getVedtak().getExpandedObject();
    assertNotNull(vedtakDTO.getId());
    assertEquals("Vedtak", vedtakDTO.getEntity());

    result = get("/moetesak/" + moetesakDTO.getId());
    moetesakDTO = gson.fromJson(result.getBody(), MoetesakDTO.class);
    assertEquals(moetesakId, moetesakDTO.getId());

    result = delete("/moetesak/" + moetesakDTO.getId());
    assertEquals(HttpStatus.OK, result.getStatusCode());

    result = get("/moetesak/" + moetesakDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());

    result = get("/utredning/" + utredningDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());

    result = get("/moetesaksbeskrivelse/" + utredningSaksbeskrivelseDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());

    result = get("/moetesaksbeskrivelse/" + utredningInnstilling.getId());
    assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());

    result = get("/moetesaksbeskrivelse/" + innstilling.getId());
    assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());

    result = get("/vedtak/" + vedtakDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());

    for (var utredningsdokumentField : utredningsdokument) {
      var utredningsdokumentDTO = utredningsdokumentField.getExpandedObject();
      result = get("/dokumentbeskrivelse/" + utredningsdokumentDTO.getId());
      assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }
  }

  @Test
  void testAddUtredning() throws Exception {
    var response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", getMoetesakJSON());
    var moetesakDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);
    var moetesakId = moetesakDTO.getId();

    response =
        post("/moetesak/" + moetesakId + "/dokumentbeskrivelse", getDokumentbeskrivelseJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var dokumentbeskrivelseDTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);
    assertNotNull(dokumentbeskrivelseDTO.getId());

    // Add another
    response =
        post("/moetesak/" + moetesakId + "/dokumentbeskrivelse", getDokumentbeskrivelseJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var dokumentbeskrivelseDTO2 = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);
    assertNotNull(dokumentbeskrivelseDTO2.getId());

    var type = new TypeToken<ResultList<DokumentbeskrivelseDTO>>() {}.getType();
    response = get("/moetesak/" + moetesakId + "/dokumentbeskrivelse");
    ResultList<DokumentbeskrivelseDTO> dokumentbeskrivelseList =
        gson.fromJson(response.getBody(), type);
    assertEquals(2, dokumentbeskrivelseList.getItems().size());
    assertEquals(dokumentbeskrivelseDTO.getId(), dokumentbeskrivelseList.getItems().get(1).getId());
    assertEquals(
        dokumentbeskrivelseDTO2.getId(), dokumentbeskrivelseList.getItems().get(0).getId());

    response = delete("/moetesak/" + moetesakId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/moetesak/" + moetesakId).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND,
        get("/dokumentbeskrivelse/" + dokumentbeskrivelseDTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND,
        get("/dokumentbeskrivelse/" + dokumentbeskrivelseDTO2.getId()).getStatusCode());
  }
}

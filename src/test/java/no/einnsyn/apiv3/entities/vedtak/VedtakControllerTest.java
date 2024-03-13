package no.einnsyn.apiv3.entities.vedtak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.reflect.TypeToken;
import java.util.List;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.apiv3.entities.vedtak.models.VedtakDTO;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class VedtakControllerTest extends EinnsynControllerTestBase {

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
  void testVedtakLifecycle() throws Exception {
    var vedtakDTO = moetesakDTO.getVedtak().getExpandedObject();
    assertNotNull(vedtakDTO);
    assertNotNull(vedtakDTO.getId());

    // Check existing vedtakstekst
    var vedtakstekstId = vedtakDTO.getVedtakstekst().getId();
    var result = get("/moetesaksbeskrivelse/" + vedtakstekstId);
    assertEquals(HttpStatus.OK, result.getStatusCode());

    // Check behandlingsprotokoll
    var behandlingsprotokollId = vedtakDTO.getBehandlingsprotokoll().getId();
    result = get("/behandlingsprotokoll/" + behandlingsprotokollId);
    assertEquals(HttpStatus.OK, result.getStatusCode());

    // Check votering
    var voteringFieldList = vedtakDTO.getVotering();
    for (var voteringField : voteringFieldList) {
      result = get("/votering/" + voteringField.getId());
      assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    // Check behandlingsprotokoll
    var vedtaksdokumentFieldList = vedtakDTO.getVedtaksdokument();
    for (var vedtaksdokumentField : vedtaksdokumentFieldList) {
      result = get("/dokumentbeskrivelse/" + vedtaksdokumentField.getId());
      assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    var updateJSON = new JSONObject();
    updateJSON.put("dato", "2021-02-02");
    updateJSON.put("vedtakstekst", getMoetesaksbeskrivelseJSON());
    updateJSON.put("behandlingsprotokoll", getBehandlingsprotokollJSON());
    updateJSON.put("votering", new JSONArray(List.of(getVoteringJSON())));
    updateJSON.put("vedtaksdokument", new JSONArray(List.of(getDokumentbeskrivelseJSON())));

    var response = put("/vedtak/" + vedtakDTO.getId(), updateJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    vedtakDTO = gson.fromJson(response.getBody(), VedtakDTO.class);

    // Check that the vedtakstekst is replaced
    result = get("/moetesaksbeskrivelse/" + vedtakstekstId);
    assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    result = get("/moetesaksbeskrivelse/" + vedtakDTO.getVedtakstekst().getId());
    assertEquals(HttpStatus.OK, result.getStatusCode());

    // Check that the behandlingsprotokoll is replaced
    result = get("/behandlingsprotokoll/" + behandlingsprotokollId);
    assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    result = get("/behandlingsprotokoll/" + vedtakDTO.getBehandlingsprotokoll().getId());
    assertEquals(HttpStatus.OK, result.getStatusCode());

    // Match the updated voteringFieldList against the old one
    var newVoteringFieldList = vedtakDTO.getVotering();
    assertEquals(voteringFieldList.size() + 1, newVoteringFieldList.size());
    for (var voteringField : voteringFieldList) {
      assertNotNull(voteringField.getExpandedObject());
      var match =
          newVoteringFieldList.stream()
              .filter(v -> v.getId().equals(voteringField.getId()))
              .findFirst()
              .orElse(null);
      assertNotNull(match);
    }

    // Make sure we can get all Votering objects
    for (var voteringField : newVoteringFieldList) {
      result = get("/votering/" + voteringField.getId());
      assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    // Match the updated vedtaksdokumentFieldList against the old one
    var newVedtaksdokumentFieldList = vedtakDTO.getVedtaksdokument();
    assertEquals(vedtaksdokumentFieldList.size() + 1, newVedtaksdokumentFieldList.size());
    for (var vedtaksdokumentField : vedtaksdokumentFieldList) {
      assertNotNull(vedtaksdokumentField.getExpandedObject());
      var match =
          newVedtaksdokumentFieldList.stream()
              .filter(v -> v.getId().equals(vedtaksdokumentField.getId()))
              .findFirst()
              .orElse(null);
      assertNotNull(match);
    }

    // Make sure we can get all Vedtaksdokument objects
    for (var vedtaksdokumentField : newVedtaksdokumentFieldList) {
      result = get("/dokumentbeskrivelse/" + vedtaksdokumentField.getId());
      assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    // Delete vedtak
    assertEquals(HttpStatus.OK, delete("/vedtak/" + vedtakDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/vedtak/" + vedtakDTO.getId()).getStatusCode());
  }

  @Test
  void testVedtaksdokumentPagination() throws Exception {
    var vedtakDTO = moetesakDTO.getVedtak().getExpandedObject();

    // Make sure there are Vedtaksdokuments
    assertTrue(vedtakDTO.getVedtaksdokument().size() > 0);

    // Delete old vedtaksdokument
    for (var dokument : vedtakDTO.getVedtaksdokument()) {
      assertEquals(
          HttpStatus.OK,
          delete("/vedtak/" + vedtakDTO.getId() + "/vedtaksdokument/" + dokument.getId())
              .getStatusCode());
    }

    // Add three Vedtaksdokument
    var response =
        post("/vedtak/" + vedtakDTO.getId() + "/vedtaksdokument", getDokumentbeskrivelseJSON());
    var dok1DTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);
    response =
        post("/vedtak/" + vedtakDTO.getId() + "/vedtaksdokument", getDokumentbeskrivelseJSON());
    var dok2DTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);
    response =
        post("/vedtak/" + vedtakDTO.getId() + "/vedtaksdokument", getDokumentbeskrivelseJSON());
    var dok3DTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);

    var type = new TypeToken<ResultList<DokumentbeskrivelseDTO>>() {}.getType();
    ResultList<DokumentbeskrivelseDTO> resultList;

    // DESC
    response = get("/vedtak/" + vedtakDTO.getId() + "/vedtaksdokument");
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(3, resultList.getItems().size());
    assertEquals(dok3DTO.getId(), resultList.getItems().get(0).getId());
    assertEquals(dok2DTO.getId(), resultList.getItems().get(1).getId());
    assertEquals(dok1DTO.getId(), resultList.getItems().get(2).getId());

    // DESC, startingAfter
    response =
        get("/vedtak/" + vedtakDTO.getId() + "/vedtaksdokument?startingAfter=" + dok2DTO.getId());
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(1, resultList.getItems().size());
    assertEquals(dok1DTO.getId(), resultList.getItems().get(0).getId());

    // DESC, endingBefore
    response =
        get("/vedtak/" + vedtakDTO.getId() + "/vedtaksdokument?endingBefore=" + dok2DTO.getId());
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(1, resultList.getItems().size());
    assertEquals(dok3DTO.getId(), resultList.getItems().get(0).getId());

    // ASC
    response = get("/vedtak/" + vedtakDTO.getId() + "/vedtaksdokument?sortOrder=asc");
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(3, resultList.getItems().size());
    assertEquals(dok1DTO.getId(), resultList.getItems().get(0).getId());
    assertEquals(dok2DTO.getId(), resultList.getItems().get(1).getId());
    assertEquals(dok3DTO.getId(), resultList.getItems().get(2).getId());

    // ASC, startingAfter
    response =
        get(
            "/vedtak/"
                + vedtakDTO.getId()
                + "/vedtaksdokument?sortOrder=asc&startingAfter="
                + dok2DTO.getId());
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(1, resultList.getItems().size());
    assertEquals(dok3DTO.getId(), resultList.getItems().get(0).getId());

    // ASC, endingBefore
    response =
        get(
            "/vedtak/"
                + vedtakDTO.getId()
                + "/vedtaksdokument?sortOrder=asc&endingBefore="
                + dok2DTO.getId());
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(1, resultList.getItems().size());
    assertEquals(dok1DTO.getId(), resultList.getItems().get(0).getId());

    // Delete
    assertEquals(
        HttpStatus.OK,
        delete("/vedtak/" + vedtakDTO.getId() + "/vedtaksdokument/" + dok1DTO.getId())
            .getStatusCode());
    assertEquals(
        HttpStatus.OK,
        delete("/vedtak/" + vedtakDTO.getId() + "/vedtaksdokument/" + dok2DTO.getId())
            .getStatusCode());
    assertEquals(
        HttpStatus.OK,
        delete("/vedtak/" + vedtakDTO.getId() + "/vedtaksdokument/" + dok3DTO.getId())
            .getStatusCode());
  }
}

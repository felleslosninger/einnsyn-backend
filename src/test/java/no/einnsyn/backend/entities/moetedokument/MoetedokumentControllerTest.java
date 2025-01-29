package no.einnsyn.backend.entities.moetedokument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.responses.models.ListResponseBody;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.shaded.com.google.common.reflect.TypeToken;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MoetedokumentControllerTest extends EinnsynControllerTestBase {

  ArkivDTO arkivDTO;
  ArkivdelDTO arkivdelDTO;

  @BeforeEach
  void setUp() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(arkivDTO.getId());

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    assertNotNull(arkivdelDTO.getId());
  }

  @AfterEach
  void tearDown() throws Exception {
    // Delete arkiv
    assertEquals(HttpStatus.OK, delete("/arkiv/" + arkivDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkiv/" + arkivDTO.getId()).getStatusCode());
  }

  @Test
  void testMoetedokumentLifecycle() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", getMoetemappeJSON());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    assertNotNull(moetemappeDTO.getId());

    var moetedokumentId = moetemappeDTO.getMoetedokument().get(0).getId();

    // GET
    response = get("/moetedokument/" + moetedokumentId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var moetedokumentDTO = gson.fromJson(response.getBody(), MoetedokumentDTO.class);
    var originalKorrespondansepartList = moetedokumentDTO.getKorrespondansepart();
    var originalDokumentbeskrivelseList = moetedokumentDTO.getDokumentbeskrivelse();

    // PUT
    var moetedokumentJSON = new JSONObject();
    moetedokumentJSON.put("moetedokumenttype", "new type");
    moetedokumentJSON.put("saksbehandler", "new saksbehandler");
    moetedokumentJSON.put("saksbehandlerSensitiv", "new saksbehandlerSensitiv");
    moetedokumentJSON.put(
        "korrespondansepart", new JSONArray(List.of(getKorrespondansepartJSON())));
    moetedokumentJSON.put(
        "dokumentbeskrivelse", new JSONArray(List.of(getDokumentbeskrivelseJSON())));
    response = patch("/moetedokument/" + moetedokumentId, moetedokumentJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    moetedokumentDTO = gson.fromJson(response.getBody(), MoetedokumentDTO.class);
    assertEquals("new type", moetedokumentDTO.getMoetedokumenttype());
    assertEquals("new saksbehandler", moetedokumentDTO.getSaksbehandler());
    assertEquals("new saksbehandlerSensitiv", moetedokumentDTO.getSaksbehandlerSensitiv());
    assertEquals(
        originalKorrespondansepartList.size() + 1, moetedokumentDTO.getKorrespondansepart().size());
    assertEquals(
        originalDokumentbeskrivelseList.size() + 1,
        moetedokumentDTO.getDokumentbeskrivelse().size());

    // DELETE
    assertEquals(HttpStatus.OK, delete("/moetedokument/" + moetedokumentId).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/moetedokument/" + moetedokumentId).getStatusCode());
  }

  @Test
  void testOrphanKorrespondansepartDeletion() throws Exception {
    // Add a Saksmappe and Moetemappe for the Journalpost and Moetedokument
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", getMoetemappeJSON());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    assertNotNull(moetemappeDTO.getId());

    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", getJournalpostJSON());
    var journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertNotNull(journalpostDTO.getId());

    response =
        post("/moetemappe/" + moetemappeDTO.getId() + "/moetedokument", getMoetedokumentJSON());
    var moetedokumentDTO = gson.fromJson(response.getBody(), MoetedokumentDTO.class);
    assertNotNull(moetedokumentDTO.getId());

    // Add dokumentbeskrivelse to the Moetedokument
    var dokbeskJSON = getDokumentbeskrivelseJSON();
    response =
        post("/moetedokument/" + moetedokumentDTO.getId() + "/dokumentbeskrivelse", dokbeskJSON);
    var dokbeskDTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);

    // Add the same dokumentbeskrivelse to the Journalpost
    var updateJSON = new JSONObject();
    updateJSON.put("dokumentbeskrivelse", new JSONArray(List.of(dokbeskDTO.getId())));
    response = patch("/journalpost/" + journalpostDTO.getId(), updateJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Delete the Moetedokument
    assertEquals(
        HttpStatus.OK, delete("/moetedokument/" + moetedokumentDTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/moetemappe/" + moetedokumentDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, get("/dokumentbeskrivelse/" + dokbeskDTO.getId()).getStatusCode());

    // Delete the Journalpost
    assertEquals(HttpStatus.OK, delete("/journalpost/" + journalpostDTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/journalpost/" + journalpostDTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/dokumentbeskrivelse/" + dokbeskDTO.getId()).getStatusCode());

    // Check the other way (delete Journalpost first)
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", getJournalpostJSON());
    journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    response =
        post("/moetemappe/" + moetemappeDTO.getId() + "/moetedokument", getMoetedokumentJSON());
    moetedokumentDTO = gson.fromJson(response.getBody(), MoetedokumentDTO.class);

    // Add Dokumentbeskrivelse to the Journalpost
    response =
        post(
            "/journalpost/" + journalpostDTO.getId() + "/dokumentbeskrivelse",
            getDokumentbeskrivelseJSON());
    dokbeskDTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);
    var dokbeskId = dokbeskDTO.getId();

    // Make sure the Dokumentbeskrivelse is added to the Journalpost
    response = get("/journalpost/" + journalpostDTO.getId());
    journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertTrue(
        journalpostDTO.getDokumentbeskrivelse().stream()
            .anyMatch(db -> db.getId().equals(dokbeskId)));

    // Add the same dokumentbeskrivelse to the Moetedokument
    updateJSON = new JSONObject();
    updateJSON.put("dokumentbeskrivelse", new JSONArray(List.of(dokbeskDTO.getId())));
    response = patch("/moetedokument/" + moetedokumentDTO.getId(), updateJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Make sure the Dokumentbeskrivelse is added to the Moetedokument
    response = get("/moetedokument/" + moetedokumentDTO.getId());
    moetedokumentDTO = gson.fromJson(response.getBody(), MoetedokumentDTO.class);
    assertTrue(
        moetedokumentDTO.getDokumentbeskrivelse().stream()
            .anyMatch(db -> db.getId().equals(dokbeskId)));

    // Delete the Journalpost
    assertEquals(HttpStatus.OK, delete("/journalpost/" + journalpostDTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/journalpost/" + journalpostDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, get("/dokumentbeskrivelse/" + dokbeskDTO.getId()).getStatusCode());

    // Delete the Moetedokument
    assertEquals(
        HttpStatus.OK, delete("/moetedokument/" + moetedokumentDTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/moetedokument/" + moetedokumentDTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/dokumentbeskrivelse/" + dokbeskDTO.getId()).getStatusCode());
  }

  @Test
  void testDokumentbeskrivelsePagination() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", getMoetemappeJSON());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    assertNotNull(moetemappeDTO.getId());

    var moetedokumentJSON = getMoetedokumentJSON();
    moetedokumentJSON.remove("dokumentbeskrivelse");
    response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetedokument", moetedokumentJSON);
    var moetedokumentDTO = gson.fromJson(response.getBody(), MoetedokumentDTO.class);
    assertNotNull(moetedokumentDTO.getId());

    // Add three Dokumentbeskrivelse
    response =
        post(
            "/moetedokument/" + moetedokumentDTO.getId() + "/dokumentbeskrivelse",
            getDokumentbeskrivelseJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var dokbesk1DTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);
    response =
        post(
            "/moetedokument/" + moetedokumentDTO.getId() + "/dokumentbeskrivelse",
            getDokumentbeskrivelseJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var dokbesk2DTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);
    response =
        post(
            "/moetedokument/" + moetedokumentDTO.getId() + "/dokumentbeskrivelse",
            getDokumentbeskrivelseJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var dokbesk3DTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);

    var type = new TypeToken<ListResponseBody<DokumentbeskrivelseDTO>>() {}.getType();
    ListResponseBody<DokumentbeskrivelseDTO> resultList;

    // DESC
    response = get("/moetedokument/" + moetedokumentDTO.getId() + "/dokumentbeskrivelse");
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(3, resultList.getItems().size());
    assertEquals(dokbesk3DTO.getId(), resultList.getItems().get(0).getId());
    assertEquals(dokbesk2DTO.getId(), resultList.getItems().get(1).getId());
    assertEquals(dokbesk1DTO.getId(), resultList.getItems().get(2).getId());

    // DESC startingAfter
    response =
        get(
            "/moetedokument/"
                + moetedokumentDTO.getId()
                + "/dokumentbeskrivelse?startingAfter="
                + dokbesk2DTO.getId());
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(1, resultList.getItems().size());
    assertEquals(dokbesk1DTO.getId(), resultList.getItems().get(0).getId());

    // DESC endingBefore
    response =
        get(
            "/moetedokument/"
                + moetedokumentDTO.getId()
                + "/dokumentbeskrivelse?endingBefore="
                + dokbesk2DTO.getId());
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(1, resultList.getItems().size());
    assertEquals(dokbesk3DTO.getId(), resultList.getItems().get(0).getId());

    // ASC
    response =
        get("/moetedokument/" + moetedokumentDTO.getId() + "/dokumentbeskrivelse?sortOrder=asc");
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(3, resultList.getItems().size());
    assertEquals(dokbesk1DTO.getId(), resultList.getItems().get(0).getId());
    assertEquals(dokbesk2DTO.getId(), resultList.getItems().get(1).getId());
    assertEquals(dokbesk3DTO.getId(), resultList.getItems().get(2).getId());

    // ASC startingAfter
    response =
        get(
            "/moetedokument/"
                + moetedokumentDTO.getId()
                + "/dokumentbeskrivelse?sortOrder=asc&startingAfter="
                + dokbesk2DTO.getId());
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(1, resultList.getItems().size());
    assertEquals(dokbesk3DTO.getId(), resultList.getItems().get(0).getId());

    // ASC endingBefore
    response =
        get(
            "/moetedokument/"
                + moetedokumentDTO.getId()
                + "/dokumentbeskrivelse?sortOrder=asc&endingBefore="
                + dokbesk2DTO.getId());
    resultList = gson.fromJson(response.getBody(), type);
    assertEquals(1, resultList.getItems().size());
    assertEquals(dokbesk1DTO.getId(), resultList.getItems().get(0).getId());

    // Delete the Moetemappe
    assertEquals(HttpStatus.OK, delete("/moetemappe/" + moetemappeDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/moetemappe/" + moetemappeDTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/moetedokument/" + moetedokumentDTO.getId()).getStatusCode());
  }

  @Test
  void addExistingDokumentbeskrivelse() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", getMoetemappeJSON());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);
    assertNotNull(moetemappeDTO.getId());

    // Add moetedokument1
    var moetedokumentJSON = getMoetedokumentJSON();
    moetedokumentJSON.remove("dokumentbeskrivelse");
    response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetedokument", moetedokumentJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetedokument1DTO = gson.fromJson(response.getBody(), MoetedokumentDTO.class);

    // Add moetedokument2
    response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetedokument", moetedokumentJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetedokument2DTO = gson.fromJson(response.getBody(), MoetedokumentDTO.class);

    // Add a Dokumentbeskrivelse
    response =
        post(
            "/moetedokument/" + moetedokument1DTO.getId() + "/dokumentbeskrivelse",
            getDokumentbeskrivelseJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var dokumentbeskrivelseDTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);
    var dokumentbeskrivelseId = dokumentbeskrivelseDTO.getId();

    // Add the same Dokumentbeskrivelse to moetedokument2
    response =
        post(
            "/moetedokument/" + moetedokument2DTO.getId() + "/dokumentbeskrivelse",
            dokumentbeskrivelseDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    dokumentbeskrivelseDTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);
    assertEquals(dokumentbeskrivelseId, dokumentbeskrivelseDTO.getId());

    // Verify that the Dokumentbeskrivelse is added to both Moetedokument
    response = get("/moetedokument/" + moetedokument1DTO.getId() + "/dokumentbeskrivelse");
    ListResponseBody<DokumentbeskrivelseDTO> resultList =
        gson.fromJson(
            response.getBody(),
            new TypeToken<ListResponseBody<DokumentbeskrivelseDTO>>() {}.getType());
    assertEquals(1, resultList.getItems().size());
    assertEquals(dokumentbeskrivelseId, resultList.getItems().getFirst().getId());

    response = get("/moetedokument/" + moetedokument2DTO.getId() + "/dokumentbeskrivelse");
    resultList =
        gson.fromJson(
            response.getBody(),
            new TypeToken<ListResponseBody<DokumentbeskrivelseDTO>>() {}.getType());
    assertEquals(1, resultList.getItems().size());
    assertEquals(dokumentbeskrivelseId, resultList.getItems().getFirst().getId());

    // Delete moetedokument1, make sure the Dokumentbeskrivelse is still there
    assertEquals(
        HttpStatus.OK, delete("/moetedokument/" + moetedokument1DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/moetedokument/" + moetedokument1DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.OK, get("/dokumentbeskrivelse/" + dokumentbeskrivelseId).getStatusCode());

    // Delete moetedokument2, make sure the Dokumentbeskrivelse is deleted
    assertEquals(
        HttpStatus.OK, delete("/moetedokument/" + moetedokument2DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/moetedokument/" + moetedokument2DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/dokumentbeskrivelse/" + dokumentbeskrivelseId).getStatusCode());

    // Delete the Moetemappe
    assertEquals(HttpStatus.OK, delete("/moetemappe/" + moetemappeDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/moetemappe/" + moetemappeDTO.getId()).getStatusCode());
  }

  @Test
  void checkLegacyArkivskaperFromJournalenhet() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", getMoetemappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    response =
        post("/moetemappe/" + moetemappeDTO.getId() + "/moetedokument", getMoetedokumentJSON());
    var moetedokumentDTO = gson.fromJson(response.getBody(), MoetedokumentDTO.class);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    var journalenhet = enhetRepository.findById(journalenhetId).orElse(null);
    var moetedokument = moetedokumentRepository.findById(moetedokumentDTO.getId()).orElse(null);
    assertEquals(journalenhet.getIri(), moetedokument.getArkivskaper());

    delete("/moetemappe/" + moetemappeDTO.getId());
    assertNull(moetedokumentRepository.findById(moetedokumentDTO.getId()).orElse(null));
  }

  @Test
  void checkLegacyArkivskaperFromAdmEnhet() throws Exception {
    var moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.put("utvalg", "UNDER");
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    response =
        post("/moetemappe/" + moetemappeDTO.getId() + "/moetedokument", getMoetedokumentJSON());
    var moetedokumentDTO = gson.fromJson(response.getBody(), MoetedokumentDTO.class);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    var journalenhet = enhetRepository.findById(underenhetId).orElse(null);
    var moetedokument = moetedokumentRepository.findById(moetedokumentDTO.getId()).orElse(null);
    assertEquals(journalenhet.getIri(), moetedokument.getArkivskaper());

    delete("/moetemappe/" + moetemappeDTO.getId());
    assertNull(moetedokumentRepository.findById(moetedokumentDTO.getId()).orElse(null));
  }
}

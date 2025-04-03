package no.einnsyn.backend.entities.journalpost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.reflect.TypeToken;
import java.time.LocalDateTime;
import java.util.List;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.korrespondansepart.models.KorrespondansepartDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class JournalpostControllerTest extends EinnsynControllerTestBase {

  ArkivDTO arkivDTO;
  ArkivdelDTO arkivdelDTO;

  @BeforeAll
  void setup() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
  }

  @AfterAll
  void teardown() throws Exception {
    delete("/arkiv/" + arkivDTO.getId());
  }

  /**
   * Test that we can:
   *
   * <ul>
   *   <li>insert a saksmappe (POST /saksmappe)
   *   <li>insert a journalpost in the
   *   <li>saksmappe (POST /journalpost)
   *   <li>update the journalpost (PATCH /journalpost/id)
   *   <li>get the journalpost (GET /journalpost/id)
   *   <li>delete the journalpost (DELETE /journalpost/id)
   *   <li>delete the saksmappe (DELETE /saksmappe/id)
   * </ul>
   *
   * @throws JSONException
   * @throws JsonProcessingException
   */
  @Test
  void addJournalpost() throws Exception {

    // A journalpost must have a saksmappe
    var saksmappeJSON = getSaksmappeJSON();
    var saksmappeResponse = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    var saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);

    // Insert Journalpost with saksmappe
    var jp = getJournalpostJSON();
    var journalpostResponse = post("/saksmappe/" + saksmappe.getId() + "/journalpost", jp);

    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    var journalpost = new JSONObject(journalpostResponse.getBody());
    assertEquals(jp.get("offentligTittel"), journalpost.get("offentligTittel"));
    assertEquals(jp.get("offentligTittelSensitiv"), journalpost.get("offentligTittelSensitiv"));
    assertEquals(jp.get("journalaar"), journalpost.get("journalaar"));
    assertEquals(jp.get("journalsekvensnummer"), journalpost.get("journalsekvensnummer"));
    assertEquals(jp.get("journalpostnummer"), journalpost.get("journalpostnummer"));
    assertEquals(jp.get("journalposttype"), journalpost.get("journalposttype"));
    var id = journalpost.get("id").toString();

    // Verify that we can get the journalpost
    assertEquals(HttpStatus.OK, get("/journalpost/" + id).getStatusCode());

    // Verify that we can get the saksmappe
    assertEquals(HttpStatus.OK, get("/saksmappe/" + saksmappe.getId()).getStatusCode());

    // Update journalpost
    jp.put("offentligTittel", "updatedOffentligTittel");
    var updateJournalpostResponse = patch("/journalpost/" + id, jp);
    assertEquals(HttpStatus.OK, updateJournalpostResponse.getStatusCode());

    // Get journalpost
    var getJournalpostResponse = get("/journalpost/" + id);
    assertEquals(HttpStatus.OK, getJournalpostResponse.getStatusCode());
    var journalpost2 = new JSONObject(getJournalpostResponse.getBody());
    assertEquals(jp.get("offentligTittel"), journalpost2.get("offentligTittel"));

    // Delete Journalpost
    assertEquals(HttpStatus.OK, delete("/journalpost/" + id).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/journalpost/" + id).getStatusCode());

    // Delete Saksmappe
    assertEquals(HttpStatus.OK, delete("/saksmappe/" + saksmappe.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/saksmappe/" + saksmappe.getId()).getStatusCode());
  }

  @Test
  void testListByIds() throws Exception {
    var saksmappeJSON = getSaksmappeJSON();
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappe = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    var pathPrefix = "/saksmappe/" + saksmappe.getId();
    var jp = getJournalpostJSON();

    response = post(pathPrefix + "/journalpost", jp);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpost1 = gson.fromJson(response.getBody(), JournalpostDTO.class);
    var jp1Id = journalpost1.getId();

    response = post(pathPrefix + "/journalpost", jp);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpost2 = gson.fromJson(response.getBody(), JournalpostDTO.class);
    var jp2Id = journalpost2.getId();

    response = get("/journalpost?ids=" + jp1Id);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var resultListType = new TypeToken<PaginatedList<JournalpostDTO>>() {}.getType();
    PaginatedList<JournalpostDTO> resultList = gson.fromJson(response.getBody(), resultListType);
    assertEquals(1, resultList.getItems().size());
    assertEquals(jp1Id, resultList.getItems().get(0).getId());

    response = get("/journalpost?ids=" + jp2Id);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    resultList = gson.fromJson(response.getBody(), resultListType);
    assertEquals(1, resultList.getItems().size());
    assertEquals(jp2Id, resultList.getItems().get(0).getId());

    response = get("/journalpost?ids=" + jp1Id + "," + jp2Id);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    resultList = gson.fromJson(response.getBody(), resultListType);
    assertEquals(2, resultList.getItems().size());
    assertEquals(jp1Id, resultList.getItems().get(0).getId());
    assertEquals(jp2Id, resultList.getItems().get(1).getId());

    // Delete Saksmappe
    var deleteSaksmappeResponse = delete("/saksmappe/" + saksmappe.getId());
    assertEquals(HttpStatus.OK, deleteSaksmappeResponse.getStatusCode());
    var getDeletedSaksmappeResponse = get("/saksmappe/" + saksmappe.getId());
    assertEquals(HttpStatus.NOT_FOUND, getDeletedSaksmappeResponse.getStatusCode());
  }

  @Test
  void testListByExternalIds() throws Exception {
    var saksmappeJSON = getSaksmappeJSON();
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappe = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    var pathPrefix = "/saksmappe/" + saksmappe.getId();
    var jp = getJournalpostJSON();

    jp.put("externalId", "externalIdWith://specialChars");
    response = post(pathPrefix + "/journalpost", jp);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpost1 = gson.fromJson(response.getBody(), JournalpostDTO.class);
    var jp1Id = journalpost1.getId();

    jp.put("externalId", "secondJournalpost");
    response = post(pathPrefix + "/journalpost", jp);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpost2 = gson.fromJson(response.getBody(), JournalpostDTO.class);
    var jp2Id = journalpost2.getId();

    response = get("/journalpost?externalIds=externalIdWith://specialChars");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var resultListType = new TypeToken<PaginatedList<JournalpostDTO>>() {}.getType();
    PaginatedList<JournalpostDTO> resultList = gson.fromJson(response.getBody(), resultListType);
    assertEquals(1, resultList.getItems().size());
    assertEquals(jp1Id, resultList.getItems().get(0).getId());

    response = get("/journalpost?externalIds=secondJournalpost");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    resultList = gson.fromJson(response.getBody(), resultListType);
    assertEquals(1, resultList.getItems().size());
    assertEquals(jp2Id, resultList.getItems().get(0).getId());

    response =
        get("/journalpost?externalIds=externalIdWith://specialChars,secondJournalpost,nonExisting");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    resultList = gson.fromJson(response.getBody(), resultListType);
    assertEquals(2, resultList.getItems().size());
    assertEquals(jp1Id, resultList.getItems().get(0).getId());
    assertEquals(jp2Id, resultList.getItems().get(1).getId());

    // Delete Saksmappe
    var deleteSaksmappeResponse = delete("/saksmappe/" + saksmappe.getId());
    assertEquals(HttpStatus.OK, deleteSaksmappeResponse.getStatusCode());
    var getDeletedSaksmappeResponse = get("/saksmappe/" + saksmappe.getId());
    assertEquals(HttpStatus.NOT_FOUND, getDeletedSaksmappeResponse.getStatusCode());
  }

  /**
   * It should fail when trying to insert a journalpost with missing properties
   *
   * @throws Exception
   */
  @Test
  void failOnMissingFields() throws Exception {

    var jp = getJournalpostJSON();

    // It should work with all properties
    var saksmappeDTO = getSaksmappeJSON();
    var saksmappeResponse = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeDTO);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    var saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    var pathPrefix = "/saksmappe/" + saksmappe.getId();
    var journalpostResponse = post(pathPrefix + "/journalpost", jp);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    String jp1 = new JSONObject(journalpostResponse.getBody()).get("id").toString();

    // Delete jp1
    var deleteJournalpostResponse = delete("/journalpost/" + jp1);
    assertEquals(HttpStatus.OK, deleteJournalpostResponse.getStatusCode());
    var getDeletedJournalpostResponse = get("/journalpost/" + jp1);
    assertEquals(HttpStatus.NOT_FOUND, getDeletedJournalpostResponse.getStatusCode());

    // It should fail with any of the following properties missing:
    jp.remove("offentligTittel");
    journalpostResponse = post(pathPrefix + "/journalpost", jp);
    assertEquals(HttpStatus.BAD_REQUEST, journalpostResponse.getStatusCode());
    assertNotNull(new JSONObject(journalpostResponse.getBody()).get("fieldError"));

    jp.put("offentligTittel", "testJournalpost");
    jp.remove("offentligTittelSensitiv");
    journalpostResponse = post(pathPrefix + "/journalpost", jp);
    assertEquals(HttpStatus.BAD_REQUEST, journalpostResponse.getStatusCode());
    assertNotNull(new JSONObject(journalpostResponse.getBody()).get("fieldError"));

    jp.put("offentligTittelSensitiv", "testJournalpost");
    jp.remove("journalposttype");
    journalpostResponse = post(pathPrefix + "/journalpost", jp);
    assertEquals(HttpStatus.BAD_REQUEST, journalpostResponse.getStatusCode());
    assertNotNull(new JSONObject(journalpostResponse.getBody()).get("fieldError"));

    jp.put("journalposttype", "inngaaende_dokument");
    jp.remove("journalaar");
    journalpostResponse = post(pathPrefix + "/journalpost", jp);
    assertEquals(HttpStatus.BAD_REQUEST, journalpostResponse.getStatusCode());
    assertNotNull(new JSONObject(journalpostResponse.getBody()).get("fieldError"));

    jp.put("journalaar", 2020);
    jp.remove("journaldato");

    assertEquals(HttpStatus.BAD_REQUEST, journalpostResponse.getStatusCode());
    assertNotNull(new JSONObject(journalpostResponse.getBody()).get("fieldError"));

    jp.put("journaldato", "2020-02-02");
    jp.remove("journalpostnummer");
    journalpostResponse = post(pathPrefix + "/journalpost", jp);
    assertEquals(HttpStatus.BAD_REQUEST, journalpostResponse.getStatusCode());
    assertNotNull(new JSONObject(journalpostResponse.getBody()).get("fieldError"));

    jp.put("journalpostnummer", 1);
    jp.remove("journalsekvensnummer");
    journalpostResponse = post(pathPrefix + "/journalpost", jp);
    assertEquals(HttpStatus.BAD_REQUEST, journalpostResponse.getStatusCode());
    assertNotNull(new JSONObject(journalpostResponse.getBody()).get("fieldError"));

    // All properties are back
    jp.put("journalsekvensnummer", "321");
    journalpostResponse = post(pathPrefix + "/journalpost", jp);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    var jp2 = new JSONObject(journalpostResponse.getBody()).get("id").toString();

    // Delete Saksmappe
    var deleteSaksmappeResponse = delete("/saksmappe/" + saksmappe.getId());
    assertEquals(HttpStatus.OK, deleteSaksmappeResponse.getStatusCode());
    var getDeletedSaksmappeResponse = get("/saksmappe/" + saksmappe.getId());
    assertEquals(HttpStatus.NOT_FOUND, getDeletedSaksmappeResponse.getStatusCode());

    // Try to delete jp2 (it should already be deleted since we deleted Saksmappe)
    deleteJournalpostResponse = delete("/journalpost/" + jp2);
    assertEquals(HttpStatus.NOT_FOUND, deleteJournalpostResponse.getStatusCode());
  }

  /** It should fail if we try to update a journalpost with a JSON object that has an ID */
  @Test
  void failOnUpdateWithId() throws Exception {

    var jp = getJournalpostJSON();
    var saksmappeDTO = getSaksmappeJSON();
    var saksmappeResponse = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeDTO);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    var saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    var pathPrefix = "/saksmappe/" + saksmappe.getId();
    var journalpostResponse = post(pathPrefix + "/journalpost", jp);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    var jp1 = new JSONObject(journalpostResponse.getBody()).get("id").toString();

    // Update journalpost with an allowed object
    var update = new JSONObject();
    update.put("offentligTittelSensitiv", "--");
    var updateJournalpostResponse = patch("/journalpost/" + jp1, update);
    assertEquals(HttpStatus.OK, updateJournalpostResponse.getStatusCode());
    assertEquals(
        new JSONObject(updateJournalpostResponse.getBody()).get("offentligTittelSensitiv"),
        update.get("offentligTittelSensitiv"));

    // It should fail when updating with an ID
    update.put("id", "123");
    updateJournalpostResponse = patch("/journalpost/" + jp1, update);
    assertEquals(HttpStatus.BAD_REQUEST, updateJournalpostResponse.getStatusCode());

    // Delete Saksmappe
    var deleteSaksmappeResponse = delete("/saksmappe/" + saksmappe.getId());
    assertEquals(HttpStatus.OK, deleteSaksmappeResponse.getStatusCode());
    var getDeletedSaksmappeResponse = get("/saksmappe/" + saksmappe.getId());
    assertEquals(HttpStatus.NOT_FOUND, getDeletedSaksmappeResponse.getStatusCode());
  }

  /**
   * Add multiple Dokumentbeskrivelses to Journalpost
   *
   * @throws Exception
   */
  @Test
  void insertDokumentbeskrivelse() throws Exception {

    var jp = getJournalpostJSON();
    var saksmappeJSON = getSaksmappeJSON();
    var saksmappeResponse = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    var saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    var pathPrefix = "/saksmappe/" + saksmappe.getId();
    var journalpostResponse = post(pathPrefix + "/journalpost", jp);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    var jpId = new JSONObject(journalpostResponse.getBody()).get("id").toString();

    var dokumentbeskrivelse = getDokumentbeskrivelseJSON();
    var dokumentbeskrivelseResponse =
        post("/journalpost/" + jpId + "/dokumentbeskrivelse", dokumentbeskrivelse);
    assertEquals(HttpStatus.CREATED, dokumentbeskrivelseResponse.getStatusCode());

    // Check if the dokumentbeskrivelse was added
    journalpostResponse = get("/journalpost/" + jpId);
    var journalpost = gson.fromJson(journalpostResponse.getBody(), JournalpostDTO.class);
    assertEquals(1, journalpost.getDokumentbeskrivelse().size());

    // Add another dokumentbeskrivelse
    var dokumentbeskrivelse2 = getDokumentbeskrivelseJSON();
    var dokumentbeskrivelseResponse2 =
        post("/journalpost/" + jpId + "/dokumentbeskrivelse", dokumentbeskrivelse2);
    assertEquals(HttpStatus.CREATED, dokumentbeskrivelseResponse2.getStatusCode());

    // Check if the dokumentbeskrivelse was added
    journalpostResponse = get("/journalpost/" + jpId);
    journalpost = gson.fromJson(journalpostResponse.getBody(), JournalpostDTO.class);
    assertEquals(2, journalpost.getDokumentbeskrivelse().size());
    var dokId1 = journalpost.getDokumentbeskrivelse().get(0).getId();
    var dokId2 = journalpost.getDokumentbeskrivelse().get(1).getId();

    // Make sure the dokumentbeskrivelses was added
    var getDokumentbeskrivelseResponse = get("/dokumentbeskrivelse/" + dokId1);
    assertEquals(HttpStatus.OK, getDokumentbeskrivelseResponse.getStatusCode());
    var getDokumentbeskrivelseResponse2 = get("/dokumentbeskrivelse/" + dokId2);
    assertEquals(HttpStatus.OK, getDokumentbeskrivelseResponse2.getStatusCode());

    // Delete Saksmappe
    var deleteSaksmappeResponse = delete("/saksmappe/" + saksmappe.getId());
    assertEquals(HttpStatus.OK, deleteSaksmappeResponse.getStatusCode());
    var getDeletedSaksmappeResponse = get("/saksmappe/" + saksmappe.getId());
    assertEquals(HttpStatus.NOT_FOUND, getDeletedSaksmappeResponse.getStatusCode());

    // Make sure the dokumentbeskrivelse was deleted
    var getJournalpostResponse = get("/journalpost/" + jpId);
    assertEquals(HttpStatus.NOT_FOUND, getJournalpostResponse.getStatusCode());
    getDokumentbeskrivelseResponse = get("/dokumentbeskrivelse/" + dokId1);
    assertEquals(HttpStatus.NOT_FOUND, getDokumentbeskrivelseResponse.getStatusCode());
    getDokumentbeskrivelseResponse2 = get("/dokumentbeskrivelse/" + dokId2);
    assertEquals(HttpStatus.NOT_FOUND, getDokumentbeskrivelseResponse2.getStatusCode());
  }

  @Test
  void addExistingDokumentbeskrivelse() throws Exception {

    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    var pathPrefix = "/saksmappe/" + saksmappeDTO.getId();

    // Add journalpost1
    response = post(pathPrefix + "/journalpost", getJournalpostJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpost1DTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Add journalpost2
    response = post(pathPrefix + "/journalpost", getJournalpostJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpost2DTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Add dokumentbeskrivelse to journalpost1
    response =
        post(
            "/journalpost/" + journalpost1DTO.getId() + "/dokumentbeskrivelse",
            getDokumentbeskrivelseJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var dokumentbeskrivelseDTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);
    var dokumentbeskrivelseId = dokumentbeskrivelseDTO.getId();

    // Add the same dokumentbeskrivelse to journalpost2
    response =
        post(
            "/journalpost/" + journalpost2DTO.getId() + "/dokumentbeskrivelse",
            dokumentbeskrivelseDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    dokumentbeskrivelseDTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);
    assertEquals(dokumentbeskrivelseId, dokumentbeskrivelseDTO.getId());

    // Delete journalpost1, make sure the dokumentbeskrivelse is still there
    response = delete("/journalpost/" + journalpost1DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = get("/journalpost/" + journalpost1DTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    response = get("/dokumentbeskrivelse/" + dokumentbeskrivelseId);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Delete journalpost2, make sure the dokumentbeskrivelse is deleted
    response = delete("/journalpost/" + journalpost2DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = get("/journalpost/" + journalpost2DTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    response = get("/dokumentbeskrivelse/" + dokumentbeskrivelseId);
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    // Delete Saksmappe
    var deleteSaksmappeResponse = delete("/saksmappe/" + saksmappeDTO.getId());
    assertEquals(HttpStatus.OK, deleteSaksmappeResponse.getStatusCode());
    response = get("/saksmappe/" + saksmappeDTO.getId());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  /**
   * Insert korrespondanseparts to Journalpost
   *
   * @throws Exception
   */
  @Test
  void insertKorrespondansepart() throws Exception {
    var jpInsert = getJournalpostJSON();
    var smInsert = getSaksmappeJSON();

    // Insert saksmappe
    var smResponse = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", smInsert);
    assertEquals(HttpStatus.CREATED, smResponse.getStatusCode());
    var smResponseJSON = gson.fromJson(smResponse.getBody(), SaksmappeDTO.class);

    // Insert journalpost
    var pathPrefix = "/saksmappe/" + smResponseJSON.getId();
    var jpResponse = post(pathPrefix + "/journalpost", jpInsert);
    assertEquals(HttpStatus.CREATED, jpResponse.getStatusCode());
    var jpResponseJSON = gson.fromJson(jpResponse.getBody(), JournalpostDTO.class);
    var jpId = jpResponseJSON.getId();

    // Insert Korrespondansepart
    var kp1Insert = getKorrespondansepartJSON();
    var kp1Response = post("/journalpost/" + jpId + "/korrespondansepart", kp1Insert);
    assertEquals(HttpStatus.CREATED, kp1Response.getStatusCode());
    var kp1ResponseJSON = gson.fromJson(kp1Response.getBody(), KorrespondansepartDTO.class);
    var kp1Id = kp1ResponseJSON.getId();

    // Check if the korrespondansepart was added
    jpResponse = get("/journalpost/" + jpId);
    assertEquals(HttpStatus.OK, jpResponse.getStatusCode());
    jpResponseJSON = gson.fromJson(jpResponse.getBody(), JournalpostDTO.class);
    assertEquals(1, jpResponseJSON.getKorrespondansepart().size());

    // Insert another Korrespondansepart
    var kp2Insert = getKorrespondansepartJSON();
    var kp2Response = post("/journalpost/" + jpId + "/korrespondansepart", kp2Insert);
    assertEquals(HttpStatus.CREATED, kp2Response.getStatusCode());
    var kp2ResponseJSON = gson.fromJson(kp2Response.getBody(), KorrespondansepartDTO.class);
    var kp2Id = kp2ResponseJSON.getId();

    // Check if the korrespondansepart was added
    jpResponse = get("/journalpost/" + jpId);
    assertEquals(HttpStatus.OK, jpResponse.getStatusCode());
    jpResponseJSON = gson.fromJson(jpResponse.getBody(), JournalpostDTO.class);
    assertEquals(2, jpResponseJSON.getKorrespondansepart().size());

    // Make sure the korrespondanseparts are reachable at their respective URLs
    var korrpartResponse = get("/korrespondansepart/" + kp1Id);
    assertEquals(HttpStatus.OK, korrpartResponse.getStatusCode());
    korrpartResponse = get("/korrespondansepart/" + kp2Id);
    assertEquals(HttpStatus.OK, korrpartResponse.getStatusCode());

    // Delete Saksmappe
    var deleteSaksmappeResponse = delete("/saksmappe/" + smResponseJSON.getId());
    assertEquals(HttpStatus.OK, deleteSaksmappeResponse.getStatusCode());
    var getDeletedSaksmappeResponse = get("/saksmappe/" + smResponseJSON.getId());
    assertEquals(HttpStatus.NOT_FOUND, getDeletedSaksmappeResponse.getStatusCode());

    // Make sure everything is deleted
    smResponse = get("/saksmappe/" + smResponseJSON.getId());
    assertEquals(HttpStatus.NOT_FOUND, smResponse.getStatusCode());
    jpResponse = get("/journalpost/" + jpId);
    assertEquals(HttpStatus.NOT_FOUND, jpResponse.getStatusCode());
    korrpartResponse = get("/korrespondansepart/" + kp1Id);
    assertEquals(HttpStatus.NOT_FOUND, korrpartResponse.getStatusCode());
    korrpartResponse = get("/korrespondansepart/" + kp2Id);
    assertEquals(HttpStatus.NOT_FOUND, korrpartResponse.getStatusCode());
  }

  // /journalpost/{id}/korrespondansepart
  @Test
  void korrespondansepartList() throws Exception {
    var resultListType = new TypeToken<PaginatedList<KorrespondansepartDTO>>() {}.getType();

    var saksmappeResponse =
        post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    var saksmappeDTO = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    var saksmappeId = saksmappeDTO.getId();

    var journalpostResponse =
        post("/saksmappe/" + saksmappeId + "/journalpost", getJournalpostJSON());
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    var journalpostDTO = gson.fromJson(journalpostResponse.getBody(), JournalpostDTO.class);
    var journalpostId = journalpostDTO.getId();

    var kpart1Response =
        post("/journalpost/" + journalpostId + "/korrespondansepart", getKorrespondansepartJSON());
    assertEquals(HttpStatus.CREATED, kpart1Response.getStatusCode());
    var kpart1DTO = gson.fromJson(kpart1Response.getBody(), KorrespondansepartDTO.class);
    var kpart2Response =
        post("/journalpost/" + journalpostId + "/korrespondansepart", getKorrespondansepartJSON());
    assertEquals(HttpStatus.CREATED, kpart2Response.getStatusCode());
    var kpart2DTO = gson.fromJson(kpart2Response.getBody(), KorrespondansepartDTO.class);
    var kpart3Response =
        post("/journalpost/" + journalpostId + "/korrespondansepart", getKorrespondansepartJSON());
    assertEquals(HttpStatus.CREATED, kpart3Response.getStatusCode());
    var kpart3DTO = gson.fromJson(kpart3Response.getBody(), KorrespondansepartDTO.class);

    // Descending
    var kpartsResponse = get("/journalpost/" + journalpostId + "/korrespondansepart");
    assertEquals(HttpStatus.OK, kpartsResponse.getStatusCode());
    PaginatedList<KorrespondansepartDTO> kpartsDTO =
        gson.fromJson(kpartsResponse.getBody(), resultListType);
    var items = kpartsDTO.getItems();
    assertEquals(3, items.size());
    assertEquals(kpart3DTO.getId(), items.get(0).getId());
    assertEquals(kpart2DTO.getId(), items.get(1).getId());
    assertEquals(kpart1DTO.getId(), items.get(2).getId());

    // Ascending
    kpartsResponse = get("/journalpost/" + journalpostId + "/korrespondansepart?sortOrder=asc");
    assertEquals(HttpStatus.OK, kpartsResponse.getStatusCode());
    kpartsDTO = gson.fromJson(kpartsResponse.getBody(), resultListType);
    items = kpartsDTO.getItems();
    assertEquals(3, items.size());
    assertEquals(kpart1DTO.getId(), items.get(0).getId());
    assertEquals(kpart2DTO.getId(), items.get(1).getId());
    assertEquals(kpart3DTO.getId(), items.get(2).getId());

    // Descending with startingAfter
    kpartsResponse =
        get(
            "/journalpost/"
                + journalpostId
                + "/korrespondansepart?sortOrder=desc&startingAfter="
                + kpart3DTO.getId());
    assertEquals(HttpStatus.OK, kpartsResponse.getStatusCode());
    kpartsDTO = gson.fromJson(kpartsResponse.getBody(), resultListType);
    items = kpartsDTO.getItems();
    assertEquals(2, items.size());
    assertEquals(kpart2DTO.getId(), items.get(0).getId());
    assertEquals(kpart1DTO.getId(), items.get(1).getId());

    // Ascending with startingAfter
    kpartsResponse =
        get(
            "/journalpost/"
                + journalpostId
                + "/korrespondansepart?sortOrder=asc&startingAfter="
                + kpart1DTO.getId());
    assertEquals(HttpStatus.OK, kpartsResponse.getStatusCode());
    kpartsDTO = gson.fromJson(kpartsResponse.getBody(), resultListType);
    items = kpartsDTO.getItems();
    assertEquals(2, items.size());
    assertEquals(kpart2DTO.getId(), items.get(0).getId());
    assertEquals(kpart3DTO.getId(), items.get(1).getId());

    // Descending with endingBefore
    kpartsResponse =
        get(
            "/journalpost/"
                + journalpostId
                + "/korrespondansepart?sortOrder=desc&endingBefore="
                + kpart1DTO.getId());
    assertEquals(HttpStatus.OK, kpartsResponse.getStatusCode());
    kpartsDTO = gson.fromJson(kpartsResponse.getBody(), resultListType);
    items = kpartsDTO.getItems();
    assertEquals(2, items.size());
    assertEquals(kpart3DTO.getId(), items.get(0).getId());
    assertEquals(kpart2DTO.getId(), items.get(1).getId());

    // Ascending with endingBefore
    kpartsResponse =
        get(
            "/journalpost/"
                + journalpostId
                + "/korrespondansepart?sortOrder=asc&endingBefore="
                + kpart3DTO.getId());
    assertEquals(HttpStatus.OK, kpartsResponse.getStatusCode());
    kpartsDTO = gson.fromJson(kpartsResponse.getBody(), resultListType);
    items = kpartsDTO.getItems();
    assertEquals(2, items.size());
    assertEquals(kpart1DTO.getId(), items.get(0).getId());
    assertEquals(kpart2DTO.getId(), items.get(1).getId());

    // Delete
    assertEquals(HttpStatus.OK, delete("/saksmappe/" + saksmappeId).getStatusCode());
  }

  // /journalpost/{id}/dokumentbeskrivelse
  @Test
  void dokumentbeskrivelseList() throws Exception {
    var resultListType = new TypeToken<PaginatedList<DokumentbeskrivelseDTO>>() {}.getType();

    var saksmappeResponse =
        post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    var saksmappeDTO = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    var saksmappeId = saksmappeDTO.getId();

    var journalpostResponse =
        post("/saksmappe/" + saksmappeId + "/journalpost", getJournalpostJSON());
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    var journalpostDTO = gson.fromJson(journalpostResponse.getBody(), JournalpostDTO.class);
    var journalpostId = journalpostDTO.getId();

    var dok1Response =
        post(
            "/journalpost/" + journalpostId + "/dokumentbeskrivelse", getDokumentbeskrivelseJSON());
    assertEquals(HttpStatus.CREATED, dok1Response.getStatusCode());
    var dok1DTO = gson.fromJson(dok1Response.getBody(), DokumentbeskrivelseDTO.class);
    var dok2Response =
        post(
            "/journalpost/" + journalpostId + "/dokumentbeskrivelse", getDokumentbeskrivelseJSON());
    assertEquals(HttpStatus.CREATED, dok2Response.getStatusCode());
    var dok2DTO = gson.fromJson(dok2Response.getBody(), DokumentbeskrivelseDTO.class);
    var dok3Response =
        post(
            "/journalpost/" + journalpostId + "/dokumentbeskrivelse", getDokumentbeskrivelseJSON());
    assertEquals(HttpStatus.CREATED, dok3Response.getStatusCode());
    var dok3DTO = gson.fromJson(dok3Response.getBody(), DokumentbeskrivelseDTO.class);

    // Descending
    var doksResponse = get("/journalpost/" + journalpostId + "/dokumentbeskrivelse");
    assertEquals(HttpStatus.OK, doksResponse.getStatusCode());
    PaginatedList<DokumentbeskrivelseDTO> doksDTO =
        gson.fromJson(doksResponse.getBody(), resultListType);
    var items = doksDTO.getItems();
    assertEquals(3, items.size());
    assertEquals(dok3DTO.getId(), items.get(0).getId());
    assertEquals(dok2DTO.getId(), items.get(1).getId());
    assertEquals(dok1DTO.getId(), items.get(2).getId());

    // Ascending
    doksResponse = get("/journalpost/" + journalpostId + "/dokumentbeskrivelse?sortOrder=asc");
    assertEquals(HttpStatus.OK, doksResponse.getStatusCode());
    doksDTO = gson.fromJson(doksResponse.getBody(), resultListType);
    items = doksDTO.getItems();
    assertEquals(3, items.size());
    assertEquals(dok1DTO.getId(), items.get(0).getId());
    assertEquals(dok2DTO.getId(), items.get(1).getId());
    assertEquals(dok3DTO.getId(), items.get(2).getId());

    // Descending with startingAfter
    doksResponse =
        get(
            "/journalpost/"
                + journalpostId
                + "/dokumentbeskrivelse?sortOrder=desc&startingAfter="
                + dok3DTO.getId());
    assertEquals(HttpStatus.OK, doksResponse.getStatusCode());
    doksDTO = gson.fromJson(doksResponse.getBody(), resultListType);
    items = doksDTO.getItems();
    assertEquals(2, items.size());
    assertEquals(dok2DTO.getId(), items.get(0).getId());
    assertEquals(dok1DTO.getId(), items.get(1).getId());

    // Ascending with startingAfter
    doksResponse =
        get(
            "/journalpost/"
                + journalpostId
                + "/dokumentbeskrivelse?sortOrder=asc&startingAfter="
                + dok1DTO.getId());
    assertEquals(HttpStatus.OK, doksResponse.getStatusCode());
    doksDTO = gson.fromJson(doksResponse.getBody(), resultListType);
    items = doksDTO.getItems();
    assertEquals(2, items.size());
    assertEquals(dok2DTO.getId(), items.get(0).getId());
    assertEquals(dok3DTO.getId(), items.get(1).getId());

    // Descending with endingBefore
    doksResponse =
        get(
            "/journalpost/"
                + journalpostId
                + "/dokumentbeskrivelse?sortOrder=desc&endingBefore="
                + dok1DTO.getId());
    assertEquals(HttpStatus.OK, doksResponse.getStatusCode());
    doksDTO = gson.fromJson(doksResponse.getBody(), resultListType);
    items = doksDTO.getItems();
    assertEquals(2, items.size());
    assertEquals(dok3DTO.getId(), items.get(0).getId());
    assertEquals(dok2DTO.getId(), items.get(1).getId());

    // Ascending with endingBefore
    doksResponse =
        get(
            "/journalpost/"
                + journalpostId
                + "/dokumentbeskrivelse?sortOrder=asc&endingBefore="
                + dok3DTO.getId());
    assertEquals(HttpStatus.OK, doksResponse.getStatusCode());
    doksDTO = gson.fromJson(doksResponse.getBody(), resultListType);
    items = doksDTO.getItems();
    assertEquals(2, items.size());
    assertEquals(dok1DTO.getId(), items.get(0).getId());
    assertEquals(dok2DTO.getId(), items.get(1).getId());

    // Delete
    assertEquals(HttpStatus.OK, delete("/saksmappe/" + saksmappeId).getStatusCode());
  }

  // Check that "expand" works
  @Test
  void testExpand() throws Exception {

    // Insert saksmappe, journalpost, korrespondansepart
    var saksmappeResponse =
        post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    var saksmappeDTO = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    var saksmappeId = saksmappeDTO.getId();

    var journalpostResponse =
        post("/saksmappe/" + saksmappeId + "/journalpost", getJournalpostJSON());
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    var journalpostDTO = gson.fromJson(journalpostResponse.getBody(), JournalpostDTO.class);
    var journalpostId = journalpostDTO.getId();

    var korrespondansepartResponse =
        post("/journalpost/" + journalpostId + "/korrespondansepart", getKorrespondansepartJSON());
    assertEquals(HttpStatus.CREATED, korrespondansepartResponse.getStatusCode());
    var korrespondansepartDTO =
        gson.fromJson(korrespondansepartResponse.getBody(), KorrespondansepartDTO.class);
    var korrespondansepartId = korrespondansepartDTO.getId();

    // Check that we can expand korrespondansepart
    var journalpostExpandResponse =
        get("/journalpost/" + journalpostId + "?expand=korrespondansepart");
    assertEquals(HttpStatus.OK, journalpostExpandResponse.getStatusCode());
    var journalpostExpandDTO =
        gson.fromJson(journalpostExpandResponse.getBody(), JournalpostDTO.class);
    assertEquals(1, journalpostExpandDTO.getKorrespondansepart().size());
    assertEquals(korrespondansepartId, journalpostExpandDTO.getKorrespondansepart().get(0).getId());
    assertNotNull(journalpostExpandDTO.getKorrespondansepart().get(0).getExpandedObject());

    // Check that journalpost.korrespondansepart handles both journalpost and
    // journalpost.korrespondansepart
    var saksmappeExpandResponse =
        get("/saksmappe/" + saksmappeId + "?expand=journalpost.korrespondansepart");
    assertEquals(HttpStatus.OK, saksmappeExpandResponse.getStatusCode());
    var saksmappeExpandDTO = gson.fromJson(saksmappeExpandResponse.getBody(), SaksmappeDTO.class);
    assertEquals(
        1,
        saksmappeExpandDTO
            .getJournalpost()
            .get(0)
            .getExpandedObject()
            .getKorrespondansepart()
            .size());
    var saksmappeJournalpostDTO = saksmappeExpandDTO.getJournalpost().get(0).getExpandedObject();
    assertNotNull(saksmappeJournalpostDTO);
    assertEquals(journalpostId, saksmappeJournalpostDTO.getId());
    var saksmappeJournalpostKorrpartDTO =
        saksmappeJournalpostDTO.getKorrespondansepart().get(0).getExpandedObject();
    assertNotNull(saksmappeJournalpostKorrpartDTO);
    assertEquals(korrespondansepartId, saksmappeJournalpostKorrpartDTO.getId());

    // Delete the Saksmappe
    delete("/saksmappe/" + saksmappeDTO.getId());
  }

  // Check that we can't POST directly to /journalpost
  @Test
  void testPostToJournalpost() throws Exception {
    var response = post("/journalpost", getJournalpostJSON());
    assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
  }

  @Test
  void checkLegacyArkivskaperFromJournalenhet() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", getJournalpostJSON());
    var journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    var journalenhet = enhetRepository.findById(journalenhetId).orElse(null);
    var journalpost = journalpostRepository.findById(journalpostDTO.getId()).orElse(null);
    assertEquals(journalenhet.getIri(), journalpost.getArkivskaper());

    delete("/saksmappe/" + saksmappeDTO.getId());
    assertNull(journalpostRepository.findById(journalpostDTO.getId()).orElse(null));
  }

  @Test
  void checkLegacyArkivskaperFromAdmEnhet() throws Exception {
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("administrativEnhet", "UNDER");
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", getJournalpostJSON());
    var journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    var admEnhet = enhetRepository.findById(underenhetId).orElse(null);
    var journalpost = journalpostRepository.findById(journalpostDTO.getId()).orElse(null);
    assertEquals(admEnhet.getIri(), journalpost.getArkivskaper());

    delete("/saksmappe/" + saksmappeDTO.getId());
    assertNull(journalpostRepository.findById(journalpostDTO.getId()).orElse(null));
  }

  @Test
  void addExistingSkjerming() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    var pathPrefix = "/saksmappe/" + saksmappeDTO.getId();

    var skjermingJSON = getSkjermingJSON();
    skjermingJSON.put("externalId", "skjerming-external-id");
    var journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("skjerming", skjermingJSON);
    response = post(pathPrefix + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    var skjermingDTO = journalpostDTO.getSkjerming().getExpandedObject();

    response = post(pathPrefix + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpostDTO2 = gson.fromJson(response.getBody(), JournalpostDTO.class);
    var skjermingDTO2 = journalpostDTO2.getSkjerming().getExpandedObject();
    assertEquals(skjermingDTO.getId(), skjermingDTO2.getId());

    delete("/saksmappe/" + saksmappeDTO.getId());
  }

  @Test
  void addExistingSkjermingAndRollbackOnForbidden() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    var pathPrefix = "/saksmappe/" + saksmappeDTO.getId();

    var skjermingJSON = getSkjermingJSON();
    skjermingJSON.put("externalId", "skjerming-external-id");
    var journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("skjerming", skjermingJSON);
    response = post(pathPrefix + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    response = post(pathPrefix + "/journalpost", journalpostJSON, journalenhet2Key);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    response = delete("/saksmappe/" + saksmappeDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testCustomOppdatertDato() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    var pathPrefix = "/saksmappe/" + saksmappeDTO.getId();

    var journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("oppdatertDato", "2002-02-02T02:02:02Z");

    // Normal user should not be allowed
    response = post(pathPrefix + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Admin user should be allowed
    response = postAdmin(pathPrefix + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertEquals("2002-02-02T02:02:02Z", journalpostDTO.getOppdatertDato());

    deleteAdmin("/saksmappe/" + saksmappeDTO.getId());
  }

  @Test
  void testCustomPublisertDato() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    var pathPrefix = "/saksmappe/" + saksmappeDTO.getId();

    var journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("publisertDato", "2002-02-02T02:02:02Z");

    // Normal user should not be allowed
    response = post(pathPrefix + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Admin user should be allowed
    response = postAdmin(pathPrefix + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertEquals("2002-02-02T02:02:02Z", journalpostDTO.getPublisertDato());

    deleteAdmin("/saksmappe/" + saksmappeDTO.getId());
  }

  @Test
  void testLegacyFoelgsakenReferanse() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    var pathPrefix = "/saksmappe/" + saksmappeDTO.getId();

    var journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("legacyFoelgsakenReferanse", new JSONArray().put("123").put("456"));

    response = post(pathPrefix + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertEquals(List.of("123", "456"), journalpostDTO.getLegacyFoelgsakenReferanse());

    // Update
    journalpostJSON.put("legacyFoelgsakenReferanse", new JSONArray().put("789"));
    response = patch("/journalpost/" + journalpostDTO.getId(), journalpostJSON);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertEquals(List.of("789"), journalpostDTO.getLegacyFoelgsakenReferanse());

    deleteAdmin("/saksmappe/" + saksmappeDTO.getId());
  }

  @Test
  void testKorrespondanseparttype() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    var journalpostJSON = getJournalpostJSON();
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    var jpPrefix = "/journalpost/" + journalpostDTO.getId();

    var korrespondansepartJSON = getKorrespondansepartJSON();
    korrespondansepartJSON.put("korrespondanseparttype", "avsender");
    response = post(jpPrefix + "/korrespondansepart", korrespondansepartJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var korrespondansepartDTO = gson.fromJson(response.getBody(), KorrespondansepartDTO.class);
    assertEquals("avsender", korrespondansepartDTO.getKorrespondanseparttype());

    korrespondansepartJSON.put("korrespondanseparttype", "mottaker");
    response = post(jpPrefix + "/korrespondansepart", korrespondansepartJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    korrespondansepartDTO = gson.fromJson(response.getBody(), KorrespondansepartDTO.class);
    assertEquals("mottaker", korrespondansepartDTO.getKorrespondanseparttype());

    korrespondansepartJSON.put("korrespondanseparttype", "kopimottaker");
    response = post(jpPrefix + "/korrespondansepart", korrespondansepartJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    korrespondansepartDTO = gson.fromJson(response.getBody(), KorrespondansepartDTO.class);
    assertEquals("kopimottaker", korrespondansepartDTO.getKorrespondanseparttype());

    korrespondansepartJSON.put("korrespondanseparttype", "gruppemottaker");
    response = post(jpPrefix + "/korrespondansepart", korrespondansepartJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    korrespondansepartDTO = gson.fromJson(response.getBody(), KorrespondansepartDTO.class);
    assertEquals("gruppemottaker", korrespondansepartDTO.getKorrespondanseparttype());

    korrespondansepartJSON.put("korrespondanseparttype", "intern_avsender");
    response = post(jpPrefix + "/korrespondansepart", korrespondansepartJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    korrespondansepartDTO = gson.fromJson(response.getBody(), KorrespondansepartDTO.class);
    assertEquals("intern_avsender", korrespondansepartDTO.getKorrespondanseparttype());

    korrespondansepartJSON.put("korrespondanseparttype", "intern_mottaker");
    response = post(jpPrefix + "/korrespondansepart", korrespondansepartJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    korrespondansepartDTO = gson.fromJson(response.getBody(), KorrespondansepartDTO.class);
    assertEquals("intern_mottaker", korrespondansepartDTO.getKorrespondanseparttype());

    korrespondansepartJSON.put("korrespondanseparttype", "intern_kopimottaker");
    response = post(jpPrefix + "/korrespondansepart", korrespondansepartJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    korrespondansepartDTO = gson.fromJson(response.getBody(), KorrespondansepartDTO.class);
    assertEquals("intern_kopimottaker", korrespondansepartDTO.getKorrespondanseparttype());

    korrespondansepartJSON.put("korrespondanseparttype", "ukjent");
    response = post(jpPrefix + "/korrespondansepart", korrespondansepartJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    korrespondansepartDTO = gson.fromJson(response.getBody(), KorrespondansepartDTO.class);
    assertEquals("ukjent", korrespondansepartDTO.getKorrespondanseparttype());

    var base = "http://www.arkivverket.no/standarder/noark5/arkivstruktur/";
    korrespondansepartJSON.put("korrespondanseparttype", base + "avsender");
    response = post(jpPrefix + "/korrespondansepart", korrespondansepartJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    korrespondansepartDTO = gson.fromJson(response.getBody(), KorrespondansepartDTO.class);
    assertEquals("avsender", korrespondansepartDTO.getKorrespondanseparttype());

    korrespondansepartJSON.put("korrespondanseparttype", base + "mottaker");
    response = post(jpPrefix + "/korrespondansepart", korrespondansepartJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    korrespondansepartDTO = gson.fromJson(response.getBody(), KorrespondansepartDTO.class);
    assertEquals("mottaker", korrespondansepartDTO.getKorrespondanseparttype());

    korrespondansepartJSON.put("korrespondanseparttype", base + "kopimottaker");
    response = post(jpPrefix + "/korrespondansepart", korrespondansepartJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    korrespondansepartDTO = gson.fromJson(response.getBody(), KorrespondansepartDTO.class);
    assertEquals("kopimottaker", korrespondansepartDTO.getKorrespondanseparttype());

    korrespondansepartJSON.put("korrespondanseparttype", base + "gruppemottaker");
    response = post(jpPrefix + "/korrespondansepart", korrespondansepartJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    korrespondansepartDTO = gson.fromJson(response.getBody(), KorrespondansepartDTO.class);
    assertEquals("gruppemottaker", korrespondansepartDTO.getKorrespondanseparttype());

    korrespondansepartJSON.put("korrespondanseparttype", base + "intern_avsender");
    response = post(jpPrefix + "/korrespondansepart", korrespondansepartJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    korrespondansepartDTO = gson.fromJson(response.getBody(), KorrespondansepartDTO.class);
    assertEquals("intern_avsender", korrespondansepartDTO.getKorrespondanseparttype());

    korrespondansepartJSON.put("korrespondanseparttype", base + "intern_mottaker");
    response = post(jpPrefix + "/korrespondansepart", korrespondansepartJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    korrespondansepartDTO = gson.fromJson(response.getBody(), KorrespondansepartDTO.class);
    assertEquals("intern_mottaker", korrespondansepartDTO.getKorrespondanseparttype());

    korrespondansepartJSON.put("korrespondanseparttype", base + "intern_kopimottaker");
    response = post(jpPrefix + "/korrespondansepart", korrespondansepartJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    korrespondansepartDTO = gson.fromJson(response.getBody(), KorrespondansepartDTO.class);
    assertEquals("intern_kopimottaker", korrespondansepartDTO.getKorrespondanseparttype());

    deleteAdmin("/saksmappe/" + saksmappeDTO.getId());
  }

  @Test
  void testAccessibleAfter() throws Exception {

    var saksmappeJSON = getSaksmappeJSON();
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappe = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    var pathPrefix = "/saksmappe/" + saksmappe.getId();
    var jp = getJournalpostAccessibleInFutureJSON();

    response = post(pathPrefix + "/journalpost", jp);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpost1DTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    var jp1Id = journalpost1DTO.getId();

    response = post(pathPrefix + "/journalpost", jp, journalenhet2Key);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpost2DTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertEquals(journalenhet2Id, journalpost2DTO.getJournalenhet().getId());
    var jp2Id = journalpost2DTO.getId();

    // anonymous should not have access
    response = getAnon("/journalpost?ids=" + jp1Id);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var resultListType = new TypeToken<PaginatedList<JournalpostDTO>>() {}.getType();
    PaginatedList<JournalpostDTO> resultList = gson.fromJson(response.getBody(), resultListType);
    assertEquals(0, resultList.getItems().size());

    // admin has access to jp1
    response = getAdmin("/journalpost?ids=" + jp1Id);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    resultList = gson.fromJson(response.getBody(), resultListType);
    assertEquals(1, resultList.getItems().size());
    assertEquals(jp1Id, resultList.getItems().get(0).getId());

    // admin has access to jp2
    response = getAdmin("/journalpost?ids=" + jp2Id);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    resultList = gson.fromJson(response.getBody(), resultListType);
    assertEquals(1, resultList.getItems().size());
    assertEquals(jp2Id, resultList.getItems().get(0).getId());

    // Only jp1 is accessible by "jp1" user
    response = get("/journalpost?ids=" + jp1Id + "," + jp2Id);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    resultList = gson.fromJson(response.getBody(), resultListType);
    assertEquals(1, resultList.getItems().size());
    assertEquals(jp1Id, resultList.getItems().get(0).getId());

    // Only jp2 is accessible by "jp2" user
    response = get("/journalpost?ids=" + jp1Id + "," + jp2Id, journalenhet2Key);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    resultList = gson.fromJson(response.getBody(), resultListType);
    assertEquals(1, resultList.getItems().size());
    assertEquals(jp2Id, resultList.getItems().get(0).getId());

    // update one journalpost with accessible from today
    var update = new JSONObject();
    update.put("accessibleAfter", LocalDateTime.now());
    var updateJournalpostResponse = patchAdmin("/journalpost/" + jp1Id, update);
    assertEquals(HttpStatus.OK, updateJournalpostResponse.getStatusCode());

    // everybody has access to the one being accessible from today
    response = getAnon("/journalpost?ids=" + jp1Id + "," + jp2Id);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    resultList = gson.fromJson(response.getBody(), resultListType);
    assertEquals(1, resultList.getItems().size());
    assertEquals(jp1Id, resultList.getItems().getFirst().getId());

    // ensure get saksmappe includes all jp for admin
    response = getAdmin("/saksmappe/" + saksmappe.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    saksmappe = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertEquals(2, saksmappe.getJournalpost().size());

    // anonymous get on saksmappe should only return the one journalpost accessible from today
    response = getAnon("/saksmappe/" + saksmappe.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    saksmappe = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertEquals(1, saksmappe.getJournalpost().size());
    assertEquals(jp1Id, saksmappe.getJournalpost().getFirst().getId());

    // Delete jp2 (not accessible by jp1 user)
    response = delete("/journalpost/" + jp2Id, journalenhet2Key);

    // Delete Saksmappe
    var deleteSaksmappeResponse = delete("/saksmappe/" + saksmappe.getId());
    assertEquals(HttpStatus.OK, deleteSaksmappeResponse.getStatusCode());
    var getDeletedSaksmappeResponse = get("/saksmappe/" + saksmappe.getId());
    assertEquals(HttpStatus.NOT_FOUND, getDeletedSaksmappeResponse.getStatusCode());
  }

  @Test
  void testAddWithAdministrativEnhet() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    var journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("administrativEnhet", "UNDER");
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertEquals(underenhetId, journalpostDTO.getAdministrativEnhetObjekt().getId());

    delete("/saksmappe/" + saksmappeDTO.getId());
  }

  @Test
  void testAddWithAdministrativEnhetObjekt() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    var journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("administrativEnhetObjekt", underenhetId);
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertEquals(underenhetId, journalpostDTO.getAdministrativEnhetObjekt().getId());

    delete("/saksmappe/" + saksmappeDTO.getId());
  }

  @Test
  void testSaksekvensnummerVisibility() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    var journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("journalsekvensnummer", "123");
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertEquals(123, journalpostDTO.getJournalsekvensnummer());

    // Normal user should not have access
    response = getAnon("/journalpost/" + journalpostDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertNull(journalpostDTO.getJournalsekvensnummer());

    // Owner of the document should have access
    response = get("/journalpost/" + journalpostDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertEquals(123, journalpostDTO.getJournalsekvensnummer());

    // Admin should have access
    response = getAdmin("/journalpost/" + journalpostDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertEquals(123, journalpostDTO.getJournalsekvensnummer());

    deleteAdmin("/saksmappe/" + saksmappeDTO.getId());
  }

  @Test
  void testSaksbehandlerVisibility() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    var journalpostJSON = getJournalpostJSON();
    var korrpartJSON = getKorrespondansepartJSON();
    korrpartJSON.put("saksbehandler", "saksbehandler");
    journalpostJSON.put("korrespondansepart", new JSONArray().put(korrpartJSON));
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    var korrespondansepartDTO = journalpostDTO.getKorrespondansepart().get(0).getExpandedObject();
    assertEquals("saksbehandler", korrespondansepartDTO.getSaksbehandler());

    // Normal user should not have access
    response = getAnon("/journalpost/" + journalpostDTO.getId() + "?expand=korrespondansepart");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    korrespondansepartDTO = journalpostDTO.getKorrespondansepart().get(0).getExpandedObject();
    assertNull(korrespondansepartDTO.getSaksbehandler());

    // Journalenhet2 should not have access
    response =
        get(
            "/journalpost/" + journalpostDTO.getId() + "?expand=korrespondansepart",
            journalenhet2Key);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    korrespondansepartDTO = journalpostDTO.getKorrespondansepart().get(0).getExpandedObject();
    assertNull(korrespondansepartDTO.getSaksbehandler());

    // Journalenhet should have access
    response = get("/journalpost/" + journalpostDTO.getId() + "?expand=korrespondansepart");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    korrespondansepartDTO = journalpostDTO.getKorrespondansepart().get(0).getExpandedObject();
    assertEquals("saksbehandler", korrespondansepartDTO.getSaksbehandler());

    // Admin should have access
    response = getAdmin("/journalpost/" + journalpostDTO.getId() + "?expand=korrespondansepart");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    korrespondansepartDTO = journalpostDTO.getKorrespondansepart().get(0).getExpandedObject();
    assertEquals("saksbehandler", korrespondansepartDTO.getSaksbehandler());

    delete("/saksmappe/" + saksmappeDTO.getId());
  }

  // When filtering by an ID or externalId, we should not get next / previous links, and limit
  // should be ignored
  @Test
  void testPaginationFilteredById() throws Exception {
    var saksmappeResponse =
        post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    var saksmappeDTO = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);

    // Add 5 journalposts
    for (int i = 0; i < 5; i++) {
      var journalpostJSON = getJournalpostJSON();
      journalpostJSON.put("externalId", "externalId-" + i);
      var journalpostResponse =
          post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
      assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    }

    // Get all items
    var journalpostListResponse =
        get("/saksmappe/" + saksmappeDTO.getId() + "/journalpost?limit=5");
    assertEquals(HttpStatus.OK, journalpostListResponse.getStatusCode());
    var resultListType = new TypeToken<PaginatedList<JournalpostDTO>>() {}.getType();
    PaginatedList<JournalpostDTO> journalpostListDTO =
        gson.fromJson(journalpostListResponse.getBody(), resultListType);
    var allItems = journalpostListDTO.getItems();
    assertEquals(5, allItems.size());

    // List with limit and externalId
    journalpostListResponse =
        get(
            "/saksmappe/"
                + saksmappeDTO.getId()
                + "/journalpost?limit=1&externalIds=externalId-0,externalId-1");
    assertEquals(HttpStatus.OK, journalpostListResponse.getStatusCode());
    journalpostListDTO = gson.fromJson(journalpostListResponse.getBody(), resultListType);
    var items = journalpostListDTO.getItems();
    assertEquals(2, items.size());
    assertEquals("externalId-0", items.get(0).getExternalId());
    assertEquals("externalId-1", items.get(1).getExternalId());

    // List with limit and id
    journalpostListResponse =
        get(
            "/saksmappe/"
                + saksmappeDTO.getId()
                + "/journalpost?limit=1&ids="
                + allItems.get(0).getId()
                + ","
                + allItems.get(1).getId()
                + ","
                + allItems.get(2).getId());
    assertEquals(HttpStatus.OK, journalpostListResponse.getStatusCode());
    journalpostListDTO = gson.fromJson(journalpostListResponse.getBody(), resultListType);
    items = journalpostListDTO.getItems();
    assertEquals(3, items.size());
    assertEquals(allItems.get(0).getId(), items.get(0).getId());
    assertEquals(allItems.get(1).getId(), items.get(1).getId());
    assertEquals(allItems.get(2).getId(), items.get(2).getId());

    // Delete
    var deleteSaksmappeResponse = delete("/saksmappe/" + saksmappeDTO.getId());
    assertEquals(HttpStatus.OK, deleteSaksmappeResponse.getStatusCode());
  }

  @Test
  void testAddExistingDokumentbeskrivelse() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Create a journalpost with a dokumentbeskrivelse
    var journalpostJSON = getJournalpostJSON();
    var dokumentbeskrivelseJSON = getDokumentbeskrivelseJSON();
    dokumentbeskrivelseJSON.put("systemId", "aUniqueId");
    journalpostJSON.put("dokumentbeskrivelse", new JSONArray(List.of(dokumentbeskrivelseJSON)));
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertEquals(1, journalpostDTO.getDokumentbeskrivelse().size());
    var dokumentbeskrivelseDTO = journalpostDTO.getDokumentbeskrivelse().get(0).getExpandedObject();

    // Create another journalpost without a dokumentbeskrivelse
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", getJournalpostJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpostDTO2 = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertEquals(0, journalpostDTO2.getDokumentbeskrivelse().size());

    // Add the same dokumentbeskrivelse to the second journalpost
    response =
        post(
            "/journalpost/" + journalpostDTO2.getId() + "/dokumentbeskrivelse",
            dokumentbeskrivelseJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var dokumentbeskrivelseDTO2 = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);
    assertEquals(dokumentbeskrivelseDTO.getId(), dokumentbeskrivelseDTO2.getId());

    // Check that the dokumentbeskrivelse is added to the second journalpost
    response = get("/journalpost/" + journalpostDTO2.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    journalpostDTO2 = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertEquals(1, journalpostDTO2.getDokumentbeskrivelse().size());
    assertEquals(
        dokumentbeskrivelseDTO.getId(), journalpostDTO2.getDokumentbeskrivelse().get(0).getId());

    // Check that it's still on the first journalpost
    response = get("/journalpost/" + journalpostDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertEquals(1, journalpostDTO.getDokumentbeskrivelse().size());
    assertEquals(
        dokumentbeskrivelseDTO.getId(), journalpostDTO.getDokumentbeskrivelse().get(0).getId());

    // Delete it from the second journalpost
    response =
        delete(
            "/journalpost/"
                + journalpostDTO2.getId()
                + "/dokumentbeskrivelse/"
                + dokumentbeskrivelseDTO2.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = get("/journalpost/" + journalpostDTO2.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    journalpostDTO2 = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertEquals(0, journalpostDTO2.getDokumentbeskrivelse().size());

    // Check that it's still on the first journalpost
    response = get("/journalpost/" + journalpostDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertEquals(1, journalpostDTO.getDokumentbeskrivelse().size());
    assertEquals(
        dokumentbeskrivelseDTO.getId(), journalpostDTO.getDokumentbeskrivelse().get(0).getId());

    delete("/saksmappe/" + saksmappeDTO.getId());
  }
}

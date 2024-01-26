package no.einnsyn.apiv3.entities.journalpost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.einnsyn.apiv3.entities.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class JournalpostControllerTest extends EinnsynControllerTestBase {

  /**
   * Test that we can:
   *
   * <ul>
   *   <li>insert a saksmappe (POST /saksmappe)
   *   <li>insert a journalpost in the
   *   <li>saksmappe (POST /journalpost)
   *   <li>update the journalpost (PUT /journalpost/id)
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
    var saksmappeResponse = post("/saksmappe", saksmappeJSON);
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
    var updateJournalpostResponse = put("/journalpost/" + id, jp);
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
    var saksmappeResponse = post("/saksmappe", saksmappeDTO);
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
    assertNotNull(new JSONObject(journalpostResponse.getBody()).get("fieldErrors"));

    jp.put("offentligTittel", "testJournalpost");
    jp.remove("offentligTittelSensitiv");
    journalpostResponse = post(pathPrefix + "/journalpost", jp);
    assertEquals(HttpStatus.BAD_REQUEST, journalpostResponse.getStatusCode());
    assertNotNull(new JSONObject(journalpostResponse.getBody()).get("fieldErrors"));

    jp.put("offentligTittelSensitiv", "testJournalpost");
    jp.remove("journalposttype");
    journalpostResponse = post(pathPrefix + "/journalpost", jp);
    assertEquals(HttpStatus.BAD_REQUEST, journalpostResponse.getStatusCode());
    assertNotNull(new JSONObject(journalpostResponse.getBody()).get("fieldErrors"));

    jp.put("journalposttype", "inngåendeDokument");
    jp.remove("journalaar");
    journalpostResponse = post(pathPrefix + "/journalpost", jp);
    assertEquals(HttpStatus.BAD_REQUEST, journalpostResponse.getStatusCode());
    assertNotNull(new JSONObject(journalpostResponse.getBody()).get("fieldErrors"));

    jp.put("journalaar", 2020);
    jp.remove("journaldato");

    assertEquals(HttpStatus.BAD_REQUEST, journalpostResponse.getStatusCode());
    assertNotNull(new JSONObject(journalpostResponse.getBody()).get("fieldErrors"));

    jp.put("journaldato", "2020-02-02");
    jp.remove("journalpostnummer");
    journalpostResponse = post(pathPrefix + "/journalpost", jp);
    assertEquals(HttpStatus.BAD_REQUEST, journalpostResponse.getStatusCode());
    assertNotNull(new JSONObject(journalpostResponse.getBody()).get("fieldErrors"));

    jp.put("journalpostnummer", 1);
    jp.remove("journalsekvensnummer");
    journalpostResponse = post(pathPrefix + "/journalpost", jp);
    assertEquals(HttpStatus.BAD_REQUEST, journalpostResponse.getStatusCode());
    assertNotNull(new JSONObject(journalpostResponse.getBody()).get("fieldErrors"));

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
    var saksmappeResponse = post("/saksmappe", saksmappeDTO);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    var saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeDTO.class);
    var pathPrefix = "/saksmappe/" + saksmappe.getId();
    var journalpostResponse = post(pathPrefix + "/journalpost", jp);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    var jp1 = new JSONObject(journalpostResponse.getBody()).get("id").toString();

    // Update journalpost with an allowed object
    var update = new JSONObject();
    update.put("offentligTittelSensitiv", "--");
    var updateJournalpostResponse = put("/journalpost/" + jp1, update);
    assertEquals(HttpStatus.OK, updateJournalpostResponse.getStatusCode());
    assertEquals(
        new JSONObject(updateJournalpostResponse.getBody()).get("offentligTittelSensitiv"),
        update.get("offentligTittelSensitiv"));

    // It should fail when updating with an ID
    update.put("id", "123");
    updateJournalpostResponse = put("/journalpost/" + jp1, update);
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
    var saksmappeResponse = post("/saksmappe", saksmappeJSON);
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
    var smResponse = post("/saksmappe", smInsert);
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
}

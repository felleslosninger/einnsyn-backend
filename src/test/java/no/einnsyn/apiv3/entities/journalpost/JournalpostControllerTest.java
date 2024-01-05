package no.einnsyn.apiv3.entities.journalpost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.einnsyn.apiv3.entities.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostJSON;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartJSON;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeJSON;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class JournalpostControllerTest extends EinnsynControllerTestBase {

  /**
   * Test that we can: - insert a saksmappe (POST /saksmappe) - insert a journalpost in the
   * saksmappe (POST /journalpost) - update the journalpost (PUT /journalpost/id) - get the
   * journalpost (GET /journalpost/id) - delete the journalpost (DELETE /journalpost/id) - delete
   * the saksmappe (DELETE /saksmappe/id)
   *
   * @throws JSONException
   * @throws JsonProcessingException
   */
  @Test
  void addJournalpost() throws Exception {
    // A journalpost must have a saksmappe
    JSONObject saksmappeJSON = getSaksmappeJSON();
    ResponseEntity<String> saksmappeResponse = post("/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    SaksmappeJSON saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeJSON.class);

    // Insert Journalpost with saksmappe
    JSONObject jp = getJournalpostJSON();
    jp.put("saksmappe", saksmappe.getId());
    ResponseEntity<String> journalpostResponse = post("/journalpost", jp);

    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    JSONObject journalpost = new JSONObject(journalpostResponse.getBody());
    assertEquals(jp.get("offentligTittel"), journalpost.get("offentligTittel"));
    assertEquals(jp.get("offentligTittelSensitiv"), journalpost.get("offentligTittelSensitiv"));
    assertEquals(jp.get("journalaar"), journalpost.get("journalaar"));
    assertEquals(jp.get("journalsekvensnummer"), journalpost.get("journalsekvensnummer"));
    assertEquals(jp.get("journalpostnummer"), journalpost.get("journalpostnummer"));
    assertEquals(jp.get("journalposttype"), journalpost.get("journalposttype"));
    String id = journalpost.get("id").toString();

    // Update journalpost
    jp.put("offentligTittel", "updatedOffentligTittel");
    ResponseEntity<String> updateJournalpostResponse = put("/journalpost/" + id, jp);
    assertEquals(HttpStatus.OK, updateJournalpostResponse.getStatusCode());

    // Get journalpost
    ResponseEntity<String> getJournalpostResponse = get("/journalpost/" + id);
    assertEquals(HttpStatus.OK, getJournalpostResponse.getStatusCode());
    JSONObject journalpost2 = new JSONObject(getJournalpostResponse.getBody());
    assertEquals(jp.get("offentligTittel"), journalpost2.get("offentligTittel"));

    // Delete Journalpost
    ResponseEntity<String> deleteJournalpostResponse = delete("/journalpost/" + id);
    assertEquals(HttpStatus.OK, deleteJournalpostResponse.getStatusCode());
    ResponseEntity<String> getDeletedJournalpostResponse = get("/journalpost/" + id);
    assertEquals(HttpStatus.NOT_FOUND, getDeletedJournalpostResponse.getStatusCode());

    // Delete Saksmappe
    ResponseEntity<String> deleteSaksmappeResponse = delete("/saksmappe/" + saksmappe.getId());
    assertEquals(HttpStatus.OK, deleteSaksmappeResponse.getStatusCode());
    ResponseEntity<String> getDeletedSaksmappeResponse = get("/saksmappe/" + saksmappe.getId());
    assertEquals(HttpStatus.NOT_FOUND, getDeletedSaksmappeResponse.getStatusCode());
  }

  /**
   * It should fail when trying to insert a journalpost with missing properties
   *
   * @throws Exception
   */
  @Test
  void failOnMissingFields() throws Exception {

    JSONObject jp = getJournalpostJSON();
    ResponseEntity<String> journalpostResponse = null;

    // It should work with all properties
    JSONObject saksmappeJSON = getSaksmappeJSON();
    ResponseEntity<String> saksmappeResponse = post("/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    SaksmappeJSON saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeJSON.class);
    jp.put("saksmappe", saksmappe.getId());
    journalpostResponse = post("/journalpost", jp);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    String jp1 = new JSONObject(journalpostResponse.getBody()).get("id").toString();

    // Delete jp1
    ResponseEntity<String> deleteJournalpostResponse = delete("/journalpost/" + jp1);
    assertEquals(HttpStatus.OK, deleteJournalpostResponse.getStatusCode());
    ResponseEntity<String> getDeletedJournalpostResponse = get("/journalpost/" + jp1);
    assertEquals(HttpStatus.NOT_FOUND, getDeletedJournalpostResponse.getStatusCode());

    // It should fail with any of the following properties missing:
    jp.remove("offentligTittel");
    journalpostResponse = post("/journalpost", jp);
    assertEquals(HttpStatus.BAD_REQUEST, journalpostResponse.getStatusCode());
    assertNotNull(new JSONObject(journalpostResponse.getBody()).get("fieldErrors"));

    jp.put("offentligTittel", "testJournalpost");
    jp.remove("offentligTittelSensitiv");
    journalpostResponse = post("/journalpost", jp);
    assertEquals(HttpStatus.BAD_REQUEST, journalpostResponse.getStatusCode());
    assertNotNull(new JSONObject(journalpostResponse.getBody()).get("fieldErrors"));

    jp.put("offentligTittelSensitiv", "testJournalpost");
    jp.remove("journalposttype");
    journalpostResponse = post("/journalpost", jp);
    assertEquals(HttpStatus.BAD_REQUEST, journalpostResponse.getStatusCode());
    assertNotNull(new JSONObject(journalpostResponse.getBody()).get("fieldErrors"));

    jp.put("journalposttype", "inngåendeDokument");
    jp.remove("journalaar");
    journalpostResponse = post("/journalpost", jp);
    assertEquals(HttpStatus.BAD_REQUEST, journalpostResponse.getStatusCode());
    assertNotNull(new JSONObject(journalpostResponse.getBody()).get("fieldErrors"));

    jp.put("journalaar", 2020);
    jp.remove("journaldato");

    assertEquals(HttpStatus.BAD_REQUEST, journalpostResponse.getStatusCode());
    assertNotNull(new JSONObject(journalpostResponse.getBody()).get("fieldErrors"));

    jp.put("journaldato", "2020-02-02");
    jp.remove("journalpostnummer");
    journalpostResponse = post("/journalpost", jp);
    assertEquals(HttpStatus.BAD_REQUEST, journalpostResponse.getStatusCode());
    assertNotNull(new JSONObject(journalpostResponse.getBody()).get("fieldErrors"));

    jp.put("journalpostnummer", 1);
    jp.remove("journalsekvensnummer");
    journalpostResponse = post("/journalpost", jp);
    assertEquals(HttpStatus.BAD_REQUEST, journalpostResponse.getStatusCode());
    assertNotNull(new JSONObject(journalpostResponse.getBody()).get("fieldErrors"));

    jp.put("journalsekvensnummer", 1);
    jp.remove("saksmappe");
    journalpostResponse = post("/journalpost", jp);
    assertEquals(HttpStatus.BAD_REQUEST, journalpostResponse.getStatusCode());
    assertNotNull(new JSONObject(journalpostResponse.getBody()).get("fieldErrors"));

    // All properties are back
    jp.put("saksmappe", saksmappe.getId());
    journalpostResponse = post("/journalpost", jp);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    String jp2 = new JSONObject(journalpostResponse.getBody()).get("id").toString();

    // Delete Saksmappe
    ResponseEntity<String> deleteSaksmappeResponse = delete("/saksmappe/" + saksmappe.getId());
    assertEquals(HttpStatus.OK, deleteSaksmappeResponse.getStatusCode());
    ResponseEntity<String> getDeletedSaksmappeResponse = get("/saksmappe/" + saksmappe.getId());
    assertEquals(HttpStatus.NOT_FOUND, getDeletedSaksmappeResponse.getStatusCode());

    // Try to delete jp2 (it should already be deleted since we deleted Saksmappe)
    deleteJournalpostResponse = delete("/journalpost/" + jp2);
    assertEquals(HttpStatus.NOT_FOUND, deleteJournalpostResponse.getStatusCode());
  }

  /** It should fail if we try to update a journalpost with a JSON object that has an ID */
  @Test
  void failOnUpdateWithId() throws Exception {

    JSONObject jp = getJournalpostJSON();
    JSONObject saksmappeJSON = getSaksmappeJSON();
    ResponseEntity<String> saksmappeResponse = post("/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    SaksmappeJSON saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeJSON.class);
    jp.put("saksmappe", saksmappe.getId());
    ResponseEntity<String> journalpostResponse = post("/journalpost", jp);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    String jp1 = new JSONObject(journalpostResponse.getBody()).get("id").toString();

    // Update journalpost with an allowed object
    JSONObject update = new JSONObject();
    update.put("offentligTittelSensitiv", "--");
    ResponseEntity<String> updateJournalpostResponse = put("/journalpost/" + jp1, update);
    assertEquals(HttpStatus.OK, updateJournalpostResponse.getStatusCode());
    assertEquals(
        new JSONObject(updateJournalpostResponse.getBody()).get("offentligTittelSensitiv"),
        update.get("offentligTittelSensitiv"));

    // It should fail when updating with an ID
    update.put("id", "123");
    updateJournalpostResponse = put("/journalpost/" + jp1, update);
    assertEquals(HttpStatus.BAD_REQUEST, updateJournalpostResponse.getStatusCode());

    // Delete Saksmappe
    ResponseEntity<String> deleteSaksmappeResponse = delete("/saksmappe/" + saksmappe.getId());
    assertEquals(HttpStatus.OK, deleteSaksmappeResponse.getStatusCode());
    ResponseEntity<String> getDeletedSaksmappeResponse = get("/saksmappe/" + saksmappe.getId());
    assertEquals(HttpStatus.NOT_FOUND, getDeletedSaksmappeResponse.getStatusCode());
  }

  /**
   * Add multiple Dokumentbeskrivelses to Journalpost
   *
   * @throws Exception
   */
  @Test
  void insertDokumentbeskrivelse() throws Exception {
    JSONObject jp = getJournalpostJSON();
    JSONObject saksmappeJSON = getSaksmappeJSON();
    ResponseEntity<String> saksmappeResponse = post("/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, saksmappeResponse.getStatusCode());
    SaksmappeJSON saksmappe = gson.fromJson(saksmappeResponse.getBody(), SaksmappeJSON.class);
    jp.put("saksmappe", saksmappe.getId());
    ResponseEntity<String> journalpostResponse = post("/journalpost", jp);
    assertEquals(HttpStatus.CREATED, journalpostResponse.getStatusCode());
    String jpId = new JSONObject(journalpostResponse.getBody()).get("id").toString();

    JSONObject dokumentbeskrivelse = getDokumentbeskrivelseJSON();
    ResponseEntity<String> dokumentbeskrivelseResponse =
        post("/journalpost/" + jpId + "/dokumentbeskrivelse", dokumentbeskrivelse);
    assertEquals(HttpStatus.CREATED, dokumentbeskrivelseResponse.getStatusCode());

    // Check if the dokumentbeskrivelse was added
    journalpostResponse = get("/journalpost/" + jpId);
    JournalpostJSON journalpost =
        gson.fromJson(journalpostResponse.getBody(), JournalpostJSON.class);
    assertEquals(1, journalpost.getDokumentbeskrivelse().size());

    // Add another dokumentbeskrivelse
    JSONObject dokumentbeskrivelse2 = getDokumentbeskrivelseJSON();
    ResponseEntity<String> dokumentbeskrivelseResponse2 =
        post("/journalpost/" + jpId + "/dokumentbeskrivelse", dokumentbeskrivelse2);
    assertEquals(HttpStatus.CREATED, dokumentbeskrivelseResponse2.getStatusCode());

    // Check if the dokumentbeskrivelse was added
    journalpostResponse = get("/journalpost/" + jpId);
    journalpost = gson.fromJson(journalpostResponse.getBody(), JournalpostJSON.class);
    assertEquals(2, journalpost.getDokumentbeskrivelse().size());
    String dokId1 = journalpost.getDokumentbeskrivelse().get(0).getId();
    String dokId2 = journalpost.getDokumentbeskrivelse().get(1).getId();

    // Make sure the dokumentbeskrivelses was added
    ResponseEntity<String> getDokumentbeskrivelseResponse = get("/dokumentbeskrivelse/" + dokId1);
    assertEquals(HttpStatus.OK, getDokumentbeskrivelseResponse.getStatusCode());
    ResponseEntity<String> getDokumentbeskrivelseResponse2 = get("/dokumentbeskrivelse/" + dokId2);
    assertEquals(HttpStatus.OK, getDokumentbeskrivelseResponse2.getStatusCode());

    // Delete Saksmappe
    ResponseEntity<String> deleteSaksmappeResponse = delete("/saksmappe/" + saksmappe.getId());
    assertEquals(HttpStatus.OK, deleteSaksmappeResponse.getStatusCode());
    ResponseEntity<String> getDeletedSaksmappeResponse = get("/saksmappe/" + saksmappe.getId());
    assertEquals(HttpStatus.NOT_FOUND, getDeletedSaksmappeResponse.getStatusCode());

    // Make sure the dokumentbeskrivelse was deleted
    ResponseEntity<String> getJournalpostResponse = get("/journalpost/" + jpId);
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
    JSONObject jpInsert = getJournalpostJSON();
    JSONObject smInsert = getSaksmappeJSON();

    // Insert saksmappe
    ResponseEntity<String> smResponse = post("/saksmappe", smInsert);
    assertEquals(HttpStatus.CREATED, smResponse.getStatusCode());
    SaksmappeJSON smResponseJSON = gson.fromJson(smResponse.getBody(), SaksmappeJSON.class);

    // Insert journalpost
    jpInsert.put("saksmappe", smResponseJSON.getId());
    ResponseEntity<String> jpResponse = post("/journalpost", jpInsert);
    assertEquals(HttpStatus.CREATED, jpResponse.getStatusCode());
    JournalpostJSON jpResponseJSON = gson.fromJson(jpResponse.getBody(), JournalpostJSON.class);
    String jpId = jpResponseJSON.getId();

    // Insert Korrespondansepart
    JSONObject kp1Insert = getKorrespondansepartJSON();
    ResponseEntity<String> kp1Response =
        post("/journalpost/" + jpId + "/korrespondansepart", kp1Insert);
    assertEquals(HttpStatus.CREATED, kp1Response.getStatusCode());
    KorrespondansepartJSON kp1ResponseJSON =
        gson.fromJson(kp1Response.getBody(), KorrespondansepartJSON.class);
    String kp1Id = kp1ResponseJSON.getId();

    // Check if the korrespondansepart was added
    jpResponse = get("/journalpost/" + jpId);
    assertEquals(HttpStatus.OK, jpResponse.getStatusCode());
    jpResponseJSON = gson.fromJson(jpResponse.getBody(), JournalpostJSON.class);
    assertEquals(1, jpResponseJSON.getKorrespondansepart().size());

    // Insert another Korrespondansepart
    JSONObject kp2Insert = getKorrespondansepartJSON();
    ResponseEntity<String> kp2Response =
        post("/journalpost/" + jpId + "/korrespondansepart", kp2Insert);
    assertEquals(HttpStatus.CREATED, kp2Response.getStatusCode());
    KorrespondansepartJSON kp2ResponseJSON =
        gson.fromJson(kp2Response.getBody(), KorrespondansepartJSON.class);
    String kp2Id = kp2ResponseJSON.getId();

    // Check if the korrespondansepart was added
    jpResponse = get("/journalpost/" + jpId);
    assertEquals(HttpStatus.OK, jpResponse.getStatusCode());
    jpResponseJSON = gson.fromJson(jpResponse.getBody(), JournalpostJSON.class);
    assertEquals(2, jpResponseJSON.getKorrespondansepart().size());

    // Make sure the korrespondanseparts are reachable at their respective URLs
    ResponseEntity<String> korrpartResponse = get("/korrespondansepart/" + kp1Id);
    assertEquals(HttpStatus.OK, korrpartResponse.getStatusCode());
    korrpartResponse = get("/korrespondansepart/" + kp2Id);
    assertEquals(HttpStatus.OK, korrpartResponse.getStatusCode());

    // Delete Saksmappe
    ResponseEntity<String> deleteSaksmappeResponse = delete("/saksmappe/" + smResponseJSON.getId());
    assertEquals(HttpStatus.OK, deleteSaksmappeResponse.getStatusCode());
    ResponseEntity<String> getDeletedSaksmappeResponse =
        get("/saksmappe/" + smResponseJSON.getId());
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

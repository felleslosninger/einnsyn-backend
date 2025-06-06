package no.einnsyn.backend.entities.saksmappe;

import static no.einnsyn.backend.testutils.Assertions.assertEqualInstants;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.reflect.TypeToken;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SaksmappeControllerTest extends EinnsynControllerTestBase {

  @LocalServerPort private int port;

  ArkivDTO arkivDTO;
  ArkivdelDTO arkivdelDTO;

  @BeforeAll
  void setup() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertNotNull(arkivDTO.getId());

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    assertNotNull(arkivdelDTO.getId());
  }

  @AfterAll
  void teardown() throws Exception {
    var response = delete("/arkiv/" + arkivDTO.getId());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    assertEquals(Boolean.TRUE, arkivDTO.getDeleted());
  }

  /**
   * Test that we can insert a Saksmappe
   *
   * @throws Exception
   */
  @Test
  void testInsertSaksmappe() throws Exception {

    var saksmappeSource = new JSONObject();
    saksmappeSource.put("offentligTittel", "testOffentligTittel");
    saksmappeSource.put("offentligTittelSensitiv", "testOffentligTittelSensitiv");
    saksmappeSource.put("saksaar", 2020);
    saksmappeSource.put("sakssekvensnummer", 1);
    saksmappeSource.put("saksdato", "2020-01-01");

    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeSource);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    var saksmappeLocation = response.getHeaders().get("Location").get(0);
    assertEquals("/saksmappe/" + saksmappeDTO.getId(), saksmappeLocation);
    assertEquals("testOffentligTittel", saksmappeDTO.getOffentligTittel());
    assertEquals("testOffentligTittelSensitiv", saksmappeDTO.getOffentligTittelSensitiv());
    assertEquals(2020, saksmappeDTO.getSaksaar());
    assertEquals(1, saksmappeDTO.getSakssekvensnummer());
    assertEquals(LocalDate.of(2020, 1, 1).toString(), saksmappeDTO.getSaksdato());
    assertNotNull(saksmappeDTO.getId());

    var deleteResponse = delete("/saksmappe/" + saksmappeDTO.getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
  }

  /** Check that we can update a Saksmappe */
  @Test
  void testUpdateSaksmappe() throws Exception {
    var saksmappeInsertSource = new JSONObject();
    saksmappeInsertSource.put("offentligTittel", "testOffentligTittel");
    saksmappeInsertSource.put("offentligTittelSensitiv", "testOffentligTittelSensitiv");
    saksmappeInsertSource.put("saksaar", 2020);
    saksmappeInsertSource.put("sakssekvensnummer", 1);
    saksmappeInsertSource.put("saksdato", "2020-01-01");

    var insertResponse =
        post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeInsertSource);
    var insertedSaksmappe = gson.fromJson(insertResponse.getBody(), SaksmappeDTO.class);
    assertEquals(HttpStatus.CREATED, insertResponse.getStatusCode());
    assertEquals("testOffentligTittel", insertedSaksmappe.getOffentligTittel());

    var id = insertedSaksmappe.getId();
    var saksmappeUpdateSource = new JSONObject();
    saksmappeUpdateSource.put("offentligTittel", "updated offentligTittel");
    var updateResponse = patch("/saksmappe/" + id, saksmappeUpdateSource);
    var updatedSaksmappe = gson.fromJson(updateResponse.getBody(), SaksmappeDTO.class);
    assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
    assertEquals("updated offentligTittel", updatedSaksmappe.getOffentligTittel());
    assertEquals("testOffentligTittelSensitiv", updatedSaksmappe.getOffentligTittelSensitiv());

    // Delete saksmappe
    var deleteSaksmappeResponse = delete("/saksmappe/" + id);
    assertEquals(HttpStatus.OK, deleteSaksmappeResponse.getStatusCode());
  }

  /** Test that we can't insert a Saksmappe with a missing required field */
  @Test
  void insertSaksmappeMissingRequiredField() throws Exception {
    var saksmappeSource = new JSONObject();
    saksmappeSource.put("offentligTittel", "testOffentligTittel");
    saksmappeSource.put("offentligTittelSensitiv", "testOffentligTittelSensitiv");
    saksmappeSource.put("sakssekvensnummer", 1);
    saksmappeSource.put("saksdato", "2020-01-01");

    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeSource);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  /**
   * Test that we can insert a Saksmappe with a Journalpost given as a JSON object
   *
   * @throws Exception
   */
  @Test
  void insertSaksmappeWithJournalpost() throws Exception {
    var journalpostSource = new JSONObject();
    journalpostSource.put("offentligTittel", "testJournalpost");
    journalpostSource.put("offentligTittelSensitiv", "testJournalpost");
    journalpostSource.put("journalposttype", "inngaaende_dokument");
    journalpostSource.put("journalaar", 2020);
    journalpostSource.put("journaldato", "2020-02-02");
    journalpostSource.put("journalpostnummer", 1);
    journalpostSource.put("journalsekvensnummer", 1);

    var journalpostSourceList = new JSONArray();
    journalpostSourceList.put(journalpostSource);

    var saksmappeSource = new JSONObject();
    saksmappeSource.put("offentligTittel", "testOffentligTittel");
    saksmappeSource.put("offentligTittelSensitiv", "testOffentligTittelSensitiv");
    saksmappeSource.put("saksaar", 2020);
    saksmappeSource.put("sakssekvensnummer", 1);
    saksmappeSource.put("saksdato", "2020-01-01");
    saksmappeSource.put("journalpost", journalpostSourceList);

    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeSource);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappe = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertEquals("testOffentligTittel", saksmappe.getOffentligTittel());
    assertEquals("testOffentligTittelSensitiv", saksmappe.getOffentligTittelSensitiv());
    assertEquals(2020, saksmappe.getSaksaar());
    assertEquals(1, saksmappe.getSakssekvensnummer());
    assertEquals(LocalDate.of(2020, 1, 1).toString(), saksmappe.getSaksdato());
    assertNotNull(saksmappe.getId());

    var journalpostList = getJournalpostList(saksmappe.getId()).getItems();
    assertEquals(1, journalpostList.size());
    var journalpostDTO = journalpostList.get(0);
    assertNotNull(journalpostDTO.getId());
    assertEquals("testJournalpost", journalpostDTO.getOffentligTittel());
    assertEquals("inngaaende_dokument", journalpostDTO.getJournalposttype());
    assertEquals(2020, journalpostDTO.getJournalaar());
    assertEquals(LocalDate.of(2020, 2, 2).toString(), journalpostDTO.getJournaldato());
    assertEquals(1, journalpostDTO.getJournalpostnummer());

    // Delete Saksmappe, verify that everything is deleted
    var deleteSaksmappeResponse = delete("/saksmappe/" + saksmappe.getId());
    assertEquals(HttpStatus.OK, deleteSaksmappeResponse.getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/saksmappe/" + saksmappe.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/journalpost/" + journalpostDTO.getId()).getStatusCode());
  }

  // Add Saksmappe with journalpost, korrespondanseparts, dokumentbeskrivelses and dokumentobjekts
  @Test
  void insertWithChildren() throws Exception {
    var smJSON = getSaksmappeJSON();
    var jpJSON = getJournalpostJSON();
    var skjermingJSON = getSkjermingJSON();
    var korrpart1JSON = getKorrespondansepartJSON();
    var korrpart2JSON = getKorrespondansepartJSON();
    var dokbesk1JSON = getDokumentbeskrivelseJSON();
    var dokbesk2JSON = getDokumentbeskrivelseJSON();
    var dokobj1JSON = getDokumentobjektJSON();
    var dokobj2JSON = getDokumentobjektJSON();

    // Build structure
    var jpArray = new JSONArray();
    jpArray.put(jpJSON);
    var korrpartArray = new JSONArray();
    korrpartArray.put(korrpart1JSON);
    korrpartArray.put(korrpart2JSON);
    var dokbeskArray = new JSONArray();
    dokbeskArray.put(dokbesk1JSON);
    dokbeskArray.put(dokbesk2JSON);
    var dokobj1Array = new JSONArray();
    dokobj1Array.put(dokobj1JSON);
    var dokobj2Array = new JSONArray();
    dokobj2Array.put(dokobj2JSON);

    dokbesk1JSON.put("dokumentobjekt", dokobj1Array);
    dokbesk2JSON.put("dokumentobjekt", dokobj2Array);
    jpJSON.put("skjerming", skjermingJSON);
    jpJSON.put("korrespondansepart", korrpartArray);
    jpJSON.put("dokumentbeskrivelse", dokbeskArray);
    smJSON.put("journalpost", jpArray);

    // Insert and verify
    var smResponse = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", smJSON);
    assertEquals(HttpStatus.CREATED, smResponse.getStatusCode());
    var smDTO = gson.fromJson(smResponse.getBody(), SaksmappeDTO.class);
    var jpListDTO =
        getJournalpostList(
                smDTO.getId(),
                "skjerming",
                "korrespondansepart",
                "dokumentbeskrivelse.dokumentobjekt")
            .getItems();
    assertEquals(1, jpListDTO.size());
    var jpDTO = jpListDTO.get(0);
    assertNotNull(jpDTO.getSkjerming());
    var skjermingDTO = jpDTO.getSkjerming().getExpandedObject();
    assertNotNull(skjermingDTO);
    assertEquals(2, jpDTO.getKorrespondansepart().size());
    var korrpart1DTO = jpDTO.getKorrespondansepart().get(0).getExpandedObject();
    var korrpart2DTO = jpDTO.getKorrespondansepart().get(1).getExpandedObject();
    assertEquals(2, jpDTO.getDokumentbeskrivelse().size());
    var dokbesk1DTO = jpDTO.getDokumentbeskrivelse().get(0).getExpandedObject();
    var dokbesk2DTO = jpDTO.getDokumentbeskrivelse().get(1).getExpandedObject();
    assertEquals(1, dokbesk1DTO.getDokumentobjekt().size());
    var dokobj1DTO = dokbesk1DTO.getDokumentobjekt().get(0).getExpandedObject();
    assertEquals(1, dokbesk2DTO.getDokumentobjekt().size());
    var dokobj2DTO = dokbesk2DTO.getDokumentobjekt().get(0).getExpandedObject();

    // Delete a dokumentobjekt
    assertEquals(HttpStatus.OK, delete("/dokumentobjekt/" + dokobj2DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/dokumentobjekt/" + dokobj2DTO.getId()).getStatusCode());

    // Delete Saksmappe, verify that everything is deleted
    var deleteSaksmappeResponse = delete("/saksmappe/" + smDTO.getId());
    assertEquals(HttpStatus.OK, deleteSaksmappeResponse.getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/saksmappe/" + smDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/journalpost/" + jpDTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/skjerming/" + skjermingDTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/korrespondansepart/" + korrpart1DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/korrespondansepart/" + korrpart2DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/dokumentbeskrivelse/" + dokbesk1DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/dokumentbeskrivelse/" + dokbesk2DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/dokumentobjekt/" + dokobj1DTO.getId()).getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND, get("/dokumentobjekt/" + dokobj2DTO.getId()).getStatusCode());
  }

  // Insert saksmappes, and get a list
  @Test
  void insertAndGetList() throws Exception {
    var sm1JSON = getSaksmappeJSON();
    sm1JSON.put("offentligTittel", "sm1");
    var sm2JSON = getSaksmappeJSON();
    sm2JSON.put("offentligTittel", "sm2");
    var sm3JSON = getSaksmappeJSON();
    sm3JSON.put("offentligTittel", "sm3");
    var sm4JSON = getSaksmappeJSON();
    sm4JSON.put("offentligTittel", "sm4");
    var sm5JSON = getSaksmappeJSON();
    sm5JSON.put("offentligTittel", "sm5");

    var sm1Response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", sm1JSON);
    var sm1 = gson.fromJson(sm1Response.getBody(), SaksmappeDTO.class);
    var sm2Response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", sm2JSON);
    var sm2 = gson.fromJson(sm2Response.getBody(), SaksmappeDTO.class);
    var sm3Response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", sm3JSON);
    var sm3 = gson.fromJson(sm3Response.getBody(), SaksmappeDTO.class);
    var sm4Response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", sm4JSON);
    var sm4 = gson.fromJson(sm4Response.getBody(), SaksmappeDTO.class);
    var sm5Response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", sm5JSON);
    var sm5 = gson.fromJson(sm5Response.getBody(), SaksmappeDTO.class);

    assertEquals(HttpStatus.CREATED, sm1Response.getStatusCode());
    assertEquals(HttpStatus.CREATED, sm2Response.getStatusCode());
    assertEquals(HttpStatus.CREATED, sm3Response.getStatusCode());
    assertEquals(HttpStatus.CREATED, sm4Response.getStatusCode());
    assertEquals(HttpStatus.CREATED, sm5Response.getStatusCode());

    var resultListType = new TypeToken<PaginatedList<SaksmappeDTO>>() {}.getType();

    var smListResponse = get("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe");
    assertEquals(HttpStatus.OK, smListResponse.getStatusCode());
    PaginatedList<SaksmappeDTO> resultListDTO =
        gson.fromJson(smListResponse.getBody(), resultListType);
    var itemsDTO = resultListDTO.getItems();
    assertEquals(sm5.getOffentligTittel(), itemsDTO.get(0).getOffentligTittel());
    assertEquals(sm4.getOffentligTittel(), itemsDTO.get(1).getOffentligTittel());
    assertEquals(sm3.getOffentligTittel(), itemsDTO.get(2).getOffentligTittel());
    assertEquals(sm2.getOffentligTittel(), itemsDTO.get(3).getOffentligTittel());
    assertEquals(sm1.getOffentligTittel(), itemsDTO.get(4).getOffentligTittel());
    assertEquals(5, resultListDTO.getItems().size());
    assertNull(resultListDTO.getNext());
    assertNull(resultListDTO.getPrevious());

    smListResponse = get("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe?limit=2");
    assertEquals(HttpStatus.OK, smListResponse.getStatusCode());
    resultListDTO = gson.fromJson(smListResponse.getBody(), resultListType);
    itemsDTO = resultListDTO.getItems();
    assertEquals(sm5.getOffentligTittel(), itemsDTO.get(0).getOffentligTittel());
    assertEquals(sm4.getOffentligTittel(), itemsDTO.get(1).getOffentligTittel());
    assertEquals(2, resultListDTO.getItems().size());
    assertNull(resultListDTO.getPrevious());
    assertNotNull(resultListDTO.getNext());
    assertTrue(resultListDTO.getNext().contains("startingAfter=" + sm4.getId()));

    smListResponse =
        get("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe?limit=2&startingAfter=" + sm5.getId());
    assertEquals(HttpStatus.OK, smListResponse.getStatusCode());
    resultListDTO = gson.fromJson(smListResponse.getBody(), resultListType);
    itemsDTO = resultListDTO.getItems();
    assertEquals(sm4.getOffentligTittel(), itemsDTO.get(0).getOffentligTittel());
    assertEquals(sm3.getOffentligTittel(), itemsDTO.get(1).getOffentligTittel());
    assertEquals(2, itemsDTO.size());
    assertNotNull(resultListDTO.getPrevious());
    assertTrue(resultListDTO.getPrevious().contains("endingBefore=" + sm4.getId()));
    assertNotNull(resultListDTO.getNext());
    assertTrue(resultListDTO.getNext().contains("startingAfter=" + sm3.getId()));

    smListResponse =
        get("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe?limit=2&startingAfter=" + sm4.getId());
    assertEquals(HttpStatus.OK, smListResponse.getStatusCode());
    resultListDTO = gson.fromJson(smListResponse.getBody(), resultListType);
    itemsDTO = resultListDTO.getItems();
    assertEquals(sm3.getOffentligTittel(), itemsDTO.get(0).getOffentligTittel());
    assertEquals(sm2.getOffentligTittel(), itemsDTO.get(1).getOffentligTittel());
    assertEquals(2, itemsDTO.size());
    assertNotNull(resultListDTO.getPrevious());
    assertTrue(resultListDTO.getPrevious().contains("endingBefore=" + sm3.getId()));
    assertNotNull(resultListDTO.getNext());
    assertTrue(resultListDTO.getNext().contains("startingAfter=" + sm2.getId()));

    smListResponse =
        get("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe?limit=2&startingAfter=" + sm3.getId());
    assertEquals(HttpStatus.OK, smListResponse.getStatusCode());
    resultListDTO = gson.fromJson(smListResponse.getBody(), resultListType);
    itemsDTO = resultListDTO.getItems();
    assertEquals(sm2.getOffentligTittel(), itemsDTO.get(0).getOffentligTittel());
    assertEquals(sm1.getOffentligTittel(), itemsDTO.get(1).getOffentligTittel());
    assertEquals(2, itemsDTO.size());
    assertNotNull(resultListDTO.getPrevious());
    assertTrue(resultListDTO.getPrevious().contains("endingBefore=" + sm2.getId()));
    assertNull(resultListDTO.getNext());

    smListResponse =
        get("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe?limit=2&endingBefore=" + sm1.getId());
    assertEquals(HttpStatus.OK, smListResponse.getStatusCode());
    resultListDTO = gson.fromJson(smListResponse.getBody(), resultListType);
    itemsDTO = resultListDTO.getItems();
    assertEquals(sm3.getOffentligTittel(), itemsDTO.get(0).getOffentligTittel());
    assertEquals(sm2.getOffentligTittel(), itemsDTO.get(1).getOffentligTittel());
    assertEquals(2, resultListDTO.getItems().size());
    assertNotNull(resultListDTO.getPrevious());
    assertTrue(resultListDTO.getPrevious().contains("endingBefore=" + sm3.getId()));
    assertNotNull(resultListDTO.getNext());
    assertTrue(resultListDTO.getNext().contains("startingAfter=" + sm2.getId()));

    // ASC
    smListResponse = get("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe?limit=2&sortOrder=asc");
    assertEquals(HttpStatus.OK, smListResponse.getStatusCode());
    resultListDTO = gson.fromJson(smListResponse.getBody(), resultListType);
    itemsDTO = resultListDTO.getItems();
    assertEquals(sm1.getOffentligTittel(), itemsDTO.get(0).getOffentligTittel());
    assertEquals(sm2.getOffentligTittel(), itemsDTO.get(1).getOffentligTittel());
    assertEquals(2, resultListDTO.getItems().size());
    assertNull(resultListDTO.getPrevious());
    assertNotNull(resultListDTO.getNext());
    assertTrue(resultListDTO.getNext().contains("startingAfter=" + sm2.getId()));

    // ASC with empty startingAfter
    smListResponse =
        get("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe?limit=2&sortOrder=asc&startingAfter=");
    assertEquals(HttpStatus.OK, smListResponse.getStatusCode());
    resultListDTO = gson.fromJson(smListResponse.getBody(), resultListType);
    itemsDTO = resultListDTO.getItems();
    assertEquals(sm1.getOffentligTittel(), itemsDTO.get(0).getOffentligTittel());
    assertEquals(sm2.getOffentligTittel(), itemsDTO.get(1).getOffentligTittel());
    assertEquals(2, resultListDTO.getItems().size());
    assertNull(resultListDTO.getPrevious());
    assertNotNull(resultListDTO.getNext());
    assertTrue(resultListDTO.getNext().contains("startingAfter=" + sm2.getId()));

    smListResponse =
        get(
            "/arkivdel/"
                + arkivdelDTO.getId()
                + "/saksmappe?limit=2&endingBefore="
                + sm3.getId()
                + "&sortOrder=asc");
    assertEquals(HttpStatus.OK, smListResponse.getStatusCode());
    resultListDTO = gson.fromJson(smListResponse.getBody(), resultListType);
    itemsDTO = resultListDTO.getItems();
    assertEquals(sm1.getOffentligTittel(), itemsDTO.get(0).getOffentligTittel());
    assertEquals(sm2.getOffentligTittel(), itemsDTO.get(1).getOffentligTittel());
    assertEquals(2, resultListDTO.getItems().size());
    assertNull(resultListDTO.getPrevious());
    assertNotNull(resultListDTO.getNext());
    assertTrue(resultListDTO.getNext().contains("startingAfter=" + sm2.getId()));

    smListResponse =
        get(
            "/arkivdel/"
                + arkivdelDTO.getId()
                + "/saksmappe?limit=2&endingBefore="
                + sm2.getId()
                + "&sortOrder=asc");
    assertEquals(HttpStatus.OK, smListResponse.getStatusCode());
    resultListDTO = gson.fromJson(smListResponse.getBody(), resultListType);
    itemsDTO = resultListDTO.getItems();
    assertEquals(sm1.getOffentligTittel(), itemsDTO.get(0).getOffentligTittel());
    assertEquals(1, itemsDTO.size());
    assertNull(resultListDTO.getPrevious());
    assertNotNull(resultListDTO.getNext());
    assertTrue(resultListDTO.getNext().contains("startingAfter=" + sm1.getId()));

    smListResponse =
        get(
            "/arkivdel/"
                + arkivdelDTO.getId()
                + "/saksmappe?limit=2&endingBefore="
                + sm1.getId()
                + "&sortOrder=asc");
    assertEquals(HttpStatus.OK, smListResponse.getStatusCode());
    resultListDTO = gson.fromJson(smListResponse.getBody(), resultListType);
    itemsDTO = resultListDTO.getItems();
    assertEquals(0, itemsDTO.size());
    assertNull(resultListDTO.getPrevious());
    assertNotNull(resultListDTO.getNext());
    assertEquals(
        "/arkivdel/" + arkivdelDTO.getId() + "/saksmappe?limit=2&sortOrder=asc&startingAfter=",
        resultListDTO.getNext());

    smListResponse =
        get(
            "/arkivdel/"
                + arkivdelDTO.getId()
                + "/saksmappe?limit=2&startingAfter="
                + sm5.getId()
                + "&sortOrder=asc");
    assertEquals(HttpStatus.OK, smListResponse.getStatusCode());
    resultListDTO = gson.fromJson(smListResponse.getBody(), resultListType);
    itemsDTO = resultListDTO.getItems();
    assertEquals(0, itemsDTO.size());
    assertNotNull(resultListDTO.getPrevious());
    assertEquals(
        "/arkivdel/" + arkivdelDTO.getId() + "/saksmappe?limit=2&sortOrder=asc&endingBefore=",
        resultListDTO.getPrevious());

    smListResponse =
        get("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe?limit=2&endingBefore=&sortOrder=asc");
    assertEquals(HttpStatus.OK, smListResponse.getStatusCode());
    resultListDTO = gson.fromJson(smListResponse.getBody(), resultListType);
    itemsDTO = resultListDTO.getItems();
    assertEquals(2, itemsDTO.size());
    assertEquals(sm4.getOffentligTittel(), itemsDTO.get(0).getOffentligTittel());
    assertEquals(sm5.getOffentligTittel(), itemsDTO.get(1).getOffentligTittel());
    assertNotNull(resultListDTO.getPrevious());
    assertEquals(
        "/arkivdel/"
            + arkivdelDTO.getId()
            + "/saksmappe?limit=2&sortOrder=asc&endingBefore="
            + sm4.getId(),
        resultListDTO.getPrevious());
    assertNull(resultListDTO.getNext());

    smListResponse =
        get(
            "/arkivdel/"
                + arkivdelDTO.getId()
                + "/saksmappe?limit=2&startingAfter="
                + sm3.getId()
                + "&sortOrder=asc");
    assertEquals(HttpStatus.OK, smListResponse.getStatusCode());
    resultListDTO = gson.fromJson(smListResponse.getBody(), resultListType);
    itemsDTO = resultListDTO.getItems();
    assertEquals(sm4.getOffentligTittel(), itemsDTO.get(0).getOffentligTittel());
    assertEquals(sm5.getOffentligTittel(), itemsDTO.get(1).getOffentligTittel());
    assertEquals(2, itemsDTO.size());
    assertNotNull(resultListDTO.getPrevious());
    assertTrue(resultListDTO.getPrevious().contains("endingBefore=" + sm4.getId()));
    assertNull(resultListDTO.getNext());

    // Delete Saksmappes
    assertEquals(HttpStatus.OK, delete("/saksmappe/" + sm1.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/saksmappe/" + sm1.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/saksmappe/" + sm2.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/saksmappe/" + sm2.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/saksmappe/" + sm3.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/saksmappe/" + sm3.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/saksmappe/" + sm4.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/saksmappe/" + sm4.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, delete("/saksmappe/" + sm5.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/saksmappe/" + sm5.getId()).getStatusCode());
  }

  // Test recursive deletion from Arkiv
  @Test
  void testDeletionFromArkiv() throws Exception {
    var arkiv1JSON = getArkivJSON();
    var response = post("/arkiv", arkiv1JSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkiv1DTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkiv1DTO.getId() + "/arkivdel", getArkivdelJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivdel1DTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    var saksmappe1JSON = getSaksmappeJSON();
    response = post("/arkivdel/" + arkivdel1DTO.getId() + "/saksmappe", saksmappe1JSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappe1DTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe1DTO.getId());

    var arkiv2JSON = getArkivJSON();
    response = post("/arkiv", arkiv2JSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkiv2DTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkiv2DTO.getId() + "/arkivdel", getArkivdelJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivdel2DTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    var saksmappe2JSON = getSaksmappeJSON();
    response = post("/arkivdel/" + arkivdel2DTO.getId() + "/saksmappe", saksmappe2JSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappe2DTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe2DTO.getId());

    // Delete arkiv1, verify that only saksmappe1 is deleted
    response = delete("/arkiv/" + arkiv1DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkiv/" + arkiv1DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/saksmappe/" + saksmappe1DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, get("/arkiv/" + arkiv2DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, get("/saksmappe/" + saksmappe2DTO.getId()).getStatusCode());

    // Delete arkiv2, verify that saksmappe2 is deleted
    response = delete("/arkiv/" + arkiv2DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkiv/" + arkiv2DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/saksmappe/" + saksmappe2DTO.getId()).getStatusCode());
  }

  // Test recursive deletion from Arkivdel
  @Test
  void testDeletionFromArkivdel() throws Exception {
    var arkivdel1JSON = getArkivdelJSON();
    var response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", arkivdel1JSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivdel1DTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    assertNotNull(arkivdel1DTO.getId());

    var saksmappe1JSON = getSaksmappeJSON();
    response = post("/arkivdel/" + arkivdel1DTO.getId() + "/saksmappe", saksmappe1JSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappe1DTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe1DTO.getId());

    var arkivdel2JSON = getArkivdelJSON();
    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", arkivdel2JSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivdel2DTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);
    assertNotNull(arkivdel2DTO.getId());

    var saksmappe2JSON = getSaksmappeJSON();
    response = post("/arkivdel/" + arkivdel2DTO.getId() + "/saksmappe", saksmappe2JSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappe2DTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe2DTO.getId());

    // Delete arkivdel1, verify that only saksmappe1 is deleted
    response = delete("/arkivdel/" + arkivdel1DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkivdel/" + arkivdel1DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/saksmappe/" + saksmappe1DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, get("/arkivdel/" + arkivdel2DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, get("/saksmappe/" + saksmappe2DTO.getId()).getStatusCode());

    // Delete arkivdel2, verify that saksmappe2 is deleted
    response = delete("/arkivdel/" + arkivdel2DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkivdel/" + arkivdel2DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/saksmappe/" + saksmappe2DTO.getId()).getStatusCode());
  }

  // Make sure we cannot POST directly to /saksmappe
  @Test
  void testPostToSaksmappe() throws Exception {
    var response = post("/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
  }

  @Test
  void checkLegacyArkivskaperFromJournalenhet() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    var journalenhet = enhetRepository.findById(journalenhetId).orElse(null);
    var saksmappe = saksmappeRepository.findById(saksmappeDTO.getId()).orElse(null);
    assertEquals(journalenhet.getIri(), saksmappe.getArkivskaper());

    delete("/saksmappe/" + saksmappeDTO.getId());
    assertNull(saksmappeRepository.findById(saksmappeDTO.getId()).orElse(null));
  }

  @Test
  void checkLegacyArkivskaperFromAdmEnhet() throws Exception {
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("administrativEnhet", "UNDER");
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    var admEnhet = enhetRepository.findById(underenhetId).orElse(null);
    var saksmappe = saksmappeRepository.findById(saksmappeDTO.getId()).orElse(null);
    assertEquals(admEnhet.getIri(), saksmappe.getArkivskaper());

    delete("/saksmappe/" + saksmappeDTO.getId());
    assertNull(saksmappeRepository.findById(saksmappeDTO.getId()).orElse(null));
  }

  @Test
  void testCustomOppdatertDato() throws Exception {
    var saksmappeJSON = getSaksmappeJSON();
    var oppdatertDato = ZonedDateTime.parse("2002-02-02T02:02:02Z");
    saksmappeJSON.put("oppdatertDato", oppdatertDato.toString());

    // Normal users should not be allowed
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Admin users should be allowed
    response = postAdmin("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertEqualInstants(oppdatertDato.toString(), saksmappeDTO.getOppdatertDato());

    deleteAdmin("/saksmappe/" + saksmappeDTO.getId());
  }

  @Test
  void testCustomPublisertDato() throws Exception {
    var saksmappeJSON = getSaksmappeJSON();
    var publisertDato = ZonedDateTime.parse("2002-02-02T02:02:02Z");
    saksmappeJSON.put("publisertDato", publisertDato.toString());

    // Normal users should not be allowed
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Admin users should be allowed
    response = postAdmin("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertEqualInstants(publisertDato.toString(), saksmappeDTO.getPublisertDato());

    deleteAdmin("/saksmappe/" + saksmappeDTO.getId());
  }

  @Test
  void findSaksmappeBySystemId() throws Exception {
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("systemId", "4b1a6279-d4a9-49f1-8c95-a0e8810bf1b5");

    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    response = get("/saksmappe/4b1a6279-d4a9-49f1-8c95-a0e8810bf1b5");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var saksmappeDTO2 = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertEquals(saksmappeDTO.getId(), saksmappeDTO2.getId());

    delete("/saksmappe/" + saksmappeDTO.getId());
    assertEquals(
        HttpStatus.NOT_FOUND,
        get("/saksmappe/4b1a6279-d4a9-49f1-8c95-a0e8810bf1b5").getStatusCode());
  }

  @Test
  void addSaksmappeWithExistingJournalpost() throws Exception {
    var response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappe1DTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe1DTO.getId());

    var journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("systemId", "uniqueId");
    journalpostJSON.put("offentligTittel", "originalTitle");
    response = post("/saksmappe/" + saksmappe1DTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var journalpostDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);
    assertEquals("originalTitle", journalpostDTO.getOffentligTittel());

    // Add another saksmappe with the same journalpost
    var saksmappeJSON = getSaksmappeJSON();
    var journalpostJSONList = new JSONArray();
    journalpostJSON.put("offentligTittel", "newTitle");
    journalpostJSONList.put(journalpostJSON);
    saksmappeJSON.put("journalpost", journalpostJSONList);
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappe2DTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe2DTO.getId());
    var journalpostList2 = getJournalpostList(saksmappe2DTO.getId()).getItems();
    assertEquals(1, journalpostList2.size());
    var journalpost2DTO = journalpostList2.get(0);
    assertEquals(journalpostDTO.getId(), journalpost2DTO.getId());
    assertEquals("newTitle", journalpost2DTO.getOffentligTittel());

    // Verify that the journalpost is removed from saksmappe1
    response = get("/saksmappe/" + saksmappe1DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    saksmappe1DTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    var journalpostList1 = getJournalpostList(saksmappe1DTO.getId()).getItems();
    assertEquals(0, journalpostList1.size());

    // Delete
    response = delete("/saksmappe/" + saksmappe1DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    response = delete("/saksmappe/" + saksmappe2DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}

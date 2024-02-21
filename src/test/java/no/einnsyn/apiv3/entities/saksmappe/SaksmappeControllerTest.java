package no.einnsyn.apiv3.entities.saksmappe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.reflect.TypeToken;
import java.time.LocalDate;
import java.util.List;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SaksmappeControllerTest extends EinnsynControllerTestBase {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;
  private ArkivDTO arkivDTO;

  private HttpEntity<String> getRequest(JSONObject requestBody) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<>(requestBody.toString(), headers);
  }

  @BeforeAll
  void addArkiv() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
  }

  @AfterAll
  void removeArkiv() throws Exception {
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

    String url = "http://localhost:" + port + "/arkiv/" + arkivDTO.getId() + "/saksmappe";
    JSONObject saksmappeSource = new JSONObject();
    saksmappeSource.put("offentligTittel", "testOffentligTittel");
    saksmappeSource.put("offentligTittelSensitiv", "testOffentligTittelSensitiv");
    saksmappeSource.put("saksaar", 2020);
    saksmappeSource.put("sakssekvensnummer", 1);
    saksmappeSource.put("saksdato", "2020-01-01");
    saksmappeSource.put("virksomhetIri", "virksomhetIri");

    HttpEntity<String> request = getRequest(saksmappeSource);
    ResponseEntity<SaksmappeDTO> response =
        this.restTemplate.postForEntity(url, request, SaksmappeDTO.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    String saksmappeLocation = response.getHeaders().get("Location").get(0);
    assertEquals("/saksmappe/" + response.getBody().getId(), saksmappeLocation);
    assertEquals("testOffentligTittel", response.getBody().getOffentligTittel());
    assertEquals("testOffentligTittelSensitiv", response.getBody().getOffentligTittelSensitiv());
    assertEquals(2020, response.getBody().getSaksaar());
    assertEquals(1, response.getBody().getSakssekvensnummer());
    assertEquals(LocalDate.of(2020, 1, 1).toString(), response.getBody().getSaksdato());
    assertNotNull(response.getBody().getId());

    var deleteResponse = delete("/saksmappe/" + response.getBody().getId());
    assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
  }

  /** Check that we can update a Saksmappe */
  @Test
  void testUpdateSaksmappe() throws Exception {
    JSONObject saksmappeInsertSource = new JSONObject();
    saksmappeInsertSource.put("offentligTittel", "testOffentligTittel");
    saksmappeInsertSource.put("offentligTittelSensitiv", "testOffentligTittelSensitiv");
    saksmappeInsertSource.put("saksaar", 2020);
    saksmappeInsertSource.put("sakssekvensnummer", 1);
    saksmappeInsertSource.put("saksdato", "2020-01-01");
    saksmappeInsertSource.put("virksomhetIri", "virksomhetIri");

    HttpEntity<String> insertRequest = getRequest(saksmappeInsertSource);
    ResponseEntity<SaksmappeDTO> insertResponse =
        this.restTemplate.postForEntity(
            "http://localhost:" + port + "/arkiv/" + arkivDTO.getId() + "/saksmappe",
            insertRequest,
            SaksmappeDTO.class);
    SaksmappeDTO insertedSaksmappe = insertResponse.getBody();

    assertEquals(HttpStatus.CREATED, insertResponse.getStatusCode());
    assertEquals("testOffentligTittel", insertedSaksmappe.getOffentligTittel());

    String id = insertedSaksmappe.getId();
    JSONObject saksmappeUpdateSource = new JSONObject();
    saksmappeUpdateSource.put("offentligTittel", "updated offentligTittel");
    HttpEntity<String> updateRequest = getRequest(saksmappeUpdateSource);
    ResponseEntity<SaksmappeDTO> updateResponse =
        this.restTemplate.exchange(
            "http://localhost:" + port + "/saksmappe/" + id,
            HttpMethod.PUT,
            updateRequest,
            SaksmappeDTO.class);
    SaksmappeDTO updatedSaksmappe = updateResponse.getBody();
    assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
    assertEquals("updated offentligTittel", updatedSaksmappe.getOffentligTittel());
    assertEquals("testOffentligTittelSensitiv", updatedSaksmappe.getOffentligTittelSensitiv());

    // Delete saksmappe
    var deleteSaksmappeResponse = delete("/saksmappe/" + id);
    assertEquals(HttpStatus.OK, deleteSaksmappeResponse.getStatusCode());
  }

  /** Test that we can't insert a Saksmappe with a missing required field */
  @Test
  void insertSaksmappeMissingRequiredField() {
    JSONObject saksmappeSource = new JSONObject();
    saksmappeSource.put("offentligTittel", "testOffentligTittel");
    saksmappeSource.put("offentligTittelSensitiv", "testOffentligTittelSensitiv");
    // saksmappeSource.put("saksaar", 2020);
    saksmappeSource.put("sakssekvensnummer", 1);
    saksmappeSource.put("saksdato", "2020-01-01");
    saksmappeSource.put("virksomhetIri", "virksomhetIri");

    HttpEntity<String> request = getRequest(saksmappeSource);
    ResponseEntity<SaksmappeDTO> response =
        this.restTemplate.postForEntity(
            "http://localhost:" + port + "/arkiv/" + arkivDTO.getId() + "/saksmappe",
            request,
            SaksmappeDTO.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  /**
   * Test that we can insert a Saksmappe with a Journalpost given as a JSON object
   *
   * @throws Exception
   */
  @Test
  void insertSaksmappeWithJournalpost() throws Exception {
    JSONObject journalpostSource = new JSONObject();
    journalpostSource.put("offentligTittel", "testJournalpost");
    journalpostSource.put("offentligTittelSensitiv", "testJournalpost");
    journalpostSource.put("journalposttype", "inngåendeDokument");
    journalpostSource.put("journalaar", 2020);
    journalpostSource.put("journaldato", "2020-02-02");
    journalpostSource.put("journalpostnummer", 1);
    journalpostSource.put("journalsekvensnummer", 1);

    JSONArray journalpostSourceList = new JSONArray();
    journalpostSourceList.add(journalpostSource);

    JSONObject saksmappeSource = new JSONObject();
    saksmappeSource.put("offentligTittel", "testOffentligTittel");
    saksmappeSource.put("offentligTittelSensitiv", "testOffentligTittelSensitiv");
    saksmappeSource.put("saksaar", 2020);
    saksmappeSource.put("sakssekvensnummer", 1);
    saksmappeSource.put("saksdato", "2020-01-01");
    saksmappeSource.put("virksomhetIri", "virksomhetIri");
    saksmappeSource.put("journalpost", journalpostSourceList);

    HttpEntity<String> request = getRequest(saksmappeSource);
    ResponseEntity<String> responseString =
        this.restTemplate.postForEntity(
            "http://localhost:" + port + "/arkiv/" + arkivDTO.getId() + "/saksmappe",
            request,
            String.class);

    assertEquals(HttpStatus.CREATED, responseString.getStatusCode());
    SaksmappeDTO saksmappe = gson.fromJson(responseString.getBody(), SaksmappeDTO.class);
    assertEquals("testOffentligTittel", saksmappe.getOffentligTittel());
    assertEquals("testOffentligTittelSensitiv", saksmappe.getOffentligTittelSensitiv());
    assertEquals(2020, saksmappe.getSaksaar());
    assertEquals(1, saksmappe.getSakssekvensnummer());
    assertEquals(LocalDate.of(2020, 1, 1).toString(), saksmappe.getSaksdato());
    assertNotNull(saksmappe.getId());

    List<ExpandableField<JournalpostDTO>> journalpostList = saksmappe.getJournalpost();
    assertEquals(1, journalpostList.size());
    JournalpostDTO journalpost = journalpostList.get(0).getExpandedObject();
    assertNotNull(journalpost.getId());
    assertEquals("testJournalpost", journalpost.getOffentligTittel());
    assertEquals("inngåendeDokument", journalpost.getJournalposttype());
    assertEquals(2020, journalpost.getJournalaar());
    assertEquals(LocalDate.of(2020, 2, 2).toString(), journalpost.getJournaldato());
    assertEquals(1, journalpost.getJournalpostnummer());

    // Delete Saksmappe, verify that everything is deleted
    var deleteSaksmappeResponse = delete("/saksmappe/" + saksmappe.getId());
    assertEquals(HttpStatus.OK, deleteSaksmappeResponse.getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/saksmappe/" + saksmappe.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/journalpost/" + journalpost.getId()).getStatusCode());
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
    jpArray.add(jpJSON);
    var korrpartArray = new JSONArray();
    korrpartArray.add(korrpart1JSON);
    korrpartArray.add(korrpart2JSON);
    var dokbeskArray = new JSONArray();
    dokbeskArray.add(dokbesk1JSON);
    dokbeskArray.add(dokbesk2JSON);
    var dokobj1Array = new JSONArray();
    dokobj1Array.add(dokobj1JSON);
    var dokobj2Array = new JSONArray();
    dokobj2Array.add(dokobj2JSON);

    dokbesk1JSON.put("dokumentobjekt", dokobj1Array);
    dokbesk2JSON.put("dokumentobjekt", dokobj2Array);
    jpJSON.put("skjerming", skjermingJSON);
    jpJSON.put("korrespondansepart", korrpartArray);
    jpJSON.put("dokumentbeskrivelse", dokbeskArray);
    smJSON.put("journalpost", jpArray);

    // Insert and verify
    var smResponse = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", smJSON);
    assertEquals(HttpStatus.CREATED, smResponse.getStatusCode());
    var smDTO = gson.fromJson(smResponse.getBody(), SaksmappeDTO.class);
    var jpListDTO = smDTO.getJournalpost();
    assertEquals(1, jpListDTO.size());
    var jpDTO = jpListDTO.get(0).getExpandedObject();
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

    var sm1Response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", sm1JSON);
    var sm1 = gson.fromJson(sm1Response.getBody(), SaksmappeDTO.class);
    var sm2Response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", sm2JSON);
    var sm2 = gson.fromJson(sm2Response.getBody(), SaksmappeDTO.class);
    var sm3Response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", sm3JSON);
    var sm3 = gson.fromJson(sm3Response.getBody(), SaksmappeDTO.class);
    var sm4Response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", sm4JSON);
    var sm4 = gson.fromJson(sm4Response.getBody(), SaksmappeDTO.class);
    var sm5Response = post("/arkiv/" + arkivDTO.getId() + "/saksmappe", sm5JSON);
    var sm5 = gson.fromJson(sm5Response.getBody(), SaksmappeDTO.class);

    assertEquals(HttpStatus.CREATED, sm1Response.getStatusCode());
    assertEquals(HttpStatus.CREATED, sm2Response.getStatusCode());
    assertEquals(HttpStatus.CREATED, sm3Response.getStatusCode());
    assertEquals(HttpStatus.CREATED, sm4Response.getStatusCode());
    assertEquals(HttpStatus.CREATED, sm5Response.getStatusCode());

    var resultListType = new TypeToken<ResultList<SaksmappeDTO>>() {}.getType();

    var smListResponse = get("/arkiv/" + arkivDTO.getId() + "/saksmappe");
    assertEquals(HttpStatus.OK, smListResponse.getStatusCode());
    ResultList<SaksmappeDTO> resultListDTO =
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

    smListResponse = get("/arkiv/" + arkivDTO.getId() + "/saksmappe?limit=2");
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
        get("/arkiv/" + arkivDTO.getId() + "/saksmappe?limit=2&startingAfter=" + sm5.getId());
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
        get("/arkiv/" + arkivDTO.getId() + "/saksmappe?limit=2&startingAfter=" + sm4.getId());
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
        get("/arkiv/" + arkivDTO.getId() + "/saksmappe?limit=2&startingAfter=" + sm3.getId());
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
        get("/arkiv/" + arkivDTO.getId() + "/saksmappe?limit=2&endingBefore=" + sm1.getId());
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
    smListResponse = get("/arkiv/" + arkivDTO.getId() + "/saksmappe?limit=2&sortOrder=asc");
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
        get("/arkiv/" + arkivDTO.getId() + "/saksmappe?limit=2&sortOrder=asc&startingAfter=");
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
            "/arkiv/"
                + arkivDTO.getId()
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
            "/arkiv/"
                + arkivDTO.getId()
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
            "/arkiv/"
                + arkivDTO.getId()
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
        "/arkiv/" + arkivDTO.getId() + "/saksmappe?startingAfter=", resultListDTO.getNext());

    smListResponse =
        get(
            "/arkiv/"
                + arkivDTO.getId()
                + "/saksmappe?limit=2&startingAfter="
                + sm5.getId()
                + "&sortOrder=asc");
    assertEquals(HttpStatus.OK, smListResponse.getStatusCode());
    resultListDTO = gson.fromJson(smListResponse.getBody(), resultListType);
    itemsDTO = resultListDTO.getItems();
    assertEquals(0, itemsDTO.size());
    assertNotNull(resultListDTO.getPrevious());
    assertEquals(
        "/arkiv/" + arkivDTO.getId() + "/saksmappe?endingBefore=", resultListDTO.getPrevious());

    smListResponse =
        get("/arkiv/" + arkivDTO.getId() + "/saksmappe?limit=2&endingBefore=&sortOrder=asc");
    assertEquals(HttpStatus.OK, smListResponse.getStatusCode());
    resultListDTO = gson.fromJson(smListResponse.getBody(), resultListType);
    itemsDTO = resultListDTO.getItems();
    assertEquals(2, itemsDTO.size());
    assertEquals(sm4.getOffentligTittel(), itemsDTO.get(0).getOffentligTittel());
    assertEquals(sm5.getOffentligTittel(), itemsDTO.get(1).getOffentligTittel());
    assertNotNull(resultListDTO.getPrevious());
    assertEquals(
        "/arkiv/" + arkivDTO.getId() + "/saksmappe?endingBefore=" + sm4.getId(),
        resultListDTO.getPrevious());
    assertNull(resultListDTO.getNext());

    smListResponse =
        get(
            "/arkiv/"
                + arkivDTO.getId()
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
    var arkiv1DTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(arkiv1DTO.getId());

    var saksmappe1JSON = getSaksmappeJSON();
    response = post("/arkiv/" + arkiv1DTO.getId() + "/saksmappe", saksmappe1JSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappe1DTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe1DTO.getId());

    var arkiv2JSON = getArkivJSON();
    response = post("/arkiv", arkiv2JSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkiv2DTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(arkiv2DTO.getId());

    var saksmappe2JSON = getSaksmappeJSON();
    response = post("/arkiv/" + arkiv2DTO.getId() + "/saksmappe", saksmappe2JSON);
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
    var arkivdel1DTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(arkivdel1DTO.getId());

    var saksmappe1JSON = getSaksmappeJSON();
    response = post("/arkivdel/" + arkivdel1DTO.getId() + "/saksmappe", saksmappe1JSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappe1DTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe1DTO.getId());

    var arkivdel2JSON = getArkivdelJSON();
    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", arkivdel2JSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivdel2DTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
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

  // Test recursive deletion from Klasse
  @Test
  void testDeletionFromKlasse() throws Exception {

    var arkivdelJSON = getArkivdelJSON();
    var response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", arkivdelJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var arkivdelDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    var klasse1JSON = getKlasseJSON();
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/klasse", klasse1JSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var klasse1DTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(klasse1DTO.getId());

    var saksmappe1JSON = getSaksmappeJSON();
    response = post("/klasse/" + klasse1DTO.getId() + "/saksmappe", saksmappe1JSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappe1DTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe1DTO.getId());

    var klasse2JSON = getKlasseJSON();
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/klasse", klasse2JSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var klasse2DTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(klasse2DTO.getId());

    var saksmappe2JSON = getSaksmappeJSON();
    response = post("/klasse/" + klasse2DTO.getId() + "/saksmappe", saksmappe2JSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var saksmappe2DTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);
    assertNotNull(saksmappe2DTO.getId());

    // Delete klasse1, verify that only saksmappe1 is deleted
    response = delete("/klasse/" + klasse1DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/klasse/" + klasse1DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/saksmappe/" + saksmappe1DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, get("/klasse/" + klasse2DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.OK, get("/saksmappe/" + saksmappe2DTO.getId()).getStatusCode());

    // Delete klasse2, verify that saksmappe2 is deleted
    response = delete("/klasse/" + klasse2DTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/klasse/" + klasse2DTO.getId()).getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/saksmappe/" + saksmappe2DTO.getId()).getStatusCode());

    // Delete Arkiv
    response = delete("/arkivdel/" + arkivdelDTO.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, get("/arkivdel/" + arkivdelDTO.getId()).getStatusCode());
  }

  // Make sure we cannot POST directly to /saksmappe
  @Test
  void testPostToSaksmappe() throws Exception {
    var response = post("/saksmappe", getSaksmappeJSON());
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }
}

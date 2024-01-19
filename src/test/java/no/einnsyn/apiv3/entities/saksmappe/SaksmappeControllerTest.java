package no.einnsyn.apiv3.entities.saksmappe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;
import java.util.List;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.EinnsynControllerTestBase;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
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

  private HttpEntity<String> getRequest(JSONObject requestBody) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<String>(requestBody.toString(), headers);
  }

  /**
   * Test that we can insert a Saksmappe
   *
   * @throws Exception
   */
  @Test
  void testInsertSaksmappe() throws Exception {
    String url = "http://localhost:" + port + "/saksmappe";
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
  }

  /** Check that we can update a Saksmappe */
  @Test
  void testUpdateSaksmappe() {
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
            "http://localhost:" + port + "/saksmappe", insertRequest, SaksmappeDTO.class);
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
            "http://localhost:" + port + "/saksmappe", request, SaksmappeDTO.class);

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
            "http://localhost:" + port + "/saksmappe", request, String.class);

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
  }
}

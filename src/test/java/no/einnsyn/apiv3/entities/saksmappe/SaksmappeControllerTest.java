package no.einnsyn.apiv3.entities.saksmappe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.time.LocalDate;
import java.util.List;
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
import org.springframework.test.context.ActiveProfiles;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import no.einnsyn.apiv3.entities.EinnsynControllerTest;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostJSON;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeJSON;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class SaksmappeControllerTest extends EinnsynControllerTest {

  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;

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
  public void insertSaksmappe() throws Exception {
    String url = "http://localhost:" + port + "/saksmappe";
    JSONObject saksmappeSource = new JSONObject();
    saksmappeSource.put("offentligTittel", "testOffentligTittel");
    saksmappeSource.put("offentligTittelSensitiv", "testOffentligTittelSensitiv");
    saksmappeSource.put("saksaar", 2020);
    saksmappeSource.put("sakssekvensnummer", 1);
    saksmappeSource.put("saksdato", "2020-01-01");
    saksmappeSource.put("virksomhetIri", "virksomhetIri");

    HttpEntity<String> request = getRequest(saksmappeSource);
    ResponseEntity<SaksmappeJSON> response =
        this.restTemplate.postForEntity(url, request, SaksmappeJSON.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    String saksmappeLocation = response.getHeaders().get("Location").get(0);
    assertEquals(url + "/" + response.getBody().getId(), saksmappeLocation);
    assertEquals("testOffentligTittel", response.getBody().getOffentligTittel());
    assertEquals("testOffentligTittelSensitiv", response.getBody().getOffentligTittelSensitiv());
    assertEquals(2020, response.getBody().getSaksaar());
    assertEquals(1, response.getBody().getSakssekvensnummer());
    assertEquals(LocalDate.of(2020, 1, 1), response.getBody().getSaksdato());
    assertNotNull(response.getBody().getId());
  }


  /**
   * Check that we can update a Saksmappe
   */
  @Test
  public void updateSaksmappe() {
    JSONObject saksmappeInsertSource = new JSONObject();
    saksmappeInsertSource.put("offentligTittel", "testOffentligTittel");
    saksmappeInsertSource.put("offentligTittelSensitiv", "testOffentligTittelSensitiv");
    saksmappeInsertSource.put("saksaar", 2020);
    saksmappeInsertSource.put("sakssekvensnummer", 1);
    saksmappeInsertSource.put("saksdato", "2020-01-01");
    saksmappeInsertSource.put("virksomhetIri", "virksomhetIri");

    HttpEntity<String> insertRequest = getRequest(saksmappeInsertSource);
    ResponseEntity<SaksmappeJSON> insertResponse = this.restTemplate.postForEntity(
        "http://localhost:" + port + "/saksmappe", insertRequest, SaksmappeJSON.class);
    SaksmappeJSON insertedSaksmappe = insertResponse.getBody();

    assertEquals(HttpStatus.CREATED, insertResponse.getStatusCode());
    assertEquals("testOffentligTittel", insertedSaksmappe.getOffentligTittel());

    String id = insertedSaksmappe.getId();
    JSONObject saksmappeUpdateSource = new JSONObject();
    saksmappeUpdateSource.put("offentligTittel", "updated offentligTittel");
    HttpEntity<String> updateRequest = getRequest(saksmappeUpdateSource);
    ResponseEntity<SaksmappeJSON> updateResponse =
        this.restTemplate.exchange("http://localhost:" + port + "/saksmappe/" + id, HttpMethod.PUT,
            updateRequest, SaksmappeJSON.class);
    SaksmappeJSON updatedSaksmappe = updateResponse.getBody();
    assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
    assertEquals("updated offentligTittel", updatedSaksmappe.getOffentligTittel());
    assertEquals("testOffentligTittelSensitiv", updatedSaksmappe.getOffentligTittelSensitiv());
  }


  /**
   * Test that we can't insert a Saksmappe with a missing required field
   */
  @Test
  public void insertSaksmappeMissingRequiredField() {
    JSONObject saksmappeSource = new JSONObject();
    saksmappeSource.put("offentligTittel", "testOffentligTittel");
    saksmappeSource.put("offentligTittelSensitiv", "testOffentligTittelSensitiv");
    // saksmappeSource.put("saksaar", 2020);
    saksmappeSource.put("sakssekvensnummer", 1);
    saksmappeSource.put("saksdato", "2020-01-01");
    saksmappeSource.put("virksomhetIri", "virksomhetIri");

    HttpEntity<String> request = getRequest(saksmappeSource);
    ResponseEntity<SaksmappeJSON> response = this.restTemplate
        .postForEntity("http://localhost:" + port + "/saksmappe", request, SaksmappeJSON.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }


  /**
   * Test that we can insert a Saksmappe with a Journalpost given as a JSON object
   * 
   * @throws Exception
   */
  @Test
  public void insertSaksmappeWithJournalpost() throws Exception {
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
    ResponseEntity<SaksmappeJSON> response = this.restTemplate
        .postForEntity("http://localhost:" + port + "/saksmappe", request, SaksmappeJSON.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    SaksmappeJSON saksmappe = response.getBody();
    assertEquals("testOffentligTittel", saksmappe.getOffentligTittel());
    assertEquals("testOffentligTittelSensitiv", saksmappe.getOffentligTittelSensitiv());
    assertEquals(2020, saksmappe.getSaksaar());
    assertEquals(1, saksmappe.getSakssekvensnummer());
    assertEquals(LocalDate.of(2020, 1, 1), saksmappe.getSaksdato());
    assertNotNull(saksmappe.getId());

    List<ExpandableField<JournalpostJSON>> journalpostList = saksmappe.getJournalpost();
    assertEquals(1, journalpostList.size());
    JournalpostJSON journalpost = journalpostList.get(0).getExpandedObject();
    assertNotNull(journalpost.getId());
    assertEquals("testJournalpost", journalpost.getOffentligTittel());
    assertEquals("inngåendeDokument", journalpost.getJournalposttype());
    assertEquals(2020, journalpost.getJournalaar());
    assertEquals(LocalDate.of(2020, 2, 2), journalpost.getJournaldato());
    assertEquals(1, journalpost.getJournalpostnummer());

  }



}

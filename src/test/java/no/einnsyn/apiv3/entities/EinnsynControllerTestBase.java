package no.einnsyn.apiv3.entities;

import java.util.UUID;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.google.gson.Gson;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.enhet.models.Enhetstype;

public abstract class EinnsynControllerTestBase extends EinnsynTestBase {

  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  protected Gson gson;


  private HttpEntity<String> getRequest(JSONObject requestBody) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<String>(requestBody.toString(), headers);
  }


  protected ResponseEntity<String> get(String endpoint) throws Exception {
    String url = "http://localhost:" + port + endpoint;
    ResponseEntity<String> response =
        restTemplate.exchange(url, HttpMethod.GET, null, String.class);
    return response;
  }


  protected ResponseEntity<String> post(String endpoint, JSONObject json, UUID journalenhetId)
      throws Exception {
    var temp = EinnsynObjectService.TEMPORARY_ADM_ENHET_ID;
    Enhet journalenhet = enhetRepository.findById(journalenhetId).get();
    EinnsynObjectService.TEMPORARY_ADM_ENHET_ID = journalenhet.getId();
    var response = post(endpoint, json);
    EinnsynObjectService.TEMPORARY_ADM_ENHET_ID = temp;
    return response;
  }

  protected ResponseEntity<String> post(String endpoint, JSONObject json) throws Exception {
    if (json == null) {
      json = new JSONObject();
    }
    String url = "http://localhost:" + port + endpoint;
    HttpEntity<String> request = getRequest(json);
    ResponseEntity<String> response =
        restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    return response;
  }


  protected ResponseEntity<String> put(String endpoint, JSONObject json) throws Exception {
    String url = "http://localhost:" + port + endpoint;
    HttpEntity<String> request = getRequest(json);
    ResponseEntity<String> response =
        restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
    return response;
  }


  protected ResponseEntity<String> delete(String endpoint) throws Exception {
    String url = "http://localhost:" + port + endpoint;
    ResponseEntity<String> response =
        restTemplate.exchange(url, HttpMethod.DELETE, null, String.class);
    return response;
  }


  private int enhetCounter = 0;

  protected JSONObject getEnhetJSON() throws Exception {
    JSONObject json = new JSONObject();
    json.put("navn", "testenhet");
    json.put("navnNynorsk", "testenhetNynorsk");
    json.put("navnEngelsk", "testenhetEngelsk");
    json.put("navnSami", "testenhetSami");
    json.put("innsynskravEpost", "innsyn@example.com");
    json.put("kontaktpunktAdresse", "kontaktpunktAdresse");
    json.put("kontaktpunktEpost", "kontaktpunkt@example.com");
    json.put("kontaktpunktTelefon", "kontaktpunktTelefon");
    json.put("orgnummer", String.valueOf(123456789 + ++enhetCounter));
    json.put("enhetskode", "enhetskode");
    json.put("enhetstype", Enhetstype.KOMMUNE);
    json.put("skjult", false);
    json.put("eFormidling", false);
    json.put("visToppnode", false);
    json.put("erTeknisk", false);
    json.put("skalKonvertereId", false);
    json.put("legacyId", UUID.randomUUID());
    json.put("avsluttetDato", "2020-01-01");
    return json;
  }


  protected JSONObject getSaksmappeJSON() throws Exception {
    JSONObject json = new JSONObject();
    json.put("offentligTittel", "testOffentligTittel");
    json.put("offentligTittelSensitiv", "testOffentligTittelSensitiv");
    json.put("saksaar", 2020);
    json.put("sakssekvensnummer", 1);
    json.put("saksdato", "2020-01-01");
    json.put("virksomhetIri", "virksomhetIri");
    return json;
  }


  protected JSONObject getJournalpostJSON() throws Exception {
    JSONObject json = new JSONObject();
    json.put("offentligTittel", "JournalpostOffentligTittel");
    json.put("offentligTittelSensitiv", "JournalpostOffentligTittelSensitiv");
    json.put("journalaar", 2020);
    json.put("journalsekvensnummer", 1);
    json.put("journaldato", "2020-01-01");
    json.put("journalpostnummer", 1);
    json.put("journalposttype", "innkommendeDokument");
    return json;
  }


  protected JSONObject getKorrespondansepartJSON() throws Exception {
    JSONObject json = new JSONObject();
    json.put("korrespondanseparttype", "avsender");
    json.put("korrespondansepartNavn", "navn");
    json.put("korrespondansepartNavnSensitiv", "navnSensitiv");
    return json;
  }


  protected JSONObject getDokumentbeskrivelseJSON() throws Exception {
    JSONObject json = new JSONObject();
    json.put("tilknyttetRegistreringSom", "journalpost");
    return json;
  }


  protected JSONObject getDokumentobjektJSON() throws Exception {
    JSONObject json = new JSONObject();
    json.put("referanseDokumentfil", "https://example.com/dokument.pdf");
    return json;
  }


  protected JSONObject getInnsynskravJSON() throws Exception {
    JSONObject json = new JSONObject();
    json.put("epost", "test@example.com");
    return json;
  }


  protected JSONObject getInnsynskravDelJSON() throws Exception {
    JSONObject json = new JSONObject();
    // We need a real journalpost-iri here
    return json;
  }


  protected JSONObject getBrukerJSON() throws Exception {
    JSONObject json = new JSONObject();
    json.put("email", "test@example.com");
    json.put("password", "abcdABCD1234");
    return json;
  }

}

package no.einnsyn.apiv3.entities;

import com.google.gson.Gson;
import java.util.UUID;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseService;
import no.einnsyn.apiv3.entities.enhet.models.EnhetstypeEnum;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public abstract class EinnsynControllerTestBase extends EinnsynTestBase {

  @LocalServerPort private int port;

  @Autowired protected Gson gson;

  @Autowired private RestTemplate restTemplate;

  private HttpEntity<String> getRequest(JSONObject requestBody, HttpHeaders headers) {
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<String>(requestBody.toString(), headers);
  }

  protected ResponseEntity<String> getWithJWT(String endpoint, String jwt) throws Exception {
    var headers = new HttpHeaders();
    headers.add("Authorization", "Bearer " + jwt);
    return get(endpoint, headers);
  }

  protected ResponseEntity<String> getWithHMAC(String endpoint, String hmac) throws Exception {
    var headers = new HttpHeaders();
    headers.add("Authorization", "HMAC-SHA256 " + hmac);
    return get(endpoint, headers);
  }

  protected ResponseEntity<String> get(String endpoint) throws Exception {
    return get(endpoint, new HttpHeaders());
  }

  protected ResponseEntity<String> get(String endpoint, HttpHeaders headers) throws Exception {
    var url = "http://localhost:" + port + endpoint;
    var requestEntity = new HttpEntity<>(headers);
    var response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
    return response;
  }

  protected ResponseEntity<String> post(String endpoint, JSONObject json, String journalenhetId)
      throws Exception {
    var temp = ArkivBaseService.TEMPORARY_ADM_ENHET_ID;
    var journalenhet = enhetRepository.findById(journalenhetId).orElse(null);
    ArkivBaseService.TEMPORARY_ADM_ENHET_ID = journalenhet.getId();
    var response = post(endpoint, json);
    ArkivBaseService.TEMPORARY_ADM_ENHET_ID = temp;
    return response;
  }

  protected ResponseEntity<String> postWithJWT(String endpoint, JSONObject json, String jwt)
      throws Exception {
    var headers = new HttpHeaders();
    headers.add("Authorization", "Bearer " + jwt);
    return post(endpoint, json, headers);
  }

  protected ResponseEntity<String> postWithHMAC(String endpoint, JSONObject json, String hmac)
      throws Exception {
    var headers = new HttpHeaders();
    headers.add("Authorization", "HMAC-SHA256 " + hmac);
    return post(endpoint, json, headers);
  }

  protected ResponseEntity<String> post(String endpoint, JSONObject json) throws Exception {
    return post(endpoint, json, new HttpHeaders());
  }

  protected ResponseEntity<String> post(String endpoint, JSONObject json, HttpHeaders headers)
      throws Exception {
    if (json == null) {
      json = new JSONObject();
    }
    var url = "http://localhost:" + port + endpoint;
    var request = getRequest(json, headers);
    var response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

    return response;
  }

  protected ResponseEntity<String> putWithJWT(String endpoint, JSONObject json, String jwt)
      throws Exception {
    var headers = new HttpHeaders();
    headers.add("Authorization", "Bearer " + jwt);
    return put(endpoint, json, headers);
  }

  protected ResponseEntity<String> putWithHMAC(String endpont, JSONObject json, String hmac)
      throws Exception {
    var headers = new HttpHeaders();
    headers.add("Authorization", "HMAC-SHA256 " + hmac);
    return put(endpont, json, headers);
  }

  protected ResponseEntity<String> put(String endpoint, JSONObject json) throws Exception {
    if (json == null) {
      json = new JSONObject();
    }
    return put(endpoint, json, new HttpHeaders());
  }

  protected ResponseEntity<String> put(String endpoint, JSONObject json, HttpHeaders headers)
      throws Exception {
    var url = "http://localhost:" + port + endpoint;
    var request = getRequest(json, headers);
    var response = restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
    return response;
  }

  protected ResponseEntity<String> deleteWithJWT(String endpoint, String jwt) throws Exception {
    var headers = new HttpHeaders();
    headers.add("Authorization", "Bearer " + jwt);
    return delete(endpoint, headers);
  }

  protected ResponseEntity<String> deleteWithHMAC(String endpoint, String hmac) throws Exception {
    var headers = new HttpHeaders();
    headers.add("Authorization", "HMAC-SHA256 " + hmac);
    return delete(endpoint, headers);
  }

  protected ResponseEntity<String> delete(String endpoint) throws Exception {
    return delete(endpoint, new HttpHeaders());
  }

  protected ResponseEntity<String> delete(String endpoint, HttpHeaders headers) throws Exception {
    var url = "http://localhost:" + port + endpoint;
    var requestEntity = new HttpEntity<>(headers);
    ResponseEntity<String> response =
        restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, String.class);
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
    json.put("enhetstype", EnhetstypeEnum.KOMMUNE.toString());
    json.put("skjult", false);
    json.put("eFormidling", false);
    json.put("visToppnode", false);
    json.put("erTeknisk", false);
    json.put("skalKonvertereId", false);
    json.put("enhetId", UUID.randomUUID());
    json.put("avsluttetDato", "2020-01-01");
    return json;
  }

  protected JSONObject getTilbakemeldingJSON() throws Exception {
    JSONObject json = new JSONObject();
    json.put("messageFromUser", "Veldig fin side");
    json.put("path", "https://example.com/somewhere");
    json.put("referer", "https://example.com/somewhereelse");
    json.put("userAgent", "Chrome/58.0.3029.110");
    json.put("screenHeight", 100);
    json.put("screenWidth", 100);
    json.put("docHeight", 50);
    json.put("docWidth", 50);
    json.put("winHeight", 20);
    json.put("winWidth", 20);
    json.put("scrollX", 5);
    json.put("scrollY", 5);
    json.put("userSatisfied", true);
    json.put("handledByAdmin", false);
    json.put("adminComment", "Perfect");
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

  protected JSONObject getSkjermingJSON() throws Exception {
    JSONObject json = new JSONObject();
    json.put("skjermingshjemmel", "Offl. § 13");
    json.put("tilgangsrestriksjon", "test");
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
    json.put("tittel", "testTittel");
    json.put("tittelSensitiv", "testTittelSensitiv");
    json.put("dokumentnummer", "1");
    return json;
  }

  protected JSONObject getDokumentobjektJSON() throws Exception {
    JSONObject json = new JSONObject();
    json.put("referanseDokumentfil", "https://example.com/dokument.pdf");
    return json;
  }

  protected JSONObject getInnsynskravJSON() throws Exception {
    JSONObject json = new JSONObject();
    json.put("email", "test@example.com");
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

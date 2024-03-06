package no.einnsyn.apiv3;

import com.google.gson.Gson;
import java.util.List;
import no.einnsyn.apiv3.entities.enhet.models.EnhetstypeEnum;
import org.json.JSONArray;
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

  private HttpHeaders getApiKeyHeaders(String method, String endpoint, String key, String secret)
      throws Exception {
    var headers = new HttpHeaders();
    headers.add("x-ein-api-key", key);
    headers.add("x-ein-api-secret", secret);
    return headers;
  }

  private HttpEntity<String> getRequest(JSONObject requestBody, HttpHeaders headers) {
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<>(requestBody.toString(), headers);
  }

  protected ResponseEntity<String> getWithJWT(String endpoint, String jwt) throws Exception {
    var headers = new HttpHeaders();
    headers.add("Authorization", "Bearer " + jwt);
    return get(endpoint, headers);
  }

  protected ResponseEntity<String> getWithApiKey(String endpoint, String key, String secret)
      throws Exception {
    return get(endpoint, getApiKeyHeaders("GET", endpoint, key, secret));
  }

  protected ResponseEntity<String> get(String endpoint, HttpHeaders headers) throws Exception {
    var url = "http://localhost:" + port + endpoint;
    var requestEntity = new HttpEntity<>(headers);
    var response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
    return response;
  }

  protected ResponseEntity<String> get(String endpoint) throws Exception {
    return getWithApiKey(endpoint, journalenhetKey, journalenhetSecret);
  }

  protected ResponseEntity<String> getAnon(String endpoint) throws Exception {
    return get(endpoint, new HttpHeaders());
  }

  protected ResponseEntity<String> getAdmin(String endpoint) throws Exception {
    return getWithApiKey(endpoint, adminKey, adminSecret);
  }

  protected ResponseEntity<String> postWithJWT(String endpoint, JSONObject json, String jwt)
      throws Exception {
    var headers = new HttpHeaders();
    headers.add("Authorization", "Bearer " + jwt);
    return post(endpoint, json, headers);
  }

  protected ResponseEntity<String> postWithApiKey(
      String endpoint, JSONObject json, String key, String secret) throws Exception {
    return post(endpoint, json, getApiKeyHeaders("POST", endpoint, key, secret));
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

  protected ResponseEntity<String> post(String endpoint, JSONObject json) throws Exception {
    return postWithApiKey(endpoint, json, journalenhetKey, journalenhetSecret);
  }

  protected ResponseEntity<String> postAnon(String endpoint, JSONObject json) throws Exception {
    return post(endpoint, json, new HttpHeaders());
  }

  protected ResponseEntity<String> postAdmin(String endpoint, JSONObject json) throws Exception {
    return postWithApiKey(endpoint, json, adminKey, adminSecret);
  }

  protected ResponseEntity<String> putWithJWT(String endpoint, JSONObject json, String jwt)
      throws Exception {
    var headers = new HttpHeaders();
    headers.add("Authorization", "Bearer " + jwt);
    return put(endpoint, json, headers);
  }

  protected ResponseEntity<String> putWithApiKey(
      String endpont, JSONObject json, String key, String secret) throws Exception {
    return put(endpont, json, getApiKeyHeaders("PUT", endpont, key, secret));
  }

  protected ResponseEntity<String> put(String endpoint, JSONObject json) throws Exception {
    return putWithApiKey(endpoint, json, journalenhetKey, journalenhetSecret);
  }

  protected ResponseEntity<String> putAnon(String endpoint, JSONObject json) throws Exception {
    return put(endpoint, json, new HttpHeaders());
  }

  protected ResponseEntity<String> putAdmin(String endpoint, JSONObject json) throws Exception {
    return putWithApiKey(endpoint, json, adminKey, adminSecret);
  }

  protected ResponseEntity<String> put(String endpoint) throws Exception {
    return put(endpoint, null);
  }

  protected ResponseEntity<String> put(String endpoint, JSONObject json, HttpHeaders headers)
      throws Exception {
    if (json == null) {
      json = new JSONObject();
    }
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

  protected ResponseEntity<String> deleteWithApiKey(String endpoint, String key, String secret)
      throws Exception {
    return delete(endpoint, getApiKeyHeaders("DELETE", endpoint, key, secret));
  }

  protected ResponseEntity<String> delete(String endpoint, HttpHeaders headers) throws Exception {
    var url = "http://localhost:" + port + endpoint;
    var requestEntity = new HttpEntity<>(headers);
    var response = restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, String.class);
    return response;
  }

  protected ResponseEntity<String> delete(String endpoint) throws Exception {
    return deleteWithApiKey(endpoint, journalenhetKey, journalenhetSecret);
  }

  protected ResponseEntity<String> deleteAnon(String endpoint) throws Exception {
    return delete(endpoint, new HttpHeaders());
  }

  protected ResponseEntity<String> deleteAdmin(String endpoint) throws Exception {
    return deleteWithApiKey(endpoint, adminKey, adminSecret);
  }

  private static int enhetCounter = 0;

  protected JSONObject getEnhetJSON() throws Exception {
    var json = new JSONObject();
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
    json.put("skalKonvertereId", false);
    json.put("avsluttetDato", "2020-01-01");
    return json;
  }

  protected JSONObject getTilbakemeldingJSON() throws Exception {
    var json = new JSONObject();
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
    var json = new JSONObject();
    json.put("offentligTittel", "testOffentligTittel");
    json.put("offentligTittelSensitiv", "testOffentligTittelSensitiv");
    json.put("saksaar", 2020);
    json.put("sakssekvensnummer", 1);
    json.put("saksdato", "2020-01-01");
    return json;
  }

  protected JSONObject getJournalpostJSON() throws Exception {
    var json = new JSONObject();
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
    var json = new JSONObject();
    json.put("skjermingshjemmel", "Offl. § 13");
    json.put("tilgangsrestriksjon", "test");
    return json;
  }

  protected JSONObject getKorrespondansepartJSON() throws Exception {
    var json = new JSONObject();
    json.put("korrespondanseparttype", "avsender");
    json.put("korrespondansepartNavn", "navn");
    json.put("korrespondansepartNavnSensitiv", "navnSensitiv");
    return json;
  }

  protected JSONObject getDokumentbeskrivelseJSON() throws Exception {
    var json = new JSONObject();
    json.put("tilknyttetRegistreringSom", "journalpost");
    json.put("tittel", "testTittel");
    json.put("tittelSensitiv", "testTittelSensitiv");
    json.put("dokumentnummer", "1");
    return json;
  }

  protected JSONObject getDokumentobjektJSON() throws Exception {
    var json = new JSONObject();
    json.put("referanseDokumentfil", "https://example.com/dokument.pdf");
    return json;
  }

  protected JSONObject getInnsynskravJSON() throws Exception {
    var json = new JSONObject();
    json.put("email", "test@example.com");
    return json;
  }

  protected JSONObject getInnsynskravDelJSON() throws Exception {
    var json = new JSONObject();
    // We need a real journalpost-iri here
    return json;
  }

  protected JSONObject getBrukerJSON() throws Exception {
    var json = new JSONObject();
    json.put("email", "test@example.com");
    json.put("password", "abcdABCD1234");
    return json;
  }

  protected JSONObject getArkivJSON() throws Exception {
    var json = new JSONObject();
    json.put("tittel", "testTittel");
    return json;
  }

  protected JSONObject getArkivdelJSON() throws Exception {
    var json = new JSONObject();
    json.put("tittel", "testTittel");
    return json;
  }

  protected JSONObject getKlasseJSON() throws Exception {
    var json = new JSONObject();
    json.put("tittel", "testTittel");
    return json;
  }

  protected JSONObject getKlassifikasjonssystemJSON() throws Exception {
    var json = new JSONObject();
    json.put("tittel", "testTittel");
    return json;
  }

  protected JSONObject getApiKeyJSON() throws Exception {
    var json = new JSONObject();
    json.put("name", "ApiKeyName");
    return json;
  }

  private Integer moetenummerIterator = 1;

  protected JSONObject getMoetemappeJSON() throws Exception {
    var json = new JSONObject();
    json.put("offentligTittel", "Møtemappe, offentlig tittel");
    json.put("offentligTittelSensitiv", "Møtemappe, offentlig tittel sensitiv");
    json.put("moetenummer", (moetenummerIterator++).toString());
    json.put("utvalg", "utvalg");
    json.put("moetedato", "2020-01-01T00:00:00Z");
    json.put("moetested", "moetested");
    json.put("videoLink", "https://example.com/video");
    json.put(
        "moetedokument",
        new JSONArray(
            List.of(getMoetedokumentJSON(), getMoetedokumentJSON(), getMoetedokumentJSON())));
    json.put(
        "moetesak",
        new JSONArray(List.of(getMoetesakJSON()))); // , getMoetesakJSON(), getMoetesakJSON())));
    return json;
  }

  protected JSONObject getMoetedokumentJSON() throws Exception {
    var json = new JSONObject();
    json.put("offentligTittel", "Møtedokument, offentlig tittel");
    json.put("offentligTittelSensitiv", "Møtedokument, offentlig tittel sensitiv");
    json.put("beskrivelse", "beskrivelse");
    json.put("moetedokumenttype", "saksliste");
    json.put(
        "korrespondansepart",
        new JSONArray(List.of(getKorrespondansepartJSON(), getKorrespondansepartJSON())));
    json.put("dokumentbeskrivelse", new JSONArray(List.of(getDokumentbeskrivelseJSON())));
    return json;
  }

  protected JSONObject getMoetesakJSON() throws Exception {
    var json = new JSONObject();
    json.put("offentligTittel", "Møtesak, offentlig tittel");
    json.put("offentligTittelSensitiv", "Møtesak, offentlig tittel sensitiv");
    json.put("moetesakstype", "type");
    json.put("moetesaksaar", 2020);
    json.put("moetesakssekvensnummer", 1);
    json.put("utvalg", "enhet");
    json.put("videoLink", "https://example.com/video");
    json.put("utredning", getUtredningJSON());
    json.put("vedtak", getVedtakJSON());
    json.put("innstilling", getMoetesaksbeskrivelseJSON());
    return json;
  }

  protected JSONObject getUtredningJSON() throws Exception {
    var json = new JSONObject();
    var saksbeskrivelse = getMoetesaksbeskrivelseJSON();
    var innstilling = getMoetesaksbeskrivelseJSON();
    var utredningsdok =
        new JSONArray(List.of(getDokumentbeskrivelseJSON(), getDokumentbeskrivelseJSON()));
    json.put("saksbeskrivelse", saksbeskrivelse);
    json.put("innstilling", innstilling);
    json.put("utredningsdokument", utredningsdok);
    return json;
  }

  protected JSONObject getMoetesaksbeskrivelseJSON() throws Exception {
    var json = new JSONObject();
    json.put("tekstInnhold", "tekstInnhold");
    json.put("tekstFormat", "tekstFormat");
    return json;
  }

  protected JSONObject getVedtakJSON() throws Exception {
    var json = new JSONObject();
    json.put("dato", "2020-01-01");
    json.put("vedtakstekst", getMoetesaksbeskrivelseJSON());
    json.put("behandlingsprotokoll", getBehandlingsprotokollJSON());
    json.put(
        "votering",
        new JSONArray(List.of(getVoteringJSON(), getVoteringJSON(), getVoteringJSON())));
    json.put(
        "vedtaksdokument",
        new JSONArray(List.of(getDokumentbeskrivelseJSON(), getDokumentbeskrivelseJSON())));
    return json;
  }

  protected JSONObject getBehandlingsprotokollJSON() throws Exception {
    var json = new JSONObject();
    json.put("tekstInnhold", "tekstInnhold");
    json.put("tekstFormat", "tekstFormat");
    return json;
  }

  protected JSONObject getMoetedeltakerJSON() throws Exception {
    var json = new JSONObject();
    json.put("moetedeltakerNavn", "navn");
    json.put("moetedeltakerFunksjon", "funksjon");
    return json;
  }

  protected JSONObject getIdentifikatorJSON() throws Exception {
    var json = new JSONObject();
    json.put("navn", "navn");
    json.put("identifikator", "identifikator");
    json.put("initialer", "initialer");
    json.put("epostadresse", "epostadresse");
    return json;
  }

  int stemmeCounter = 0;

  protected JSONObject getVoteringJSON() throws Exception {
    var json = new JSONObject();
    json.put("moetedeltaker", getMoetedeltakerJSON());
    json.put("representerer", getIdentifikatorJSON());
    var mod = ++stemmeCounter % 3;
    json.put("stemme", mod == 0 ? "Ja" : mod == 1 ? "Nei" : "Blankt");
    return json;
  }
}

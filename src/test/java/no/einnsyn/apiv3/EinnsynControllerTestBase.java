package no.einnsyn.apiv3;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import java.lang.reflect.Type;
import java.util.List;
import no.einnsyn.apiv3.common.hasid.HasId;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.enhet.models.EnhetstypeEnum;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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

  protected ResponseEntity<String> get(String endpoint, HttpHeaders headers) throws Exception {
    var url = "http://localhost:" + port + endpoint;
    var requestEntity = new HttpEntity<>(headers);
    var response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
    return response;
  }

  protected ResponseEntity<String> get(String endpoint) throws Exception {
    return get(endpoint, journalenhetKey, journalenhetSecret);
  }

  protected ResponseEntity<String> getAnon(String endpoint) throws Exception {
    return get(endpoint, new HttpHeaders());
  }

  protected ResponseEntity<String> getAdmin(String endpoint) throws Exception {
    return get(endpoint, adminKey, adminSecret);
  }

  protected ResponseEntity<String> get(String endpoint, String apiKeyOrJWT) throws Exception {
    return get(endpoint, apiKeyOrJWT, null);
  }

  protected ResponseEntity<String> get(String endpoint, String apiKeyOrJWT, String apiSecret)
      throws Exception {
    if (apiKeyOrJWT == null) {
      return get(endpoint);
    }
    if (apiSecret == null) {
      var headers = new HttpHeaders();
      headers.add("Authorization", "Bearer " + apiKeyOrJWT);
      return get(endpoint, headers);
    }
    return get(endpoint, getApiKeyHeaders("GET", endpoint, apiKeyOrJWT, apiSecret));
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
    return post(endpoint, json, journalenhetKey, journalenhetSecret);
  }

  protected ResponseEntity<String> postAnon(String endpoint, JSONObject json) throws Exception {
    return post(endpoint, json, new HttpHeaders());
  }

  protected ResponseEntity<String> postAdmin(String endpoint, JSONObject json) throws Exception {
    return post(endpoint, json, adminKey, adminSecret);
  }

  protected ResponseEntity<String> post(String endpoint, JSONObject json, String apiKeyOrJWT)
      throws Exception {
    return post(endpoint, json, apiKeyOrJWT, null);
  }

  protected ResponseEntity<String> post(
      String endpoint, JSONObject json, String apiKeyOrJWT, String apiSecret) throws Exception {
    if (apiKeyOrJWT == null) {
      return post(endpoint, json);
    }
    if (apiSecret == null) {
      var headers = new HttpHeaders();
      headers.add("Authorization", "Bearer " + apiKeyOrJWT);
      return post(endpoint, json, headers);
    }
    return post(endpoint, json, getApiKeyHeaders("POST", endpoint, apiKeyOrJWT, apiSecret));
  }

  protected ResponseEntity<String> put(String endpoint, JSONObject json) throws Exception {
    return put(endpoint, json, journalenhetKey, journalenhetSecret);
  }

  protected ResponseEntity<String> putAnon(String endpoint, JSONObject json) throws Exception {
    return put(endpoint, json, new HttpHeaders());
  }

  protected ResponseEntity<String> putAdmin(String endpoint, JSONObject json) throws Exception {
    return put(endpoint, json, adminKey, adminSecret);
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

  protected ResponseEntity<String> put(String endpoint, JSONObject json, String apiKeyOrJWT)
      throws Exception {
    return put(endpoint, json, apiKeyOrJWT, null);
  }

  protected ResponseEntity<String> put(
      String endpoint, JSONObject json, String apiKeyOrJWT, String apiSecret) throws Exception {
    if (apiKeyOrJWT == null) {
      return put(endpoint, json);
    }
    if (apiSecret == null) {
      var headers = new HttpHeaders();
      headers.add("Authorization", "Bearer " + apiKeyOrJWT);
      return put(endpoint, json, headers);
    }
    return put(endpoint, json, getApiKeyHeaders("PUT", endpoint, apiKeyOrJWT, apiSecret));
  }

  protected ResponseEntity<String> delete(String endpoint, HttpHeaders headers) throws Exception {
    var url = "http://localhost:" + port + endpoint;
    var requestEntity = new HttpEntity<>(headers);
    var response = restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, String.class);
    return response;
  }

  protected ResponseEntity<String> delete(String endpoint) throws Exception {
    return delete(endpoint, journalenhetKey, journalenhetSecret);
  }

  protected ResponseEntity<String> deleteAnon(String endpoint) throws Exception {
    return delete(endpoint, new HttpHeaders());
  }

  protected ResponseEntity<String> deleteAdmin(String endpoint) throws Exception {
    return delete(endpoint, adminKey, adminSecret);
  }

  protected ResponseEntity<String> delete(String endpoint, String apiKeyOrJWT) throws Exception {
    return delete(endpoint, apiKeyOrJWT, null);
  }

  protected ResponseEntity<String> delete(String endpoint, String apiKeyOrJWT, String apiSecret)
      throws Exception {
    if (apiKeyOrJWT == null) {
      return delete(endpoint);
    }
    if (apiSecret == null) {
      var headers = new HttpHeaders();
      headers.add("Authorization", "Bearer " + apiKeyOrJWT);
      return delete(endpoint, headers);
    }
    return delete(endpoint, getApiKeyHeaders("DELETE", endpoint, apiKeyOrJWT, apiSecret));
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

  private static int brukerCounter = 0;

  protected JSONObject getBrukerJSON() throws Exception {
    var json = new JSONObject();
    json.put("email", "test" + brukerCounter++ + "@example.com");
    json.put("password", "abcdABCD1234");
    return json;
  }

  protected JSONObject getLoginJSON(JSONObject brukerJSON) throws Exception {
    var json = new JSONObject();
    json.put("username", brukerJSON.get("email"));
    json.put("password", brukerJSON.get("password"));
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

  /**
   * Generic helper function to test ASC / DESC list endpoints with startingAfter / endingBefore
   *
   * @param <T>
   * @param actualItems
   * @param endpoint
   * @param apiKeyOrJWT
   * @param apiSecret
   * @throws Exception
   */
  protected <T extends HasId> void testGenericList(
      Type resultListType,
      List<T> actualItems,
      String endpoint,
      String apiKeyOrJWT,
      String apiSecret)
      throws Exception {

    var pivotNo = (int) Math.floor(actualItems.size() / 2);
    var pivotNoCeil = (int) Math.ceil((float) actualItems.size() / 2);
    var pivotId = actualItems.get(pivotNo).getId();
    var itemsAfterPivot = actualItems.size() - pivotNo - 1;
    var itemsBeforePivot = pivotNo;
    var fullSize = actualItems.size();

    // DESC
    var response = get(endpoint, apiKeyOrJWT, apiSecret);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    ResultList<T> resultListDTO = gson.fromJson(response.getBody(), resultListType);
    var items = resultListDTO.getItems();
    assertEquals(fullSize, items.size());
    for (var i = 0; i < fullSize; i++) {
      var invertI = fullSize - 1 - i; // DESC
      var actualItemId = actualItems.get(i).getId();
      var gotItemId = items.get(invertI).getId();
      assertEquals(actualItemId, gotItemId, "ResultList[" + i + "] is not correct");
    }

    // DESC startingAfter
    response = get(endpoint + "?sortOrder=desc&startingAfter=" + pivotId, apiKeyOrJWT, apiSecret);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    resultListDTO = gson.fromJson(response.getBody(), resultListType);
    items = resultListDTO.getItems();
    assertEquals(itemsBeforePivot, items.size());
    for (var i = 0; i < fullSize; i++) {
      if (i < pivotNo) {
        var invertI = fullSize - 1 - i - pivotNoCeil;
        assertEquals(actualItems.get(i).getId(), items.get(invertI).getId());
      }
    }

    // DESC endingBefore
    response = get(endpoint + "?sortOrder=desc&endingBefore=" + pivotId, apiKeyOrJWT, apiSecret);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    resultListDTO = gson.fromJson(response.getBody(), resultListType);
    items = resultListDTO.getItems();
    assertEquals(itemsAfterPivot, items.size());
    for (var i = 0; i < fullSize; i++) {
      if (i > pivotNo) {
        var invertI = fullSize - 1 - i;
        assertEquals(actualItems.get(i).getId(), items.get(invertI).getId());
      }
    }

    // ASC
    response = get(endpoint + "?sortOrder=asc", apiKeyOrJWT, apiSecret);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    resultListDTO = gson.fromJson(response.getBody(), resultListType);
    items = resultListDTO.getItems();
    assertEquals(fullSize, items.size());
    for (var i = 0; i < fullSize; i++) {
      assertEquals(actualItems.get(i).getId(), items.get(i).getId());
    }

    // ASC startingAfter
    response = get(endpoint + "?sortOrder=asc&startingAfter=" + pivotId, apiKeyOrJWT, apiSecret);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    resultListDTO = gson.fromJson(response.getBody(), resultListType);
    items = resultListDTO.getItems();
    assertEquals(itemsAfterPivot, items.size());
    for (var i = 0; i < fullSize; i++) {
      if (i > pivotNo) {
        assertEquals(actualItems.get(i).getId(), items.get(i - pivotNo - 1).getId());
      }
    }

    // ASC endingBefore
    response = get(endpoint + "?sortOrder=asc&endingBefore=" + pivotId, apiKeyOrJWT, apiSecret);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    resultListDTO = gson.fromJson(response.getBody(), resultListType);
    items = resultListDTO.getItems();
    assertEquals(itemsBeforePivot, items.size());
    for (var i = 0; i < fullSize; i++) {
      if (i < pivotNo) {
        assertEquals(actualItems.get(i).getId(), items.get(i).getId());
      }
    }
  }
}

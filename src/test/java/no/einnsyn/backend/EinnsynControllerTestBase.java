package no.einnsyn.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;
import no.einnsyn.backend.common.hasid.HasId;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.clients.ip.IPSender;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;

public abstract class EinnsynControllerTestBase extends EinnsynTestBase {

  @LocalServerPort private int port;

  @Autowired
  @Qualifier("gsonPrettyAllowUnknown")
  protected Gson gson;

  @Autowired private RestTemplate restTemplate;

  @MockitoBean protected IPSender ipSender;

  protected HttpHeaders getAuthHeaders(String key) {
    var headers = new HttpHeaders();
    if (key == null) {
      // Noop
    } else if (key.startsWith("secret_")) {
      headers.add("API-KEY", key);
    } else {
      headers.add("Authorization", "Bearer " + key);
    }
    return headers;
  }

  private HttpEntity<String> getRequest(JSONObject requestBody, HttpHeaders headers) {
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<>(requestBody.toString(), headers);
  }

  // GET

  protected ResponseEntity<String> get(String endpoint, HttpHeaders headers) throws Exception {
    var url = "http://localhost:" + port + endpoint;
    var requestEntity = new HttpEntity<>(headers);
    var response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
    awaitSideEffects();
    return response;
  }

  protected ResponseEntity<String> get(String endpoint) throws Exception {
    return get(endpoint, journalenhetKey);
  }

  protected ResponseEntity<String> getAnon(String endpoint) throws Exception {
    return get(endpoint, new HttpHeaders());
  }

  protected ResponseEntity<String> getAdmin(String endpoint) throws Exception {
    return get(endpoint, adminKey);
  }

  protected ResponseEntity<String> get(String endpoint, String apiKeyOrJWT) throws Exception {
    return get(endpoint, getAuthHeaders(apiKeyOrJWT));
  }

  // POST JSON

  protected ResponseEntity<String> post(String endpoint, JSONObject json, HttpHeaders headers)
      throws Exception {
    if (json == null) {
      json = new JSONObject();
    }
    var url = "http://localhost:" + port + endpoint;
    var request = getRequest(json, headers);
    var response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    awaitSideEffects();

    return response;
  }

  protected ResponseEntity<String> post(String endpoint, JSONObject json) throws Exception {
    return post(endpoint, json, journalenhetKey);
  }

  protected ResponseEntity<String> postAnon(String endpoint, JSONObject json) throws Exception {
    return post(endpoint, json, new HttpHeaders());
  }

  protected ResponseEntity<String> postAdmin(String endpoint, JSONObject json) throws Exception {
    return post(endpoint, json, adminKey);
  }

  protected ResponseEntity<String> post(String endpoint, JSONObject json, String apiKeyOrJWT)
      throws Exception {
    return post(endpoint, json, getAuthHeaders(apiKeyOrJWT));
  }

  // POST NON-JSON

  protected ResponseEntity<String> post(String endpoint, String body, HttpHeaders headers)
      throws Exception {
    var url = "http://localhost:" + port + endpoint;
    headers.setContentType(MediaType.APPLICATION_JSON);
    var request = new HttpEntity<>("\"" + body + "\"", headers);
    var response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    return response;
  }

  protected ResponseEntity<String> post(String endpoint, String body) throws Exception {
    return post(endpoint, body, journalenhetKey);
  }

  protected ResponseEntity<String> postAnon(String endpoint, String body) throws Exception {
    return post(endpoint, body, new HttpHeaders());
  }

  protected ResponseEntity<String> postAdmin(String endpoint, String body) throws Exception {
    return post(endpoint, body, adminKey);
  }

  protected ResponseEntity<String> post(String endpoint, String body, String apiKeyOrJWT)
      throws Exception {
    if (apiKeyOrJWT == null) {
      return post(endpoint, body);
    }
    return post(endpoint, body, getAuthHeaders(apiKeyOrJWT));
  }

  // PATCH

  protected ResponseEntity<String> patch(String endpoint, JSONObject json) throws Exception {
    return patch(endpoint, json, journalenhetKey);
  }

  protected ResponseEntity<String> patchAnon(String endpoint, JSONObject json) throws Exception {
    return patch(endpoint, json, new HttpHeaders());
  }

  protected ResponseEntity<String> patchAdmin(String endpoint, JSONObject json) throws Exception {
    return patch(endpoint, json, adminKey);
  }

  protected ResponseEntity<String> patch(String endpoint) throws Exception {
    return patch(endpoint, null);
  }

  protected ResponseEntity<String> patch(String endpoint, JSONObject json, HttpHeaders headers)
      throws Exception {
    if (json == null) {
      json = new JSONObject();
    }
    var url = "http://localhost:" + port + endpoint;
    var request = getRequest(json, headers);
    var response = restTemplate.exchange(url, HttpMethod.PATCH, request, String.class);
    awaitSideEffects();
    return response;
  }

  protected ResponseEntity<String> patch(String endpoint, JSONObject json, String apiKeyOrJWT)
      throws Exception {
    return patch(endpoint, json, getAuthHeaders(apiKeyOrJWT));
  }

  // DELETE

  protected ResponseEntity<String> delete(String endpoint, HttpHeaders headers) throws Exception {
    var url = "http://localhost:" + port + endpoint;
    var requestEntity = new HttpEntity<>(headers);
    var response = restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, String.class);
    awaitSideEffects();
    return response;
  }

  protected ResponseEntity<String> delete(String endpoint) throws Exception {
    return delete(endpoint, journalenhetKey);
  }

  protected ResponseEntity<String> deleteAnon(String endpoint) throws Exception {
    return delete(endpoint, new HttpHeaders());
  }

  protected ResponseEntity<String> deleteAdmin(String endpoint) throws Exception {
    return delete(endpoint, adminKey);
  }

  protected ResponseEntity<String> delete(String endpoint, String apiKeyOrJWT) throws Exception {
    return delete(endpoint, getAuthHeaders(apiKeyOrJWT));
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
    json.put("enhetstype", EnhetDTO.EnhetstypeEnum.KOMMUNE.toString());
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
    json.put("journalposttype", "inngaaende_dokument");
    return json;
  }

  protected JSONObject getJournalpostAccessibleInFutureJSON() throws Exception {
    var json = new JSONObject();
    json.put("offentligTittel", "JournalpostOffentligTittel not yet accessible");
    json.put("offentligTittelSensitiv", "JournalpostOffentligTittelSensitiv not yet accessible");
    json.put("journalaar", 2020);
    json.put("journalsekvensnummer", 1);
    json.put("journaldato", "2020-01-01");
    json.put("journalpostnummer", 1);
    json.put("journalposttype", "inngaaende_dokument");
    json.put("accessibleAfter", LocalDateTime.now().plusDays(2));
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

  protected JSONObject getInnsynskravBestillingJSON() throws Exception {
    var json = new JSONObject();
    json.put("email", "test@example.com");
    return json;
  }

  protected JSONObject getInnsynskravJSON() {
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
    json.put("moetesak", new JSONArray(List.of(getMoetesakJSON())));
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
    json.put("moetesakstype", "moete");
    json.put("moetesaksaar", 2020);
    json.put("moetesakssekvensnummer", 1);
    json.put("utvalg", "enhet");
    json.put("videoLink", "https://example.com/video");
    json.put("utredning", getUtredningJSON());
    json.put("vedtak", getVedtakJSON());
    json.put("innstilling", getMoetesaksbeskrivelseJSON());
    json.put("legacyReferanseTilMoetesak", "http://example.com/referanseTilMoetesak");
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
    json.put("epostadresse", "epostadresse@example.com");
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

  protected JSONObject getLagretSakJSON() throws Exception {
    var json = new JSONObject();
    json.put("subscribe", true);
    return json;
  }

  protected JSONObject getLagretSoekJSON() throws Exception {
    var json = new JSONObject();
    json.put("label", "testLabel");
    json.put("legacyQuery", "?f=foo");
    json.put("subscribe", true);

    var searchParameters = new JSONObject();
    searchParameters.put("limit", 10);
    searchParameters.put("sortOrder", "asc");
    searchParameters.put("sortBy", "id");
    searchParameters.put("query", "foo");
    json.put("searchParameters", searchParameters);
    return json;
  }

  protected PaginatedList<JournalpostDTO> getJournalpostList(String saksmappeId, String... expand)
      throws Exception {
    var queryString = "?expand=" + String.join(",", expand);
    var response = get("/saksmappe/" + saksmappeId + "/journalpost" + queryString);
    var resultListType = new TypeToken<PaginatedList<JournalpostDTO>>() {}.getType();
    return gson.fromJson(response.getBody(), resultListType);
  }

  protected PaginatedList<JournalpostDTO> getJournalpostListAsAdmin(
      String saksmappeId, String... expand) throws Exception {
    var queryString = "?expand=" + String.join(",", expand);
    var response = getAdmin("/saksmappe/" + saksmappeId + "/journalpost" + queryString);
    var resultListType = new TypeToken<PaginatedList<JournalpostDTO>>() {}.getType();
    return gson.fromJson(response.getBody(), resultListType);
  }

  protected PaginatedList<JournalpostDTO> getJournalpostListAsAnon(
      String saksmappeId, String... expand) throws Exception {
    var queryString = "?expand=" + String.join(",", expand);
    var response = getAnon("/saksmappe/" + saksmappeId + "/journalpost" + queryString);
    var resultListType = new TypeToken<PaginatedList<JournalpostDTO>>() {}.getType();
    return gson.fromJson(response.getBody(), resultListType);
  }

  /**
   * Generic helper function to test ASC / DESC list endpoints with startingAfter / endingBefore
   *
   * @param <T>
   * @param actualItems
   * @param endpoint
   * @param apiKeyOrJWT
   * @throws Exception
   */
  protected <T extends HasId> void testGenericList(
      Type resultListType, List<T> actualItems, String endpoint, String apiKeyOrJWT)
      throws Exception {

    var pivotNo = (int) Math.floor(actualItems.size() / 2);
    var pivotNoCeil = (int) Math.ceil((float) actualItems.size() / 2);
    var pivotId = actualItems.get(pivotNo).getId();
    var itemsAfterPivot = actualItems.size() - pivotNo - 1;
    var itemsBeforePivot = pivotNo;
    var fullSize = actualItems.size();

    // DESC
    var response = get(endpoint, apiKeyOrJWT);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<T> resultListDTO = gson.fromJson(response.getBody(), resultListType);
    var items = resultListDTO.getItems();
    assertEquals(fullSize, items.size());
    for (var i = 0; i < fullSize; i++) {
      var invertI = fullSize - 1 - i; // DESC
      var actualItemId = actualItems.get(i).getId();
      var gotItemId = items.get(invertI).getId();
      assertEquals(actualItemId, gotItemId, "ResultList[" + i + "] is not correct");
    }

    // DESC startingAfter
    response = get(endpoint + "?sortOrder=desc&startingAfter=" + pivotId, apiKeyOrJWT);
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
    response = get(endpoint + "?sortOrder=desc&endingBefore=" + pivotId, apiKeyOrJWT);
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
    response = get(endpoint + "?sortOrder=asc", apiKeyOrJWT);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    resultListDTO = gson.fromJson(response.getBody(), resultListType);
    items = resultListDTO.getItems();
    assertEquals(fullSize, items.size());
    for (var i = 0; i < fullSize; i++) {
      assertEquals(actualItems.get(i).getId(), items.get(i).getId());
    }

    // ASC startingAfter
    response = get(endpoint + "?sortOrder=asc&startingAfter=" + pivotId, apiKeyOrJWT);
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
    response = get(endpoint + "?sortOrder=asc&endingBefore=" + pivotId, apiKeyOrJWT);
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

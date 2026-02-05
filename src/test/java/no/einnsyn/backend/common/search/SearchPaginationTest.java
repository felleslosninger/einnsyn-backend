package no.einnsyn.backend.common.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.stream.Stream;
import net.minidev.json.JSONArray;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.backend.utils.TimeConverter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SearchPaginationTest extends EinnsynControllerTestBase {

  ArkivDTO arkivDTO;
  ArkivdelDTO arkivdelDTO;

  SaksmappeDTO saksmappeAlphaDTO;
  SaksmappeDTO saksmappeBetaDTO;

  JournalpostDTO journalpostAlphaDTO;
  JournalpostDTO journalpostBetaDTO;
  JournalpostDTO journalpostGammaDTO;

  MoetemappeDTO moetemappeDeltaDTO;
  MoetemappeDTO moetemappeEpsilonDTO;

  Type jptype = new TypeToken<PaginatedList<BaseDTO>>() {}.getType();

  @BeforeAll
  void setup() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    // Create Saksmappe "Alpha" with sakssekvensnummer=1
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("offentligTittel", "Alpha Saksmappe");
    saksmappeJSON.put("offentligTittelSensitiv", "Alpha Saksmappe sensitiv");
    saksmappeJSON.put("saksaar", "2023");
    saksmappeJSON.put("sakssekvensnummer", "1");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappeAlphaDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Create Saksmappe "Beta" with sakssekvensnummer=2
    saksmappeJSON.put("offentligTittel", "Beta Saksmappe");
    saksmappeJSON.put("offentligTittelSensitiv", "Beta Saksmappe sensitiv");
    saksmappeJSON.put("saksaar", "2024");
    saksmappeJSON.put("sakssekvensnummer", "2");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    saksmappeBetaDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Create Journalpost "Alpha" with distinct field values
    var korrespondansepartAlpha = getKorrespondansepartJSON();
    korrespondansepartAlpha.put("korrespondansepartNavn", "Arne Avsender");
    korrespondansepartAlpha.put("korrespondansepartNavnSensitiv", "Arne Avsender sensitiv");

    var journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("offentligTittel", "Alpha Journalpost");
    journalpostJSON.put("offentligTittelSensitiv", "Alpha Journalpost sensitiv");
    journalpostJSON.put("journalsekvensnummer", "1");
    journalpostJSON.put("journalpostnummer", 101);
    journalpostJSON.put("journalposttype", "inngaaende_dokument");
    journalpostJSON.put("journaldato", "2023-01-15");
    journalpostJSON.put("dokumentetsDato", "2023-01-10");
    journalpostJSON.put(
        "korrespondansepart", new JSONArray().appendElement(korrespondansepartAlpha));
    response = post("/saksmappe/" + saksmappeAlphaDTO.getId() + "/journalpost", journalpostJSON);
    journalpostAlphaDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Create Journalpost "Beta" with distinct field values
    var korrespondansepartBeta = getKorrespondansepartJSON();
    korrespondansepartBeta.put("korrespondansepartNavn", "Berit Bruker");
    korrespondansepartBeta.put("korrespondansepartNavnSensitiv", "Berit Bruker sensitiv");

    journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("offentligTittel", "Beta Journalpost");
    journalpostJSON.put("offentligTittelSensitiv", "Beta Journalpost sensitiv");
    journalpostJSON.put("administrativEnhetObjekt", journalenhet2Id);
    journalpostJSON.put("journalsekvensnummer", "2");
    journalpostJSON.put("journalpostnummer", 102);
    journalpostJSON.put("journalposttype", "utgaaende_dokument");
    journalpostJSON.put("journaldato", "2023-02-20");
    journalpostJSON.put("dokumentetsDato", "2023-02-15");
    journalpostJSON.put(
        "korrespondansepart", new JSONArray().appendElement(korrespondansepartBeta));
    response =
        post(
            "/saksmappe/" + saksmappeAlphaDTO.getId() + "/journalpost",
            journalpostJSON,
            journalenhet2Key);
    journalpostBetaDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Create Journalpost "Gamma" with distinct field values
    var korrespondansepartGamma = getKorrespondansepartJSON();
    korrespondansepartGamma.put("korrespondansepartNavn", "Carl Contact");
    korrespondansepartGamma.put("korrespondansepartNavnSensitiv", "Carl Contact sensitiv");

    journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("offentligTittel", "Gamma Journalpost");
    journalpostJSON.put("offentligTittelSensitiv", "Gamma Journalpost sensitiv");
    journalpostJSON.put("administrativEnhetObjekt", underenhetId);
    journalpostJSON.put("journalsekvensnummer", "3");
    journalpostJSON.put("journalpostnummer", 103);
    journalpostJSON.put("journalposttype", "organinternt_dokument_uten_oppfoelging");
    journalpostJSON.put("journaldato", "2023-03-25");
    journalpostJSON.put("dokumentetsDato", "2023-03-20");
    journalpostJSON.put(
        "korrespondansepart", new JSONArray().appendElement(korrespondansepartGamma));
    response = post("/saksmappe/" + saksmappeBetaDTO.getId() + "/journalpost", journalpostJSON);
    journalpostGammaDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Create Moetemappe "Delta" with moetedato for entity diversity
    var moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.remove("moetesak");
    moetemappeJSON.put("offentligTittel", "Delta Moetemappe");
    moetemappeJSON.put("offentligTittelSensitiv", "Delta Moetemappe sensitiv");
    moetemappeJSON.put("moetedato", "2023-04-01T10:00:00Z");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    moetemappeDeltaDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    // Create Moetemappe "Epsilon" with different moetedato
    moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.remove("moetesak");
    moetemappeJSON.put("offentligTittel", "Epsilon Moetemappe");
    moetemappeJSON.put("offentligTittelSensitiv", "Epsilon Moetemappe sensitiv");
    moetemappeJSON.put("moetedato", "2023-05-15T14:00:00Z");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    moetemappeEpsilonDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    // Refresh indices
    esClient.indices().refresh(r -> r.index(elasticsearchIndex));
  }

  @AfterAll
  void teardown() throws Exception {
    deleteAdmin("/arkiv/" + arkivDTO.getId());
  }

  @Test
  void testLimit() throws Exception {
    // Get total count first
    var response = get("/search?limit=100");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> allResults = gson.fromJson(response.getBody(), jptype);
    var totalCount = allResults.getItems().size();
    assertEquals(7, totalCount, "Expected 7 items (2 Saksmappe + 3 Journalpost + 2 Moetemappe)");

    // Test limit=1 returns exactly 1 item with next page
    response = get("/search?limit=1");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    assertNull(searchResult.getPrevious());

    // Test limit=2 returns exactly 2 items with next page
    response = get("/search?limit=2");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(2, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    assertNull(searchResult.getPrevious());

    // Test limit greater than total returns all items without next page
    response = get("/search?limit=" + (totalCount + 1));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(totalCount, searchResult.getItems().size());
    assertNull(searchResult.getNext());
    assertNull(searchResult.getPrevious());
  }

  @Test
  void testIdPagination() throws Exception {
    // Navigate forward through pages, collecting IDs
    var ids = new ArrayList<String>();

    var response = get("/search?limit=1&sortBy=id");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), jptype);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    assertNull(searchResult.getPrevious());
    ids.add(searchResult.getItems().getFirst().getId());

    // Navigate forward until we reach the last page
    while (searchResult.getNext() != null) {
      response = get(searchResult.getNext());
      assertEquals(HttpStatus.OK, response.getStatusCode());
      searchResult = gson.fromJson(response.getBody(), jptype);
      assertNotNull(searchResult);
      assertEquals(1, searchResult.getItems().size());
      assertNotNull(searchResult.getPrevious());
      ids.add(searchResult.getItems().getFirst().getId());
    }

    // Verify we collected multiple IDs
    assertEquals(7, ids.size(), "Expected 7 items");

    // Navigate backward through all pages
    var reverseIndex =
        ids.size() - 2; // searchResult is currently at last item, so start from second last
    while (searchResult.getPrevious() != null) {
      response = get(searchResult.getPrevious());
      assertEquals(HttpStatus.OK, response.getStatusCode());
      searchResult = gson.fromJson(response.getBody(), jptype);
      assertNotNull(searchResult);
      assertEquals(1, searchResult.getItems().size());
      assertEquals(ids.get(reverseIndex), searchResult.getItems().getFirst().getId());
      reverseIndex--;
    }

    // Verify we're back at the first page
    assertEquals(ids.getFirst(), searchResult.getItems().getFirst().getId());
    assertNull(searchResult.getPrevious());
  }

  enum SortType {
    STRING,
    NUMBER,
    DATE,
    DATETIME
  }

  /**
   * Provides test parameters for sortBy pagination tests. Each argument contains:
   *
   * <ul>
   *   <li>sortBy: the field to sort by
   *   <li>jsonField: the JSON field name to extract for comparison (null to skip verification)
   *   <li>sortType: the type of comparison to use (null to skip verification)
   * </ul>
   */
  static Stream<Arguments> sortByParameters() {
    return Stream.of(
        // sortBy, jsonField, sortType
        Arguments.of("score", null, null), // score not in response
        Arguments.of("id", "id", SortType.STRING),
        Arguments.of("entity", "entity", SortType.STRING),
        Arguments.of("publisertDato", "publisertDato", SortType.DATETIME),
        Arguments.of("oppdatertDato", "oppdatertDato", SortType.DATETIME),
        Arguments.of("moetedato", "moetedato", SortType.DATETIME),
        Arguments.of("journalposttype", "journalposttype", SortType.STRING),
        Arguments.of("dokumentetsDato", "dokumentetsDato", SortType.DATE),
        Arguments.of("journaldato", "journaldato", SortType.DATE),
        Arguments.of("journalpostnummer", "journalpostnummer", SortType.NUMBER),
        Arguments.of("sakssekvensnummer", "sakssekvensnummer", SortType.NUMBER),
        Arguments.of("tittel", "offentligTittel", SortType.STRING),
        Arguments.of("fulltekst", null, null), // not directly in response
        Arguments.of("korrespondansepartNavn", null, null), // not directly in response
        Arguments.of("administrativEnhetNavn", null, null) // not directly in response
        );
  }

  @ParameterizedTest(name = "sortBy={0}, sortOrder=desc")
  @MethodSource("sortByParameters")
  void testPaginationWithSortByDesc(String sortBy, String jsonField, SortType sortType)
      throws Exception {
    testPaginationWithSortBy(sortBy, jsonField, sortType, "desc");
  }

  @ParameterizedTest(name = "sortBy={0}, sortOrder=asc")
  @MethodSource("sortByParameters")
  void testPaginationWithSortByAsc(String sortBy, String jsonField, SortType sortType)
      throws Exception {
    testPaginationWithSortBy(sortBy, jsonField, sortType, "asc");
  }

  private void testPaginationWithSortBy(
      String sortBy, String jsonField, SortType sortType, String sortOrder) throws Exception {
    var url = "/search?limit=1&sortBy=" + sortBy + "&sortOrder=" + sortOrder;

    // Get first page
    var response = get(url);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var page1Body = response.getBody();
    PaginatedList<BaseDTO> page1 = gson.fromJson(page1Body, jptype);
    assertNotNull(page1);
    assertEquals(1, page1.getItems().size());
    var nextUrl = page1.getNext();
    assertNotNull(nextUrl, "Expected next page for sortBy=" + sortBy);

    // Get second page
    response = get(nextUrl);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var page2Body = response.getBody();
    PaginatedList<BaseDTO> page2 = gson.fromJson(page2Body, jptype);
    assertNotNull(page2);
    assertEquals(1, page2.getItems().size());

    // Verify sort order if we have a field and type to compare
    if (jsonField != null && sortType != null) {
      var json1 = gson.fromJson(page1Body, JsonObject.class);
      var items = json1.getAsJsonArray("items");
      var json2 = gson.fromJson(page2Body, JsonObject.class);
      var items2 = json2.getAsJsonArray("items");
      verifySortOrder(items, items2, jsonField, sortType, sortOrder);
    }
  }

  private void verifySortOrder(
      JsonArray page1, JsonArray page2, String jsonField, SortType sortType, String sortOrder) {
    // Compare last item of page1 with first item of page2
    var lastOfPage1 = page1.get(page1.size() - 1).getAsJsonObject();
    var firstOfPage2 = page2.get(0).getAsJsonObject();

    var value1 = getJsonValue(lastOfPage1, jsonField);
    var value2 = getJsonValue(firstOfPage2, jsonField);

    // NULL values should always come last, regardless of sort order. If both are NULL, they are
    // equal.
    int comparison;
    if (value1 == null && value2 == null) {
      comparison = 0;
    } else if (value1 == null) {
      comparison = 1; // nulls last
    } else if (value2 == null) {
      comparison = -1; // nulls last
    } else {
      comparison = compareValues(value1, value2, sortType);
      if ("desc".equals(sortOrder)) {
        comparison = -comparison;
      }
    }

    assertTrue(
        comparison <= 0,
        "Expected " + sortOrder + " order for " + jsonField + ": " + value1 + " before " + value2);
  }

  private String getJsonValue(JsonObject obj, String field) {
    if (!obj.has(field) || obj.get(field).isJsonNull()) {
      return null;
    }
    return obj.get(field).getAsString();
  }

  private int compareValues(String v1, String v2, SortType sortType) {
    return switch (sortType) {
      case NUMBER -> Double.compare(Double.parseDouble(v1), Double.parseDouble(v2));
      case DATE -> LocalDate.parse(v1).compareTo(LocalDate.parse(v2));
      case DATETIME ->
          TimeConverter.timestampToInstant(v1).compareTo(TimeConverter.timestampToInstant(v2));
      case STRING -> v1.compareTo(v2);
    };
  }

  @Test
  void testPaginationWithEndingBeforeAscSortOrder() throws Exception {
    // Get first page with sortOrder=asc
    var response = get("/search?limit=2&sortBy=id&sortOrder=asc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(2, searchResult.getItems().size());
    assertNotNull(searchResult.getNext());
    var firstId = searchResult.getItems().get(0).getId();
    var secondId = searchResult.getItems().get(1).getId();

    // Get second page
    response = get(searchResult.getNext());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(2, searchResult.getItems().size());
    assertNotNull(searchResult.getPrevious());
    var thirdId = searchResult.getItems().get(0).getId();

    // Go back using previous link (which uses endingBefore)
    // The previous URL should contain sortOrder=asc and endingBefore
    var previousUrl = searchResult.getPrevious();
    assertTrue(previousUrl.contains("endingBefore"));
    assertTrue(previousUrl.contains("sortOrder=asc"));

    response = get(previousUrl);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(2, searchResult.getItems().size());

    // Verify we got the first two items back
    assertEquals(firstId, searchResult.getItems().get(0).getId());
    assertEquals(secondId, searchResult.getItems().get(1).getId());

    // Verify we can go forward again
    assertNotNull(searchResult.getNext());
    response = get(searchResult.getNext());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), jptype);
    assertEquals(thirdId, searchResult.getItems().get(0).getId());
  }
}

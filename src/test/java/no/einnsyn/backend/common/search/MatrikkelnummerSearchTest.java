package no.einnsyn.backend.common.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.exceptions.models.ValidationException;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MatrikkelnummerSearchTest extends EinnsynControllerTestBase {

  // Distinct values unlikely to appear in other test data
  private static final String K = "8888";
  private static final int G = 301;
  private static final int B = 77;
  private static final int G2 = 302;
  private static final int B2 = 78;
  private static final int F2 = 5;
  private static final int G3 = 303;
  private static final int B3 = 79;
  private static final int F3 = 6;
  private static final int S3 = 7;
  private static final String MISSING_K = "8999";
  private static final int MISSING_G = 9901;
  private static final int MISSING_B = 9902;

  ArkivDTO arkivDTO;
  ArkivdelDTO arkivdelDTO;

  // Gnr/Bnr only (festenummer=0, seksjonsnummer=0)
  SaksmappeDTO saksmappeSimpleDTO;
  JournalpostDTO journalpostSimpleDTO;
  MoetemappeDTO moetemappeSimpleDTO;
  MoetesakDTO moetesakSimpleDTO;
  MoetedokumentDTO moetedokumentSimpleDTO;
  // Gnr/Bnr/Fnr (seksjonsnummer=0)
  SaksmappeDTO saksmappeFestDTO;
  // Gnr/Bnr/Fnr/Snr (all four)
  SaksmappeDTO saksmappeFullDTO;

  private final Type searchResultType = new TypeToken<PaginatedList<BaseDTO>>() {}.getType();

  @BeforeAll
  void setup() throws Exception {
    var response = post("/arkiv", getArkivJSON());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);
    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    var sm1JSON = getSaksmappeJSON();
    sm1JSON.put("matrikkelnummer", new JSONArray().put(getMatrikkelnummerJSON(K, G, B)));
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", sm1JSON);
    saksmappeSimpleDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    var journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("matrikkelnummer", new JSONArray().put(getMatrikkelnummerJSON(K, G, B)));
    response = post("/saksmappe/" + saksmappeSimpleDTO.getId() + "/journalpost", journalpostJSON);
    journalpostSimpleDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    var moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.remove("moetedokument");
    moetemappeJSON.remove("moetesak");
    moetemappeJSON.put("matrikkelnummer", new JSONArray().put(getMatrikkelnummerJSON(K, G, B)));
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    moetemappeSimpleDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    var moetesakJSON = getMoetesakJSON();
    moetesakJSON.put("matrikkelnummer", new JSONArray().put(getMatrikkelnummerJSON(K, G, B)));
    response = post("/moetemappe/" + moetemappeSimpleDTO.getId() + "/moetesak", moetesakJSON);
    moetesakSimpleDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);

    var moetedokumentJSON = getMoetedokumentJSON();
    moetedokumentJSON.put("matrikkelnummer", new JSONArray().put(getMatrikkelnummerJSON(K, G, B)));
    response =
        post("/moetemappe/" + moetemappeSimpleDTO.getId() + "/moetedokument", moetedokumentJSON);
    moetedokumentSimpleDTO = gson.fromJson(response.getBody(), MoetedokumentDTO.class);

    var sm2JSON = getSaksmappeJSON();
    var mn2 = getMatrikkelnummerJSON(K, G2, B2);
    mn2.put("festenummer", F2);
    sm2JSON.put("matrikkelnummer", new JSONArray().put(mn2));
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", sm2JSON);
    saksmappeFestDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    var sm3JSON = getSaksmappeJSON();
    var mn3 = getMatrikkelnummerJSON(K, G3, B3);
    mn3.put("festenummer", F3);
    mn3.put("seksjonsnummer", S3);
    sm3JSON.put("matrikkelnummer", new JSONArray().put(mn3));
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", sm3JSON);
    saksmappeFullDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    esClient.indices().refresh(r -> r.index(elasticsearchIndex));
  }

  @AfterAll
  void teardown() throws Exception {
    delete("/arkiv/" + arkivDTO.getId());
  }

  private void assertEndpointFindsOnly(String query, BaseDTO... expected) throws Exception {
    var expectedIds = new HashSet<String>();
    for (var dto : expected) {
      expectedIds.add(dto.getId());
    }
    var ids = searchEndpointIds(query);
    assertEquals(
        expectedIds,
        Set.copyOf(ids),
        "Search for '" + query + "' should only find " + expectedIds + ", but got: " + ids);
  }

  private void assertEndpointFindsAllParentTypes(String query) throws Exception {
    assertEndpointFindsOnly(
        query,
        saksmappeSimpleDTO,
        journalpostSimpleDTO,
        moetemappeSimpleDTO,
        moetesakSimpleDTO,
        moetedokumentSimpleDTO);
  }

  private List<String> searchEndpointIds(String query) throws Exception {
    var response = get("/search?query=" + encodeQueryParam(query));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), searchResultType);
    assertNotNull(searchResult);
    return searchResult.getItems().stream().map(BaseDTO::getId).toList();
  }

  private String encodeQueryParam(String query) {
    return URLEncoder.encode(query, StandardCharsets.UTF_8);
  }

  private void assertSearchValidationError(String endpoint, String fieldName) throws Exception {
    var response = get(endpoint);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    var errorResponse = gson.fromJson(response.getBody(), ValidationException.ClientResponse.class);
    assertEquals("validationError", errorResponse.getType());
    assertNotNull(errorResponse.getMessage());
    assertNotNull(errorResponse.getFieldError());
    assertEquals(1, errorResponse.getFieldError().size());
    assertEquals(fieldName, errorResponse.getFieldError().getFirst().getFieldName());
  }

  @Test
  void searchEndpointRejectsTooLongQuery() throws Exception {
    assertSearchValidationError("/search?query=" + encodeQueryParam("a".repeat(501)), "query");
  }

  @Test
  void searchEndpointRejectsInvalidDateFilter() throws Exception {
    assertSearchValidationError(
        "/search?query=" + encodeQueryParam(K) + "&standardDatoFrom=not-a-date",
        "standardDatoFrom");
  }

  @Test
  void searchEndpointFindsByKommunenr() throws Exception {
    assertEndpointFindsOnly(
        K,
        saksmappeSimpleDTO,
        journalpostSimpleDTO,
        moetemappeSimpleDTO,
        moetesakSimpleDTO,
        moetedokumentSimpleDTO,
        saksmappeFestDTO,
        saksmappeFullDTO);
  }

  @Test
  void searchEndpointFindsByGnrBnrSlash() throws Exception {
    assertEndpointFindsAllParentTypes(G + "/" + B);
  }

  @Test
  void searchEndpointFindsByGnrBnrPeriod() throws Exception {
    assertEndpointFindsAllParentTypes(G + "." + B);
  }

  @Test
  void searchEndpointFindsByGnrBnrHyphen() throws Exception {
    assertEndpointFindsAllParentTypes(G + "-" + B);
  }

  @Test
  void searchEndpointFindsByKommunenrGnrBnrHyphen() throws Exception {
    assertEndpointFindsAllParentTypes(K + "-" + G + "/" + B);
  }

  @Test
  void searchEndpointFindsByKommunenrGnrBnrSlash() throws Exception {
    assertEndpointFindsAllParentTypes(K + "/" + G + "/" + B);
  }

  @Test
  void searchEndpointFindsByFullFormatHyphen() throws Exception {
    assertEndpointFindsAllParentTypes(K + "-" + G + "/" + B + "/0/0");
  }

  @Test
  void searchEndpointFindsByFullFormatSlash() throws Exception {
    assertEndpointFindsAllParentTypes(K + "/" + G + "/" + B + "/0/0");
  }

  @Test
  void searchEndpointDoesNotFindUnknownGnrBnrSlash() throws Exception {
    assertEndpointFindsOnly(MISSING_G + "/" + MISSING_B);
  }

  @Test
  void searchEndpointDoesNotFindKnownKommunenrWithUnknownGnrBnrHyphen() throws Exception {
    assertEndpointFindsOnly(K + "-" + MISSING_G + "/" + MISSING_B);
  }

  @Test
  void searchEndpointDoesNotFindKnownKommunenrWithUnknownGnrBnrSlash() throws Exception {
    assertEndpointFindsOnly(K + "/" + MISSING_G + "/" + MISSING_B);
  }

  @Test
  void searchEndpointDoesNotFindUnknownKommunenrWithExistingGnrBnrHyphen() throws Exception {
    assertEndpointFindsOnly(MISSING_K + "-" + G + "/" + B);
  }

  @Test
  void searchEndpointDoesNotFindUnknownKommunenrWithExistingGnrBnrSlash() throws Exception {
    assertEndpointFindsOnly(MISSING_K + "/" + G + "/" + B);
  }

  // Prefix format with spaces must be quoted so the query parser treats them
  // as a single phrase and the char_filter can normalize the whole string.
  @Test
  void searchEndpointFindsByGnrBnrPrefixFormatQuoted() throws Exception {
    assertEndpointFindsAllParentTypes("\"gnr " + G + " bnr " + B + "\"");
  }

  @Test
  void searchEndpointFindsByGnrBnrPrefixWithDotsQuoted() throws Exception {
    assertEndpointFindsAllParentTypes("\"gnr. " + G + " bnr. " + B + "\"");
  }

  // --- With festenummer ---

  @Test
  void searchEndpointFindsByGnrBnrFnrThreeComponent() throws Exception {
    assertEndpointFindsOnly(G2 + "/" + B2 + "/" + F2, saksmappeFestDTO);
  }

  @Test
  void searchEndpointFindsByKommunenrGnrBnrFnr() throws Exception {
    assertEndpointFindsOnly(K + "-" + G2 + "/" + B2 + "/" + F2, saksmappeFestDTO);
  }

  @Test
  void searchEndpointFindsByGnrBnrFnrPrefixFormatQuoted() throws Exception {
    assertEndpointFindsOnly("\"gnr " + G2 + " bnr " + B2 + " fnr " + F2 + "\"", saksmappeFestDTO);
  }

  // --- With festenummer and seksjonsnummer ---

  @Test
  void searchEndpointFindsByGnrBnrFnrSnrFourComponent() throws Exception {
    assertEndpointFindsOnly(G3 + "/" + B3 + "/" + F3 + "/" + S3, saksmappeFullDTO);
  }

  @Test
  void searchEndpointFindsByKommunenrGnrBnrFnrSnr() throws Exception {
    assertEndpointFindsOnly(K + "-" + G3 + "/" + B3 + "/" + F3 + "/" + S3, saksmappeFullDTO);
  }

  @Test
  void searchEndpointFindsByGnrBnrFnrSnrPrefixFormatQuoted() throws Exception {
    assertEndpointFindsOnly(
        "\"gnr " + G3 + " bnr " + B3 + " fnr " + F3 + " snr " + S3 + "\"", saksmappeFullDTO);
  }
}

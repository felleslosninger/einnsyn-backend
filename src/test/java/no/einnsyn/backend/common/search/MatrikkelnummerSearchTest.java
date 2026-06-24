package no.einnsyn.backend.common.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.elastic.clients.elasticsearch.core.search.Hit;
import java.util.List;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
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

  ArkivDTO arkivDTO;
  ArkivdelDTO arkivdelDTO;

  // Gnr/Bnr only (festenummer=0, seksjonsnummer=0)
  SaksmappeDTO saksmappeSimpleDTO;
  // Gnr/Bnr/Fnr (seksjonsnummer=0)
  SaksmappeDTO saksmappeFestDTO;
  // Gnr/Bnr/Fnr/Snr (all four)
  SaksmappeDTO saksmappeFullDTO;

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

  private void assertFinds(String query, SaksmappeDTO expected) throws Exception {
    // Use SearchQueryParser directly to build the ES query and search - avoids HTTP URL encoding
    // issues with '/' and other special characters in query strings
    var esQuery =
        SearchQueryParser.parse(
            query, List.of("search_id", "search_innhold", "search_tittel"), 3.0f, 2.0f);
    var result =
        esClient.search(
            s ->
                s.index(elasticsearchIndex)
                    .query(
                        q ->
                            q.bool(
                                b ->
                                    b.filter(
                                            f ->
                                                f.range(
                                                    r ->
                                                        r.date(
                                                            d ->
                                                                d.field("accessibleAfter")
                                                                    .lte("now"))))
                                        .must(esQuery))),
            Void.class);
    var ids = result.hits().hits().stream().map(Hit::id).toList();
    assertTrue(
        ids.contains(expected.getId()),
        "Search for '" + query + "' should find " + expected.getId() + ", but got: " + ids);
  }

  // --- Search endpoint tests (subset of queries without URL-encoding issues) ---

  @Test
  void searchEndpointFindsByKommuneNumber() throws Exception {
    var response = get("/search?query=" + K);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().contains(saksmappeSimpleDTO.getId()));
  }

  @Test
  void searchEndpointFindsByGnrBnrHyphen() throws Exception {
    var response = get("/search?query=" + G + "-" + B);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().contains(saksmappeSimpleDTO.getId()));
  }

  // --- Gnr/Bnr without festenummer/seksjonsnummer ---

  @Test
  void gnrBnrSlash() throws Exception {
    assertFinds(G + "/" + B, saksmappeSimpleDTO);
  }

  @Test
  void gnrBnrPeriod() throws Exception {
    assertFinds(G + "." + B, saksmappeSimpleDTO);
  }

  @Test
  void gnrBnrHyphen() throws Exception {
    assertFinds(G + "-" + B, saksmappeSimpleDTO);
  }

  @Test
  void kommunenrGnrBnrHyphen() throws Exception {
    assertFinds(K + "-" + G + "/" + B, saksmappeSimpleDTO);
  }

  @Test
  void kommunenrGnrBnrSlash() throws Exception {
    assertFinds(K + "/" + G + "/" + B, saksmappeSimpleDTO);
  }

  @Test
  void fullFormatHyphen() throws Exception {
    assertFinds(K + "-" + G + "/" + B + "/0/0", saksmappeSimpleDTO);
  }

  @Test
  void fullFormatSlash() throws Exception {
    assertFinds(K + "/" + G + "/" + B + "/0/0", saksmappeSimpleDTO);
  }

  @Test
  void kommunenummerOnly() throws Exception {
    assertFinds(K, saksmappeSimpleDTO);
  }

  // Prefix format with spaces must be quoted so the query parser treats them
  // as a single phrase and the char_filter can normalize the whole string.
  @Test
  void gnrBnrPrefixFormatQuoted() throws Exception {
    assertFinds("\"gnr " + G + " bnr " + B + "\"", saksmappeSimpleDTO);
  }

  @Test
  void gnrBnrPrefixWithDotsQuoted() throws Exception {
    assertFinds("\"gnr. " + G + " bnr. " + B + "\"", saksmappeSimpleDTO);
  }

  // --- With festenummer ---

  @Test
  void gnrBnrFnrThreeComponent() throws Exception {
    assertFinds(G2 + "/" + B2 + "/" + F2, saksmappeFestDTO);
  }

  @Test
  void kommunenrGnrBnrFnr() throws Exception {
    assertFinds(K + "-" + G2 + "/" + B2 + "/" + F2, saksmappeFestDTO);
  }

  @Test
  void gnrBnrFnrPrefixFormatQuoted() throws Exception {
    assertFinds("\"gnr " + G2 + " bnr " + B2 + " fnr " + F2 + "\"", saksmappeFestDTO);
  }

  // --- With festenummer and seksjonsnummer ---

  @Test
  void gnrBnrFnrSnrFourComponent() throws Exception {
    assertFinds(G3 + "/" + B3 + "/" + F3 + "/" + S3, saksmappeFullDTO);
  }

  @Test
  void kommunenrGnrBnrFnrSnr() throws Exception {
    assertFinds(K + "-" + G3 + "/" + B3 + "/" + F3 + "/" + S3, saksmappeFullDTO);
  }

  @Test
  void gnrBnrFnrSnrPrefixFormatQuoted() throws Exception {
    assertFinds(
        "\"gnr " + G3 + " bnr " + B3 + " fnr " + F3 + " snr " + S3 + "\"", saksmappeFullDTO);
  }
}

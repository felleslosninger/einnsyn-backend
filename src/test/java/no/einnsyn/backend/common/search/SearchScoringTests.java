package no.einnsyn.backend.common.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests verifying that exact phrase searches score higher than loose (stemmed/synonym)
 * matches in Elasticsearch.
 *
 * <p>These tests verify the actual scoring behavior by creating documents and executing searches
 * against a real Elasticsearch instance. Tests cover both stemming (e.g., "søknad" vs "søknader")
 * and synonym matching (e.g., "bil" vs "kjøretøy").
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SearchScoringTests extends EinnsynControllerTestBase {

  ArkivDTO arkivDTO;
  ArkivdelDTO arkivdelDTO;
  SaksmappeDTO saksmappeDTO;

  JournalpostDTO exactMatchDTO; // Contains "søknader" (plural)
  JournalpostDTO stemmedMatchDTO; // Contains "søknad" (singular - stemmed form)

  JournalpostDTO synonymExactMatchDTO; // Contains "bil" (exact match for synonym test)
  JournalpostDTO synonymMatchDTO; // Contains "kjøretøy" (synonym of "bil")

  JournalpostDTO contentMatchDTO; // Contains searchable content in search_innhold field
  JournalpostDTO idMatchDTO; // Contains searchable ID in search_id field

  JournalpostDTO recentDocDTO;
  JournalpostDTO mediumDocDTO;
  JournalpostDTO oldDocDTO;

  Type searchResultType = new TypeToken<PaginatedList<BaseDTO>>() {}.getType();

  @BeforeAll
  void setup() throws Exception {
    // Create arkiv structure
    var response = post("/arkiv", getArkivJSON());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("saksaar", "2024");
    saksmappeJSON.put("sakssekvensnummer", "1");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    saksmappeDTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Create journalpost with plural form: "søknader"
    var journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("offentligTittel", "Dokument om søknader til kommunen");
    journalpostJSON.put("journalsekvensnummer", "1");
    journalpostJSON.put("journalpostnummer", 1);
    journalpostJSON.put("journalposttype", "inngaaende_dokument");
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    exactMatchDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Create journalpost with singular form: "søknad" (will match via stemming)
    journalpostJSON.put("offentligTittel", "Dokument om søknad til kommunen");
    journalpostJSON.put("journalsekvensnummer", "2");
    journalpostJSON.put("journalpostnummer", 2);
    journalpostJSON.put("journalposttype", "utgaaende_dokument");
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    stemmedMatchDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Create journalpost with "bil" (exact match for synonym test)
    journalpostJSON.put("offentligTittel", "Vedtak angående bil for ledelsen");
    journalpostJSON.put("journalsekvensnummer", "3");
    journalpostJSON.put("journalpostnummer", 3);
    journalpostJSON.put("journalposttype", "utgaaende_dokument");
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    synonymExactMatchDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Create journalpost with "kjøretøy" (synonym of "bil")
    journalpostJSON.put("offentligTittel", "Vedtak angående kjøretøy for ledelsen");
    journalpostJSON.put("journalsekvensnummer", "4");
    journalpostJSON.put("journalpostnummer", 4);
    journalpostJSON.put("journalposttype", "utgaaende_dokument");
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    synonymMatchDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Create journalpost with content that populates search_innhold via korrespondansepart
    journalpostJSON.put("offentligTittel", "Generic title for content test");
    journalpostJSON.put("journalsekvensnummer", "5");
    journalpostJSON.put("journalpostnummer", 5);
    journalpostJSON.put("journalposttype", "utgaaende_dokument");
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    contentMatchDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Add korrespondansepart to populate search_innhold field
    var korrespondansepartJSON = getKorrespondansepartJSON();
    korrespondansepartJSON.put("korrespondansepartNavn", "Testkorrespondansepart Pedersen");
    response =
        post(
            "/journalpost/" + contentMatchDTO.getId() + "/korrespondansepart",
            korrespondansepartJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    // Create journalpost with unique ID to test search_id field
    // journalpostnummer copies to search_id field
    journalpostJSON.put("offentligTittel", "Document with unique identifier");
    journalpostJSON.put("journalsekvensnummer", "99");
    journalpostJSON.put("journalpostnummer", 99999);
    journalpostJSON.put("externalId", "externalId-12345");
    journalpostJSON.put("journalposttype", "inngaaende_dokument");
    response = post("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    idMatchDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Create recent document
    journalpostJSON.remove("externalId");
    journalpostJSON.put("offentligTittel", "Recency test document");
    journalpostJSON.put("journalsekvensnummer", "10");
    journalpostJSON.put("journalpostnummer", 10);
    journalpostJSON.put(
        "publisertDato", ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    response = postAdmin("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    recentDocDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Create old document
    journalpostJSON.put("offentligTittel", "Recency test document");
    journalpostJSON.put("journalsekvensnummer", "11");
    journalpostJSON.put("journalpostnummer", 11);
    journalpostJSON.put(
        "publisertDato",
        ZonedDateTime.now().minusDays(100).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    response = postAdmin("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    oldDocDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Create medium recent document (1 day old) - inserted last to verify sort order != insertion
    // order
    journalpostJSON.put("offentligTittel", "Recency test document");
    journalpostJSON.put("journalsekvensnummer", "12");
    journalpostJSON.put("journalpostnummer", 12);
    journalpostJSON.put(
        "publisertDato",
        ZonedDateTime.now().minusDays(1).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    response = postAdmin("/saksmappe/" + saksmappeDTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    mediumDocDTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Refresh indices to make documents searchable
    esClient.indices().refresh(r -> r.index(elasticsearchIndex));
  }

  @AfterAll
  void teardown() throws Exception {
    deleteAdmin("/arkiv/" + arkivDTO.getId());
  }

  @Test
  void testExactPhraseScoresHigherThanLooseMatch() throws Exception {
    // Search for "søknader" (unquoted) - should match both documents via stemming
    // The document with exact word "søknader" should score higher than "søknad"
    var response = get("/search?query=søknader");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), searchResultType);
    assertNotNull(searchResult);
    assertEquals(2, searchResult.getItems().size(), "Both documents should match via loose search");

    // Exact match "søknader" should score higher and appear first
    assertEquals(
        exactMatchDTO.getId(),
        searchResult.getItems().get(0).getId(),
        "Document with exact word 'søknader' should score higher than stemmed 'søknad'");
    assertEquals(
        stemmedMatchDTO.getId(),
        searchResult.getItems().get(1).getId(),
        "Document with stemmed form 'søknad' should score lower");

    // Reverse sorting
    response = get("/search?query=søknader&sortOrder=asc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), searchResultType);
    assertNotNull(searchResult);
    assertEquals(2, searchResult.getItems().size(), "Both documents should match via loose search");

    // Stemmed match should appear first in ascending order
    assertEquals(
        stemmedMatchDTO.getId(),
        searchResult.getItems().get(0).getId(),
        "In ascending order, stemmed 'søknad' should appear first");
    assertEquals(
        exactMatchDTO.getId(),
        searchResult.getItems().get(1).getId(),
        "In ascending order, exact 'søknader' should appear last");
  }

  @Test
  void testQuotedPhraseMatchesExactlyOnly() throws Exception {
    // Search for quoted phrase "søknader til kommunen" - should only match exact phrase
    var response = get("/search?query=\"søknader til kommunen\"");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), searchResultType);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size(), "Only document with exact phrase should match");
    assertEquals(
        exactMatchDTO.getId(),
        searchResult.getItems().get(0).getId(),
        "Should match the document with exact phrase");

    // Search for quoted "søknad til kommunen" - should only match that phrase
    response = get("/search?query=\"søknad til kommunen\"");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    searchResult = gson.fromJson(response.getBody(), searchResultType);
    assertNotNull(searchResult);
    assertEquals(1, searchResult.getItems().size(), "Only document with exact phrase should match");
    assertEquals(
        stemmedMatchDTO.getId(),
        searchResult.getItems().get(0).getId(),
        "Should match the document with exact phrase");
  }

  @Test
  void testQuotedPhraseGetsHigherBoostInMixedQuery() throws Exception {
    // Use OR query with quoted and unquoted terms
    // "søknader" | søknad
    // The quoted exact match should get 2.0x boost, unquoted should get 1.0x boost
    var response = get("/search?query=\"søknader\"|søknad");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), searchResultType);
    assertNotNull(searchResult);
    assertEquals(2, searchResult.getItems().size(), "Both documents should match");

    assertEquals(
        exactMatchDTO.getId(),
        searchResult.getItems().get(0).getId(),
        "Quoted phrase 'søknader' with 2.0x boost should score higher");
    assertEquals(
        stemmedMatchDTO.getId(),
        searchResult.getItems().get(1).getId(),
        "Unquoted 'søknad' with 1.0x boost should score lower");

    // Reverse sorting
    response = get("/search?query=\"søknader\"|søknad&sortOrder=asc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), searchResultType);
    assertNotNull(searchResult);
    assertEquals(2, searchResult.getItems().size(), "Both documents should match");

    assertEquals(
        stemmedMatchDTO.getId(),
        searchResult.getItems().get(0).getId(),
        "In ascending order, unquoted 'søknad' should appear first");
    assertEquals(
        exactMatchDTO.getId(),
        searchResult.getItems().get(1).getId(),
        "In ascending order, quoted 'søknader' should appear last");
  }

  @Test
  void testExactFieldVsLooseFieldMatching() throws Exception {
    // Unquoted "søknader" matches both via loose field (stemming)
    var response = get("/search?query=søknader");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), searchResultType);
    assertEquals(
        2,
        searchResult.getItems().size(),
        "Unquoted search should match both via stemming on .loose field");

    // Quoted "søknader" only matches exact on .exact field
    response = get("/search?query=\"søknader\"");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), searchResultType);
    assertEquals(
        1,
        searchResult.getItems().size(),
        "Quoted search should only match exact on .exact field (no stemming)");
    assertEquals(exactMatchDTO.getId(), searchResult.getItems().get(0).getId());

    // Quoted "søknad" only matches that exact word
    response = get("/search?query=\"søknad\"&entity=Journalpost");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), searchResultType);
    assertEquals(
        1,
        searchResult.getItems().size(),
        "Quoted 'søknad' should only match documents with exact word 'søknad'");
    assertEquals(stemmedMatchDTO.getId(), searchResult.getItems().get(0).getId());
  }

  @Test
  void testExactMatchScoresHigherThanStemmed() throws Exception {
    // Search with loose phrase, should match both, but rank exact higher
    // Document 1: "Dokument om søknader til kommunen" (exact match)
    // Document 2: "Dokument om søknad til kommunen" (stemmed match)
    var response = get("/search?query=Dokument om søknader til kommunen");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), searchResultType);
    assertNotNull(searchResult);
    assertEquals(2, searchResult.getItems().size(), "Should match two documents");

    // Exact phrase should rank higher than loose (1.0x boost)
    assertEquals(
        exactMatchDTO.getId(),
        searchResult.getItems().get(0).getId(),
        "Document with exact phrase 'Dokument om søknader til kommunen' should score highest");
    assertEquals(
        stemmedMatchDTO.getId(),
        searchResult.getItems().get(1).getId(),
        "Document with stemmed match should score lower");

    // Reverse sorting
    response = get("/search?query=Dokument om søknader til kommunen&sortOrder=asc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), searchResultType);
    assertNotNull(searchResult);
    assertEquals(2, searchResult.getItems().size(), "Should match two documents");

    // Stemmed match should rank lower in ascending order
    assertEquals(
        stemmedMatchDTO.getId(),
        searchResult.getItems().get(0).getId(),
        "In ascending order, stemmed match should appear first");
    assertEquals(
        exactMatchDTO.getId(),
        searchResult.getItems().get(1).getId(),
        "In ascending order, exact match should appear last");
  }

  @Test
  void testExactMatchScoresHigherThanSynonymMatch() throws Exception {
    // Search for "bil" (unquoted) - should match both documents via synonym
    // The document with exact word "bil" should score higher than "kjøretøy"
    // Note: Due to multi-shard IDF variations, we need to refresh and use dfs_query_then_fetch
    esClient.indices().refresh(r -> r.index(elasticsearchIndex));
    var response = get("/search?query=bil");
    System.err.println(response.getBody());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), searchResultType);
    assertNotNull(searchResult);
    assertEquals(2, searchResult.getItems().size(), "Both documents should match via synonym");

    // Exact match "bil" should score higher and appear first
    assertEquals(
        synonymExactMatchDTO.getId(),
        searchResult.getItems().get(0).getId(),
        "Document with exact word 'bil' should score higher than synonym 'kjøretøy'");
    assertEquals(
        synonymMatchDTO.getId(),
        searchResult.getItems().get(1).getId(),
        "Document with synonym 'kjøretøy' should score lower");

    // Reverse sorting
    response = get("/search?query=bil&sortOrder=asc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), searchResultType);
    assertNotNull(searchResult);
    assertEquals(2, searchResult.getItems().size(), "Both documents should match via synonym");

    // Synonym match should appear first in ascending order
    assertEquals(
        synonymMatchDTO.getId(),
        searchResult.getItems().get(0).getId(),
        "In ascending order, synonym 'kjøretøy' should appear first");
    assertEquals(
        synonymExactMatchDTO.getId(),
        searchResult.getItems().get(1).getId(),
        "In ascending order, exact 'bil' should appear last");
  }

  @Test
  void testQuotedPhraseDoesNotMatchSynonym() throws Exception {
    // Search for quoted "bil" - should only match exact word, not synonym
    var response = get("/search?query=\"bil\"");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), searchResultType);
    assertNotNull(searchResult);
    assertEquals(
        1,
        searchResult.getItems().size(),
        "Quoted search should only match exact word, not synonym");
    assertEquals(
        synonymExactMatchDTO.getId(),
        searchResult.getItems().get(0).getId(),
        "Should only match document with exact word 'bil'");

    // Search for quoted "kjøretøy" - should only match exact word
    response = get("/search?query=\"kjøretøy\"");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    searchResult = gson.fromJson(response.getBody(), searchResultType);
    assertNotNull(searchResult);
    assertEquals(
        1,
        searchResult.getItems().size(),
        "Quoted search should only match exact word, not synonym");
    assertEquals(
        synonymMatchDTO.getId(),
        searchResult.getItems().get(0).getId(),
        "Should only match document with exact word 'kjøretøy'");
  }

  @Test
  void testExactPhraseMatchScoresHigherThanSynonymPhrase() throws Exception {
    // Search for phrase with "bil" - both documents should match
    // Document 1: "Vedtak angående bil for ledelsen" (exact match)
    // Document 2: "Vedtak angående kjøretøy for ledelsen" (synonym match)
    var response = get("/search?query=Vedtak angående bil");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), searchResultType);
    assertNotNull(searchResult);
    assertEquals(2, searchResult.getItems().size(), "Both documents should match via synonym");

    // Exact phrase should rank higher
    assertEquals(
        synonymExactMatchDTO.getId(),
        searchResult.getItems().get(0).getId(),
        "Document with exact phrase containing 'bil' should score highest");
    assertEquals(
        synonymMatchDTO.getId(),
        searchResult.getItems().get(1).getId(),
        "Document with synonym 'kjøretøy' should score lower");

    // Reverse sorting
    response = get("/search?query=Vedtak angående bil&sortOrder=asc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), searchResultType);
    assertNotNull(searchResult);
    assertEquals(2, searchResult.getItems().size(), "Both documents should match via synonym");

    // Synonym match should rank lower in ascending order
    assertEquals(
        synonymMatchDTO.getId(),
        searchResult.getItems().get(0).getId(),
        "In ascending order, synonym match should appear first");
    assertEquals(
        synonymExactMatchDTO.getId(),
        searchResult.getItems().get(1).getId(),
        "In ascending order, exact match should appear last");
  }

  @Test
  void testSynonymMatchingWorksInBothDirections() throws Exception {
    // Search for "kjøretøy" should also match "bil" (synonym works both ways)
    var response = get("/search?query=kjøretøy");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), searchResultType);
    assertNotNull(searchResult);
    assertEquals(2, searchResult.getItems().size(), "Both documents should match via synonym");

    // This time "kjøretøy" is the exact match, so it should score higher
    assertEquals(
        synonymMatchDTO.getId(),
        searchResult.getItems().get(0).getId(),
        "Document with exact word 'kjøretøy' should score higher than synonym 'bil'");
    assertEquals(
        synonymExactMatchDTO.getId(),
        searchResult.getItems().get(1).getId(),
        "Document with synonym 'bil' should score lower");
  }

  @Test
  void testSearchMatchesInSearchInnholdField() throws Exception {
    // Search for term that only exists in korrespondansepartNavn (which copies to search_innhold)
    var response = get("/search?query=Testkorrespondansepart");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), searchResultType);
    assertNotNull(searchResult);
    assertEquals(
        1,
        searchResult.getItems().size(),
        "Should find document with 'Testkorrespondansepart' in korrespondansepartNavn via"
            + " search_innhold");
    assertEquals(
        contentMatchDTO.getId(),
        searchResult.getItems().get(0).getId(),
        "Should match document with content in search_innhold field");

    response = get("/search?query=Pedersen");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), searchResultType);
    assertNotNull(searchResult);
    assertEquals(
        1,
        searchResult.getItems().size(),
        "Should find document with 'Pedersen' in search_innhold");
    assertEquals(
        contentMatchDTO.getId(),
        searchResult.getItems().get(0).getId(),
        "Should match document via search_innhold field");
  }

  @Test
  void testMatchExternalId() throws Exception {
    // The externalId field is copied to search_id
    // Hyphenated IDs can now be searched without quotes thanks to the tokenizer fix
    var response = get("/search?query=externalId-12345");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), searchResultType);
    assertNotNull(searchResult);
    assertEquals(
        1,
        searchResult.getItems().size(),
        "Should find one document with externalId 'externalId-12345'");
    assertEquals(
        idMatchDTO.getId(),
        searchResult.getItems().get(0).getId(),
        "Should match document with externalId 'externalId-12345' via search_id field");
  }

  @Test
  void testSearchMatchesJournalpostnummerInSearchIdField() throws Exception {
    // The journalpostnummer field is copied to search_id
    var response = get("/search?query=99999");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), searchResultType);
    assertNotNull(searchResult);
    var items = searchResult.getItems();
    assertEquals(1, items.size(), "Should find one document with journalpostnummer '99999'");
    assertEquals(
        idMatchDTO.getId(),
        items.get(0).getId(),
        "Should match document with journalpostnummer '99999' via search_id field");
  }

  @Test
  void testCombinedSearchAcrossMultipleFields() throws Exception {
    // Search that combines title search with content search
    // This tests that all fields (search_tittel, search_innhold, search_id) are queried
    var response = get("/search?query=Generic Pedersen 5");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), searchResultType);
    assertNotNull(searchResult);
    assertEquals(
        1,
        searchResult.getItems().size(),
        "Should find one document matching across multiple fields");
    assertEquals(
        contentMatchDTO.getId(),
        searchResult.getItems().get(0).getId(),
        "Should match document via combined fields search");
  }

  @Test
  void testRecentDocumentsScoreHigher() throws Exception {
    var response = getAdmin("/search?query=Recency test document&sortBy=score");
    assertEquals(HttpStatus.OK, response.getStatusCode());

    PaginatedList<BaseDTO> searchResult = gson.fromJson(response.getBody(), searchResultType);
    assertNotNull(searchResult);
    assertEquals(
        3, searchResult.getItems().size(), "Should find recent, medium and older document");
    assertEquals(
        recentDocDTO.getId(),
        searchResult.getItems().get(0).getId(),
        "Recent document should be ranked first due to recency boost");
    assertEquals(
        mediumDocDTO.getId(),
        searchResult.getItems().get(1).getId(),
        "Medium recent (1 day old) document should be ranked second");
    assertEquals(
        oldDocDTO.getId(),
        searchResult.getItems().get(2).getId(),
        "Old document should be ranked last");

    // Reverse order
    response = getAdmin("/search?query=Recency test document&sortBy=score&sortOrder=asc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    searchResult = gson.fromJson(response.getBody(), searchResultType);
    assertNotNull(searchResult);
    assertEquals(
        oldDocDTO.getId(),
        searchResult.getItems().get(0).getId(),
        "In ascending order, old document should appear first");
    assertEquals(
        mediumDocDTO.getId(),
        searchResult.getItems().get(1).getId(),
        "In ascending order, medium recent document should appear second");
    assertEquals(
        recentDocDTO.getId(),
        searchResult.getItems().get(2).getId(),
        "In ascending order, recent document should appear last");
  }
}

package no.einnsyn.backend.common.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.time.LocalDate;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

/**
 * Comprehensive tests for search filters and sorting capabilities. Tests all filter parameters and
 * sortBy properties defined in FilterParameters and SortByMapper.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SearchFilterAndSortTest extends EinnsynControllerTestBase {

  ArkivDTO arkivDTO;
  ArkivdelDTO arkivdelDTO;

  SaksmappeDTO saksmappe2023DTO;
  SaksmappeDTO saksmappe2024DTO;

  JournalpostDTO journalpost1DTO; // 2023, with dokumentetsDato, journaldato
  JournalpostDTO journalpost2DTO; // 2024, with different dates
  JournalpostDTO journalpost3DTO; // With fulltext
  JournalpostDTO journalpost4DTO; // Without fulltext
  JournalpostDTO journalpost5DTO; // With skjermingshjemmel

  MoetemappeDTO moetemappeDTO;
  MoetesakDTO moetesakDTO;

  Type baseDTOListType = new TypeToken<PaginatedList<BaseDTO>>() {}.getType();
  Type saksmappeDTOListType = new TypeToken<PaginatedList<SaksmappeDTO>>() {}.getType();
  Type journalpostDTOListType = new TypeToken<PaginatedList<JournalpostDTO>>() {}.getType();

  @BeforeAll
  void setup() throws Exception {
    // Create base structure
    var response = post("/arkiv", getArkivJSON());
    arkivDTO = gson.fromJson(response.getBody(), ArkivDTO.class);

    response = post("/arkiv/" + arkivDTO.getId() + "/arkivdel", getArkivdelJSON());
    arkivdelDTO = gson.fromJson(response.getBody(), ArkivdelDTO.class);

    // Create Saksmappe for 2023
    var saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("saksaar", "2023");
    saksmappeJSON.put("sakssekvensnummer", "100");
    saksmappeJSON.put("offentligTittel", "Saksmappe Alpha");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    saksmappe2023DTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Create Saksmappe for 2024
    saksmappeJSON = getSaksmappeJSON();
    saksmappeJSON.put("saksaar", "2024");
    saksmappeJSON.put("sakssekvensnummer", "200");
    saksmappeJSON.put("offentligTittel", "Saksmappe Beta");
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/saksmappe", saksmappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    saksmappe2024DTO = gson.fromJson(response.getBody(), SaksmappeDTO.class);

    // Create Journalpost 1: 2023, with specific dates
    var journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("offentligTittel", "Alpha Document");
    journalpostJSON.put("journalsekvensnummer", "1");
    journalpostJSON.put("journalpostnummer", 1);
    journalpostJSON.put("journalposttype", "inngaaende_dokument");
    journalpostJSON.put("dokumentetsDato", "2023-06-15");
    journalpostJSON.put("journaldato", "2023-06-20");
    response = post("/saksmappe/" + saksmappe2023DTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    journalpost1DTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Add a korrespondansepart to journalpost1
    var korrespondansepartJSON = getKorrespondansepartJSON();
    korrespondansepartJSON.put("korrespondansepartNavn", "Alpha Sender");
    response =
        post(
            "/journalpost/" + journalpost1DTO.getId() + "/korrespondansepart",
            korrespondansepartJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    // Create Journalpost 2: 2024, with different dates
    journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("offentligTittel", "Beta Document");
    journalpostJSON.put("journalsekvensnummer", "2");
    journalpostJSON.put("journalpostnummer", 2);
    journalpostJSON.put("journalposttype", "utgaaende_dokument");
    journalpostJSON.put("dokumentetsDato", "2024-03-10");
    journalpostJSON.put("journaldato", "2024-03-15");
    response = post("/saksmappe/" + saksmappe2024DTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    journalpost2DTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Add a korrespondansepart to journalpost2
    korrespondansepartJSON = getKorrespondansepartJSON();
    korrespondansepartJSON.put("korrespondansepartNavn", "Beta Recipient");
    response =
        post(
            "/journalpost/" + journalpost2DTO.getId() + "/korrespondansepart",
            korrespondansepartJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    // Create Journalpost 3: With fulltext (has dokumentobjekt)
    journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("offentligTittel", "Gamma Fulltext Document");
    journalpostJSON.put("journalsekvensnummer", "3");
    journalpostJSON.put("journalpostnummer", 3);
    journalpostJSON.put("journalposttype", "inngaaende_dokument");
    journalpostJSON.put("journaldato", "2020-06-20");
    response = post("/saksmappe/" + saksmappe2023DTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    journalpost3DTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Add dokumentbeskrivelse and dokumentobjekt to make it fulltext
    var dokumentbeskrivelseJSON = getDokumentbeskrivelseJSON();
    dokumentbeskrivelseJSON.put("tilknyttetRegistreringSom", "hoveddokument");
    response =
        post(
            "/journalpost/" + journalpost3DTO.getId() + "/dokumentbeskrivelse",
            dokumentbeskrivelseJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var dokumentbeskrivelseDTO = gson.fromJson(response.getBody(), DokumentbeskrivelseDTO.class);

    var dokumentobjektJSON = getDokumentobjektJSON();
    dokumentobjektJSON.put("referanseDokumentfil", "https://example.com/fulltext.pdf");
    response =
        post(
            "/dokumentbeskrivelse/" + dokumentbeskrivelseDTO.getId() + "/dokumentobjekt",
            dokumentobjektJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    // Create Journalpost 4: Without fulltext
    journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("offentligTittel", "Delta No-Fulltext Document");
    journalpostJSON.put("journalsekvensnummer", "4");
    journalpostJSON.put("journalpostnummer", 4);
    journalpostJSON.put("journalposttype", "utgaaende_dokument");
    journalpostJSON.put("journaldato", "2021-01-01");
    response = post("/saksmappe/" + saksmappe2024DTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    journalpost4DTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Create Journalpost 5: With skjermingshjemmel
    journalpostJSON = getJournalpostJSON();
    journalpostJSON.put("offentligTittel", "Epsilon Classified Document");
    journalpostJSON.put("journalsekvensnummer", "5");
    journalpostJSON.put("journalpostnummer", 5);
    journalpostJSON.put("journalposttype", "inngaaende_dokument");
    journalpostJSON.put("journaldato", "2024-01-15");
    response = post("/saksmappe/" + saksmappe2024DTO.getId() + "/journalpost", journalpostJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    journalpost5DTO = gson.fromJson(response.getBody(), JournalpostDTO.class);

    // Add skjermingshjemmel to journalpost5
    var skjermingJSON = getSkjermingJSON();
    skjermingJSON.put("skjermingshjemmel", "offl § 13 jf fvl § 13.1");
    response = post("/journalpost/" + journalpost5DTO.getId() + "/skjerming", skjermingJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    // Add a korrespondansepart to journalpost5
    korrespondansepartJSON = getKorrespondansepartJSON();
    korrespondansepartJSON.put("korrespondansepartNavn", "Gamma Corporation");
    response =
        post(
            "/journalpost/" + journalpost5DTO.getId() + "/korrespondansepart",
            korrespondansepartJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());

    // Create a moetemappe with moetesak
    var moetemappeJSON = getMoetemappeJSON();
    moetemappeJSON.put("moetedato", "2024-05-10T09:00:00Z");
    moetemappeJSON.put("offentligTittel", "Møtemappe May 2024");
    moetemappeJSON.remove("moetesak"); // We'll add it separately
    moetemappeJSON.remove("moetedokument"); // Remove default moetedokuments
    response = post("/arkivdel/" + arkivdelDTO.getId() + "/moetemappe", moetemappeJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    moetemappeDTO = gson.fromJson(response.getBody(), MoetemappeDTO.class);

    // Create a moetesak
    var moetesakJSON = getMoetesakJSON();
    moetesakJSON.put("moetesaksaar", "2024");
    moetesakJSON.put("moetesakssekvensnummer", "15");
    moetesakJSON.put("offentligTittel", "Møtesak 2024/15");
    moetesakJSON.remove("utredning");
    moetesakJSON.remove("vedtak");
    moetesakJSON.remove("innstilling");
    response = post("/moetemappe/" + moetemappeDTO.getId() + "/moetesak", moetesakJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    moetesakDTO = gson.fromJson(response.getBody(), MoetesakDTO.class);

    // Refresh indices
    esClient.indices().refresh(r -> r.index(elasticsearchIndex));
  }

  @AfterAll
  void teardown() throws Exception {
    deleteAdmin("/arkiv/" + arkivDTO.getId());
  }

  // Filter tests

  @Test
  void testFilterByDokumentetsDato() throws Exception {
    // Filter by dokumentetsDatoFrom
    var response = get("/search?entity=Journalpost&dokumentetsDatoFrom=2024-01-01");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(journalpost2DTO.getId(), result.getItems().get(0).getId());

    // Filter by dokumentetsDatoTo
    response = get("/search?entity=Journalpost&dokumentetsDatoTo=2023-12-31");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(journalpost1DTO.getId(), result.getItems().get(0).getId());

    // Filter by date range
    response =
        get(
            "/search?entity=Journalpost&dokumentetsDatoFrom=2023-06-01&dokumentetsDatoTo=2023-06-30");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(journalpost1DTO.getId(), result.getItems().get(0).getId());

    // "from" and "to" should be inclusive
    response =
        get(
            "/search?entity=Journalpost&dokumentetsDatoFrom=2024-03-10&dokumentetsDatoTo=2024-03-10");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(journalpost2DTO.getId(), result.getItems().get(0).getId());

    // Filter with time included
    response = get("/search?entity=Journalpost&dokumentetsDatoFrom=2023-06-15T09:00:00Z");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    var ids = result.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(ids.contains(journalpost2DTO.getId()));
  }

  @Test
  void testFilterByJournaldato() throws Exception {
    // Filter by journaldatoFrom
    var response = get("/search?entity=Journalpost&journaldatoFrom=2024-01-01");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(2, result.getItems().size());
    var ids = result.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(ids.contains(journalpost2DTO.getId()));
    assertTrue(ids.contains(journalpost5DTO.getId()));

    // Filter by journaldatoTo
    response = get("/search?entity=Journalpost&journaldatoTo=2023-12-31");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(3, result.getItems().size());
    ids = result.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(ids.contains(journalpost1DTO.getId()));
    assertTrue(ids.contains(journalpost3DTO.getId()));
    assertTrue(ids.contains(journalpost4DTO.getId()));

    // Filter by date range
    response =
        get("/search?entity=Journalpost&journaldatoFrom=2023-06-19&journaldatoTo=2023-06-21");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(journalpost1DTO.getId(), result.getItems().get(0).getId());
  }

  @Test
  void testFilterByMoetedato() throws Exception {
    // Filter by moetedatoFrom
    var response = get("/search?entity=Moetemappe&moetedatoFrom=2024-05-01");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(moetemappeDTO.getId(), result.getItems().get(0).getId());

    // Filter by moetedatoTo
    response = get("/search?entity=Moetemappe&moetedatoTo=2024-05-31");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(moetemappeDTO.getId(), result.getItems().get(0).getId());

    // Filter by date range
    response = get("/search?entity=Moetemappe&moetedatoFrom=2024-05-01&moetedatoTo=2024-05-31");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(moetemappeDTO.getId(), result.getItems().get(0).getId());

    // Filter with no results
    response = get("/search?entity=Moetemappe&moetedatoFrom=2025-01-01");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(0, result.getItems().size());
  }

  @Test
  void testFilterByMoetesaksaar() throws Exception {
    var response = get("/search?entity=Moetesak&moetesaksaar=2024");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(moetesakDTO.getId(), result.getItems().get(0).getId());

    // Test with non-existent year
    response = get("/search?entity=Moetesak&moetesaksaar=2023");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(0, result.getItems().size());

    // Test with multiple years
    response = get("/search?entity=Moetesak&moetesaksaar=2023&moetesaksaar=2024");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(moetesakDTO.getId(), result.getItems().get(0).getId());
  }

  @Test
  void testFilterByMoetesakssekvensnummer() throws Exception {
    var response = get("/search?entity=Moetesak&moetesakssekvensnummer=15");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(moetesakDTO.getId(), result.getItems().get(0).getId());

    // Test with non-existent number
    response = get("/search?entity=Moetesak&moetesakssekvensnummer=99");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(0, result.getItems().size());

    // Test combined with year
    response = get("/search?entity=Moetesak&moetesaksaar=2024&moetesakssekvensnummer=15");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(moetesakDTO.getId(), result.getItems().get(0).getId());
  }

  @Test
  void testFilterBySaksnummer() throws Exception {
    var response = get("/search?entity=Saksmappe&saksnummer=2023/100");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(saksmappe2023DTO.getId(), result.getItems().get(0).getId());

    response = get("/search?entity=Saksmappe&saksnummer=2024/200");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(saksmappe2024DTO.getId(), result.getItems().get(0).getId());

    // Test with multiple saksnummer
    response = get("/search?entity=Saksmappe&saksnummer=2023/100&saksnummer=2024/200");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(2, result.getItems().size());

    // Test with non-existent saksnummer
    response = get("/search?entity=Saksmappe&saksnummer=2025/999");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(0, result.getItems().size());
  }

  @Test
  void testFilterByFulltext() throws Exception {
    // Filter for documents with fulltext
    var response = get("/search?entity=Journalpost&fulltext=true");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(journalpost3DTO.getId(), result.getItems().get(0).getId());

    // Without fulltext filter, we get all documents
    response = get("/search?entity=Journalpost");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(5, result.getItems().size());
    var ids = result.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(ids.contains(journalpost1DTO.getId()));
    assertTrue(ids.contains(journalpost2DTO.getId()));
    assertTrue(ids.contains(journalpost3DTO.getId()));
    assertTrue(ids.contains(journalpost4DTO.getId()));
    assertTrue(ids.contains(journalpost5DTO.getId()));
  }

  @Test
  void testExcludeAdministrativEnhetExact() throws Exception {
    // First verify we have documents for journalenhetId
    var response = get("/search?administrativEnhetExact=" + journalenhetId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> result = gson.fromJson(response.getBody(), baseDTOListType);
    var countBefore = result.getItems().size();
    assertTrue(countBefore > 0);

    // Now exclude the exact journalenhetId
    response = get("/search?excludeAdministrativEnhetExact=" + journalenhetId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    var countAfterExclude = result.getItems().size();

    // The count should be less after excluding
    assertTrue(countAfterExclude < countBefore);

    // Verify the excluded items don't contain journalenhetId documents
    var excludedIds = result.getItems().stream().map(BaseDTO::getId).toList();
    // Documents directly under journalenhetId should not be in results
    assertTrue(!excludedIds.contains(journalpost1DTO.getId()));
  }

  @Test
  void testFilterByTittel() throws Exception {
    // Test simple word search
    var response = get("/search?entity=Journalpost&tittel=Alpha");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(journalpost1DTO.getId(), result.getItems().get(0).getId());

    // Test quoted phrase search
    response = get("/search?entity=Journalpost&tittel=\"Beta Document\"");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(journalpost2DTO.getId(), result.getItems().get(0).getId());

    // Test OR operator with |
    response = get("/search?entity=Journalpost&tittel=Alpha|Beta");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(2, result.getItems().size());
    var ids = result.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(ids.contains(journalpost1DTO.getId()));
    assertTrue(ids.contains(journalpost2DTO.getId()));

    // Test AND operator with +
    response = get("/search?entity=Journalpost&tittel=Gamma+Fulltext");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(journalpost3DTO.getId(), result.getItems().get(0).getId());

    // Test wildcard (should match Delta ...)
    response = get("/search?entity=Journalpost&tittel=Delt*");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(journalpost4DTO.getId(), result.getItems().get(0).getId());

    // Test no match
    response = get("/search?entity=Journalpost&tittel=NonExistent");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(0, result.getItems().size());
  }

  @Test
  void testFilterByTittelDefaultOperatorAnd() throws Exception {
    // Test defaultOperator=AND: all terms must match by default

    // Single token: should match any document with "Alpha"
    var response = get("/search?entity=Journalpost&tittel=Alpha");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(journalpost1DTO.getId(), result.getItems().get(0).getId());

    // Two tokens without explicit operator: both must match (AND)
    // "Alpha Document" should match journalpost1 which has "Alpha Document" in title
    response = get("/search?entity=Journalpost&tittel=Alpha Document");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(journalpost1DTO.getId(), result.getItems().get(0).getId());

    // Two tokens where only one matches: should NOT match (both required by AND)
    // "Alpha Nonexistent" should not match anything since "Nonexistent" is not in any title
    response = get("/search?entity=Journalpost&tittel=Alpha Nonexistent");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(0, result.getItems().size());

    // Three tokens: all must match (AND)
    // "Gamma Fulltext Document" - all three words are in journalpost3's title
    response = get("/search?entity=Journalpost&tittel=Gamma Fulltext Document");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(journalpost3DTO.getId(), result.getItems().get(0).getId());

    // Three tokens where only 2 match: should NOT match (all required by AND)
    // "Gamma Fulltext Nonexistent" - only 2 out of 3 match, so no results
    response = get("/search?entity=Journalpost&tittel=Gamma Fulltext Nonexistent");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(0, result.getItems().size());

    // Four tokens where only 3 match: should NOT match (all required by AND)
    // "Epsilon Classified Document Something" - only 3 out of 4 match, so no results
    response = get("/search?entity=Journalpost&tittel=Epsilon Classified Document Something");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(0, result.getItems().size());

    // Four tokens where only 2 match: should NOT match (all required by AND)
    // "Epsilon Document Nonexistent Another" - only 2 out of 4 match, so no results
    response = get("/search?entity=Journalpost&tittel=Epsilon Document Nonexistent Another");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(0, result.getItems().size());
  }

  @Test
  void testFilterBySkjermingshjemmel() throws Exception {
    // Test exact match
    var response = get("/search?entity=Journalpost&skjermingshjemmel=\"offl § 13 jf fvl § 13.1\"");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(journalpost5DTO.getId(), result.getItems().get(0).getId());

    // Test partial match
    response = get("/search?entity=Journalpost&skjermingshjemmel=offl");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(journalpost5DTO.getId(), result.getItems().get(0).getId());

    // Test wildcard
    response = get("/search?entity=Journalpost&skjermingshjemmel=offl*");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(journalpost5DTO.getId(), result.getItems().get(0).getId());

    // Test no match
    response = get("/search?entity=Journalpost&skjermingshjemmel=\"offl § 99\"");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(0, result.getItems().size());
  }

  @Test
  void testFilterByKorrespondansepartNavn() throws Exception {
    // Test simple word search
    var response = get("/search?entity=Journalpost&korrespondansepartNavn=Alpha");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(journalpost1DTO.getId(), result.getItems().get(0).getId());

    // Test quoted phrase search
    response = get("/search?entity=Journalpost&korrespondansepartNavn=\"Beta Recipient\"");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(journalpost2DTO.getId(), result.getItems().get(0).getId());

    // Test OR operator with |
    response = get("/search?entity=Journalpost&korrespondansepartNavn=Alpha|Gamma");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertTrue(result.getItems().size() >= 2);
    var ids = result.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(ids.contains(journalpost1DTO.getId()));
    assertTrue(ids.contains(journalpost5DTO.getId()));

    // Test AND operator with +
    response = get("/search?entity=Journalpost&korrespondansepartNavn=Gamma+Corporation");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(journalpost5DTO.getId(), result.getItems().get(0).getId());

    // Test wildcard
    response = get("/search?entity=Journalpost&korrespondansepartNavn=Beta*");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(journalpost2DTO.getId(), result.getItems().get(0).getId());

    // Test no match
    response = get("/search?entity=Journalpost&korrespondansepartNavn=NonExistent");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(0, result.getItems().size());
  }

  @Test
  void testCombinedFilters() throws Exception {
    // Combine entity, saksaar, and journalposttype
    var response =
        get("/search?entity=Journalpost&saksaar=2023&journalposttype=inngaaende_dokument");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> result = gson.fromJson(response.getBody(), baseDTOListType);
    assertTrue(result.getItems().size() >= 1);
    var ids = result.getItems().stream().map(BaseDTO::getId).toList();
    assertTrue(ids.contains(journalpost1DTO.getId()));

    // Combine date filters with entity
    response =
        get("/search?entity=Journalpost&dokumentetsDatoFrom=2024-01-01&journaldatoTo=2024-12-31");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(1, result.getItems().size());
    assertEquals(journalpost2DTO.getId(), result.getItems().get(0).getId());
  }

  // Sorting tests

  @Test
  void testSortByTittel() throws Exception {
    var response = get("/search?entity=Saksmappe&sortBy=tittel&sortOrder=asc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<SaksmappeDTO> result = gson.fromJson(response.getBody(), saksmappeDTOListType);
    assertEquals(2, result.getItems().size());
    var items = result.getItems();

    // Alpha should come before Beta
    var firstTitle = items.get(0).getOffentligTittel();
    var secondTitle = items.get(1).getOffentligTittel();
    assertTrue(firstTitle.compareTo(secondTitle) <= 0);

    // Test descending order
    response = get("/search?entity=Saksmappe&sortBy=tittel&sortOrder=desc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), saksmappeDTOListType);
    assertTrue(result.getItems().size() >= 2);
    items = result.getItems();

    firstTitle = (items.get(0)).getOffentligTittel();
    secondTitle = (items.get(1)).getOffentligTittel();
    assertTrue(firstTitle.compareTo(secondTitle) >= 0);
  }

  @Test
  void testSortBySakssekvensnummer() throws Exception {
    var response = get("/search?entity=Saksmappe&sortBy=sakssekvensnummer&sortOrder=asc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<SaksmappeDTO> result = gson.fromJson(response.getBody(), saksmappeDTOListType);
    assertTrue(result.getItems().size() >= 2);

    var first = result.getItems().get(0);
    var second = result.getItems().get(1);
    assertTrue(first.getSakssekvensnummer() <= second.getSakssekvensnummer());

    // Test descending order
    response = get("/search?entity=Saksmappe&sortBy=sakssekvensnummer&sortOrder=desc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), saksmappeDTOListType);
    assertTrue(result.getItems().size() >= 2);

    first = result.getItems().get(0);
    second = result.getItems().get(1);
    assertTrue(first.getSakssekvensnummer() >= second.getSakssekvensnummer());
  }

  @Test
  void testSortByJournalpostnummer() throws Exception {
    var response = get("/search?entity=Journalpost&sortBy=journalpostnummer&sortOrder=asc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<JournalpostDTO> result =
        gson.fromJson(response.getBody(), journalpostDTOListType);
    assertEquals(5, result.getItems().size());

    var first = result.getItems().get(0);
    var second = result.getItems().get(1);
    assertTrue(first.getJournalpostnummer() <= second.getJournalpostnummer());

    // Test descending order
    response = get("/search?entity=Journalpost&sortBy=journalpostnummer&sortOrder=desc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), journalpostDTOListType);
    assertEquals(5, result.getItems().size());

    first = result.getItems().get(0);
    second = result.getItems().get(1);
    assertTrue(first.getJournalpostnummer() >= second.getJournalpostnummer());
  }

  @Test
  void testSortByJournalposttype() throws Exception {
    var response = get("/search?entity=Journalpost&sortBy=journalposttype&sortOrder=asc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<JournalpostDTO> result =
        gson.fromJson(response.getBody(), journalpostDTOListType);
    assertEquals(5, result.getItems().size());
    var first = result.getItems().get(0);
    var second = result.getItems().get(1);
    assertTrue(first.getJournalposttype().compareTo(second.getJournalposttype()) <= 0);

    // Test descending order
    response = get("/search?entity=Journalpost&sortBy=journalposttype&sortOrder=desc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), journalpostDTOListType);
    assertEquals(5, result.getItems().size());
    first = result.getItems().get(0);
    second = result.getItems().get(1);
    assertTrue(first.getJournalposttype().compareTo(second.getJournalposttype()) >= 0);
  }

  @Test
  void testSortByDokumentetsDato() throws Exception {
    var response =
        get(
            "/search?entity=Journalpost&sortBy=dokumentetsDato&sortOrder=asc&limit=2&dokumentetsDatoFrom=2000-01-01");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<JournalpostDTO> result =
        gson.fromJson(response.getBody(), journalpostDTOListType);
    assertEquals(2, result.getItems().size());
    var firstDate = LocalDate.parse(result.getItems().get(0).getDokumentetsDato());
    var secondDate = LocalDate.parse(result.getItems().get(1).getDokumentetsDato());
    assertTrue(firstDate.compareTo(secondDate) <= 0);

    // Test descending order
    response =
        get(
            "/search?entity=Journalpost&sortBy=dokumentetsDato&sortOrder=desc&limit=2&dokumentetsDatoFrom=2000-01-01");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), journalpostDTOListType);
    assertEquals(2, result.getItems().size());
    firstDate = LocalDate.parse(result.getItems().get(0).getDokumentetsDato());
    secondDate = LocalDate.parse(result.getItems().get(1).getDokumentetsDato());
    assertTrue(firstDate.compareTo(secondDate) >= 0);
  }

  @Test
  void testSortByJournaldato() throws Exception {
    var response = get("/search?entity=Journalpost&sortBy=journaldato&sortOrder=asc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<JournalpostDTO> result =
        gson.fromJson(response.getBody(), journalpostDTOListType);
    assertTrue(
        result
                .getItems()
                .get(0)
                .getJournaldato()
                .compareTo(result.getItems().get(1).getJournaldato())
            <= 0);

    // Test descending order
    response = get("/search?entity=Journalpost&sortBy=journaldato&sortOrder=desc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), journalpostDTOListType);
    assertTrue(
        result
                .getItems()
                .get(0)
                .getJournaldato()
                .compareTo(result.getItems().get(1).getJournaldato())
            >= 0);
  }

  @Test
  void testSortByAdministrativEnhetNavn() throws Exception {
    var response =
        get(
            "/search?entity=Journalpost&sortBy=administrativEnhetNavn&sortOrder=asc&expand=administrativEnhetObjekt");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<JournalpostDTO> result =
        gson.fromJson(response.getBody(), journalpostDTOListType);
    var firstName =
        result.getItems().get(0).getAdministrativEnhetObjekt().getExpandedObject().getNavn();
    var secondName =
        result.getItems().get(1).getAdministrativEnhetObjekt().getExpandedObject().getNavn();
    assertTrue(firstName.compareTo(secondName) <= 0);

    // Test descending order
    response =
        get(
            "/search?entity=Journalpost&sortBy=administrativEnhetNavn&sortOrder=desc&expand=administrativEnhetObjekt");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), journalpostDTOListType);
    firstName =
        result.getItems().get(0).getAdministrativEnhetObjekt().getExpandedObject().getNavn();
    secondName =
        result.getItems().get(1).getAdministrativEnhetObjekt().getExpandedObject().getNavn();
    assertTrue(firstName.compareTo(secondName) >= 0);
  }

  @Test
  void testSortByKorrespondansepartNavn() throws Exception {
    var response =
        get(
            "/search?entity=Journalpost&sortBy=korrespondansepartNavn&sortOrder=asc&expand=korrespondansepart");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<JournalpostDTO> result =
        gson.fromJson(response.getBody(), journalpostDTOListType);
    var firstName =
        result
            .getItems()
            .get(0)
            .getKorrespondansepart()
            .get(0)
            .getExpandedObject()
            .getKorrespondansepartNavn();
    var secondName =
        result
            .getItems()
            .get(1)
            .getKorrespondansepart()
            .get(0)
            .getExpandedObject()
            .getKorrespondansepartNavn();
    assertTrue(firstName.compareTo(secondName) <= 0);

    // Test descending order
    response =
        get(
            "/search?entity=Journalpost&sortBy=korrespondansepartNavn&sortOrder=desc&expand=korrespondansepart");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), journalpostDTOListType);
    firstName =
        result
            .getItems()
            .get(0)
            .getKorrespondansepart()
            .get(0)
            .getExpandedObject()
            .getKorrespondansepartNavn();
    secondName =
        result
            .getItems()
            .get(1)
            .getKorrespondansepart()
            .get(0)
            .getExpandedObject()
            .getKorrespondansepartNavn();
    assertTrue(firstName.compareTo(secondName) >= 0);
  }

  @Test
  void testSortByMoetedato() throws Exception {
    var response = get("/search?entity=Moetemappe&sortBy=moetedato&sortOrder=asc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> result = gson.fromJson(response.getBody(), baseDTOListType);
    assertTrue(result.getItems().size() >= 1);
    assertEquals(moetemappeDTO.getId(), result.getItems().get(0).getId());

    // Test descending order
    response = get("/search?entity=Moetemappe&sortBy=moetedato&sortOrder=desc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertTrue(result.getItems().size() >= 1);
    assertEquals(moetemappeDTO.getId(), result.getItems().get(0).getId());
  }

  @Test
  void testSortByOppdatertDato() throws Exception {
    var response = get("/search?entity=Journalpost&sortBy=oppdatertDato&sortOrder=asc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<JournalpostDTO> result =
        gson.fromJson(response.getBody(), journalpostDTOListType);
    assertEquals(5, result.getItems().size());
    var first = result.getItems().get(0);
    var second = result.getItems().get(1);
    assertTrue(first.getOppdatertDato().compareTo(second.getOppdatertDato()) <= 0);

    // Test descending order
    response = get("/search?entity=Journalpost&sortBy=oppdatertDato&sortOrder=desc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), journalpostDTOListType);
    assertEquals(5, result.getItems().size());
    first = result.getItems().get(0);
    second = result.getItems().get(1);
    assertTrue(first.getOppdatertDato().compareTo(second.getOppdatertDato()) >= 0);
  }

  @Test
  void testSortByPublisertDato() throws Exception {
    var response = get("/search?entity=Journalpost&sortBy=publisertDato&sortOrder=asc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<JournalpostDTO> result =
        gson.fromJson(response.getBody(), journalpostDTOListType);
    assertEquals(5, result.getItems().size());
    var first = result.getItems().get(0);
    var second = result.getItems().get(1);
    assertTrue(first.getPublisertDato().compareTo(second.getPublisertDato()) <= 0);

    // Test descending order
    response = get("/search?entity=Journalpost&sortBy=publisertDato&sortOrder=desc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), journalpostDTOListType);
    assertEquals(5, result.getItems().size());
    first = result.getItems().get(0);
    second = result.getItems().get(1);
    assertTrue(first.getPublisertDato().compareTo(second.getPublisertDato()) >= 0);
  }

  @Test
  void testSortByEntity() throws Exception {
    var response = get("/search?sortBy=entity&sortOrder=asc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    // BaseDTO doesn't have .getEntity, so we parse manually
    var jsonResponse = gson.fromJson(response.getBody(), JsonObject.class);
    var items = jsonResponse.getAsJsonArray("items");
    assertTrue(items.size() >= 2);
    var firstEntity = items.get(0).getAsJsonObject().get("entity").getAsString();
    var secondEntity = items.get(1).getAsJsonObject().get("entity").getAsString();
    assertTrue(firstEntity.compareTo(secondEntity) <= 0);

    // Test descending order
    response = get("/search?sortBy=entity&sortOrder=desc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    jsonResponse = gson.fromJson(response.getBody(), JsonObject.class);
    items = jsonResponse.getAsJsonArray("items");
    assertTrue(items.size() >= 2);
    firstEntity = items.get(0).getAsJsonObject().get("entity").getAsString();
    secondEntity = items.get(1).getAsJsonObject().get("entity").getAsString();
    assertTrue(firstEntity.compareTo(secondEntity) >= 0);
  }

  @Test
  void testSortById() throws Exception {
    var response = get("/search?entity=Journalpost&sortBy=id&sortOrder=asc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<JournalpostDTO> result =
        gson.fromJson(response.getBody(), journalpostDTOListType);
    assertEquals(5, result.getItems().size());
    var first = result.getItems().get(0);
    var second = result.getItems().get(1);
    assertTrue(first.getId().compareTo(second.getId()) <= 0);

    // Test descending order
    response = get("/search?entity=Journalpost&sortBy=id&sortOrder=desc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), journalpostDTOListType);
    assertEquals(5, result.getItems().size());
    first = result.getItems().get(0);
    second = result.getItems().get(1);
    assertTrue(first.getId().compareTo(second.getId()) >= 0);
  }

  @Test
  void testSortByFulltext() throws Exception {
    // Sort by fulltext ascending (false < true, so documents without fulltext come first)
    var response = get("/search?entity=Journalpost&sortBy=fulltekst&sortOrder=asc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<JournalpostDTO> result =
        gson.fromJson(response.getBody(), journalpostDTOListType);
    assertEquals(5, result.getItems().size());

    // Find positions of our test journalposts
    var journalpost3Index = -1;
    var journalpost4Index = -1;
    for (int i = 0; i < result.getItems().size(); i++) {
      var item = result.getItems().get(i);
      if (item.getId().equals(journalpost3DTO.getId())) {
        journalpost3Index = i;
      } else if (item.getId().equals(journalpost4DTO.getId())) {
        journalpost4Index = i;
      }
    }

    // In ascending order, journalpost4 (no fulltext) should come before journalpost3 (with
    // fulltext)
    assertTrue(journalpost4Index >= 0, "journalpost4 should be in results");
    assertTrue(journalpost3Index >= 0, "journalpost3 should be in results");
    assertTrue(
        journalpost4Index < journalpost3Index,
        "Documents without fulltext should come before documents with fulltext in ascending order");

    // Test descending order (true > false, so documents with fulltext come first)
    response = get("/search?entity=Journalpost&sortBy=fulltekst&sortOrder=desc");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), journalpostDTOListType);
    assertEquals(5, result.getItems().size());

    // Find positions again
    journalpost3Index = -1;
    journalpost4Index = -1;
    for (int i = 0; i < result.getItems().size(); i++) {
      var item = result.getItems().get(i);
      if (item.getId().equals(journalpost3DTO.getId())) {
        journalpost3Index = i;
      } else if (item.getId().equals(journalpost4DTO.getId())) {
        journalpost4Index = i;
      }
    }

    // In descending order, journalpost3 (with fulltext) should come before journalpost4 (no
    // fulltext)
    assertTrue(journalpost4Index >= 0, "journalpost4 should be in results");
    assertTrue(journalpost3Index >= 0, "journalpost3 should be in results");
    assertTrue(
        journalpost3Index < journalpost4Index,
        "Documents with fulltext should come before documents without fulltext in descending"
            + " order");
  }

  @Test
  void testSortingWithPagination() throws Exception {
    // Test that sorting works correctly with pagination
    var response = get("/search?sortBy=id&sortOrder=asc&limit=2");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    PaginatedList<BaseDTO> result = gson.fromJson(response.getBody(), baseDTOListType);
    assertEquals(2, result.getItems().size());
    assertNotNull(result.getNext());

    var firstPageFirstId = result.getItems().get(0).getId();
    var firstPageSecondId = result.getItems().get(1).getId();

    // Get next page
    response = get(result.getNext());
    assertEquals(HttpStatus.OK, response.getStatusCode());
    result = gson.fromJson(response.getBody(), baseDTOListType);
    assertTrue(result.getItems().size() >= 1);

    // Verify IDs are different (no duplicates)
    var secondPageFirstId = result.getItems().get(0).getId();
    assertFalse(secondPageFirstId.equals(firstPageFirstId));
    assertFalse(secondPageFirstId.equals(firstPageSecondId));
  }
}

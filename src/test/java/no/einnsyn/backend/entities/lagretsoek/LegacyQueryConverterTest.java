package no.einnsyn.backend.entities.lagretsoek;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import no.einnsyn.backend.EinnsynServiceTestBase;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.search.models.SearchParameters;
import no.einnsyn.backend.entities.enhet.EnhetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

/**
 * Tests for the LegacyQueryConverter class, which converts legacy query format to the new
 * SearchParameters format. Uses programmatic JSON building to work around Jackson's limitations
 * with non-static inner classes.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LegacyQueryConverterTest extends EinnsynServiceTestBase {

  @Autowired private ObjectMapper objectMapper;
  @Autowired private EnhetService enhetService;

  private LegacyQueryConverter converter;

  @BeforeEach
  void setup() {
    converter = new LegacyQueryConverter(objectMapper, enhetService);
  }

  @Test
  void testConvertIdsFilter() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "ids": ["id1", "id2", "id3"]
        }
        """;

    var result = converter.convertLegacyQuery(legacyQueryJson);

    assertNotNull(result.getExternalIds());
    assertEquals(3, result.getExternalIds().size());
    assertTrue(result.getExternalIds().contains("id1"));
    assertTrue(result.getExternalIds().contains("id2"));
    assertTrue(result.getExternalIds().contains("id3"));
  }

  @Test
  void testSizeClamping() throws EInnsynException {
    // Test size too large
    var legacyQueryJson1 = "{\"size\": 500}";
    var result1 = converter.convertLegacyQuery(legacyQueryJson1);
    assertEquals(100, result1.getLimit()); // Should be clamped to 100

    // Test size too small
    var legacyQueryJson2 = "{\"size\": 0}";
    var result2 = converter.convertLegacyQuery(legacyQueryJson2);
    assertEquals(1, result2.getLimit()); // Should be clamped to 1

    // Test valid size
    var legacyQueryJson3 = "{\"size\": 50}";
    var result3 = converter.convertLegacyQuery(legacyQueryJson3);
    assertEquals(50, result3.getLimit());
  }

  @ParameterizedTest(name = "Sort by {0} {1}")
  @CsvSource({
    "dokumentetsDato, ASC, dokumentetsDato, asc",
    "journaldato, DESC, journaldato, desc",
    "search_tittel_sort, ASC, tittel, asc",
    "arkivskaperSorteringNavn, DESC, administrativEnhetNavn, desc",
    "journalpostnummer_sort, ASC, journalpostnummer, asc",
    "_score, DESC, score, desc",
    "sakssekvensnummer_sort, ASC, sakssekvensnummer, asc",
    "search_korrespodansepart_sort, DESC, korrespondansepartNavn, desc",
    "standardDato, ASC, standardDato, asc",
    "moetedato, DESC, moetedato, desc",
    "publisertDato, ASC, publisertDato, asc",
    "journalposttype, DESC, journalposttype, desc"
  })
  void testConvertSortField(
      String legacyFieldName, String order, String expectedSortBy, String expectedOrder)
      throws EInnsynException {
    var legacyQueryJson =
        String.format(
            """
            {
              "sort": {
                "fieldName": "%s",
                "order": "%s"
              }
            }
            """,
            legacyFieldName, order);
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertEquals(expectedSortBy, result.getSortBy());
    assertEquals(expectedOrder, result.getSortOrder());
  }

  @Test
  void testDefaultSize() throws EInnsynException {
    // When no size is specified, default is 20
    var legacyQueryJson = "{}";
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertEquals(20, result.getLimit());
  }

  @Test
  void testConvertBasicSearchTerm() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "searchTerm": "test query"
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertEquals("test query", result.getQuery());
  }

  @Test
  void testConvertSearchTermsSaksaar() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "searchTerms": [
            {
              "field": "search_saksaar",
              "searchTerm": "2023 2024",
              "operator": "OR"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertNotNull(result.getSaksaar());
    assertEquals(2, result.getSaksaar().size());
    assertTrue(result.getSaksaar().contains("2023"));
    assertTrue(result.getSaksaar().contains("2024"));
  }

  @Test
  void testConvertSearchTermsSakssekvensnummer() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "searchTerms": [
            {
              "field": "search_sakssekvensnummer",
              "searchTerm": "123 456 789",
              "operator": "OR"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertNotNull(result.getSakssekvensnummer());
    assertEquals(3, result.getSakssekvensnummer().size());
    assertTrue(result.getSakssekvensnummer().contains("123"));
    assertTrue(result.getSakssekvensnummer().contains("456"));
    assertTrue(result.getSakssekvensnummer().contains("789"));
  }

  @Test
  void testConvertSearchTermsJournalpostnummer() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "searchTerms": [
            {
              "field": "journalpostnummer",
              "searchTerm": "1 2",
              "operator": "OR"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertNotNull(result.getJournalpostnummer());
    assertEquals(2, result.getJournalpostnummer().size());
    assertTrue(result.getJournalpostnummer().contains("1"));
    assertTrue(result.getJournalpostnummer().contains("2"));
  }

  @ParameterizedTest(name = "{0} with {2} operator")
  @MethodSource("searchTermsWithOperatorsTestCases")
  void testConvertSearchTermsWithOperators(
      String testDescription,
      String field,
      String operator,
      String searchTerm,
      String expectedOutput,
      Function<SearchParameters, List<String>> resultGetter)
      throws EInnsynException {
    var legacyQueryJson =
        String.format(
            """
            {
              "searchTerms": [
                {
                  "field": "%s",
                  "searchTerm": "%s",
                  "operator": "%s"
                }
              ]
            }
            """,
            field, searchTerm, operator);
    var result = converter.convertLegacyQuery(legacyQueryJson);
    var actualList = resultGetter.apply(result);
    assertNotNull(actualList);
    assertEquals(1, actualList.size());
    assertEquals(expectedOutput, actualList.get(0));
  }

  private static Stream<Arguments> searchTermsWithOperatorsTestCases() {
    return Stream.of(
        Arguments.of(
            "tittel",
            "search_tittel",
            "PHRASE",
            "exact phrase match",
            "\"exact phrase match\"",
            (Function<SearchParameters, List<String>>) SearchParameters::getTittel),
        Arguments.of(
            "tittel",
            "search_tittel",
            "OR",
            "word1 word2",
            "(word1 | word2)",
            (Function<SearchParameters, List<String>>) SearchParameters::getTittel),
        Arguments.of(
            "tittel",
            "search_tittel",
            "AND",
            "word1 word2 word3",
            "(+word1 +word2 +word3)",
            (Function<SearchParameters, List<String>>) SearchParameters::getTittel),
        Arguments.of(
            "tittel",
            "search_tittel",
            "NOT_ANY",
            "word1 word2",
            "(-word1 -word2)",
            (Function<SearchParameters, List<String>>) SearchParameters::getTittel),
        Arguments.of(
            "tittel",
            "search_tittel",
            "SIMPLE_QUERY_STRING",
            "word1 word2",
            "word1 word2",
            (Function<SearchParameters, List<String>>) SearchParameters::getTittel),
        Arguments.of(
            "korrespondansepartNavn",
            "korrespondansepart.korrespondansepartNavn",
            "PHRASE",
            "John Smith",
            "\"John Smith\"",
            (Function<SearchParameters, List<String>>) SearchParameters::getKorrespondansepartNavn),
        Arguments.of(
            "korrespondansepartNavn",
            "korrespondansepart.korrespondansepartNavn",
            "OR",
            "Hansen Olsen",
            "(Hansen | Olsen)",
            (Function<SearchParameters, List<String>>) SearchParameters::getKorrespondansepartNavn),
        Arguments.of(
            "korrespondansepartNavn",
            "korrespondansepart.korrespondansepartNavn",
            "AND",
            "Ola Nordmann AS",
            "(+Ola +Nordmann +AS)",
            (Function<SearchParameters, List<String>>) SearchParameters::getKorrespondansepartNavn),
        Arguments.of(
            "korrespondansepartNavn",
            "korrespondansepart.korrespondansepartNavn",
            "NOT_ANY",
            "spam unwanted",
            "(-spam -unwanted)",
            (Function<SearchParameters, List<String>>) SearchParameters::getKorrespondansepartNavn),
        Arguments.of(
            "korrespondansepartNavn",
            "korrespondansepart.korrespondansepartNavn",
            "SIMPLE_QUERY_STRING",
            "John*",
            "John*",
            (Function<SearchParameters, List<String>>) SearchParameters::getKorrespondansepartNavn),
        Arguments.of(
            "skjermingshjemmel",
            "skjerming.skjermingshjemmel",
            "PHRASE",
            "Offentleglova § 13",
            "\"Offentleglova § 13\"",
            (Function<SearchParameters, List<String>>) SearchParameters::getSkjermingshjemmel),
        Arguments.of(
            "skjermingshjemmel",
            "skjerming.skjermingshjemmel",
            "OR",
            "§13 §14",
            "(§13 | §14)",
            (Function<SearchParameters, List<String>>) SearchParameters::getSkjermingshjemmel),
        Arguments.of(
            "skjermingshjemmel",
            "skjerming.skjermingshjemmel",
            "AND",
            "Offentleglova personopplysninger",
            "(+Offentleglova +personopplysninger)",
            (Function<SearchParameters, List<String>>) SearchParameters::getSkjermingshjemmel),
        Arguments.of(
            "skjermingshjemmel",
            "skjerming.skjermingshjemmel",
            "NOT_ANY",
            "confidential classified",
            "(-confidential -classified)",
            (Function<SearchParameters, List<String>>) SearchParameters::getSkjermingshjemmel),
        Arguments.of(
            "skjermingshjemmel",
            "skjerming.skjermingshjemmel",
            "SIMPLE_QUERY_STRING",
            "§13 OR §14",
            "§13 OR §14",
            (Function<SearchParameters, List<String>>) SearchParameters::getSkjermingshjemmel));
  }

  @Test
  void testConvertMultipleTextSearchFields() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "searchTerms": [
            {
              "field": "search_tittel",
              "searchTerm": "budget plan",
              "operator": "AND"
            },
            {
              "field": "korrespondansepart.korrespondansepartNavn",
              "searchTerm": "Olsen Hansen",
              "operator": "OR"
            },
            {
              "field": "skjerming.skjermingshjemmel",
              "searchTerm": "Offentleglova § 13",
              "operator": "PHRASE"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);

    // Verify tittel with AND operator
    assertNotNull(result.getTittel());
    assertEquals(1, result.getTittel().size());
    assertEquals("(+budget +plan)", result.getTittel().get(0));

    // Verify korrespondansepartNavn with OR operator
    assertNotNull(result.getKorrespondansepartNavn());
    assertEquals(1, result.getKorrespondansepartNavn().size());
    assertEquals("(Olsen | Hansen)", result.getKorrespondansepartNavn().get(0));

    // Verify skjermingshjemmel with PHRASE operator
    assertNotNull(result.getSkjermingshjemmel());
    assertEquals(1, result.getSkjermingshjemmel().size());
    assertEquals("\"Offentleglova § 13\"", result.getSkjermingshjemmel().get(0));
  }

  @Test
  void testConvertSearchTermsWithSpecialCharactersEscaping() throws EInnsynException {
    // Test that special characters are properly escaped in OR and AND operators
    var legacyQueryJson =
        """
        {
          "searchTerms": [
            {
              "field": "search_tittel",
              "searchTerm": "word+ word| word*",
              "operator": "OR"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertNotNull(result.getTittel());
    assertEquals(1, result.getTittel().size());
    // Special characters should be escaped with backslashes
    assertEquals("(word\\+ | word\\| | word\\*)", result.getTittel().get(0));
  }

  @Test
  void testConvertTermQueryFilterType() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "appliedFilters": [
            {
              "type": "termQueryFilter",
              "fieldName": "type",
              "fieldValue": ["Journalpost", "Moetedokument"]
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertNotNull(result.getEntity());
    assertEquals(2, result.getEntity().size());
    assertTrue(result.getEntity().contains("Journalpost"));
    assertTrue(result.getEntity().contains("Moetedokument"));
  }

  @Test
  void testConvertTermQueryFilterJournalposttype() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "appliedFilters": [
            {
              "type": "termQueryFilter",
              "fieldName": "journalposttype",
              "fieldValue": ["Inngående dokument", "Utgående dokument"]
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertNotNull(result.getJournalposttype());
    assertEquals(2, result.getJournalposttype().size());
    assertTrue(result.getJournalposttype().contains("Inngående dokument"));
    assertTrue(result.getJournalposttype().contains("Utgående dokument"));
  }

  @ParameterizedTest(name = "Range filter for {0}")
  @MethodSource("rangeQueryFilterTestCases")
  void testConvertRangeQueryFilter(
      String fieldName,
      String fromDate,
      String toDate,
      Function<SearchParameters, String> fromGetter,
      Function<SearchParameters, String> toGetter)
      throws EInnsynException {
    var legacyQueryJson =
        String.format(
            """
            {
              "appliedFilters": [
                {
                  "type": "rangeQueryFilter",
                  "fieldName": "%s",
                  "from": "%s",
                  "to": "%s"
                }
              ]
            }
            """,
            fieldName, fromDate, toDate);
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertEquals(fromDate, fromGetter.apply(result));
    assertEquals(toDate, toGetter.apply(result));
  }

  private static Stream<Arguments> rangeQueryFilterTestCases() {
    return Stream.of(
        Arguments.of(
            "dokumentetsDato",
            "2023-01-01",
            "2023-12-31",
            (Function<SearchParameters, String>) SearchParameters::getDokumentetsDatoFrom,
            (Function<SearchParameters, String>) SearchParameters::getDokumentetsDatoTo),
        Arguments.of(
            "journaldato",
            "2023-06-01",
            "2023-06-30",
            (Function<SearchParameters, String>) SearchParameters::getJournaldatoFrom,
            (Function<SearchParameters, String>) SearchParameters::getJournaldatoTo),
        Arguments.of(
            "moetedato",
            "2023-03-01",
            "2023-03-31",
            (Function<SearchParameters, String>) SearchParameters::getMoetedatoFrom,
            (Function<SearchParameters, String>) SearchParameters::getMoetedatoTo),
        Arguments.of(
            "publisertDato",
            "2023-09-01",
            "2023-09-30",
            (Function<SearchParameters, String>) SearchParameters::getPublisertDatoFrom,
            (Function<SearchParameters, String>) SearchParameters::getPublisertDatoTo));
  }

  @Test
  void testConvertRangeQueryFilterFromOnly() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "appliedFilters": [
            {
              "type": "rangeQueryFilter",
              "fieldName": "dokumentetsDato",
              "from": "2023-01-01"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertEquals("2023-01-01", result.getDokumentetsDatoFrom());
    assertNull(result.getDokumentetsDatoTo());
  }

  @Test
  void testConvertRangeQueryFilterToOnly() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "appliedFilters": [
            {
              "type": "rangeQueryFilter",
              "fieldName": "dokumentetsDato",
              "to": "2023-12-31"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertNull(result.getDokumentetsDatoFrom());
    assertEquals("2023-12-31", result.getDokumentetsDatoTo());
  }

  @ParameterizedTest(name = "Date field {0} with ||/d suffix")
  @MethodSource("dateFieldsWithDateMathTestCases")
  void testConvertAllDateFieldsWithDateMathSuffix(
      String fieldName,
      Function<SearchParameters, String> fromGetter,
      Function<SearchParameters, String> toGetter)
      throws EInnsynException {
    var legacyQueryJson =
        String.format(
            """
            {
              "appliedFilters": [
                {
                  "type": "rangeQueryFilter",
                  "fieldName": "%s",
                  "from": "2023-01-01||/d",
                  "to": "2023-12-31||/d"
                }
              ]
            }
            """,
            fieldName);
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertEquals("2023-01-01", fromGetter.apply(result));
    assertEquals("2023-12-31", toGetter.apply(result));
  }

  private static Stream<Arguments> dateFieldsWithDateMathTestCases() {
    return Stream.of(
        Arguments.of(
            "dokumentetsDato",
            (Function<SearchParameters, String>) SearchParameters::getDokumentetsDatoFrom,
            (Function<SearchParameters, String>) SearchParameters::getDokumentetsDatoTo),
        Arguments.of(
            "journaldato",
            (Function<SearchParameters, String>) SearchParameters::getJournaldatoFrom,
            (Function<SearchParameters, String>) SearchParameters::getJournaldatoTo),
        Arguments.of(
            "moetedato",
            (Function<SearchParameters, String>) SearchParameters::getMoetedatoFrom,
            (Function<SearchParameters, String>) SearchParameters::getMoetedatoTo),
        Arguments.of(
            "publisertDato",
            (Function<SearchParameters, String>) SearchParameters::getPublisertDatoFrom,
            (Function<SearchParameters, String>) SearchParameters::getPublisertDatoTo));
  }

  @Test
  void testConvertComplexQuery() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "searchTerm": "budget",
          "size": 50,
          "sort": {
            "fieldName": "dokumentetsDato",
            "order": "DESC"
          },
          "appliedFilters": [
            {
              "type": "rangeQueryFilter",
              "fieldName": "journaldato",
              "from": "2023-01-01",
              "to": "2023-12-31"
            },
            {
              "type": "termQueryFilter",
              "fieldName": "journalposttype",
              "fieldValue": ["Inngående dokument"]
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertEquals("budget", result.getQuery());
    assertEquals(50, result.getLimit());
    assertEquals("dokumentetsDato", result.getSortBy());
    assertEquals("desc", result.getSortOrder());
    assertEquals("2023-01-01", result.getJournaldatoFrom());
    assertEquals("2023-12-31", result.getJournaldatoTo());
    assertNotNull(result.getJournalposttype());
    assertEquals(1, result.getJournalposttype().size());
    assertTrue(result.getJournalposttype().contains("Inngående dokument"));
  }

  @Test
  void testConvertMultipleFilters() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "appliedFilters": [
            {
              "type": "rangeQueryFilter",
              "fieldName": "dokumentetsDato",
              "from": "2023-01-01",
              "to": "2023-12-31"
            },
            {
              "type": "rangeQueryFilter",
              "fieldName": "journaldato",
              "from": "2023-06-01",
              "to": "2023-06-30"
            },
            {
              "type": "termQueryFilter",
              "fieldName": "type",
              "fieldValue": ["Journalpost"]
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertEquals("2023-01-01", result.getDokumentetsDatoFrom());
    assertEquals("2023-12-31", result.getDokumentetsDatoTo());
    assertEquals("2023-06-01", result.getJournaldatoFrom());
    assertEquals("2023-06-30", result.getJournaldatoTo());
    assertNotNull(result.getEntity());
    assertEquals(1, result.getEntity().size());
    assertTrue(result.getEntity().contains("Journalpost"));
  }

  @Test
  void testConvertMultipleSearchTerms() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "searchTerms": [
            {
              "field": "search_saksaar",
              "searchTerm": "2023",
              "operator": "OR"
            },
            {
              "field": "search_sakssekvensnummer",
              "searchTerm": "100",
              "operator": "OR"
            },
            {
              "field": "search_tittel",
              "searchTerm": "important meeting",
              "operator": "PHRASE"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertNotNull(result.getSaksaar());
    assertEquals(1, result.getSaksaar().size());
    assertTrue(result.getSaksaar().contains("2023"));
    assertNotNull(result.getSakssekvensnummer());
    assertEquals(1, result.getSakssekvensnummer().size());
    assertTrue(result.getSakssekvensnummer().contains("100"));
    assertNotNull(result.getTittel());
    assertEquals(1, result.getTittel().size());
    assertEquals("\"important meeting\"", result.getTittel().get(0));
  }

  @Test
  void testIncompleteSearchTermIgnored() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "searchTerms": [
            {
              "field": "search_saksaar",
              "searchTerm": null,
              "operator": "OR"
            },
            {
              "field": "search_sakssekvensnummer",
              "searchTerm": "123",
              "operator": "OR"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    // First search term should be ignored due to null searchTerm
    // Only second one should be processed
    assertNull(result.getSaksaar());
    assertNotNull(result.getSakssekvensnummer());
    assertEquals(1, result.getSakssekvensnummer().size());
    assertTrue(result.getSakssekvensnummer().contains("123"));
  }

  @Test
  void testConvertPostQueryFilterWithIriResolution() throws EInnsynException {
    // This test uses the existing test enhet from EinnsynServiceTestBase
    // which has been set up with a real IRI. We'll use the externalId to look it up.
    var enhet = enhetService.findById(journalenhetId);
    assertNotNull(enhet, "Test enhet should exist");

    var enhetId = enhet.getId();
    var enhetIri = enhet.getIri();

    var legacyQueryJson =
        String.format(
            """
            {
              "appliedFilters": [
                {
                  "type": "postQueryFilter",
                  "fieldName": "arkivskaperTransitive",
                  "fieldValue": ["%s"]
                }
              ]
            }
            """,
            enhetIri);

    var result = converter.convertLegacyQuery(legacyQueryJson);

    assertNotNull(result.getAdministrativEnhet());
    assertEquals(1, result.getAdministrativEnhet().size());
    assertTrue(result.getAdministrativEnhet().contains(enhetId));
  }

  @Test
  void testConvertNotQueryFilterWithIriResolution() throws EInnsynException {
    // Test exclusion filter with IRI resolution
    var enhet = enhetService.findById(journalenhetId);
    assertNotNull(enhet, "Test enhet should exist");

    var enhetId = enhet.getId();
    var enhetIri = enhet.getIri();

    var legacyQueryJson =
        String.format(
            """
            {
              "appliedFilters": [
                {
                  "type": "notQueryFilter",
                  "fieldName": "arkivskaperTransitive",
                  "fieldValue": ["%s"]
                }
              ]
            }
            """,
            enhetIri);

    var result = converter.convertLegacyQuery(legacyQueryJson);

    assertNotNull(result.getExcludeAdministrativEnhet());
    assertEquals(1, result.getExcludeAdministrativEnhet().size());
    assertTrue(result.getExcludeAdministrativEnhet().contains(enhetId));
  }

  @Test
  void testConvertPostQueryFilterWithMultipleIris() throws EInnsynException {
    // Test with multiple IRIs (both virksomhet and utvalg)
    var enhet1 = enhetService.findById(journalenhetId);
    var enhet2 = enhetService.findById(journalenhet2Id);
    assertNotNull(enhet1, "Test enhet 1 should exist");
    assertNotNull(enhet2, "Test enhet 2 should exist");

    var enhetId1 = enhet1.getId();
    var enhetId2 = enhet2.getId();
    var enhetIri1 = enhet1.getIri();
    var enhetIri2 = enhet2.getIri();

    var legacyQueryJson =
        String.format(
            """
            {
              "appliedFilters": [
                {
                  "type": "postQueryFilter",
                  "fieldName": "arkivskaperTransitive",
                  "fieldValue": ["%s", "%s"]
                }
              ]
            }
            """,
            enhetIri1, enhetIri2);

    var result = converter.convertLegacyQuery(legacyQueryJson);

    assertNotNull(result.getAdministrativEnhet());
    assertEquals(2, result.getAdministrativEnhet().size());
    assertTrue(result.getAdministrativEnhet().contains(enhetId1));
    assertTrue(result.getAdministrativEnhet().contains(enhetId2));
  }

  @Test
  void testConvertComplexQueryWithIrisAndMultipleFilters() throws EInnsynException {
    var enhet = enhetService.findById(journalenhetId);
    var underenhet = enhetService.findById(journalenhet2Id);
    assertNotNull(enhet, "Test enhet should exist");
    assertNotNull(underenhet, "Test underenhet should exist");

    var enhetId = enhet.getId();
    var underenhetId = underenhet.getId();
    var enhetIri = enhet.getIri();
    var underenhetIri = underenhet.getIri();

    var legacyQueryJson =
        String.format(
            """
            {
              "size": 50,
              "appliedFilters": [
                {
                  "type": "termQueryFilter",
                  "fieldName": "type",
                  "fieldValue": ["Moetemappe", "Journalpost"]
                },
                {
                  "type": "postQueryFilter",
                  "fieldName": "arkivskaperTransitive",
                  "fieldValue": ["%s"]
                },
                {
                  "type": "notQueryFilter",
                  "fieldName": "arkivskaperTransitive",
                  "fieldValue": ["%s"]
                },
                {
                  "type": "rangeQueryFilter",
                  "fieldName": "dokumentetsDato",
                  "from": "2023-01-01",
                  "to": "2023-12-31"
                }
              ],
              "searchTerms": [
                {
                  "field": "search_tittel",
                  "operator": "AND",
                  "searchTerm": "detaljregulering bolig"
                }
              ]
            }
            """,
            enhetIri, underenhetIri);

    var result = converter.convertLegacyQuery(legacyQueryJson);

    // Verify size
    assertEquals(50, result.getLimit());

    // Verify entity filter
    assertNotNull(result.getEntity());
    assertEquals(2, result.getEntity().size());
    assertTrue(result.getEntity().contains("Moetemappe"));
    assertTrue(result.getEntity().contains("Journalpost"));

    // Verify include administrative unit (with IRI resolved)
    assertNotNull(result.getAdministrativEnhet());
    assertEquals(1, result.getAdministrativEnhet().size());
    assertTrue(result.getAdministrativEnhet().contains(enhetId));

    // Verify exclude administrative unit (with IRI resolved)
    assertNotNull(result.getExcludeAdministrativEnhet());
    assertEquals(1, result.getExcludeAdministrativEnhet().size());
    assertTrue(result.getExcludeAdministrativEnhet().contains(underenhetId));

    // Verify date range
    assertEquals("2023-01-01", result.getDokumentetsDatoFrom());
    assertEquals("2023-12-31", result.getDokumentetsDatoTo());

    // Verify tittel search terms (AND operator creates a single query with + prefixes)
    assertNotNull(result.getTittel());
    assertEquals(1, result.getTittel().size());
    assertEquals("(+detaljregulering +bolig)", result.getTittel().get(0));
  }

  @Test
  void testConvertPostQueryFilterWithArkivskaperTransitiveFilterFieldName()
      throws EInnsynException {
    // Test with the alternative field name "arkivskaperTransitive_filter"
    var enhet = enhetService.findById(journalenhetId);
    assertNotNull(enhet, "Test enhet should exist");

    var enhetId = enhet.getId();
    var enhetIri = enhet.getIri();

    var legacyQueryJson =
        String.format(
            """
            {
              "appliedFilters": [
                {
                  "type": "postQueryFilter",
                  "fieldName": "arkivskaperTransitive_filter",
                  "fieldValue": ["%s"]
                }
              ]
            }
            """,
            enhetIri);

    var result = converter.convertLegacyQuery(legacyQueryJson);

    assertNotNull(result.getAdministrativEnhet());
    assertEquals(1, result.getAdministrativEnhet().size());
    assertTrue(result.getAdministrativEnhet().contains(enhetId));
  }

  @Test
  void testConvertNotQueryFilterWithArkivskaperTransitiveFilterFieldName() throws EInnsynException {
    // Test exclusion with the alternative field name "arkivskaperTransitive_filter"
    var enhet = enhetService.findById(journalenhetId);
    assertNotNull(enhet, "Test enhet should exist");

    var enhetId = enhet.getId();
    var enhetIri = enhet.getIri();

    var legacyQueryJson =
        String.format(
            """
            {
              "appliedFilters": [
                {
                  "type": "notQueryFilter",
                  "fieldName": "arkivskaperTransitive_filter",
                  "fieldValue": ["%s"]
                }
              ]
            }
            """,
            enhetIri);

    var result = converter.convertLegacyQuery(legacyQueryJson);

    assertNotNull(result.getExcludeAdministrativEnhet());
    assertEquals(1, result.getExcludeAdministrativEnhet().size());
    assertTrue(result.getExcludeAdministrativEnhet().contains(enhetId));
  }

  @Test
  void testConvertPostQueryFilterTypeFilter() throws EInnsynException {
    // Test legacy type mapping for postQueryFilter with type_filter field
    var legacyQueryJson =
        """
        {
          "appliedFilters": [
            {
              "type": "postQueryFilter",
              "fieldName": "type_filter",
              "fieldValue": ["Journalpost", "Moetesak"]
            }
          ]
        }
        """;

    var result = converter.convertLegacyQuery(legacyQueryJson);

    assertNotNull(result.getEntity());
    assertEquals(2, result.getEntity().size());
    assertTrue(result.getEntity().contains("Journalpost"));
    assertTrue(result.getEntity().contains("Moetesak"));
  }

  @Test
  void testConvertTermQueryFilterTypeFilter() throws EInnsynException {
    // Test with type_filter instead of type field name
    var legacyQueryJson =
        """
        {
          "appliedFilters": [
            {
              "type": "termQueryFilter",
              "fieldName": "type_filter",
              "fieldValue": ["Journalpost"]
            }
          ]
        }
        """;

    var result = converter.convertLegacyQuery(legacyQueryJson);

    assertNotNull(result.getEntity());
    assertEquals(1, result.getEntity().size());
    assertTrue(result.getEntity().contains("Journalpost"));
  }

  @Test
  void testConvertTermQueryFilterJournalposttypeFilter() throws EInnsynException {
    // Test with journalposttype_filter instead of journalposttype field name
    var legacyQueryJson =
        """
        {
          "appliedFilters": [
            {
              "type": "termQueryFilter",
              "fieldName": "journalposttype_filter",
              "fieldValue": ["Inngående dokument"]
            }
          ]
        }
        """;

    var result = converter.convertLegacyQuery(legacyQueryJson);

    assertNotNull(result.getJournalposttype());
    assertEquals(1, result.getJournalposttype().size());
    assertTrue(result.getJournalposttype().contains("Inngående dokument"));
  }

  @Test
  void testConvertRangeQueryFilterWithDateTimeMathSuffix() throws EInnsynException {
    // Test that datetime with time component and ||/d suffix is properly rounded to start of day
    var legacyQueryJson =
        """
        {
          "appliedFilters": [
            {
              "type": "rangeQueryFilter",
              "fieldName": "dokumentetsDato",
              "from": "2023-01-15T10:30:00||/d",
              "to": "2023-12-20T23:59:59||/d"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    // Datetime should be rounded to start of day (just the date part)
    assertEquals("2023-01-15", result.getDokumentetsDatoFrom());
    assertEquals("2023-12-20", result.getDokumentetsDatoTo());
  }

  @Test
  void testConvertRangeQueryFilterWithMixedDateFormats() throws EInnsynException {
    // Test with mixed date formats: datetime with time, and date without time
    var legacyQueryJson =
        """
        {
          "appliedFilters": [
            {
              "type": "rangeQueryFilter",
              "fieldName": "journaldato",
              "from": "2023-01-01T14:30:00||/d",
              "to": "2023-12-31||/d"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    // Both should be normalized to date format
    assertEquals("2023-01-01", result.getJournaldatoFrom());
    assertEquals("2023-12-31", result.getJournaldatoTo());
  }

  @Test
  void testConvertRangeQueryFilterWithMonthRounding() throws EInnsynException {
    // Test ||/M (round to start of month)
    var legacyQueryJson =
        """
        {
          "appliedFilters": [
            {
              "type": "rangeQueryFilter",
              "fieldName": "dokumentetsDato",
              "from": "2023-06-15||/M",
              "to": "2023-12-20T10:30:00||/M"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    // Should round to first day of month
    assertEquals("2023-06-01", result.getDokumentetsDatoFrom());
    assertEquals("2023-12-01", result.getDokumentetsDatoTo());
  }

  @Test
  void testConvertRangeQueryFilterWithYearRounding() throws EInnsynException {
    // Test ||/y (round to start of year)
    var legacyQueryJson =
        """
        {
          "appliedFilters": [
            {
              "type": "rangeQueryFilter",
              "fieldName": "journaldato",
              "from": "2023-06-15||/y",
              "to": "2024-12-31||/y"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    // Should round to first day of year
    assertEquals("2023-01-01", result.getJournaldatoFrom());
    assertEquals("2024-01-01", result.getJournaldatoTo());
  }

  @Test
  void testConvertRangeQueryFilterWithWeekRounding() throws EInnsynException {
    // Test ||/w (round to start of week/Monday)
    // 2023-01-15 is a Sunday, previous Monday is 2023-01-09
    var legacyQueryJson =
        """
        {
          "appliedFilters": [
            {
              "type": "rangeQueryFilter",
              "fieldName": "moetedato",
              "from": "2023-01-15||/w",
              "to": "2023-01-20||/w"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    // 2023-01-15 (Sunday) should round to 2023-01-09 (Monday)
    // 2023-01-20 (Friday) should round to 2023-01-16 (Monday)
    assertEquals("2023-01-09", result.getMoetedatoFrom());
    assertEquals("2023-01-16", result.getMoetedatoTo());
  }

  @Test
  void testConvertRangeQueryFilterWithHourRounding() throws EInnsynException {
    // Test ||/h (round to start of hour)
    var legacyQueryJson =
        """
        {
          "appliedFilters": [
            {
              "type": "rangeQueryFilter",
              "fieldName": "publisertDato",
              "from": "2023-01-01T10:30:45||/h",
              "to": "2023-01-01T15:45:30||/H"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    // Should round to start of hour
    assertEquals("2023-01-01T10:00", result.getPublisertDatoFrom());
    assertEquals("2023-01-01T15:00", result.getPublisertDatoTo());
  }

  @Test
  void testConvertRangeQueryFilterWithMinuteRounding() throws EInnsynException {
    // Test ||/m (round to start of minute)
    var legacyQueryJson =
        """
        {
          "appliedFilters": [
            {
              "type": "rangeQueryFilter",
              "fieldName": "publisertDato",
              "from": "2023-01-01T10:30:45||/m",
              "to": "2023-01-01T15:45:30||/m"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    // Should round to start of minute
    assertEquals("2023-01-01T10:30", result.getPublisertDatoFrom());
    assertEquals("2023-01-01T15:45", result.getPublisertDatoTo());
  }

  @Test
  void testConvertRangeQueryFilterWithSecondRounding() throws EInnsynException {
    // Test ||/s (round to start of second)
    var legacyQueryJson =
        """
        {
          "appliedFilters": [
            {
              "type": "rangeQueryFilter",
              "fieldName": "publisertDato",
              "from": "2023-01-01T10:30:45.123456||/s",
              "to": "2023-01-01T15:45:30.999||/s"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    // Should round to start of second (strip nanoseconds)
    assertEquals("2023-01-01T10:30:45", result.getPublisertDatoFrom());
    assertEquals("2023-01-01T15:45:30", result.getPublisertDatoTo());
  }
}

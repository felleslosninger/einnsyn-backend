package no.einnsyn.backend.entities.lagretsoek;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.einnsyn.backend.EinnsynServiceTestBase;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.entities.enhet.EnhetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    var legacyQueryJson1 =
        """
        {"size": 500}
        """;

    var result1 = converter.convertLegacyQuery(legacyQueryJson1);
    assertEquals(100, result1.getLimit()); // Should be clamped to 100

    // Test size too small
    var legacyQueryJson2 =
        """
        {"size": 0}
        """;

    var result2 = converter.convertLegacyQuery(legacyQueryJson2);
    assertEquals(1, result2.getLimit()); // Should be clamped to 1

    // Test valid size
    var legacyQueryJson3 =
        """
        {"size": 50}
        """;

    var result3 = converter.convertLegacyQuery(legacyQueryJson3);
    assertEquals(50, result3.getLimit());
  }

  @Test
  void testConvertSortByDokumentetsDato() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "sort": {
            "fieldName": "dokumentetsDato",
            "order": "ASC"
          }
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertEquals("dokumentetsDato", result.getSortBy());
    assertEquals("asc", result.getSortOrder());
  }

  @Test
  void testConvertSortByJournaldatoDesc() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "sort": {
            "fieldName": "journaldato",
            "order": "DESC"
          }
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertEquals("journaldato", result.getSortBy());
    assertEquals("desc", result.getSortOrder());
  }

  @Test
  void testConvertSortByTittelAsc() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "sort": {
            "fieldName": "search_tittel_sort",
            "order": "ASC"
          }
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertEquals("tittel", result.getSortBy());
    assertEquals("asc", result.getSortOrder());
  }

  @Test
  void testConvertSortByArkivskaperSorteringNavn() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "sort": {
            "fieldName": "arkivskaperSorteringNavn",
            "order": "DESC"
          }
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertEquals("administrativEnhetNavn", result.getSortBy());
    assertEquals("desc", result.getSortOrder());
  }

  @Test
  void testConvertSortByJournalpostnummer() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "sort": {
            "fieldName": "journalpostnummer_sort",
            "order": "ASC"
          }
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertEquals("journalpostnummer", result.getSortBy());
    assertEquals("asc", result.getSortOrder());
  }

  @Test
  void testConvertSortByScore() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "sort": {
            "fieldName": "_score",
            "order": "DESC"
          }
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertEquals("score", result.getSortBy());
    assertEquals("desc", result.getSortOrder());
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

  @Test
  void testConvertSearchTermsTittelPhrase() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "searchTerms": [
            {
              "field": "search_tittel",
              "searchTerm": "exact phrase match",
              "operator": "PHRASE"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertNotNull(result.getTittel());
    assertEquals(1, result.getTittel().size());
    assertEquals("\"exact phrase match\"", result.getTittel().get(0));
  }

  @Test
  void testConvertSearchTermsTittelOr() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "searchTerms": [
            {
              "field": "search_tittel",
              "searchTerm": "word1 word2",
              "operator": "OR"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertNotNull(result.getTittel());
    assertEquals(1, result.getTittel().size());
    assertEquals("(word1 | word2)", result.getTittel().get(0));
  }

  @Test
  void testConvertSearchTermsTittelAnd() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "searchTerms": [
            {
              "field": "search_tittel",
              "searchTerm": "word1 word2 word3",
              "operator": "AND"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertNotNull(result.getTittel());
    assertEquals(1, result.getTittel().size());
    assertEquals("(+word1 +word2 +word3)", result.getTittel().get(0));
  }

  @Test
  void testConvertSearchTermsKorrespondansepartNavnPhrase() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "searchTerms": [
            {
              "field": "korrespondansepart.korrespondansepartNavn",
              "searchTerm": "John Smith",
              "operator": "PHRASE"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertNotNull(result.getKorrespondansepartNavn());
    assertEquals(1, result.getKorrespondansepartNavn().size());
    assertEquals("\"John Smith\"", result.getKorrespondansepartNavn().get(0));
  }

  @Test
  void testConvertSearchTermsKorrespondansepartNavnOr() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "searchTerms": [
            {
              "field": "korrespondansepart.korrespondansepartNavn",
              "searchTerm": "Hansen Olsen",
              "operator": "OR"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertNotNull(result.getKorrespondansepartNavn());
    assertEquals(1, result.getKorrespondansepartNavn().size());
    assertEquals("(Hansen | Olsen)", result.getKorrespondansepartNavn().get(0));
  }

  @Test
  void testConvertSearchTermsKorrespondansepartNavnAnd() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "searchTerms": [
            {
              "field": "korrespondansepart.korrespondansepartNavn",
              "searchTerm": "Ola Nordmann AS",
              "operator": "AND"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertNotNull(result.getKorrespondansepartNavn());
    assertEquals(1, result.getKorrespondansepartNavn().size());
    assertEquals("(+Ola +Nordmann +AS)", result.getKorrespondansepartNavn().get(0));
  }

  @Test
  void testConvertSearchTermsSkjermingshjemmelPhrase() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "searchTerms": [
            {
              "field": "skjerming.skjermingshjemmel",
              "searchTerm": "Offentleglova § 13",
              "operator": "PHRASE"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertNotNull(result.getSkjermingshjemmel());
    assertEquals(1, result.getSkjermingshjemmel().size());
    assertEquals("\"Offentleglova § 13\"", result.getSkjermingshjemmel().get(0));
  }

  @Test
  void testConvertSearchTermsSkjermingshjemmelOr() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "searchTerms": [
            {
              "field": "skjerming.skjermingshjemmel",
              "searchTerm": "§13 §14",
              "operator": "OR"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertNotNull(result.getSkjermingshjemmel());
    assertEquals(1, result.getSkjermingshjemmel().size());
    assertEquals("(§13 | §14)", result.getSkjermingshjemmel().get(0));
  }

  @Test
  void testConvertSearchTermsSkjermingshjemmelAnd() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "searchTerms": [
            {
              "field": "skjerming.skjermingshjemmel",
              "searchTerm": "Offentleglova personopplysninger",
              "operator": "AND"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertNotNull(result.getSkjermingshjemmel());
    assertEquals(1, result.getSkjermingshjemmel().size());
    assertEquals("(+Offentleglova +personopplysninger)", result.getSkjermingshjemmel().get(0));
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

  @Test
  void testConvertRangeQueryFilterDokumentetsDato() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "appliedFilters": [
            {
              "type": "rangeQueryFilter",
              "fieldName": "dokumentetsDato",
              "from": "2023-01-01",
              "to": "2023-12-31"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertEquals("2023-01-01", result.getDokumentetsDatoFrom());
    assertEquals("2023-12-31", result.getDokumentetsDatoTo());
  }

  @Test
  void testConvertRangeQueryFilterJournaldato() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "appliedFilters": [
            {
              "type": "rangeQueryFilter",
              "fieldName": "journaldato",
              "from": "2023-06-01",
              "to": "2023-06-30"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertEquals("2023-06-01", result.getJournaldatoFrom());
    assertEquals("2023-06-30", result.getJournaldatoTo());
  }

  @Test
  void testConvertRangeQueryFilterMoetedato() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "appliedFilters": [
            {
              "type": "rangeQueryFilter",
              "fieldName": "moetedato",
              "from": "2023-03-01",
              "to": "2023-03-31"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertEquals("2023-03-01", result.getMoetedatoFrom());
    assertEquals("2023-03-31", result.getMoetedatoTo());
  }

  @Test
  void testConvertRangeQueryFilterPublisertDato() throws EInnsynException {
    var legacyQueryJson =
        """
        {
          "appliedFilters": [
            {
              "type": "rangeQueryFilter",
              "fieldName": "publisertDato",
              "from": "2023-09-01",
              "to": "2023-09-30"
            }
          ]
        }
        """;
    var result = converter.convertLegacyQuery(legacyQueryJson);
    assertEquals("2023-09-01", result.getPublisertDatoFrom());
    assertEquals("2023-09-30", result.getPublisertDatoTo());
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
    assertEquals(null, result.getDokumentetsDatoTo());
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
    assertEquals(null, result.getDokumentetsDatoFrom());
    assertEquals("2023-12-31", result.getDokumentetsDatoTo());
  }

  @Test
  void testConvertSortFieldMappings() throws EInnsynException {
    // Test sakssekvensnummer_sort
    var legacyQueryJson1 =
        """
        {
          "sort": {
            "fieldName": "sakssekvensnummer_sort",
            "order": "ASC"
          }
        }
        """;
    var result1 = converter.convertLegacyQuery(legacyQueryJson1);
    assertEquals("sakssekvensnummer", result1.getSortBy());
    assertEquals("asc", result1.getSortOrder());

    // Test search_korrespodansepart_sort
    var legacyQueryJson2 =
        """
        {
          "sort": {
            "fieldName": "search_korrespodansepart_sort",
            "order": "DESC"
          }
        }
        """;
    var result2 = converter.convertLegacyQuery(legacyQueryJson2);
    assertEquals("korrespondansepartNavn", result2.getSortBy());
    assertEquals("desc", result2.getSortOrder());

    // Test standardDato
    var legacyQueryJson3 =
        """
        {
          "sort": {
            "fieldName": "standardDato",
            "order": "ASC"
          }
        }
        """;
    var result3 = converter.convertLegacyQuery(legacyQueryJson3);
    assertEquals("standardDato", result3.getSortBy());
    assertEquals("asc", result3.getSortOrder());

    // Test moetedato
    var legacyQueryJson4 =
        """
        {
          "sort": {
            "fieldName": "moetedato",
            "order": "DESC"
          }
        }
        """;
    var result4 = converter.convertLegacyQuery(legacyQueryJson4);
    assertEquals("moetedato", result4.getSortBy());
    assertEquals("desc", result4.getSortOrder());

    // Test publisertDato
    var legacyQueryJson5 =
        """
        {
          "sort": {
            "fieldName": "publisertDato",
            "order": "ASC"
          }
        }
        """;
    var result5 = converter.convertLegacyQuery(legacyQueryJson5);
    assertEquals("publisertDato", result5.getSortBy());
    assertEquals("asc", result5.getSortOrder());

    // Test journalposttype
    var legacyQueryJson6 =
        """
        {
          "sort": {
            "fieldName": "journalposttype",
            "order": "DESC"
          }
        }
        """;
    var result6 = converter.convertLegacyQuery(legacyQueryJson6);
    assertEquals("journalposttype", result6.getSortBy());
    assertEquals("desc", result6.getSortOrder());
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
              "fieldValue": ["Journalpost", "Moeteregistrering"]
            }
          ]
        }
        """;

    var result = converter.convertLegacyQuery(legacyQueryJson);

    assertNotNull(result.getEntity());
    assertEquals(2, result.getEntity().size());
    assertTrue(result.getEntity().contains("Journalpost"));
    // Moeteregistrering should be mapped to Moetedokument
    assertTrue(result.getEntity().contains("Moetedokument"));
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
}

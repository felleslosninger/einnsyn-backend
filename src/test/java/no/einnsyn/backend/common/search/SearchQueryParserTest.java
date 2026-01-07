package no.einnsyn.backend.common.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.List;
import org.junit.jupiter.api.Test;

class SearchQueryParserTest {

  // Helper methods to reduce repetitive assertions
  private void assertIsSimpleQueryString(
      Query query, String expectedQuery, String... expectedFields) {
    assertTrue(query.isSimpleQueryString());
    assertEquals(expectedQuery, query.simpleQueryString().query());
    var fields = query.simpleQueryString().fields();
    assertEquals(expectedFields.length, fields.size());
    for (String expectedField : expectedFields) {
      assertTrue(fields.contains(expectedField));
    }
  }

  private void assertIsMultiMatch(Query query, String expectedQuery, String... expectedFields) {
    assertTrue(query.isMultiMatch());
    assertEquals(expectedQuery, query.multiMatch().query());
    var fields = query.multiMatch().fields();
    assertEquals(expectedFields.length, fields.size());
    for (String expectedField : expectedFields) {
      assertTrue(fields.contains(expectedField));
    }
  }

  @Test
  void testParseWithEmptyInput() {
    var query1 = SearchQueryParser.parse("", List.of("search_tittel"));
    assertNotNull(query1);
    assertTrue(query1.isMatchAll());

    var query2 = SearchQueryParser.parse(null, List.of("search_tittel"));
    assertNotNull(query2);
    assertTrue(query2.isMatchAll());

    var query3 = SearchQueryParser.parse("\"exact phrase\"", List.of("search_tittel"));
    assertNotNull(query3);
    assertIsMultiMatch(query3, "exact phrase", "search_tittel.exact");
  }

  @Test
  void testParseWithUnquotedTerms() {
    var query = SearchQueryParser.parse("loose terms", List.of("search_tittel"));
    assertNotNull(query);
    assertTrue(query.isBool());

    var boolQuery = query.bool();
    assertEquals(2, boolQuery.must().size());
    assertIsSimpleQueryString(boolQuery.must().get(0), "loose", "search_tittel.loose");
    assertIsSimpleQueryString(boolQuery.must().get(1), "terms", "search_tittel.loose");
  }

  @Test
  void testParseWithMixedQuotedAndUnquoted() {
    var query = SearchQueryParser.parse("\"exact phrase\" loose", List.of("search_tittel"));
    assertNotNull(query);
    assertTrue(query.isBool());

    var boolQuery = query.bool();
    assertEquals(2, boolQuery.must().size());
    assertIsMultiMatch(boolQuery.must().get(0), "exact phrase", "search_tittel.exact");
    assertIsSimpleQueryString(boolQuery.must().get(1), "loose", "search_tittel.loose");
  }

  @Test
  void testParseWithMultipleQuotedPhrases() {
    var query =
        SearchQueryParser.parse("\"first phrase\" \"second phrase\"", List.of("search_tittel"));
    assertNotNull(query);
    assertTrue(query.isBool());

    var boolQuery = query.bool();
    assertEquals(2, boolQuery.must().size());
    assertIsMultiMatch(boolQuery.must().get(0), "first phrase", "search_tittel.exact");
    assertIsMultiMatch(boolQuery.must().get(1), "second phrase", "search_tittel.exact");
  }

  @Test
  void testParseWithMultipleFields() {
    var query =
        SearchQueryParser.parse("\"exact phrase\"", List.of("search_tittel", "search_innhold"));
    assertNotNull(query);
    assertIsMultiMatch(query, "exact phrase", "search_tittel.exact", "search_innhold.exact");
  }

  @Test
  void testParseWithFieldBoosts() {
    var query = SearchQueryParser.parse("\"exact phrase\"", List.of("search_tittel^3.0"));
    assertNotNull(query);
    assertIsMultiMatch(query, "exact phrase", "search_tittel.exact^3.0");
  }

  @Test
  void testParseWithCustomBoosts() {
    var query =
        SearchQueryParser.parse("\"exact phrase\" loose", List.of("search_tittel"), 2.0f, 1.5f);
    assertNotNull(query);
    assertTrue(query.isBool());

    var boolQuery = query.bool();
    assertEquals(2, boolQuery.must().size());

    // Validate structure, query values, and fields
    assertIsMultiMatch(boolQuery.must().get(0), "exact phrase", "search_tittel.exact");
    assertIsSimpleQueryString(boolQuery.must().get(1), "loose", "search_tittel.loose");

    // Validate custom boosts
    assertEquals(Float.valueOf(2.0f), boolQuery.must().get(0).multiMatch().boost());
    assertEquals(Float.valueOf(1.5f), boolQuery.must().get(1).simpleQueryString().boost());
  }

  @Test
  void testParsePreservesFieldBoostsWithMultipleFields() {
    var query =
        SearchQueryParser.parse(
            "loose terms", List.of("search_tittel^3.0", "search_innhold^1.0"), 1.0f, 1.0f);
    assertNotNull(query);
    assertTrue(query.isBool());

    var boolQuery = query.bool();
    assertEquals(2, boolQuery.must().size());

    assertIsSimpleQueryString(
        boolQuery.must().get(0), "loose", "search_tittel.loose^3.0", "search_innhold.loose^1.0");
    assertIsSimpleQueryString(
        boolQuery.must().get(1), "terms", "search_tittel.loose^3.0", "search_innhold.loose^1.0");
  }

  @Test
  void testParseWithOperatorsBetweenQuotedPhrases_OR() {
    var query = SearchQueryParser.parse("\"foo\" | \"bar\"", List.of("search_tittel"));
    assertNotNull(query);
    assertTrue(query.isBool());

    var boolQuery = query.bool();
    assertEquals("1", boolQuery.minimumShouldMatch());
    assertEquals(2, boolQuery.should().size());
    assertEquals(0, boolQuery.must().size());
    assertEquals(0, boolQuery.mustNot().size());

    assertIsMultiMatch(boolQuery.should().get(0), "foo", "search_tittel.exact");
    assertIsMultiMatch(boolQuery.should().get(1), "bar", "search_tittel.exact");
  }

  @Test
  void testParseWithAndOperatorBetweenQuotedPhrases() {
    var query =
        SearchQueryParser.parse("\"first phrase\" + \"second phrase\"", List.of("search_tittel"));
    assertNotNull(query);
    assertTrue(query.isBool());

    var boolQuery = query.bool();
    assertEquals(2, boolQuery.must().size());
    assertEquals(0, boolQuery.mustNot().size());
    assertEquals(0, boolQuery.should().size());

    assertIsMultiMatch(boolQuery.must().get(0), "first phrase", "search_tittel.exact");
    assertIsMultiMatch(boolQuery.must().get(1), "second phrase", "search_tittel.exact");
  }

  @Test
  void testParseWithNotOperatorBetweenQuotedPhrases() {
    var query = SearchQueryParser.parse("\"wanted\" - \"unwanted\"", List.of("search_tittel"));
    assertNotNull(query);
    assertTrue(query.isBool());

    var boolQuery = query.bool();
    assertEquals(2, boolQuery.must().size());
    assertEquals(0, boolQuery.mustNot().size());
    assertEquals(0, boolQuery.should().size());

    assertIsMultiMatch(boolQuery.must().get(0), "wanted", "search_tittel.exact");

    var secondMust = boolQuery.must().get(1);
    assertTrue(secondMust.isBool());
    assertFalse(secondMust.bool().mustNot().isEmpty());
  }

  @Test
  void testImplicitAnd() {
    var query = SearchQueryParser.parse("foo bar \"baz\"", List.of("search_tittel"));
    assertNotNull(query);
    assertTrue(query.isBool());

    var boolQuery = query.bool();
    assertEquals(3, boolQuery.must().size());
    assertEquals(0, boolQuery.mustNot().size());
    assertEquals(0, boolQuery.should().size());

    assertIsSimpleQueryString(boolQuery.must().get(0), "foo", "search_tittel.loose");
    assertIsSimpleQueryString(boolQuery.must().get(1), "bar", "search_tittel.loose");
    assertIsMultiMatch(boolQuery.must().get(2), "baz", "search_tittel.exact");
  }

  @Test
  void testMixedQuotedAndUnquotedWithOr() {
    var query = SearchQueryParser.parse("foo | \"bar\"", List.of("search_tittel"));
    assertNotNull(query);
    assertTrue(query.isBool());

    var boolQuery = query.bool();
    assertEquals("1", boolQuery.minimumShouldMatch());
    assertEquals(2, boolQuery.should().size());

    assertIsSimpleQueryString(boolQuery.should().get(0), "foo", "search_tittel.loose");
    assertIsMultiMatch(boolQuery.should().get(1), "bar", "search_tittel.exact");
  }

  @Test
  void testComplexNestedQuery() {
    // Query: (foo | "bar" | ("foo" + bar | ("baz" | "biz")))
    var query =
        SearchQueryParser.parse(
            "(foo | \"bar\" | (\"foo\" + bar | (\"baz\" | \"biz\")))", List.of("search_tittel"));

    assertTrue(query.isBool());

    // Top-level OR with 3 branches
    var topLevel = query.bool();
    assertEquals("1", topLevel.minimumShouldMatch());
    assertEquals(3, topLevel.should().size());

    // foo
    assertIsSimpleQueryString(topLevel.should().get(0), "foo", "search_tittel.loose");

    // "bar"
    assertIsMultiMatch(topLevel.should().get(1), "bar", "search_tittel.exact");

    // ("foo" + bar | ("baz" | "biz"))
    var nestedOr = topLevel.should().get(2).bool();
    assertEquals(2, nestedOr.should().size());

    // "foo" + bar
    var andQuery = nestedOr.should().get(0).bool();
    assertEquals(2, andQuery.must().size());
    assertIsMultiMatch(andQuery.must().get(0), "foo", "search_tittel.exact");
    assertIsSimpleQueryString(andQuery.must().get(1), "bar", "search_tittel.loose");

    // ("baz" | "biz")
    var deepestOr = nestedOr.should().get(1).bool();
    assertEquals(2, deepestOr.should().size());
    assertIsMultiMatch(deepestOr.should().get(0), "baz", "search_tittel.exact");
    assertIsMultiMatch(deepestOr.should().get(1), "biz", "search_tittel.exact");
  }

  @Test
  void testNestedParentheses() {
    // Query: ((foo + bar) | (baz + qux))
    var query = SearchQueryParser.parse("((foo + bar) | (baz + qux))", List.of("search_tittel"));

    assertNotNull(query);
    assertTrue(query.isBool());

    var boolQuery = query.bool();
    assertEquals("1", boolQuery.minimumShouldMatch());
    assertEquals(2, boolQuery.should().size());

    // (foo + bar)
    assertTrue(boolQuery.should().get(0).isBool());
    var firstAnd = boolQuery.should().get(0).bool();
    assertEquals(2, firstAnd.must().size());
    assertIsSimpleQueryString(firstAnd.must().get(0), "foo", "search_tittel.loose");
    assertIsSimpleQueryString(firstAnd.must().get(1), "bar", "search_tittel.loose");

    // (baz + qux)
    assertTrue(boolQuery.should().get(1).isBool());
    var secondAnd = boolQuery.should().get(1).bool();
    assertEquals(2, secondAnd.must().size());
    assertIsSimpleQueryString(secondAnd.must().get(0), "baz", "search_tittel.loose");
    assertIsSimpleQueryString(secondAnd.must().get(1), "qux", "search_tittel.loose");
  }

  @Test
  void testComplexWithNot() {
    // Query: (foo + "bar") -unwanted
    var query = SearchQueryParser.parse("(foo + \"bar\") -unwanted", List.of("search_tittel"));

    assertNotNull(query);
    assertTrue(query.isBool());

    var boolQuery = query.bool();
    assertEquals(2, boolQuery.must().size());

    // (foo + "bar")
    var firstMust = boolQuery.must().get(0);
    assertTrue(firstMust.isBool());
    var andClause = firstMust.bool();
    assertEquals(2, andClause.must().size());
    assertIsSimpleQueryString(andClause.must().get(0), "foo", "search_tittel.loose");
    assertIsMultiMatch(andClause.must().get(1), "bar", "search_tittel.exact");

    // -unwanted
    var secondMust = boolQuery.must().get(1);
    assertTrue(secondMust.isBool());
    var notBool = secondMust.bool();
    assertFalse(notBool.mustNot().isEmpty());
    assertEquals(1, notBool.mustNot().size());
    assertIsSimpleQueryString(notBool.mustNot().get(0), "unwanted", "search_tittel.loose");
  }

  @Test
  void testComplexRealWorldQuery() {
    // Query: ("Oslo kommune" | "Bergen kommune") + budsjett -skatt
    var query =
        SearchQueryParser.parse(
            "(\"Oslo kommune\" | \"Bergen kommune\") + budsjett -skatt", List.of("search_tittel"));

    assertNotNull(query);
    assertTrue(query.isBool());

    var boolQuery = query.bool();
    assertEquals(3, boolQuery.must().size());

    // ("Oslo kommune" | "Bergen kommune")
    var firstMust = boolQuery.must().get(0);
    assertTrue(firstMust.isBool());
    var orBool = firstMust.bool();
    assertEquals("1", orBool.minimumShouldMatch());
    assertEquals(2, orBool.should().size());
    assertIsMultiMatch(orBool.should().get(0), "Oslo kommune", "search_tittel.exact");
    assertIsMultiMatch(orBool.should().get(1), "Bergen kommune", "search_tittel.exact");

    // budsjett
    assertIsSimpleQueryString(boolQuery.must().get(1), "budsjett", "search_tittel.loose");

    // -skatt
    var lastMust = boolQuery.must().get(2);
    assertTrue(lastMust.isBool());
    var notClause = lastMust.bool();
    assertFalse(notClause.mustNot().isEmpty());
    assertEquals(1, notClause.mustNot().size());
    assertIsSimpleQueryString(notClause.mustNot().get(0), "skatt", "search_tittel.loose");
  }

  @Test
  void testNestedOrInAnd() {
    // Query: foo + (bar | baz) + "exact"
    var query = SearchQueryParser.parse("foo + (bar | baz) + \"exact\"", List.of("search_tittel"));

    assertNotNull(query);
    assertTrue(query.isBool());

    var boolQuery = query.bool();
    assertEquals(3, boolQuery.must().size());

    // foo
    assertIsSimpleQueryString(boolQuery.must().get(0), "foo", "search_tittel.loose");

    // (bar | baz)
    var middleMust = boolQuery.must().get(1);
    assertTrue(middleMust.isBool());
    var orBool = middleMust.bool();
    assertEquals("1", orBool.minimumShouldMatch());
    assertEquals(2, orBool.should().size());
    assertIsSimpleQueryString(orBool.should().get(0), "bar", "search_tittel.loose");
    assertIsSimpleQueryString(orBool.should().get(1), "baz", "search_tittel.loose");

    // "exact"
    assertIsMultiMatch(boolQuery.must().get(2), "exact", "search_tittel.exact");
  }

  @Test
  void testComplexOperatorsBetweenQuotedPhrases() {
    // Query: ("phrase one" | "phrase two") + "required phrase"
    var query =
        SearchQueryParser.parse(
            "(\"phrase one\" | \"phrase two\") + \"required phrase\"", List.of("search_tittel"));

    assertNotNull(query);
    assertTrue(query.isBool());

    var boolQuery = query.bool();
    assertEquals(2, boolQuery.must().size());

    // ("phrase one" | "phrase two")
    var firstMust = boolQuery.must().get(0);
    assertTrue(firstMust.isBool());
    var orClause = firstMust.bool();
    assertEquals("1", orClause.minimumShouldMatch());
    assertEquals(2, orClause.should().size());
    assertIsMultiMatch(orClause.should().get(0), "phrase one", "search_tittel.exact");
    assertIsMultiMatch(orClause.should().get(1), "phrase two", "search_tittel.exact");

    // "required phrase"
    assertIsMultiMatch(boolQuery.must().get(1), "required phrase", "search_tittel.exact");
  }

  @Test
  void testTripleNestedUnwrap() {
    var query = SearchQueryParser.parse("(((foo)))", List.of("search_tittel"));
    assertNotNull(query);
    assertIsSimpleQueryString(query, "foo", "search_tittel.loose");
  }

  @Test
  void testAllOperatorsInOneQuery() {
    // Query: foo + bar | baz -qux
    // Parses as: (foo + bar) | (baz -qux) due to operator precedence
    var query = SearchQueryParser.parse("foo + bar | baz -qux", List.of("search_tittel"));

    assertNotNull(query);
    assertTrue(query.isBool());

    var boolQuery = query.bool();
    assertEquals("1", boolQuery.minimumShouldMatch());
    assertEquals(2, boolQuery.should().size());

    // foo + bar (AND)
    assertTrue(boolQuery.should().get(0).isBool());
    var firstAnd = boolQuery.should().get(0).bool();
    assertEquals(2, firstAnd.must().size());
    assertIsSimpleQueryString(firstAnd.must().get(0), "foo", "search_tittel.loose");
    assertIsSimpleQueryString(firstAnd.must().get(1), "bar", "search_tittel.loose");

    // baz -qux (AND with NOT)
    assertTrue(boolQuery.should().get(1).isBool());
    var secondAnd = boolQuery.should().get(1).bool();
    assertEquals(2, secondAnd.must().size());
    assertIsSimpleQueryString(secondAnd.must().get(0), "baz", "search_tittel.loose");
    // Second must is wrapped in mustNot
    assertTrue(secondAnd.must().get(1).isBool());
    assertFalse(secondAnd.must().get(1).bool().mustNot().isEmpty());
  }

  @Test
  void testWildcardsInUnquotedTerms() {
    var query = SearchQueryParser.parse("bud* + \"exact phrase\"", List.of("search_tittel"));
    assertNotNull(query);
    assertTrue(query.isBool());
    var boolQuery = query.bool();
    assertEquals(2, boolQuery.must().size());
    assertIsSimpleQueryString(boolQuery.must().get(0), "bud*", "search_tittel.loose");
    assertIsMultiMatch(boolQuery.must().get(1), "exact phrase", "search_tittel.exact");
  }

  @Test
  void testParseWithNorwegianCharacters() {
    var query =
        SearchQueryParser.parse("\"søknad om innsyn\" arkivskaper", List.of("search_tittel"));
    assertNotNull(query);
    assertTrue(query.isBool());
    var boolQuery = query.bool();
    assertEquals(2, boolQuery.must().size());
    assertIsMultiMatch(boolQuery.must().get(0), "søknad om innsyn", "search_tittel.exact");
    assertIsSimpleQueryString(boolQuery.must().get(1), "arkivskaper", "search_tittel.loose");
  }

  @Test
  void testNorwegianCharactersInComplexQuery() {
    // Query: ("søknad om innsyn" | arkivskaper) + (Trøndelag | Oslo)
    var query =
        SearchQueryParser.parse(
            "(\"søknad om innsyn\" | arkivskaper) + (Trøndelag | Oslo)", List.of("search_tittel"));

    assertNotNull(query);
    assertTrue(query.isBool());

    var boolQuery = query.bool();
    assertEquals(2, boolQuery.must().size());

    // ("søknad om innsyn" | arkivskaper)
    assertTrue(boolQuery.must().get(0).isBool());
    var firstOr = boolQuery.must().get(0).bool();
    assertEquals("1", firstOr.minimumShouldMatch());
    assertEquals(2, firstOr.should().size());
    assertIsMultiMatch(firstOr.should().get(0), "søknad om innsyn", "search_tittel.exact");
    assertIsSimpleQueryString(firstOr.should().get(1), "arkivskaper", "search_tittel.loose");

    // (Trøndelag | Oslo)
    assertTrue(boolQuery.must().get(1).isBool());
    var secondOr = boolQuery.must().get(1).bool();
    assertEquals("1", secondOr.minimumShouldMatch());
    assertEquals(2, secondOr.should().size());
    assertIsSimpleQueryString(secondOr.should().get(0), "Trøndelag", "search_tittel.loose");
    assertIsSimpleQueryString(secondOr.should().get(1), "Oslo", "search_tittel.loose");
  }

  @Test
  void testOnlyOperators() {
    // Edge case: query string with only operators, the query is in effect empty
    var query = SearchQueryParser.parse("+ | -", List.of("search_tittel"));
    assertNotNull(query);
    assertTrue(query.isMatchAll());
  }

  @Test
  void testOnlyQuotedPhrasesWithOperators() {
    // Query: "foo" | "bar" + "baz"
    // Parses as: "foo" | ("bar" + "baz") due to operator precedence (AND > OR)
    var query = SearchQueryParser.parse("\"foo\" | \"bar\" + \"baz\"", List.of("search_tittel"));
    assertNotNull(query);
    assertTrue(query.isBool());

    var boolQuery = query.bool();
    assertEquals("1", boolQuery.minimumShouldMatch());
    assertEquals(2, boolQuery.should().size());

    // "foo"
    assertIsMultiMatch(boolQuery.should().get(0), "foo", "search_tittel.exact");

    // "bar" + "baz" (AND)
    assertTrue(boolQuery.should().get(1).isBool());
    var andBranch = boolQuery.should().get(1).bool();
    assertEquals(2, andBranch.must().size());
    assertIsMultiMatch(andBranch.must().get(0), "bar", "search_tittel.exact");
    assertIsMultiMatch(andBranch.must().get(1), "baz", "search_tittel.exact");
  }

  @Test
  void testQuotedPhrasesAndActualUnquotedText() {
    var query = SearchQueryParser.parse("\"exact\" regular words", List.of("search_tittel"));
    assertNotNull(query);
    assertTrue(query.isBool());
    var boolQuery = query.bool();
    assertEquals(3, boolQuery.must().size());
    assertIsMultiMatch(boolQuery.must().get(0), "exact", "search_tittel.exact");
    assertIsSimpleQueryString(boolQuery.must().get(1), "regular", "search_tittel.loose");
    assertIsSimpleQueryString(boolQuery.must().get(2), "words", "search_tittel.loose");
  }

  @Test
  void testMultipleFieldsWithBoosts() {
    var query =
        SearchQueryParser.parse("foo + \"bar\"", List.of("search_tittel^3.0", "search_innhold"));
    assertNotNull(query);
    assertTrue(query.isBool());

    var boolQuery = query.bool();
    assertEquals(2, boolQuery.must().size());

    assertIsSimpleQueryString(
        boolQuery.must().get(0), "foo", "search_tittel.loose^3.0", "search_innhold.loose");
    assertIsMultiMatch(
        boolQuery.must().get(1), "bar", "search_tittel.exact^3.0", "search_innhold.exact");
  }

  @Test
  void testConsecutiveOperators() {
    // Edge case: consecutive operators without terms in between
    var query = SearchQueryParser.parse("foo ++ -- || baz", List.of("search_tittel"));
    assertNotNull(query);
    assertTrue(query.isBool());

    var boolQuery = query.bool();
    assertEquals("1", boolQuery.minimumShouldMatch());
    assertEquals(2, boolQuery.should().size());

    // foo
    assertIsSimpleQueryString(boolQuery.should().get(0), "foo", "search_tittel.loose");

    // baz
    assertIsSimpleQueryString(boolQuery.should().get(1), "baz", "search_tittel.loose");
  }
}

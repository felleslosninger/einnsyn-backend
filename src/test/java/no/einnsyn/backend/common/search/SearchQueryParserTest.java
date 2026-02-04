package no.einnsyn.backend.common.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class SearchQueryParserTest {

  // Helper methods to reduce repetitive assertions
  private void assertIsSimpleQueryString(
      Query query, String expectedQuery, String... expectedFields) {
    assertTrue(query.isSimpleQueryString(), "Query should be a SimpleQueryString query");
    assertEquals(
        expectedQuery,
        query.simpleQueryString().query(),
        "SimpleQueryString query string mismatch");
    var fields = query.simpleQueryString().fields();
    assertEquals(expectedFields.length, fields.size(), "SimpleQueryString fields count mismatch");
    for (var expectedField : expectedFields) {
      assertTrue(
          fields.contains(expectedField),
          "SimpleQueryString missing expected field: " + expectedField);
    }
  }

  private void assertIsMultiMatch(Query query, String expectedQuery, String... expectedFields) {
    assertTrue(query.isMultiMatch(), "Query should be a MultiMatch query");
    assertEquals(expectedQuery, query.multiMatch().query(), "MultiMatch query string mismatch");
    var fields = query.multiMatch().fields();
    assertEquals(expectedFields.length, fields.size(), "MultiMatch fields count mismatch");

    for (var expectedField : expectedFields) {
      assertTrue(
          fields.contains(expectedField), "MultiMatch missing expected field: " + expectedField);
    }
  }

  private void assertIsQuotedTerm(Query query, String expectedQuery, String... baseFields) {
    assertTrue(query.isMultiMatch(), "Quoted term should produce a MultiMatch query");
    assertIsMultiMatch(query, expectedQuery, buildFieldsWithSuffix(baseFields, "exact"));
  }

  /**
   * Assert that a query is an unquoted term (bool query with both exact and loose should clauses).
   *
   * @param query the query to check
   * @param expectedQuery the expected query string
   * @param baseFields the base field names (without .exact or .loose suffixes)
   */
  private void assertIsUnquotedTerm(Query query, String expectedQuery, String... baseFields) {
    assertTrue(query.isBool(), "Unquoted term should produce a bool query");
    var boolQuery = query.bool();
    assertEquals(
        "1",
        boolQuery.minimumShouldMatch(),
        "Unquoted term bool query should have minimum_should_match=1");
    assertEquals(2, boolQuery.should().size(), "Should have both exact and loose clauses");

    // First should clause: exact match (MultiMatch phrase query on .exact fields)
    var exactClause = boolQuery.should().get(0);
    var exactFields = buildFieldsWithSuffix(baseFields, "exact");
    assertIsMultiMatch(exactClause, expectedQuery, exactFields);

    // Second should clause: loose match (SimpleQueryString on .loose fields)
    var looseClause = boolQuery.should().get(1);
    var looseFields = buildFieldsWithSuffix(baseFields, "loose");
    assertIsSimpleQueryString(looseClause, expectedQuery, looseFields);
  }

  private String[] buildFieldsWithSuffix(String[] baseFields, String suffix) {
    return Arrays.stream(baseFields)
        .map(
            field -> {
              var parts = field.split("\\^");
              var fieldName = parts[0];
              var boost = parts.length > 1 ? "^" + parts[1] : "";
              return fieldName + "." + suffix + boost;
            })
        .toArray(String[]::new);
  }

  @Test
  void testParseWithEmptyInput() {
    var query1 = SearchQueryParser.parse("", List.of("search_tittel"));
    assertNotNull(query1);
    assertTrue(query1.isMatchAll());

    var query2 = SearchQueryParser.parse(null, List.of("search_tittel"));
    assertNotNull(query2);
    assertTrue(query2.isMatchAll());
  }

  @Test
  void testParseWithUnquotedTerms() {
    var query = SearchQueryParser.parse("loose terms", List.of("search_tittel"));
    assertNotNull(query);
    assertTrue(query.isBool());

    var boolQuery = query.bool();
    assertEquals(2, boolQuery.must().size());
    assertIsUnquotedTerm(boolQuery.must().get(0), "loose", "search_tittel");
    assertIsUnquotedTerm(boolQuery.must().get(1), "terms", "search_tittel");
  }

  @Test
  void testParseWithMixedQuotedAndUnquoted() {
    var query = SearchQueryParser.parse("\"exact phrase\" loose", List.of("search_tittel"));
    assertNotNull(query);
    assertTrue(query.isBool());

    var boolQuery = query.bool();
    assertEquals(2, boolQuery.must().size());
    assertIsQuotedTerm(boolQuery.must().get(0), "exact phrase", "search_tittel");
    assertIsUnquotedTerm(boolQuery.must().get(1), "loose", "search_tittel");
  }

  @Test
  void testParseWithQuotedPhrase() {
    var query = SearchQueryParser.parse("\"exact phrase\"", List.of("search_tittel"));
    assertNotNull(query);
    assertIsQuotedTerm(query, "exact phrase", "search_tittel");
  }

  @Test
  void testParseWithMultipleQuotedPhrases() {
    var query =
        SearchQueryParser.parse("\"first phrase\" \"second phrase\"", List.of("search_tittel"));
    assertNotNull(query);
    assertTrue(query.isBool());

    var boolQuery = query.bool();
    assertEquals(2, boolQuery.must().size());
    assertIsQuotedTerm(boolQuery.must().get(0), "first phrase", "search_tittel");
    assertIsQuotedTerm(boolQuery.must().get(1), "second phrase", "search_tittel");
  }

  @Test
  void testParseWithMultipleFields() {
    var query =
        SearchQueryParser.parse("\"exact phrase\"", List.of("search_tittel", "search_innhold"));
    assertNotNull(query);
    assertIsQuotedTerm(query, "exact phrase", "search_tittel", "search_innhold");
  }

  @Test
  void testParseWithFieldBoosts() {
    var query = SearchQueryParser.parse("\"exact phrase\"", List.of("search_tittel^3.0"));
    assertNotNull(query);
    assertIsQuotedTerm(query, "exact phrase", "search_tittel^3.0");
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
    assertIsQuotedTerm(boolQuery.must().get(0), "exact phrase", "search_tittel");
    assertIsUnquotedTerm(boolQuery.must().get(1), "loose", "search_tittel");

    // Validate custom boosts for unquoted term
    var looseBool = boolQuery.must().get(1).bool();
    assertEquals(Float.valueOf(1.5f), looseBool.should().get(0).multiMatch().boost());
    assertEquals(Float.valueOf(1.5f), looseBool.should().get(1).simpleQueryString().boost());
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

    assertIsUnquotedTerm(
        boolQuery.must().get(0), "loose", "search_tittel^3.0", "search_innhold^1.0");
    assertIsUnquotedTerm(
        boolQuery.must().get(1), "terms", "search_tittel^3.0", "search_innhold^1.0");
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

    assertIsQuotedTerm(boolQuery.should().get(0), "foo", "search_tittel");
    assertIsQuotedTerm(boolQuery.should().get(1), "bar", "search_tittel");
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

    assertIsQuotedTerm(boolQuery.must().get(0), "first phrase", "search_tittel");
    assertIsQuotedTerm(boolQuery.must().get(1), "second phrase", "search_tittel");
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

    assertIsQuotedTerm(boolQuery.must().get(0), "wanted", "search_tittel");

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

    assertIsUnquotedTerm(boolQuery.must().get(0), "foo", "search_tittel");
    assertIsUnquotedTerm(boolQuery.must().get(1), "bar", "search_tittel");
    assertIsQuotedTerm(boolQuery.must().get(2), "baz", "search_tittel");
  }

  @Test
  void testMixedQuotedAndUnquotedWithOr() {
    var query = SearchQueryParser.parse("foo | \"bar\"", List.of("search_tittel"));
    assertNotNull(query);
    assertTrue(query.isBool());

    var boolQuery = query.bool();
    assertEquals("1", boolQuery.minimumShouldMatch());
    assertEquals(2, boolQuery.should().size());

    assertIsUnquotedTerm(boolQuery.should().get(0), "foo", "search_tittel");
    assertIsQuotedTerm(boolQuery.should().get(1), "bar", "search_tittel");
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
    assertIsUnquotedTerm(topLevel.should().get(0), "foo", "search_tittel");

    // "bar"
    assertIsQuotedTerm(topLevel.should().get(1), "bar", "search_tittel");

    // ("foo" + bar | ("baz" | "biz"))
    var nestedOr = topLevel.should().get(2).bool();
    assertEquals(2, nestedOr.should().size());

    // "foo" + bar
    var andQuery = nestedOr.should().get(0).bool();
    assertEquals(2, andQuery.must().size());
    assertIsQuotedTerm(andQuery.must().get(0), "foo", "search_tittel");
    assertIsUnquotedTerm(andQuery.must().get(1), "bar", "search_tittel");

    // ("baz" | "biz")
    var deepestOr = nestedOr.should().get(1).bool();
    assertEquals(2, deepestOr.should().size());
    assertIsQuotedTerm(deepestOr.should().get(0), "baz", "search_tittel");
    assertIsQuotedTerm(deepestOr.should().get(1), "biz", "search_tittel");
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
    assertIsUnquotedTerm(firstAnd.must().get(0), "foo", "search_tittel");
    assertIsUnquotedTerm(firstAnd.must().get(1), "bar", "search_tittel");

    // (baz + qux)
    assertTrue(boolQuery.should().get(1).isBool());
    var secondAnd = boolQuery.should().get(1).bool();
    assertEquals(2, secondAnd.must().size());
    assertIsUnquotedTerm(secondAnd.must().get(0), "baz", "search_tittel");
    assertIsUnquotedTerm(secondAnd.must().get(1), "qux", "search_tittel");
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
    assertIsUnquotedTerm(andClause.must().get(0), "foo", "search_tittel");
    assertIsQuotedTerm(andClause.must().get(1), "bar", "search_tittel");

    // -unwanted
    var secondMust = boolQuery.must().get(1);
    assertTrue(secondMust.isBool());
    var notBool = secondMust.bool();
    assertFalse(notBool.mustNot().isEmpty());
    assertEquals(1, notBool.mustNot().size());
    assertIsUnquotedTerm(notBool.mustNot().get(0), "unwanted", "search_tittel");
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
    assertIsQuotedTerm(orBool.should().get(0), "Oslo kommune", "search_tittel");
    assertIsQuotedTerm(orBool.should().get(1), "Bergen kommune", "search_tittel");

    // budsjett
    assertIsUnquotedTerm(boolQuery.must().get(1), "budsjett", "search_tittel");

    // -skatt
    var lastMust = boolQuery.must().get(2);
    assertTrue(lastMust.isBool());
    var notClause = lastMust.bool();
    assertFalse(notClause.mustNot().isEmpty());
    assertEquals(1, notClause.mustNot().size());
    assertIsUnquotedTerm(notClause.mustNot().get(0), "skatt", "search_tittel");
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
    assertIsUnquotedTerm(boolQuery.must().get(0), "foo", "search_tittel");

    // (bar | baz)
    var middleMust = boolQuery.must().get(1);
    assertTrue(middleMust.isBool());
    var orBool = middleMust.bool();
    assertEquals("1", orBool.minimumShouldMatch());
    assertEquals(2, orBool.should().size());
    assertIsUnquotedTerm(orBool.should().get(0), "bar", "search_tittel");
    assertIsUnquotedTerm(orBool.should().get(1), "baz", "search_tittel");

    // "exact"
    assertIsQuotedTerm(boolQuery.must().get(2), "exact", "search_tittel");
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
    assertIsQuotedTerm(orClause.should().get(0), "phrase one", "search_tittel");
    assertIsQuotedTerm(orClause.should().get(1), "phrase two", "search_tittel");

    // "required phrase"
    assertIsQuotedTerm(boolQuery.must().get(1), "required phrase", "search_tittel");
  }

  @Test
  void testTripleNestedUnwrap() {
    var query = SearchQueryParser.parse("(((foo)))", List.of("search_tittel"));
    assertNotNull(query);
    assertIsUnquotedTerm(query, "foo", "search_tittel");
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
    assertIsUnquotedTerm(firstAnd.must().get(0), "foo", "search_tittel");
    assertIsUnquotedTerm(firstAnd.must().get(1), "bar", "search_tittel");

    // baz -qux (AND with NOT)
    assertTrue(boolQuery.should().get(1).isBool());
    var secondAnd = boolQuery.should().get(1).bool();
    assertEquals(2, secondAnd.must().size());
    assertIsUnquotedTerm(secondAnd.must().get(0), "baz", "search_tittel");
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
    assertIsUnquotedTerm(boolQuery.must().get(0), "bud*", "search_tittel");
    assertIsQuotedTerm(boolQuery.must().get(1), "exact phrase", "search_tittel");
  }

  @Test
  void testParseWithNorwegianCharacters() {
    var query =
        SearchQueryParser.parse("\"søknad om innsyn\" arkivskaper", List.of("search_tittel"));
    assertNotNull(query);
    assertTrue(query.isBool());
    var boolQuery = query.bool();
    assertEquals(2, boolQuery.must().size());
    assertIsQuotedTerm(boolQuery.must().get(0), "søknad om innsyn", "search_tittel");
    assertIsUnquotedTerm(boolQuery.must().get(1), "arkivskaper", "search_tittel");
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
    assertIsQuotedTerm(firstOr.should().get(0), "søknad om innsyn", "search_tittel");
    assertIsUnquotedTerm(firstOr.should().get(1), "arkivskaper", "search_tittel");

    // (Trøndelag | Oslo)
    assertTrue(boolQuery.must().get(1).isBool());
    var secondOr = boolQuery.must().get(1).bool();
    assertEquals("1", secondOr.minimumShouldMatch());
    assertEquals(2, secondOr.should().size());
    assertIsUnquotedTerm(secondOr.should().get(0), "Trøndelag", "search_tittel");
    assertIsUnquotedTerm(secondOr.should().get(1), "Oslo", "search_tittel");
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
    assertIsQuotedTerm(boolQuery.should().get(0), "foo", "search_tittel");

    // "bar" + "baz" (AND)
    assertTrue(boolQuery.should().get(1).isBool());
    var andBranch = boolQuery.should().get(1).bool();
    assertEquals(2, andBranch.must().size());
    assertIsQuotedTerm(andBranch.must().get(0), "bar", "search_tittel");
    assertIsQuotedTerm(andBranch.must().get(1), "baz", "search_tittel");
  }

  @Test
  void testQuotedPhrasesAndActualUnquotedText() {
    var query = SearchQueryParser.parse("\"exact\" regular words", List.of("search_tittel"));
    assertNotNull(query);
    assertTrue(query.isBool());
    var boolQuery = query.bool();
    assertEquals(3, boolQuery.must().size());
    assertIsQuotedTerm(boolQuery.must().get(0), "exact", "search_tittel");
    assertIsUnquotedTerm(boolQuery.must().get(1), "regular", "search_tittel");
    assertIsUnquotedTerm(boolQuery.must().get(2), "words", "search_tittel");
  }

  @Test
  void testMultipleFieldsWithBoosts() {
    var query =
        SearchQueryParser.parse("foo + \"bar\"", List.of("search_tittel^3.0", "search_innhold"));
    assertNotNull(query);
    assertTrue(query.isBool());

    var boolQuery = query.bool();
    assertEquals(2, boolQuery.must().size());

    assertIsUnquotedTerm(boolQuery.must().get(0), "foo", "search_tittel^3.0", "search_innhold");
    assertIsQuotedTerm(boolQuery.must().get(1), "bar", "search_tittel^3.0", "search_innhold");
  }

  @Test
  void testConsecutiveOperators() {
    // Edge case: consecutive operators where first operator in sequence acts as operator,
    // and subsequent ones become search terms (like C++)
    // This parses as: (foo AND + AND NOT -) OR (| AND baz)
    var query = SearchQueryParser.parse("foo ++ -- || baz", List.of("search_tittel"));
    assertNotNull(query);
    assertTrue(query.isBool());

    var boolQuery = query.bool();
    assertEquals("1", boolQuery.minimumShouldMatch());
    assertEquals(2, boolQuery.should().size());

    // First OR clause: (foo AND + AND NOT -)
    var firstClause = boolQuery.should().get(0);
    assertTrue(firstClause.isBool());
    var firstBool = firstClause.bool();
    assertEquals(3, firstBool.must().size());
    assertIsUnquotedTerm(firstBool.must().get(0), "foo", "search_tittel");
    assertIsUnquotedTerm(firstBool.must().get(1), "+", "search_tittel");
    // Third must clause contains the NOT - (wrapped in a bool with must_not)
    var notClause = firstBool.must().get(2);
    assertTrue(notClause.isBool());
    var notBool = notClause.bool();
    assertEquals(1, notBool.mustNot().size());
    assertIsUnquotedTerm(notBool.mustNot().get(0), "-", "search_tittel");

    // Second OR clause: (| AND baz)
    var secondClause = boolQuery.should().get(1);
    assertTrue(secondClause.isBool());
    var secondBool = secondClause.bool();
    assertEquals(2, secondBool.must().size());
    assertIsUnquotedTerm(secondBool.must().get(0), "|", "search_tittel");
    assertIsUnquotedTerm(secondBool.must().get(1), "baz", "search_tittel");
  }
}

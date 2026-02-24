package no.einnsyn.backend.common.search;

import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.SimpleQueryStringQuery;
import java.util.List;
import no.einnsyn.backend.common.search.parser.QueryParser;
import no.einnsyn.backend.common.search.parser.QueryTokenizer;
import no.einnsyn.backend.common.search.parser.QueryTransformer;

/**
 * Parser that intelligently routes quoted phrases to .exact fields and unquoted terms to .loose
 * fields for better search precision.
 *
 * <p><strong>IMPORTANT:</strong> This parser requires that all Elasticsearch properties have both
 * .exact and .loose multi-fields configured. It will not work correctly with fields that don't have
 * these suffixes.
 *
 * <p>This parser supports full boolean query syntax with proper precedence and nesting:
 *
 * <ul>
 *   <li>Quoted phrases → .exact fields (no stemming/synonyms)
 *   <li>Unquoted words → .loose fields (with stemming/synonyms)
 *   <li>Operators: + (AND), | (OR), - (NOT), () (grouping)
 *   <li>Implicit AND between adjacent terms
 * </ul>
 *
 * <p>Operator precedence (highest to lowest):
 *
 * <ol>
 *   <li>Parentheses
 *   <li>NOT (-)
 *   <li>AND (+) and implicit AND
 *   <li>OR (|)
 * </ol>
 *
 * <p>Usage example:
 *
 * <pre>
 * Query query = searchQueryParser.parse(
 *   "(foo | \"bar\") + \"baz\"",
 *   List.of("search_tittel", "search_innhold")
 * );
 * </pre>
 */
public final class SearchQueryParser {

  private SearchQueryParser() {}

  /**
   * Parse a query string and build an Elasticsearch query that routes quoted phrases to .exact
   * fields and unquoted terms to .loose fields.
   *
   * <p><strong>Note:</strong> All fields must have .exact and .loose multi-fields configured.
   *
   * @param queryString the raw query string from the user
   * @param baseFields the base field names (without .exact or .loose suffixes)
   * @return an Elasticsearch Query object
   */
  public static Query parse(String queryString, List<String> baseFields) {
    return parse(queryString, baseFields, 2.0f, 1.0f);
  }

  /**
   * Parse a query string and build an Elasticsearch query with custom boosting for exact and loose
   * matches.
   *
   * <p><strong>Note:</strong> All fields must have .exact and .loose multi-fields configured.
   *
   * @param queryString the raw query string from the user
   * @param baseFields the base field names (without .exact or .loose suffixes)
   * @param exactBoost boost factor for exact phrase matches (default: 1.0)
   * @param looseBoost boost factor for loose term matches (default: 1.0)
   * @return an Elasticsearch Query object
   */
  public static Query parse(
      String queryString, List<String> baseFields, float exactBoost, float looseBoost) {
    if (queryString == null || queryString.isBlank()) {
      return Query.of(q -> q.matchAll(m -> m));
    }

    try {
      // Tokenize
      var tokenizer = new QueryTokenizer(queryString);
      var tokens = tokenizer.tokenize();

      // Parse into abstract syntax tree (AST)
      var parser = new QueryParser(tokens);
      var ast = parser.parse();

      // Transform to Elasticsearch query
      var transformer = new QueryTransformer(baseFields, exactBoost, looseBoost);
      return transformer.transform(ast);
    } catch (Exception _) {
      // Fallback to simple query string on error
      return buildFallbackQuery(queryString, baseFields);
    }
  }

  /**
   * Fallback query when parsing fails - uses SimpleQueryString on base fields.
   *
   * @param queryString the query string
   * @param baseFields the base fields
   * @return a simple query string query
   */
  private static Query buildFallbackQuery(String queryString, List<String> baseFields) {
    return SimpleQueryStringQuery.of(
            s ->
                s.query(queryString).fields(baseFields).defaultOperator(Operator.And).lenient(true))
        ._toQuery();
  }
}

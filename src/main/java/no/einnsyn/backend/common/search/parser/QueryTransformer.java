package no.einnsyn.backend.common.search.parser;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.SimpleQueryStringQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import java.util.List;

/**
 * Transforms a query AST into Elasticsearch queries with intelligent field routing.
 *
 * <p>Routing strategy:
 *
 * <ul>
 *   <li>Quoted phrases (isPhrase=true) → .exact fields (no stemming/synonyms)
 *   <li>Unquoted words (isPhrase=false) → .loose fields (with stemming/synonyms)
 * </ul>
 */
public class QueryTransformer {

  private final List<String> baseFields;
  private final float exactBoost;
  private final float looseBoost;

  public QueryTransformer(List<String> baseFields, float exactBoost, float looseBoost) {
    this.baseFields = baseFields;
    this.exactBoost = exactBoost;
    this.looseBoost = looseBoost;
  }

  /** Transform an AST node into an Elasticsearch Query. */
  public Query transform(QueryNode node) {
    if (node == null) {
      return Query.of(q -> q.matchAll(m -> m));
    }

    return switch (node) {
      case QueryNode.TermNode term -> transformTerm(term);
      case QueryNode.AndNode and -> transformAnd(and);
      case QueryNode.OrNode or -> transformOr(or);
      case QueryNode.NotNode not -> transformNot(not);
      default -> throw new IllegalArgumentException("Unknown node type: " + node.getClass());
    };
  }

  private Query transformTerm(QueryNode.TermNode term) {
    if (term.value() == null || term.value().isBlank()) {
      return Query.of(q -> q.matchAll(m -> m));
    }

    if (term.isPhrase()) {
      // Quoted phrase → .exact fields
      var exactFields = buildFieldsWithSuffix(baseFields, "exact");
      return buildPhraseQuery(term.value(), exactFields, exactBoost);
    } else {
      // Unquoted word → .loose fields with wildcard support
      var looseFields = buildFieldsWithSuffix(baseFields, "loose");
      return buildWordQuery(term.value(), looseFields, looseBoost);
    }
  }

  private Query transformAnd(QueryNode.AndNode and) {
    var boolQuery = new BoolQuery.Builder();

    for (var child : and.children()) {
      // TODO: This could be optimized with .filter() if we sort by anything other than _score.
      boolQuery.must(transform(child));
    }

    return boolQuery.build()._toQuery();
  }

  private Query transformOr(QueryNode.OrNode or) {
    var boolQuery = new BoolQuery.Builder();
    boolQuery.minimumShouldMatch("1");

    for (var child : or.children()) {
      boolQuery.should(transform(child));
    }

    return boolQuery.build()._toQuery();
  }

  private Query transformNot(QueryNode.NotNode not) {
    var boolQuery = new BoolQuery.Builder();
    boolQuery.mustNot(transform(not.child()));
    return boolQuery.build()._toQuery();
  }

  private Query buildPhraseQuery(String phrase, List<String> fields, float boost) {
    return MultiMatchQuery.of(
            m -> m.query(phrase).fields(fields).type(TextQueryType.Phrase).boost(boost))
        ._toQuery();
  }

  private Query buildWordQuery(String word, List<String> fields, float boost) {
    // Use SimpleQueryString to support wildcards (*)
    return SimpleQueryStringQuery.of(
            s -> s.query(word).fields(fields).analyzeWildcard(true).boost(boost).lenient(true))
        ._toQuery();
  }

  private List<String> buildFieldsWithSuffix(List<String> baseFields, String suffix) {
    return baseFields.stream()
        .map(
            field -> {
              var parts = field.split("\\^");
              var fieldName = parts[0];
              var boostPart = parts.length > 1 ? "^" + parts[1] : "";
              return fieldName + "." + suffix + boostPart;
            })
        .toList();
  }
}

package no.einnsyn.backend.common.search.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Recursive descent parser for query strings.
 *
 * <p>Grammar:
 *
 * <pre>
 * query                 := orExpr
 * orExpr                := andExpr ('|' andExpr)*
 * andExpr               := optionallyNegatedTerm ('+' optionallyNegatedTerm | optionallyNegatedTerm)*
 * optionallyNegatedTerm := '-' term | term
 * term                  := '(' query ')' | PHRASE | WORD
 * </pre>
 *
 * <p>Operator precedence (highest to lowest):
 *
 * <ol>
 *   <li>Parentheses
 *   <li>NOT (-)
 *   <li>AND (+) and implicit AND
 *   <li>OR (|)
 * </ol>
 */
public class QueryParser {

  private final List<QueryToken> tokens;
  private int position = -1;
  private QueryToken currentToken;

  public QueryParser(List<QueryToken> tokens) {
    this.tokens = tokens;
    nextToken();
  }

  /** Parse the token stream into an AST. */
  public QueryNode parse() {
    if (tokens.isEmpty() || tokens.get(0).getType() == QueryToken.Type.EOF) {
      return null;
    }

    return parseOrExpr();
  }

  /** Parse OR expression: andExpr ('|' andExpr)* */
  private QueryNode parseOrExpr() {
    var left = parseAndExpr();
    if (left == null) {
      return null;
    }

    var children = new ArrayList<QueryNode>();
    children.add(left);

    while (currentToken.getType() == QueryToken.Type.OR) {
      nextToken(); // consume '|'
      var right = parseAndExpr();
      if (right != null) {
        children.add(right);
      }
    }

    return children.size() == 1 ? children.get(0) : new QueryNode.OrNode(children);
  }

  /**
   * Parse AND expression: optionallyNegatedTerm ('+' optionallyNegatedTerm |
   * optionallyNegatedTerm)*
   */
  private QueryNode parseAndExpr() {
    var left = parseOptionallyNegatedTerm();
    if (left == null) {
      return null;
    }

    var children = new ArrayList<QueryNode>();
    children.add(left);

    while (true) {
      // Skip AND operator(s) '+'
      while (currentToken.getType() == QueryToken.Type.AND) {
        nextToken(); // consume '+'
      }

      // Try to parse next term (implicit or explicit AND)
      var right = parseOptionallyNegatedTerm();
      if (right == null) {
        break;
      }
      children.add(right);
    }

    return children.size() == 1 ? children.get(0) : new QueryNode.AndNode(children);
  }

  /** Parse optionally negated term: '-' term | term */
  private QueryNode parseOptionallyNegatedTerm() {
    // Check if there's at least one NOT operator (treat consecutive NOTs as single NOT)
    var hasNot = false;
    while (currentToken.getType() == QueryToken.Type.NOT) {
      hasNot = true;
      nextToken(); // consume '-'
    }

    var child = parseTerm();
    if (child == null) {
      return null;
    }

    // Wrap in NotNode if there was a NOT operator
    return hasNot ? new QueryNode.NotNode(child) : child;
  }

  /** Parse term: '(' query ')' | PHRASE | WORD */
  private QueryNode parseTerm() {
    return switch (currentToken.getType()) {
      case LPAREN -> {
        nextToken(); // consume '('
        var expr = parseOrExpr();
        // The tokenizer ensures that we have a matching ')'
        nextToken(); // consume ')'
        yield expr;
      }

      case PHRASE -> {
        var node = new QueryNode.TermNode(currentToken.getValue(), true);
        nextToken();
        yield node;
      }

      case WORD -> {
        var node = new QueryNode.TermNode(currentToken.getValue(), false);
        nextToken();
        yield node;
      }

      default -> null;
    };
  }

  private void nextToken() {
    if (position < tokens.size() - 1) {
      position++;
      currentToken = tokens.get(position);
    }
  }
}

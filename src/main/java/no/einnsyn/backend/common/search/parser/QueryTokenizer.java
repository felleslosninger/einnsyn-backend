package no.einnsyn.backend.common.search.parser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Tokenizes a query string into a stream of tokens. */
public class QueryTokenizer {

  private final String input;
  private final Map<Integer, Integer> quotePositions;
  private final Set<Integer> parenPositions;
  private int position = 0;

  public QueryTokenizer(String input) {
    this.input = input != null ? input : "";
    this.quotePositions = findMatchedQuotes(this.input);
    this.parenPositions = findMatchedParens(this.input);
  }

  /** Tokenize the entire input string. */
  public List<QueryToken> tokenize() {
    position = 0;
    var tokens = new ArrayList<QueryToken>();

    while (position < input.length()) {
      skipWhitespace();
      if (position >= input.length()) {
        break;
      }

      var ch = input.charAt(position);

      // Quoted phrase
      if (ch == '"' && quotePositions.containsKey(position)) {
        tokens.add(readQuotedPhrase());
      }
      // Operators and parentheses
      else if (ch == '+' && shouldTreatAsOperator()) {
        tokens.add(new QueryToken(QueryToken.Type.AND, "+", position));
        position++;
      } else if (ch == '-' && shouldTreatAsOperator()) {
        tokens.add(new QueryToken(QueryToken.Type.NOT, "-", position));
        position++;
      } else if (ch == '|' && shouldTreatAsOperator()) {
        tokens.add(new QueryToken(QueryToken.Type.OR, "|", position));
        position++;
      } else if (ch == '(') {
        if (parenPositions.contains(position)) {
          tokens.add(new QueryToken(QueryToken.Type.LPAREN, "(", position));
        }
        position++;
      } else if (ch == ')') {
        if (parenPositions.contains(position)) {
          tokens.add(new QueryToken(QueryToken.Type.RPAREN, ")", position));
        }
        position++;
      }
      // Word
      else {
        tokens.add(readWord());
      }
    }

    tokens.add(new QueryToken(QueryToken.Type.EOF, "", position));
    return tokens;
  }

  private void skipWhitespace() {
    while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
      position++;
    }
  }

  /**
   * Check if the current position should be treated as an operator.
   *
   * <p>Operators (+, -, |) are only treated as operators when:
   *
   * <ul>
   *   <li>At the start of the input
   *   <li>Preceded by whitespace
   *   <li>Preceded by an opening parenthesis
   *   <li>Preceded by a closing parenthesis or quote (for operator chaining)
   * </ul>
   *
   * <p>This allows hyphenated terms like "foo-bar" or "ID-12345" to be treated as single words,
   * while still supporting compact operator syntax like "(foo)|bar" or "foo"+bar.
   */
  private boolean shouldTreatAsOperator() {
    // At start of input
    if (position == 0) {
      return true;
    }

    char prevChar = input.charAt(position - 1);

    // After whitespace, parentheses, or quotes
    return Character.isWhitespace(prevChar)
        || prevChar == '('
        || prevChar == ')'
        || prevChar == '"';
  }

  /** Find positions of all matched open- and close-quotes. */
  private Map<Integer, Integer> findMatchedQuotes(String input) {
    var matchedQuotePositions = new HashMap<Integer, Integer>();
    var openPos = -1;
    for (var i = 0; i < input.length(); i++) {
      if (input.charAt(i) != '"') {
        continue;
      }
      if (openPos == -1) {
        // A quote can open at start, or after whitespace or '('
        var validStart =
            i == 0 || Character.isWhitespace(input.charAt(i - 1)) || input.charAt(i - 1) == '(';
        if (validStart) {
          openPos = i;
        }
      } else {
        // Closing quote found — map open → close
        matchedQuotePositions.put(openPos, i);
        openPos = -1;
      }
    }
    return matchedQuotePositions;
  }

  /** Find positions of all matched open- and close-parentheses, skipping quoted regions. */
  private Set<Integer> findMatchedParens(String input) {
    var matchedParenPositions = new HashSet<Integer>();
    var stack = new ArrayDeque<Integer>();
    var i = 0;
    while (i < input.length()) {
      // Skip matched quoted regions
      if (quotePositions.containsKey(i)) {
        i = quotePositions.get(i) + 1;
        continue;
      }
      var ch = input.charAt(i);
      if (ch == '(') {
        stack.push(i);
      } else if (ch == ')' && !stack.isEmpty()) {
        matchedParenPositions.add(stack.pop());
        matchedParenPositions.add(i);
      }
      i++;
    }
    return matchedParenPositions;
  }

  private QueryToken readQuotedPhrase() {
    var openPos = position;
    var closePos = quotePositions.get(openPos);
    var phrase = input.substring(openPos + 1, closePos);
    position = closePos + 1;
    return new QueryToken(QueryToken.Type.PHRASE, phrase, openPos);
  }

  private QueryToken readWord() {
    var start = position;
    var word = new StringBuilder();

    while (position < input.length()) {
      char ch = input.charAt(position);
      // Break on whitespace or parentheses
      // Note: We no longer break on +, -, | to allow them within words (e.g., "foo-bar", "C++")
      if (Character.isWhitespace(ch) || ch == '(' || ch == ')') {
        break;
      }
      word.append(ch);
      position++;
    }

    return new QueryToken(QueryToken.Type.WORD, word.toString(), start);
  }
}

package no.einnsyn.backend.common.search.parser;

import java.util.ArrayList;
import java.util.List;

/** Tokenizes a query string into a stream of tokens. */
public class QueryTokenizer {

  private final String input;
  private int position = 0;

  public QueryTokenizer(String input) {
    this.input = input != null ? input : "";
  }

  /** Tokenize the entire input string. */
  public List<QueryToken> tokenize() {
    var tokens = new ArrayList<QueryToken>();

    while (position < input.length()) {
      skipWhitespace();
      if (position >= input.length()) {
        break;
      }

      var ch = input.charAt(position);

      // Quoted phrase
      if (ch == '"' && canStartQuotedPhrase()) {
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
      } else if (ch == '(' && hasMatchingClosingParen()) {
        tokens.add(new QueryToken(QueryToken.Type.LPAREN, "(", position));
        position++;
      } else if (ch == ')' && hasMatchingOpeningParen()) {
        tokens.add(new QueryToken(QueryToken.Type.RPAREN, ")", position));
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
    return Character.isWhitespace(prevChar) || prevChar == '(' || prevChar == ')' || prevChar == '"';
  }

  private boolean canStartQuotedPhrase() {
    // Check if preceded by valid delimiter
    if (position > 0) {
      char prevChar = input.charAt(position - 1);
      if (!Character.isWhitespace(prevChar) && prevChar != '(') {
        return false;
      }
    }

    // Check if there's a closing quote
    var searchPos = position + 1;
    while (searchPos < input.length()) {
      if (input.charAt(searchPos) == '"') {
        return true;
      }
      searchPos++;
    }
    return false;
  }

  private boolean hasMatchingClosingParen() {
    int depth = 0;
    var searchPos = position;

    while (searchPos < input.length()) {
      var ch = input.charAt(searchPos);
      if (ch == '(') {
        depth++;
      } else if (ch == ')') {
        depth--;
        if (depth == 0) {
          return true;
        }
      }
      searchPos++;
    }
    return false;
  }

  private boolean hasMatchingOpeningParen() {
    int depth = 0;
    var searchPos = position;

    while (searchPos >= 0) {
      var ch = input.charAt(searchPos);
      if (ch == ')') {
        depth++;
      } else if (ch == '(') {
        depth--;
        if (depth == 0) {
          return true;
        }
      }
      searchPos--;
    }
    return false;
  }

  private QueryToken readQuotedPhrase() {
    var start = position;
    position++; // Skip opening quote

    var phrase = new StringBuilder();
    while (position < input.length() && input.charAt(position) != '"') {
      phrase.append(input.charAt(position));
      position++;
    }

    if (position < input.length()) {
      position++; // Skip closing quote
    }

    return new QueryToken(QueryToken.Type.PHRASE, phrase.toString(), start);
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

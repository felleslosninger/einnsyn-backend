package no.einnsyn.backend.common.search.parser;

/** Represents a token in a query string. */
public record QueryToken(Type type, String value, int position) {
  public enum Type {
    WORD, // Unquoted word
    PHRASE, // Quoted phrase
    AND, // + operator
    OR, // | operator
    NOT, // - operator
    LPAREN, // (
    RPAREN, // )
    EOF // End of input
  }
}

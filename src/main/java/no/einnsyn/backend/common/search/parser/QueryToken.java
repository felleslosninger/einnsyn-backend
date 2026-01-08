package no.einnsyn.backend.common.search.parser;

import lombok.Getter;

/** Represents a token in a query string. */
public class QueryToken {
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

  @Getter private final Type type;
  @Getter private final String value;
  @Getter private final int position;

  public QueryToken(Type type, String value, int position) {
    this.type = type;
    this.value = value;
    this.position = position;
  }
}

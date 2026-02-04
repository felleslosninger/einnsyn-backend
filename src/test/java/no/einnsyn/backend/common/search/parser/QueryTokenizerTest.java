package no.einnsyn.backend.common.search.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class QueryTokenizerTest {

  @ParameterizedTest
  @CsvSource({
    // Normal tokens
    "hello,WORD,hello",
    "bud*get,WORD,bud*get",
    // Valid quoted phrases
    "'\"hello world\"',PHRASE,'hello world'",
    "'\"søknad om innsyn\"',PHRASE,'søknad om innsyn'",
    // Literal quotes (no space before quote, unclosed quotes, etc.)
    "foo\"bar\",WORD,'foo\"bar\"'",
    "'\"unclosed',WORD,'\"unclosed'",
    "hello\"world\",WORD,'hello\"world\"'",
    "wo\"rd,WORD,'wo\"rd'",
    "foo\",WORD,'foo\"'",
    "\",WORD,'\"'"
  })
  void testSingleToken(String input, String type, String expectedValue) {
    var tokenizer = new QueryTokenizer(input);
    var tokens = tokenizer.tokenize();

    assertEquals(2, tokens.size());
    assertEquals(QueryToken.Type.valueOf(type), tokens.get(0).type());
    assertEquals(expectedValue, tokens.get(0).value());
    assertEquals(QueryToken.Type.EOF, tokens.get(1).type());
  }

  @Test
  void testQuotedPhraseWithSpaceBefore() {
    var tokenizer = new QueryTokenizer("foo \"bar baz\"");
    var tokens = tokenizer.tokenize();

    assertEquals(3, tokens.size());
    assertEquals(QueryToken.Type.WORD, tokens.get(0).type());
    assertEquals("foo", tokens.get(0).value());
    assertEquals(QueryToken.Type.PHRASE, tokens.get(1).type());
    assertEquals("bar baz", tokens.get(1).value());
    assertEquals(QueryToken.Type.EOF, tokens.get(2).type());
  }

  @Test
  void testUnclosedQuoteAfterWord_TreatedAsLiteral() {
    // foo "unclosed should tokenize as two words with quote as literal
    var tokenizer = new QueryTokenizer("foo \"unclosed");
    var tokens = tokenizer.tokenize();

    assertEquals(3, tokens.size());
    assertEquals(QueryToken.Type.WORD, tokens.get(0).type());
    assertEquals("foo", tokens.get(0).value());
    assertEquals(QueryToken.Type.WORD, tokens.get(1).type());
    assertEquals("\"unclosed", tokens.get(1).value());
    assertEquals(QueryToken.Type.EOF, tokens.get(2).type());
  }

  @Test
  void testOperators() {
    var tokenizer = new QueryTokenizer("foo + bar | baz - qux");
    var tokens = tokenizer.tokenize();

    assertEquals(8, tokens.size());
    assertEquals(QueryToken.Type.WORD, tokens.get(0).type());
    assertEquals("foo", tokens.get(0).value());
    assertEquals(QueryToken.Type.AND, tokens.get(1).type());
    assertEquals(QueryToken.Type.WORD, tokens.get(2).type());
    assertEquals("bar", tokens.get(2).value());
    assertEquals(QueryToken.Type.OR, tokens.get(3).type());
    assertEquals(QueryToken.Type.WORD, tokens.get(4).type());
    assertEquals("baz", tokens.get(4).value());
    assertEquals(QueryToken.Type.NOT, tokens.get(5).type());
    assertEquals(QueryToken.Type.WORD, tokens.get(6).type());
    assertEquals("qux", tokens.get(6).value());
    assertEquals(QueryToken.Type.EOF, tokens.get(7).type());
  }

  @Test
  void testParentheses() {
    var tokenizer = new QueryTokenizer("(foo | bar)");
    var tokens = tokenizer.tokenize();

    assertEquals(6, tokens.size());
    assertEquals(QueryToken.Type.LPAREN, tokens.get(0).type());
    assertEquals(QueryToken.Type.WORD, tokens.get(1).type());
    assertEquals("foo", tokens.get(1).value());
    assertEquals(QueryToken.Type.OR, tokens.get(2).type());
    assertEquals(QueryToken.Type.WORD, tokens.get(3).type());
    assertEquals("bar", tokens.get(3).value());
    assertEquals(QueryToken.Type.RPAREN, tokens.get(4).type());
    assertEquals(QueryToken.Type.EOF, tokens.get(5).type());
  }

  @Test
  void testComplexQuery() {
    var tokenizer = new QueryTokenizer("(\"exact phrase\" | loose) + required");
    var tokens = tokenizer.tokenize();

    assertEquals(8, tokens.size());
    assertEquals(QueryToken.Type.LPAREN, tokens.get(0).type());
    assertEquals(QueryToken.Type.PHRASE, tokens.get(1).type());
    assertEquals("exact phrase", tokens.get(1).value());
    assertEquals(QueryToken.Type.OR, tokens.get(2).type());
    assertEquals(QueryToken.Type.WORD, tokens.get(3).type());
    assertEquals("loose", tokens.get(3).value());
    assertEquals(QueryToken.Type.RPAREN, tokens.get(4).type());
    assertEquals(QueryToken.Type.AND, tokens.get(5).type());
    assertEquals(QueryToken.Type.WORD, tokens.get(6).type());
    assertEquals("required", tokens.get(6).value());
    assertEquals(QueryToken.Type.EOF, tokens.get(7).type());
  }

  @ParameterizedTest
  @CsvSource(
      value = {"''", "null", "'   \t  \n  '"},
      nullValues = "null")
  void testEmptyInput(String input) {
    var tokenizer = new QueryTokenizer(input);
    var tokens = tokenizer.tokenize();

    assertEquals(1, tokens.size());
    assertEquals(QueryToken.Type.EOF, tokens.get(0).type());
  }

  @Test
  void testMultipleQuotedPhrases() {
    var tokenizer = new QueryTokenizer("\"first phrase\" \"second phrase\"");
    var tokens = tokenizer.tokenize();

    assertEquals(3, tokens.size());
    assertEquals(QueryToken.Type.PHRASE, tokens.get(0).type());
    assertEquals("first phrase", tokens.get(0).value());
    assertEquals(QueryToken.Type.PHRASE, tokens.get(1).type());
    assertEquals("second phrase", tokens.get(1).value());
    assertEquals(QueryToken.Type.EOF, tokens.get(2).type());
  }

  @Test
  void testNorwegianCharacters() {
    var tokenizer = new QueryTokenizer("\"søknad om innsyn\" arkivskaper");
    var tokens = tokenizer.tokenize();

    assertEquals(3, tokens.size());
    assertEquals(QueryToken.Type.PHRASE, tokens.get(0).type());
    assertEquals("søknad om innsyn", tokens.get(0).value());
    assertEquals(QueryToken.Type.WORD, tokens.get(1).type());
    assertEquals("arkivskaper", tokens.get(1).value());
    assertEquals(QueryToken.Type.EOF, tokens.get(2).type());
  }

  @Test
  void testConsecutiveOperators() {
    // Operators without spaces are treated as part of the word
    var tokenizer = new QueryTokenizer("foo+-bar");
    var tokens = tokenizer.tokenize();

    assertEquals(2, tokens.size());
    assertEquals(QueryToken.Type.WORD, tokens.get(0).type());
    assertEquals("foo+-bar", tokens.get(0).value());
    assertEquals(QueryToken.Type.EOF, tokens.get(1).type());
  }

  @Test
  void testConsecutiveOperatorsWithSpaces() {
    // Operators with spaces are treated as operators
    var tokenizer = new QueryTokenizer("foo + -bar");
    var tokens = tokenizer.tokenize();

    assertEquals(5, tokens.size());
    assertEquals(QueryToken.Type.WORD, tokens.get(0).type());
    assertEquals("foo", tokens.get(0).value());
    assertEquals(QueryToken.Type.AND, tokens.get(1).type());
    assertEquals(QueryToken.Type.NOT, tokens.get(2).type());
    assertEquals(QueryToken.Type.WORD, tokens.get(3).type());
    assertEquals("bar", tokens.get(3).value());
    assertEquals(QueryToken.Type.EOF, tokens.get(4).type());
  }

  @Test
  void testHyphenatedTerms() {
    // Hyphenated terms like IDs should be treated as single words
    var tokenizer = new QueryTokenizer("externalId-12345");
    var tokens = tokenizer.tokenize();

    assertEquals(2, tokens.size());
    assertEquals(QueryToken.Type.WORD, tokens.get(0).type());
    assertEquals("externalId-12345", tokens.get(0).value());
    assertEquals(QueryToken.Type.EOF, tokens.get(1).type());
  }

  @Test
  void testHyphenatedTermsWithSpaces() {
    // Hyphens with spaces should be treated as NOT operator
    var tokenizer = new QueryTokenizer("foo - bar");
    var tokens = tokenizer.tokenize();

    assertEquals(4, tokens.size());
    assertEquals(QueryToken.Type.WORD, tokens.get(0).type());
    assertEquals("foo", tokens.get(0).value());
    assertEquals(QueryToken.Type.NOT, tokens.get(1).type());
    assertEquals(QueryToken.Type.WORD, tokens.get(2).type());
    assertEquals("bar", tokens.get(2).value());
    assertEquals(QueryToken.Type.EOF, tokens.get(3).type());
  }

  @Test
  void testMixedHyphenatedAndOperators() {
    // Mix of hyphenated terms and actual NOT operators
    var tokenizer = new QueryTokenizer("foo-bar -baz");
    var tokens = tokenizer.tokenize();

    assertEquals(4, tokens.size());
    assertEquals(QueryToken.Type.WORD, tokens.get(0).type());
    assertEquals("foo-bar", tokens.get(0).value());
    assertEquals(QueryToken.Type.NOT, tokens.get(1).type());
    assertEquals(QueryToken.Type.WORD, tokens.get(2).type());
    assertEquals("baz", tokens.get(2).value());
    assertEquals(QueryToken.Type.EOF, tokens.get(3).type());
  }

  @Test
  void testOperatorAfterParenthesis() {
    // Operators after opening parenthesis should be treated as operators
    var tokenizer = new QueryTokenizer("(-bar)");
    var tokens = tokenizer.tokenize();

    assertEquals(5, tokens.size());
    assertEquals(QueryToken.Type.LPAREN, tokens.get(0).type());
    assertEquals(QueryToken.Type.NOT, tokens.get(1).type());
    assertEquals(QueryToken.Type.WORD, tokens.get(2).type());
    assertEquals("bar", tokens.get(2).value());
    assertEquals(QueryToken.Type.RPAREN, tokens.get(3).type());
    assertEquals(QueryToken.Type.EOF, tokens.get(4).type());
  }

  @Test
  void testPlusInWords() {
    // Plus signs in words (like C++) should be preserved
    var tokenizer = new QueryTokenizer("C++");
    var tokens = tokenizer.tokenize();

    assertEquals(2, tokens.size());
    assertEquals(QueryToken.Type.WORD, tokens.get(0).type());
    assertEquals("C++", tokens.get(0).value());
    assertEquals(QueryToken.Type.EOF, tokens.get(1).type());
  }

  @Test
  void testOperatorAfterQuote() {
    // Operators after closing quotes should be treated as operators
    var tokenizer = new QueryTokenizer("\"foo\"|bar");
    var tokens = tokenizer.tokenize();

    assertEquals(4, tokens.size());
    assertEquals(QueryToken.Type.PHRASE, tokens.get(0).type());
    assertEquals("foo", tokens.get(0).value());
    assertEquals(QueryToken.Type.OR, tokens.get(1).type());
    assertEquals(QueryToken.Type.WORD, tokens.get(2).type());
    assertEquals("bar", tokens.get(2).value());
    assertEquals(QueryToken.Type.EOF, tokens.get(3).type());
  }

  @Test
  void testOperatorAfterClosingParenthesis() {
    // Operators after closing parenthesis should be treated as operators
    var tokenizer = new QueryTokenizer("(foo)+bar");
    var tokens = tokenizer.tokenize();

    assertEquals(6, tokens.size());
    assertEquals(QueryToken.Type.LPAREN, tokens.get(0).type());
    assertEquals(QueryToken.Type.WORD, tokens.get(1).type());
    assertEquals("foo", tokens.get(1).value());
    assertEquals(QueryToken.Type.RPAREN, tokens.get(2).type());
    assertEquals(QueryToken.Type.AND, tokens.get(3).type());
    assertEquals(QueryToken.Type.WORD, tokens.get(4).type());
    assertEquals("bar", tokens.get(4).value());
    assertEquals(QueryToken.Type.EOF, tokens.get(5).type());
  }
}

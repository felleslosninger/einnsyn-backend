// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.journalpost.models;

public enum JournalposttypeEnum {
  INNGAAENDE_DOKUMENT("inngaaende_dokument"),
  UTGAAENDE_DOKUMENT("utgaaende_dokument"),
  ORGANINTERNT_DOKUMENT_UTEN_OPPFOELGING("organinternt_dokument_uten_oppfoelging"),
  ORGANINTERNT_DOKUMENT_FOR_OPPFOELGING("organinternt_dokument_for_oppfoelging"),
  SAKSFRAMLEGG("saksframlegg"),
  SAKSKART("sakskart"),
  MOETEPROTOKOLL("moeteprotokoll"),
  MOETEBOK("moetebok"),
  UKJENT("ukjent");

  private final String value;

  JournalposttypeEnum(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public String toJson() {
    return value;
  }

  public static JournalposttypeEnum fromValue(String value) {
    value = value.toLowerCase();
    for (JournalposttypeEnum val : values()) {
      if (val.value.toLowerCase().equals(value)) {
        return val;
      }
    }
    throw new IllegalArgumentException("No enum constant for value: " + value);
  }
}

// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.enhet.models;

public enum EnhetstypeEnum {
  VIRKSOMHET("VIRKSOMHET"),
  UTVALG("UTVALG"),
  AVDELING("AVDELING"),
  ADMINISTRATIVENHET("ADMINISTRATIVENHET"),
  SEKSJON("SEKSJON"),
  BYDEL("BYDEL"),
  KOMMUNE("KOMMUNE");

  private final String value;

  EnhetstypeEnum(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public String toJson() {
    return value;
  }

  public static EnhetstypeEnum fromValue(String value) {
    for (EnhetstypeEnum val : values()) {
      if (val.value.equals(value)) {
        return val;
      }
    }
    throw new IllegalArgumentException("No enum constant for value: " + value);
  }
}

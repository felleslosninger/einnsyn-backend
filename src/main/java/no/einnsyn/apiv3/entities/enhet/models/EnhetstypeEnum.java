// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.enhet.models;

public enum EnhetstypeEnum {
  ADMINISTRATIVENHET("ADMINISTRATIVENHET"),
  AVDELING("AVDELING"),
  BYDEL("BYDEL"),
  DUMMYENHET("DUMMYENHET"),
  FYLKE("FYLKE"),
  KOMMUNE("KOMMUNE"),
  ORGAN("ORGAN"),
  SEKSJON("SEKSJON"),
  UTVALG("UTVALG"),
  VIRKSOMHET("VIRKSOMHET");

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
    value = value.toLowerCase();
    for (EnhetstypeEnum val : values()) {
      if (val.value.toLowerCase().equals(value)) {
        return val;
      }
    }
    throw new IllegalArgumentException("No enum constant for value: " + value);
  }
}

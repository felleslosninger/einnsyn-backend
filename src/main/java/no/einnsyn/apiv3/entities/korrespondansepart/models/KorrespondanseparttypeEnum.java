// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.korrespondansepart.models;

public enum KorrespondanseparttypeEnum {
  AVSENDER("avsender"),
  MOTTAKER("mottaker"),
  KOPIMOTTAKER("kopimottaker"),
  GRUPPEMOTTAKER("gruppemottaker"),
  INTERN_AVSENDER("intern_avsender"),
  INTERN_MOTTAKER("intern_mottaker"),
  INTERN_KOPIMOTTAKER("intern_kopimottaker");

  private final String value;

  KorrespondanseparttypeEnum(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public String toJson() {
    return value;
  }

  public static KorrespondanseparttypeEnum fromValue(String value) {
    value = value.toLowerCase();
    for (KorrespondanseparttypeEnum val : values()) {
      if (val.value.toLowerCase().equals(value)) {
        return val;
      }
    }
    throw new IllegalArgumentException("No enum constant for value: " + value);
  }
}

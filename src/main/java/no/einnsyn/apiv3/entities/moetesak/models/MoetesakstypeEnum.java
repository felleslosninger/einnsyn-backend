// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.moetesak.models;

public enum MoetesakstypeEnum {
  MOETE("moete"),
  POLITISK("politisk"),
  DELEGERT("delegert"),
  INTERPELLASJON("interpellasjon"),
  GODKJENNING("godkjenning"),
  ORIENTERING("orientering"),
  REFERAT("referat"),
  ANNET("annet");

  private final String value;

  MoetesakstypeEnum(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public String toJson() {
    return value;
  }

  public static MoetesakstypeEnum fromValue(String value) {
    value = value.toLowerCase();
    for (MoetesakstypeEnum val : values()) {
      if (val.value.toLowerCase().equals(value)) {
        return val;
      }
    }
    throw new IllegalArgumentException("No enum constant for value: " + value);
  }
}

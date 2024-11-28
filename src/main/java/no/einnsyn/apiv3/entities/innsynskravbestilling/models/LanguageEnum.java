// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.innsynskravbestilling.models;

public enum LanguageEnum {
  NB("nb"),
  NN("nn"),
  EN("en"),
  SE("se");

  private final String value;

  LanguageEnum(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public String toJson() {
    return value;
  }

  public static LanguageEnum fromValue(String value) {
    value = value.toLowerCase();
    for (LanguageEnum val : values()) {
      if (val.value.toLowerCase().equals(value)) {
        return val;
      }
    }
    throw new IllegalArgumentException("No enum constant for value: " + value);
  }
}

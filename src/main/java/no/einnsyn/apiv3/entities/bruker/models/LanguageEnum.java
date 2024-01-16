// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.bruker.models;

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
}

// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.votering.models;

public enum StemmeEnum {
  JA("Ja"),
  NEI("Nei"),
  BLANKT("Blankt");

  private final String value;

  StemmeEnum(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public String toJson() {
    return value;
  }

  public static StemmeEnum fromValue(String value) {
    value = value.toLowerCase();
    for (StemmeEnum val : values()) {
      if (val.value.toLowerCase().equals(value)) {
        return val;
      }
    }
    throw new IllegalArgumentException("No enum constant for value: " + value);
  }
}

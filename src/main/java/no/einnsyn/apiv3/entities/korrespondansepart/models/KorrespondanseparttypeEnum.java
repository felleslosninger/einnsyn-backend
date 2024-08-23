// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.korrespondansepart.models;

public enum KorrespondanseparttypeEnum {
  AVSENDER("avsender"),
  HTTP___WWW_ARKIVVERKET_NO_STANDARDER_NOARK5_ARKIVSTRUKTUR_AVSENDER(
      "http://www.arkivverket.no/standarder/noark5/arkivstruktur/avsender"),
  MOTTAKER("mottaker"),
  HTTP___WWW_ARKIVVERKET_NO_STANDARDER_NOARK5_ARKIVSTRUKTUR_MOTTAKER(
      "http://www.arkivverket.no/standarder/noark5/arkivstruktur/mottaker"),
  KOPIMOTTAKER("kopimottaker"),
  HTTP___WWW_ARKIVVERKET_NO_STANDARDER_NOARK5_ARKIVSTRUKTUR_KOPIMOTTAKER(
      "http://www.arkivverket.no/standarder/noark5/arkivstruktur/kopimottaker"),
  GRUPPEMOTTAKER("gruppemottaker"),
  HTTP___WWW_ARKIVVERKET_NO_STANDARDER_NOARK5_ARKIVSTRUKTUR_GRUPPEMOTTAKER(
      "http://www.arkivverket.no/standarder/noark5/arkivstruktur/gruppemottaker"),
  INTERN_AVSENDER("intern_avsender"),
  HTTP___WWW_ARKIVVERKET_NO_STANDARDER_NOARK5_ARKIVSTRUKTUR_INTERN_AVSENDER(
      "http://www.arkivverket.no/standarder/noark5/arkivstruktur/intern_avsender"),
  INTERN_MOTTAKER("intern_mottaker"),
  HTTP___WWW_ARKIVVERKET_NO_STANDARDER_NOARK5_ARKIVSTRUKTUR_INTERN_MOTTAKER(
      "http://www.arkivverket.no/standarder/noark5/arkivstruktur/intern_mottaker"),
  INTERN_KOPIMOTTAKER("intern_kopimottaker"),
  HTTP___WWW_ARKIVVERKET_NO_STANDARDER_NOARK5_ARKIVSTRUKTUR_INTERN_KOPIMOTTAKER(
      "http://www.arkivverket.no/standarder/noark5/arkivstruktur/intern_kopimottaker");

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

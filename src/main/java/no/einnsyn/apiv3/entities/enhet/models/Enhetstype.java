package no.einnsyn.apiv3.entities.enhet.models;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Enhetstype {
  // @formatter:off
  DUMMYENHET("DummyEnhet"),
  ADMINISTRATIVENHET("AdministrativEnhet"),
  AVDELING("Avdeling"),
  SEKSJON("Seksjon"),
  UTVALG("Utvalg"),
  BYDEL("Bydel"),
  KOMMUNE("Kommune"),
  FYLKE("Fylke"),
  ORGAN("Organ"),
  VIRKSOMHET("Virksomhet");
  // @formatter:on

  private final String enhetstype;

  Enhetstype(String enhetstype) {
    this.enhetstype = enhetstype;
  }

  @JsonValue
  @Override
  public String toString() {
    return enhetstype;
  }

  public boolean obligatoriskOrgnummer() {
    return this == BYDEL || this == KOMMUNE || this == FYLKE || this == ORGAN || this == VIRKSOMHET;
  }

  public boolean obligatoriskKontaktpunkt() {
    return obligatoriskOrgnummer();
  }

  public boolean obligatoriskInnsynskravEpost() {
    return obligatoriskOrgnummer();
  }

  public boolean obligatoriskEnhetskode() {
    return this == ADMINISTRATIVENHET || this == AVDELING || this == SEKSJON || this == UTVALG;
  }

  public boolean obligatoriskParent() {
    return this != DUMMYENHET;
  }

  public boolean frivilligKontaktpunkt() {
    return this != DUMMYENHET;
  }

  public boolean frivilligInnsynskravEpost() {
    return this != DUMMYENHET;
  }
}

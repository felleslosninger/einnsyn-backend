package no.einnsyn.apiv3.entities.enhet.models;

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

  private final String value;

  Enhetstype(String enhetstype) {
    this.value = enhetstype;
  }

  @Override
  public String toString() {
    return value;
  }

  public String toJson() {
    return value;
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

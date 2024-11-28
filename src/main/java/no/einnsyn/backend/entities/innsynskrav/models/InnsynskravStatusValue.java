package no.einnsyn.backend.entities.innsynskrav.models;

public enum InnsynskravStatusValue {
  PURRING_SENDT("purringSendt"),
  NARMERE_BEGRUNNELSE("nærmereBegrunnelse"),
  KLAGE_SENDT("klageSendt"),
  SENDT_TIL_VIRKSOMHET("sendtTilVirksomhet"),
  INNVILGET_INNSYN("innvilgetInnsyn"),
  AVSLATT_INNSYN("avslåttInnsyn"),
  DELVIS_INNVILGET("delvisInnvilget"),
  AVSLATT_KLAGE("avslåttKlage"),
  OPPRETTET("opprettet");

  private final String value;

  InnsynskravStatusValue(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }
}

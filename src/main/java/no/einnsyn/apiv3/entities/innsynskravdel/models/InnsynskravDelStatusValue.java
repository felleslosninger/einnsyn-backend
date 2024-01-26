package no.einnsyn.apiv3.entities.innsynskravdel.models;

public enum InnsynskravDelStatusValue {
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

  InnsynskravDelStatusValue(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }
}

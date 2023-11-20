package no.einnsyn.apiv3.entities.bruker.models;

public enum BrukerType {
  // @formatter:off
  SLUTTBRUKER("Sluttbruker"),
  VIRKSOMHETSBRUKER("Virksomhetsbruker"),
  ADMINBRUKER("Adminbruker");
  // @formatter:on

  private final String value;

  BrukerType(String brukerType) {
    this.value = brukerType;
  }

  @Override
  public String toString() {
    return value;
  }

  public String toJson() {
    return value;
  }
}

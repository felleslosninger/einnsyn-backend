package no.einnsyn.apiv3.entities.bruker.models;

public enum BrukerType {
  // @formatter:off
  SLUTTBRUKER("Sluttbruker"),
  VIRKSOMHETSBRUKER("Virksomhetsbruker"),
  ADMINBRUKER("Adminbruker");
  // @formatter:on

  private final String brukerType;

  BrukerType(String brukerType) {
    this.brukerType = brukerType;
  }

  public String toString() {
    return brukerType;
  }
}

package no.einnsyn.apiv3.entities.bruker.models;

public enum BrukerType {
  SLUTTBRUKER("Sluttbruker"),
  VIRKSOMHETSBRUKER("Virksomhetsbruker"),
  ADMINBRUKER("Adminbruker");

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

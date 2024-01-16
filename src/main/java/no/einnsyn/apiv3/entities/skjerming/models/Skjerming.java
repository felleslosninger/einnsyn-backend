package no.einnsyn.apiv3.entities.skjerming.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBase;

@Getter
@Setter
@Entity
public class Skjerming extends ArkivBase {

  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "skjerm_seq")
  @SequenceGenerator(name = "skjerm_seq", sequenceName = "skjerming_seq", allocationSize = 1)
  @Column(name = "skjerming_id", unique = true)
  private Integer skjermingId;

  private String skjermingIri;

  private String tilgangsrestriksjon;

  private String skjermingshjemmel;
}

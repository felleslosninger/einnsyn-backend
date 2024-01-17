package no.einnsyn.apiv3.entities.skjerming.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBase;
import org.hibernate.annotations.Generated;

@Getter
@Setter
@Entity
public class Skjerming extends ArkivBase {

  @Generated
  @Column(name = "skjerming_id", unique = true)
  private Integer skjermingId;

  private String skjermingIri;

  private String tilgangsrestriksjon;

  private String skjermingshjemmel;
}

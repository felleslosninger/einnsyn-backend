package no.einnsyn.backend.entities.matrikkelnummer.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBase;
import org.hibernate.annotations.Generated;

@Getter
@Setter
@Entity
public class Matrikkelnummer extends ArkivBase {

  @Generated
  @Column(name = "matrikkelnummer_id", unique = true)
  private Integer matrikkelnummerId;

  private String kommunenummer;

  private Integer gaardsnummer;

  private Integer bruksnummer;

  private Integer festenummer;

  private Integer seksjonsnummer;
}

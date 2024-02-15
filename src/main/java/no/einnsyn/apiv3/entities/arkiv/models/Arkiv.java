package no.einnsyn.apiv3.entities.arkiv.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBase;
import org.hibernate.annotations.Generated;

@Getter
@Setter
@Entity
public class Arkiv extends ArkivBase {

  @Generated
  @Column(name = "arkiv_id", unique = true)
  private Integer arkivId;

  private String tittel;

  private String arkivIri;

  @ManyToOne
  @JoinColumn(name = "parentarkiv_id", referencedColumnName = "arkiv_id")
  private Arkiv parent;

  private Instant publisertDato;
}

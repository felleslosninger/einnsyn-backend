package no.einnsyn.apiv3.entities.arkivdel.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkiv.models.Arkiv;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBase;
import org.hibernate.annotations.Generated;

@Getter
@Setter
@Entity
public class Arkivdel extends ArkivBase {

  @Generated
  @Column(name = "arkivdel_id", unique = true)
  private Integer arkivdelId;

  private String arkivdelIri;

  @NotNull private String virksomhetIri;

  private String systemId;

  private String tittel;

  @ManyToOne
  @JoinColumn(name = "arkiv_id", referencedColumnName = "arkiv_id")
  private Arkiv arkiv;

  private Instant publisertDato;
}

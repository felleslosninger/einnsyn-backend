package no.einnsyn.apiv3.entities.arkivdel;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkiv.Arkiv;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObject;

@Getter
@Setter
@Entity
public class Arkivdel extends EinnsynObject {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "arkivd_seq")
  @SequenceGenerator(name = "arkivd_seq", sequenceName = "arkivdel_seq", allocationSize = 1)
  private Integer arkivdelId;

  private String arkivdelIri;

  @NotNull private String virksomhetIri;

  private String systemId;

  private String tittel;

  @ManyToOne
  @JoinColumn(name = "arkiv_id")
  private Arkiv arkiv;

  private Instant publisertDato;
}

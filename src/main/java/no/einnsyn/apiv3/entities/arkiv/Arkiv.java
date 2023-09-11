package no.einnsyn.apiv3.entities.arkiv;

import java.time.Instant;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObject;

@Getter
@Setter
@Entity
public class Arkiv extends EinnsynObject {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ark_seq")
  @SequenceGenerator(name = "ark_seq", sequenceName = "arkiv_seq", allocationSize = 1)
  private Integer arkivId;

  private String arkivIri;

  @NotNull
  private String virksomhetIri;

  private String systemId;

  private String tittel;

  @ManyToOne
  @JoinColumn(name = "parentarkiv_id")
  private Arkiv parentarkivId;

  private Instant publisertDato;
}

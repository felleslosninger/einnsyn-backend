package no.einnsyn.apiv3.entities.klasse;

import jakarta.persistence.Column;
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
import no.einnsyn.apiv3.entities.arkivdel.Arkivdel;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObject;

@Getter
@Setter
@Entity
public class Klasse extends EinnsynObject {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "klasses_seq")
  @SequenceGenerator(name = "klasses_seq", sequenceName = "klasse_seq", allocationSize = 1)
  private Integer klasseId;

  private String systemId;

  private String klasseIdString;

  private String tittel;

  @ManyToOne
  @JoinColumn(name = "parentklasse")
  private Klasse parentklasse;

  @ManyToOne
  @JoinColumn(name = "arkivdel_id")
  private Arkivdel arkivdelId;

  private String nøkkelord;

  // Legacy
  @Column(name = "klasse_iri")
  private String klasseIri;

  // Legacy
  @NotNull private String virksomhetIri;
}

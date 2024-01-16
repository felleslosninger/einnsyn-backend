package no.einnsyn.apiv3.entities.arkiv.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBase;

@Getter
@Setter
@Entity
public class Arkiv extends ArkivBase {

  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ark_seq")
  @SequenceGenerator(name = "ark_seq", sequenceName = "arkiv_seq", allocationSize = 1)
  @Column(name = "arkiv_id", unique = true)
  private Integer arkivId;

  private String tittel;

  private String arkivIri;

  @NotNull private String virksomhetIri;

  @ManyToOne
  @JoinColumn(name = "parentarkiv_id", referencedColumnName = "arkiv_id")
  private Arkiv parentarkivId;

  private Instant publisertDato;

  // Set legacy values
  @PrePersist
  public void prePersist() {
    // Set legacy value virksomhetIri
    if (this.getVirksomhetIri() == null) {
      var journalenhet = this.getJournalenhet();
      this.setVirksomhetIri(journalenhet.getExternalId());
      // Legacy documents might not have externalId, use IRI instead
      if (this.getVirksomhetIri() == null) {
        this.setVirksomhetIri(journalenhet.getIri());
      }
    }
  }
}

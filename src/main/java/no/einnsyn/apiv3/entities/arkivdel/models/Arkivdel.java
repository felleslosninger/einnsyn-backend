package no.einnsyn.apiv3.entities.arkivdel.models;

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
import no.einnsyn.apiv3.entities.arkiv.models.Arkiv;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBase;

@Getter
@Setter
@Entity
public class Arkivdel extends ArkivBase {

  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "arkivd_seq")
  @SequenceGenerator(name = "arkivd_seq", sequenceName = "arkivdel_seq", allocationSize = 1)
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

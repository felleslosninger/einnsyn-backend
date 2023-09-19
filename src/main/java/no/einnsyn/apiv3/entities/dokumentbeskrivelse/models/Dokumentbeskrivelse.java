package no.einnsyn.apiv3.entities.dokumentbeskrivelse.models;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.dokumentobjekt.models.Dokumentobjekt;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObject;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;

@Getter
@Setter
@Entity
public class Dokumentbeskrivelse extends EinnsynObject {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dokbeskr_seq")
  @SequenceGenerator(name = "dokbeskr_seq", sequenceName = "dokumentbeskrivelse_seq",
      allocationSize = 1)
  private Integer dokumentbeskrivelseId;

  private String systemId;

  private Integer dokumentnummer;

  private String tilknyttetRegistreringSom;

  private String dokumenttype;

  private String tittel;

  private String tittel_SENSITIV;

  // Legacy
  @NotNull
  private String dokumentbeskrivelseIri;

  @ManyToOne
  @JoinColumn(name = "virksomhet_id")
  private Enhet virksomhet;

  // Legacy
  @NotNull
  private String virksomhetIri;

  @OneToMany(fetch = FetchType.EAGER, mappedBy = "dokumentbeskrivelse",
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH})
  private List<Dokumentobjekt> dokumentobjekt = new ArrayList<>();


  public void addDokumentobjekt(Dokumentobjekt dokumentobjekt) {
    this.dokumentobjekt.add(dokumentobjekt);
    dokumentobjekt.setDokumentbeskrivelse(this);
  }


  // Set legacy values
  @PrePersist
  public void prePersist() {
    super.prePersist();

    // Set values to legacy field DokumentbeskrivelseIri
    // Try externalId first (if one is given), use generated id if not
    if (this.getDokumentbeskrivelseIri() == null) {
      if (this.getExternalId() != null) {
        this.setDokumentbeskrivelseIri(this.getExternalId());
      } else {
        this.setDokumentbeskrivelseIri(this.getId());
      }
    }

    // Set legacy value virksomhetIri
    if (this.getVirksomhetIri() == null) {
      Enhet virksomhet = this.getVirksomhet();
      if (virksomhet != null) {
        this.setVirksomhetIri(virksomhet.getExternalId());
        // Legacy documents might not have externalId, use IRI instead
        if (this.getVirksomhetIri() == null) {
          this.setVirksomhetIri(virksomhet.getIri());
        }
      }
    }
  }
}

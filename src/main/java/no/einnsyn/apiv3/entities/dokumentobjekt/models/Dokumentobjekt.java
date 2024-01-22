package no.einnsyn.apiv3.entities.dokumentobjekt.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBase;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import org.hibernate.annotations.Generated;

@Getter
@Setter
@Entity
public class Dokumentobjekt extends ArkivBase {

  @Generated
  @Column(name = "dokumentobjekt_id", unique = true)
  private Integer dokumentobjektId;

  @ManyToOne
  @JoinColumn(name = "dokumentbeskrivelse_id", referencedColumnName = "dokumentbeskrivelse_id")
  private Dokumentbeskrivelse dokumentbeskrivelse;

  private String referanseDokumentfil;

  private String dokumentFormat;

  private String sjekksum;

  private String sjekksumalgoritme;

  // Legacy
  private String dokumentobjektIri;

  // Legacy
  private String dokumentbeskrivelseIri;

  @PrePersist
  public void prePersist() {
    // Set values to legacy field DokumentbeskrivelseIri
    // Try externalId first (if one is given), use generated id if not
    if (this.getDokumentobjektIri() == null) {
      if (this.getExternalId() != null) {
        this.setDokumentobjektIri(this.getExternalId());
      } else {
        this.setDokumentobjektIri(this.getId());
      }
    }

    // Set values to legacy field DokumentbeskrivelseIri
    if (this.getDokumentbeskrivelseIri() == null) {
      Dokumentbeskrivelse dokbesk = this.getDokumentbeskrivelse();
      if (dokbesk != null) {
        this.setDokumentbeskrivelseIri(dokbesk.getDokumentbeskrivelseIri());
      }
    }
  }
}

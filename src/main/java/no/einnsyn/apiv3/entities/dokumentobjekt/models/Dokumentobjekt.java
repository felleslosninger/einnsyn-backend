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
  @Override
  protected void prePersist() {
    super.prePersist();

    // Set values to legacy field DokumentbeskrivelseIri
    // Try externalId first (if one is given), use generated id if not
    if (dokumentobjektIri == null) {
      if (externalId != null && externalId.startsWith("http://")) {
        dokumentobjektIri = externalId;
      } else {
        dokumentobjektIri = "http://" + id;
        // The legacy API requires an externalId
        if (externalId == null) {
          externalId = dokumentobjektIri;
        }
      }
    }

    // Set values to legacy field DokumentbeskrivelseIri
    if (dokumentbeskrivelseIri == null && dokumentbeskrivelse != null) {
      setDokumentbeskrivelseIri(dokumentbeskrivelse.getDokumentbeskrivelseIri());
    }
  }
}

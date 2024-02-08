package no.einnsyn.apiv3.entities.dokumentbeskrivelse.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBase;
import no.einnsyn.apiv3.entities.dokumentobjekt.models.Dokumentobjekt;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.moetesak.models.Moetesak;
import org.hibernate.annotations.Generated;

@Getter
@Setter
@Entity
public class Dokumentbeskrivelse extends ArkivBase {

  @Generated
  @Column(name = "dokumentbeskrivelse_id", unique = true)
  private Integer dokumentbeskrivelseId;

  private Integer dokumentnummer;

  private String tilknyttetRegistreringSom;

  private String dokumenttype;

  private String tittel;

  @SuppressWarnings("java:S116")
  private String tittel_SENSITIV;

  // Legacy
  @NotNull private String dokumentbeskrivelseIri;

  @OneToMany(
      fetch = FetchType.EAGER,
      mappedBy = "dokumentbeskrivelse",
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH})
  private List<Dokumentobjekt> dokumentobjekt;

  // Required for list-by-journalpost queries
  @JoinTable(
      name = "journalpost_dokumentbeskrivelse",
      joinColumns = {
        @JoinColumn(
            name = "dokumentbeskrivelse_id",
            referencedColumnName = "dokumentbeskrivelse_id")
      },
      inverseJoinColumns = {
        @JoinColumn(name = "journalpost_id", referencedColumnName = "journalpost_id")
      })
  @ManyToMany(fetch = FetchType.LAZY)
  private List<Journalpost> journalpost;

  // Required for list-by-moetesak queries
  @JoinTable(
      name = "møtesaksregistrering_dokumentbeskrivelse",
      joinColumns = {
        @JoinColumn(
            name = "dokumentbeskrivelse_id",
            referencedColumnName = "dokumentbeskrivelse_id")
      },
      inverseJoinColumns = {
        @JoinColumn(
            name = "møtesaksregistrering_id",
            referencedColumnName = "møtesaksregistrering_id")
      })
  @ManyToMany(fetch = FetchType.LAZY)
  private List<Moetesak> moetesak;

  public void addDokumentobjekt(Dokumentobjekt dobj) {
    if (dokumentobjekt == null) {
      dokumentobjekt = new ArrayList<>();
    }
    dokumentobjekt.add(dobj);
    dobj.setDokumentbeskrivelse(this);
  }

  // Set legacy values
  @PrePersist
  public void prePersist() {
    // Set values to legacy field DokumentbeskrivelseIri
    // Try externalId first (if one is given), use generated id if not
    if (this.getDokumentbeskrivelseIri() == null) {
      if (this.getExternalId() != null) {
        this.setDokumentbeskrivelseIri(this.getExternalId());
      } else {
        this.setDokumentbeskrivelseIri(this.getId());
      }
    }
  }
}

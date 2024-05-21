package no.einnsyn.apiv3.entities.moetedokument.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.korrespondansepart.models.Korrespondansepart;
import no.einnsyn.apiv3.entities.moetemappe.models.Moetemappe;
import no.einnsyn.apiv3.entities.registrering.models.Registrering;
import org.hibernate.annotations.Generated;

@Getter
@Setter
@Entity
@Table(name = "møtedokumentregistrering")
public class Moetedokument extends Registrering {

  // Legacy
  @Generated
  @Column(name = "møtedokumentregistrering_id", unique = true)
  private Integer moetedokumentregistreringId;

  // Legacy
  @Column(name = "møtedokumentregistrering_iri")
  private String moetedokumentregistreringIri;

  @ManyToOne(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH})
  @JoinColumn(name = "møtemappe_id", referencedColumnName = "møtemappe_id")
  private Moetemappe moetemappe;

  @Column(name = "møtedokumentregistreringstype")
  private String moetedokumentregistreringstype;

  private String saksbehandler;

  private String saksbehandlerSensitiv;

  @OneToMany(
      fetch = FetchType.LAZY,
      mappedBy = "parentMoetedokument",
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH})
  private List<Korrespondansepart> korrespondansepart;

  public void addKorrespondansepart(Korrespondansepart korrespondansepart) {
    if (this.korrespondansepart == null) {
      this.korrespondansepart = new ArrayList<>();
    }
    if (!this.korrespondansepart.contains(korrespondansepart)) {
      this.korrespondansepart.add(korrespondansepart);
      korrespondansepart.setParentMoetedokument(this);
    }
  }

  @JoinTable(
      name = "møtedokumentregistrering_dokumentbeskrivelse",
      joinColumns = {
        @JoinColumn(
            name = "møtedokumentregistrering_id",
            referencedColumnName = "møtedokumentregistrering_id")
      },
      inverseJoinColumns = {
        @JoinColumn(
            name = "dokumentbeskrivelse_id",
            referencedColumnName = "dokumentbeskrivelse_id")
      })
  @ManyToMany(
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH})
  private List<Dokumentbeskrivelse> dokumentbeskrivelse;

  public void addDokumentbeskrivelse(Dokumentbeskrivelse dokumentbeskrivelse) {
    if (this.dokumentbeskrivelse == null) {
      this.dokumentbeskrivelse = new ArrayList<>();
    }
    if (!this.dokumentbeskrivelse.contains(dokumentbeskrivelse)) {
      this.dokumentbeskrivelse.add(dokumentbeskrivelse);
    }
  }

  @PrePersist
  void prePersistMoetedokument() {
    // Populate required legacy fields. Use externalId / id as a replacement for IRIs
    if (getMoetedokumentregistreringIri() == null) {
      if (getExternalId() != null) {
        setMoetedokumentregistreringIri(getExternalId());
      } else {
        setMoetedokumentregistreringIri(getId());
      }
    }
  }
}

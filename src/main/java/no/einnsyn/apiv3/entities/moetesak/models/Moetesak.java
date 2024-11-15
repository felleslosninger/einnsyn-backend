package no.einnsyn.apiv3.entities.moetesak.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.indexable.Indexable;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.moetemappe.models.Moetemappe;
import no.einnsyn.apiv3.entities.moetesaksbeskrivelse.models.Moetesaksbeskrivelse;
import no.einnsyn.apiv3.entities.registrering.models.Registrering;
import no.einnsyn.apiv3.entities.utredning.models.Utredning;
import no.einnsyn.apiv3.entities.vedtak.models.Vedtak;
import org.hibernate.annotations.Generated;

@Getter
@Setter
@Entity
@Table(name = "møtesaksregistrering")
public class Moetesak extends Registrering implements Indexable {
  // Legacy
  @Generated
  @Column(name = "møtesaksregistrering_id", unique = true)
  private Integer legacyId;

  @Column(name = "møtesaksregistrering_iri")
  private String moetesakIri;

  // TODO: When the old API is no longer in use, rename this PG column
  @Column(name = "sorteringstype")
  private String moetesakstype;

  // TODO: When the old API is no longer in use, rename this PG column
  @Column(name = "møtesakstype")
  private String legacyMoetesakstype;

  @Column(name = "administrativ_enhet")
  private String utvalg;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "administrativ_enhet__id")
  private Enhet utvalgObjekt;

  @Column(name = "møtesakssekvensnummer")
  private Integer moetesakssekvensnummer;

  @Column(name = "møtesaksår")
  private Integer moetesaksaar;

  @Column(name = "videolink")
  private String videoLink;

  private String saksbehandler;

  private String saksbehandlerSensitiv;

  // lastIndexed should not be updated through JPA
  @Column(insertable = false, updatable = false)
  private Instant lastIndexed;

  @OneToOne(
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH})
  @JoinColumn(name = "utredning__id")
  private Utredning utredning;

  @OneToOne(
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH})
  @JoinColumn(name = "innstilling__id")
  private Moetesaksbeskrivelse innstilling;

  @OneToOne(
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH})
  @JoinColumn(name = "vedtak__id")
  private Vedtak vedtak;

  @ManyToOne
  @JoinColumn(name = "møtemappe_id", referencedColumnName = "møtemappe_id")
  private Moetemappe moetemappe;

  @OneToOne
  @JoinColumn(name = "journalpost__id")
  private Journalpost journalpost;

  // Legacy, referanseTilMoetesak?
  private String journalpostIri;

  @JoinTable(
      name = "møtesaksregistrering_dokumentbeskrivelse",
      joinColumns = {
        @JoinColumn(
            name = "møtesaksregistrering_id",
            referencedColumnName = "møtesaksregistrering_id")
      },
      inverseJoinColumns = {
        @JoinColumn(
            name = "dokumentbeskrivelse_id",
            referencedColumnName = "dokumentbeskrivelse_id")
      })
  @ManyToMany
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
  @Override
  protected void prePersist() {
    // Try to update arkivskaper before super.prePersist()
    updateArkivskaper();

    super.prePersist();

    // Populate required legacy fields. Use id as a replacement for IRIs
    if (getMoetesakIri() == null) {
      if (externalId != null && externalId.matches("^https?://.*")) {
        moetesakIri = externalId;
      } else {
        moetesakIri = "http://" + id;
        // The legacy API requires an externalId
        if (externalId == null) {
          externalId = moetesakIri;
        }
      }
    }
  }

  @PreUpdate
  private void updateArkivskaper() {
    if (getUtvalgObjekt() != null && getUtvalgObjekt().getIri() != getArkivskaper()) {
      setArkivskaper(getUtvalgObjekt().getIri());
    }
  }
}

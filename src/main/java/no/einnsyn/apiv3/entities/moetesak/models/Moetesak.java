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
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.indexable.Indexable;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
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
  private Integer moetesaksregistreringId;

  @Column(name = "møtesaksregistrering_iri")
  private String moetesaksregistreringIri;

  @Column(name = "møtesakstype")
  private String moetesakstype;

  private String sorteringstype;

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

  // Legacy
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
    this.dokumentbeskrivelse.add(dokumentbeskrivelse);
  }

  @PrePersist
  void prePersistMoetesak() {
    // Populate required legacy fields. Use id as a replacement for IRIs
    if (getMoetesaksregistreringIri() == null) {
      if (getExternalId() != null) {
        setMoetesaksregistreringIri(getExternalId());
      } else {
        setMoetesaksregistreringIri(getId());
      }
    }
  }
}

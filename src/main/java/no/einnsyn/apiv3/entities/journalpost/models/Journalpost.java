package no.einnsyn.apiv3.entities.journalpost.models;

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
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.indexable.Indexable;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.korrespondansepart.models.Korrespondansepart;
import no.einnsyn.apiv3.entities.registrering.models.Registrering;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;
import no.einnsyn.apiv3.entities.skjerming.models.Skjerming;
import org.hibernate.annotations.Generated;

@Getter
@Setter
@Entity
public class Journalpost extends Registrering implements Indexable {

  @Generated
  @Column(name = "journalpost_id", unique = true)
  private Long journalpostId;

  private Integer journalaar;

  private Integer journalsekvensnummer;

  private Integer journalpostnummer;

  private String journalposttype;

  private LocalDate journaldato;

  private LocalDate dokumentdato;

  private String sorteringstype;

  private String saksbehandler;

  private Instant lastIndexed;

  // TODO: Implement "følg saken referanse"
  // @ElementCollection
  // @JoinTable(name = "journalpost_følgsakenreferanse",
  // joinColumns = @JoinColumn(name = "journalpost_fra_id"))
  // @Column(name = "journalpost_til_iri")
  // private List<String> følgsakenReferanse;

  private String administrativEnhet;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "administrativ_enhet_id", referencedColumnName = "id")
  private Enhet administrativEnhetObjekt;

  @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  @JoinColumn(name = "skjerming_id", referencedColumnName = "skjerming_id")
  private Skjerming skjerming;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "journalpost", cascade = CascadeType.ALL)
  private List<Korrespondansepart> korrespondansepart;

  @JoinTable(
      name = "journalpost_dokumentbeskrivelse",
      joinColumns = {@JoinColumn(name = "journalpost_id", referencedColumnName = "journalpost_id")},
      inverseJoinColumns = {
        @JoinColumn(
            name = "dokumentbeskrivelse_id",
            referencedColumnName = "dokumentbeskrivelse_id")
      })
  @ManyToMany(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.ALL})
  private List<Dokumentbeskrivelse> dokumentbeskrivelse;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "saksmappe_id", referencedColumnName = "saksmappe_id")
  private Saksmappe saksmappe;

  // Legacy
  private String journalpostIri;

  // Legacy
  private String saksmappeIri;

  /**
   * Helper that adds a korrespondansepart to the list of korrespondanseparts and sets the
   * journalpost on the korrespondansepart
   *
   * @param korrespondansepart
   */
  public void addKorrespondansepart(Korrespondansepart kp) {
    if (korrespondansepart == null) {
      korrespondansepart = new ArrayList<>();
    }
    korrespondansepart.add(kp);
    kp.setJournalpost(this);
  }

  /** Populate legacy (and other) required fields before saving to database. */
  @PrePersist
  public void prePersistJournalpost() {
    // Set Journalenhet as fallback for administrativEnhetObjekt
    if (getAdministrativEnhetObjekt() == null) {
      setAdministrativEnhetObjekt(this.getJournalenhet());
    }

    if (getJournalpostIri() == null) {
      setJournalpostIri(this.getId());
    }
  }

  @PreUpdate
  void preUpdateJournalpost() {
    if (saksmappe != null
        && saksmappe.getExternalId() != null
        && !saksmappe.getExternalId().equals(saksmappeIri)) {
      saksmappeIri = saksmappe.getExternalId();
    }
  }
}

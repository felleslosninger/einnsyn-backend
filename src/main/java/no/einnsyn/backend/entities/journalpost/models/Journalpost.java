package no.einnsyn.backend.entities.journalpost.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.indexable.Indexable;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import no.einnsyn.backend.entities.korrespondansepart.models.Korrespondansepart;
import no.einnsyn.backend.entities.registrering.models.Registrering;
import no.einnsyn.backend.entities.saksmappe.models.Saksmappe;
import no.einnsyn.backend.entities.skjerming.models.Skjerming;
import no.einnsyn.backend.utils.IRIMatcher;
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

  // TODO: When the old API is no longer in use, rename this PG column
  @Column(name = "sorteringstype")
  private String journalposttype;

  // TODO: When the old API is no longer in use, rename this PG column
  @Column(name = "journalposttype")
  private String legacyJournalposttype;

  protected String administrativEnhet;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "administrativ_enhet__id")
  protected Enhet administrativEnhetObjekt;

  private LocalDate journaldato;

  private LocalDate dokumentdato;

  // lastIndexed should not be updated through JPA
  @Column(insertable = false, updatable = false)
  private Instant lastIndexed;

  // TODO: The concept følgsakenReferanse should be revised
  @ElementCollection(fetch = FetchType.EAGER)
  @JoinTable(
      name = "journalpost_følgsakenreferanse",
      joinColumns =
          @JoinColumn(name = "journalpost_fra_id", referencedColumnName = "journalpost_id"))
  @Column(name = "journalpost_til_iri")
  private List<String> foelgsakenReferanse;

  @ManyToOne(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH})
  @JoinColumn(name = "skjerming_id", referencedColumnName = "skjerming_id")
  private Skjerming skjerming;

  @OneToMany(
      fetch = FetchType.LAZY,
      mappedBy = "parentJournalpost",
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH})
  @OrderBy("id ASC")
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
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH})
  @OrderBy("id ASC")
  private List<Dokumentbeskrivelse> dokumentbeskrivelse;

  @ManyToOne
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
   * @param kp the korrespondansepart to add
   */
  public void addKorrespondansepart(Korrespondansepart kp) {
    if (korrespondansepart == null) {
      korrespondansepart = new ArrayList<>();
    }
    if (!korrespondansepart.contains(kp)) {
      korrespondansepart.add(kp);
      korrespondansepart.sort((kp1, kp2) -> kp1.getId().compareTo(kp2.getId()));
      kp.setParentJournalpost(this);
    }
  }

  /**
   * Helper that adds a dokumentbeskrivelse to the list of dokumentbeskrivelses and sets the
   * journalpost on the dokumentbeskrivelse
   *
   * @param db the dokumentbeskrivelse to add
   */
  public void addDokumentbeskrivelse(Dokumentbeskrivelse db) {
    if (dokumentbeskrivelse == null) {
      dokumentbeskrivelse = new ArrayList<>();
    }
    if (!dokumentbeskrivelse.contains(db)) {
      dokumentbeskrivelse.add(db);
      dokumentbeskrivelse.sort((db1, db2) -> db1.getId().compareTo(db2.getId()));
    }
  }

  /** Populate legacy (and other) required fields before saving to database. */
  @PrePersist
  @Override
  protected void prePersist() {
    super.prePersist();

    if (journalpostIri == null) {
      if (externalId != null && IRIMatcher.matches(externalId)) {
        journalpostIri = externalId;
      } else {
        journalpostIri = "http://" + id;
        // The legacy API requires an externalId
        if (externalId == null) {
          externalId = journalpostIri;
        }
      }
    }
  }

  @PreUpdate
  void preUpdateJournalpost() {
    if (saksmappe != null
        && saksmappe.getSaksmappeIri() != null
        && !saksmappe.getSaksmappeIri().equals(saksmappeIri)) {
      saksmappeIri = saksmappe.getSaksmappeIri();
    }
  }
}

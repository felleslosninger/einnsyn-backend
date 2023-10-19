package no.einnsyn.apiv3.entities.journalpost.models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.DynamicUpdate;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.korrespondansepart.models.Korrespondansepart;
import no.einnsyn.apiv3.entities.registrering.models.Registrering;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;
import no.einnsyn.apiv3.entities.skjerming.models.Skjerming;

@Getter
@Setter
@Entity
@DynamicUpdate
public class Journalpost extends Registrering {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "journalpost_seq")
  @SequenceGenerator(name = "journalpost_seq", sequenceName = "journalpost_seq", allocationSize = 1)
  private Long journalpostId;

  private Integer journalaar;

  private Integer journalsekvensnummer;

  private Integer journalpostnummer;

  private String journalposttype;

  private LocalDate journaldato;

  private LocalDate dokumentdato;

  private String sorteringstype;

  private String saksbehandler;


  // @ElementCollection
  // @JoinTable(name = "journalpost_følgsakenreferanse",
  // joinColumns = @JoinColumn(name = "journalpost_fra_id"))
  // @Column(name = "journalpost_til_iri")
  // private List<String> følgsakenReferanse = new ArrayList<>();

  @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
  @JoinColumn(name = "skjerming_id")
  private Skjerming skjerming;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "journalpost", cascade = {CascadeType.ALL})
  private List<Korrespondansepart> korrespondansepart = new ArrayList<>();

  @JoinTable(name = "journalpost_dokumentbeskrivelse",
      joinColumns = {@JoinColumn(name = "journalpost_id")},
      inverseJoinColumns = {@JoinColumn(name = "dokumentbeskrivelse_id")})
  @ManyToMany(cascade = {CascadeType.ALL})
  private List<Dokumentbeskrivelse> dokumentbeskrivelse = new ArrayList<>();

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "saksmappe_id")
  private Saksmappe saksmappe;


  // Legacy
  private String journalpostIri;

  // Legacy
  private String arkivskaper;

  // Legacy
  private String saksmappeIri;


  /**
   * Helper that adds a korrespondansepart to the list of korrespondanseparts and sets the
   * journalpost on the korrespondansepart
   * 
   * @param korrespondansepart
   */
  public void addKorrespondansepart(Korrespondansepart korrespondansepart) {
    this.korrespondansepart.add(korrespondansepart);
    korrespondansepart.setJournalpost(this);
  }


  /**
   * Populate legacy (and other) required fields before saving to database.
   */
  @PrePersist
  public void prePersist() {
    super.prePersist();

    if (this.getJournalpostIri() == null) {
      this.setJournalpostIri(this.getId());
    }

    // Saksmappe is required, no need to check for null
    Saksmappe saksmappe = this.getSaksmappe();
    this.setSaksmappeIri(saksmappe.getExternalId());

  }
}

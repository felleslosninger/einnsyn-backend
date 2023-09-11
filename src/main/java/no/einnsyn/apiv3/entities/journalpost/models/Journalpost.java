package no.einnsyn.apiv3.entities.journalpost.models;

import java.time.LocalDate;
import org.hibernate.annotations.DynamicUpdate;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.registrering.models.Registrering;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;
import no.einnsyn.apiv3.utils.IdGenerator;

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

  private String journalenhet;

  private String sorteringstype;

  // @ElementCollection
  // @JoinTable(name = "journalpost_følgsakenreferanse",
  // joinColumns = @JoinColumn(name = "journalpost_fra_id"))
  // @Column(name = "journalpost_til_iri")
  // private List<String> følgsakenReferanse = new ArrayList<>();

  private String saksmappeIri;

  /*
   * @ManyToOne(fetch = FetchType.EAGER)
   * 
   * @JoinColumn(name = "skjerming_id") private Skjerming skjerming;
   */

  /*
   * @OneToMany(fetch = FetchType.LAZY, mappedBy = "journalpost", cascade = CascadeType.ALL) private
   * List<Korrespondansepart> korrespondansepart = new ArrayList<>();
   */

  /*
   * @JoinTable(name = "journalpost_dokumentbeskrivelse", joinColumns = {@JoinColumn(name =
   * "journalpost_id")}, inverseJoinColumns = {@JoinColumn(name = "dokumentbeskrivelse_id")})
   * 
   * @ManyToMany private List<Dokumentbeskrivelse> dokumentbeskrivelse = new ArrayList<>();
   */

  // This is legacy, but we will need it until all old (and new, coming from the old import)
  // items has valid "saksmappe" fields. Hopefully we can remove it at some point, to avoid having
  // multiple sequence generators for each record.
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "saksmappe_id")
  private Saksmappe saksmappe;


  // Legacy fields
  private String journalpostIri;

  private String arkivskaper;


  /**
   * Populate legacy (and other) required fields before saving to database.
   */
  @PrePersist
  public void prePersist() {
    if (this.getId() == null) {
      this.setId(IdGenerator.generate("journalpost"));
    }

    this.setJournalpostIri(this.getExternalId());

    // Saksmappe is required, no need to check for null
    Saksmappe saksmappe = this.getSaksmappe();
    this.setSaksmappeIri(saksmappe.getExternalId());

    // TODO: Link "saksmappe" to saksmappe's internal id, to be able to remove the saksmappe_id
    // sequence in the future

    // TODO: Virksomhet
  }
}

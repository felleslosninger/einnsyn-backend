package no.einnsyn.apiv3.entities.journalpost.models;

import java.time.LocalDate;
import org.hibernate.annotations.DynamicUpdate;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.registrering.models.Registrering;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;

@Getter
@Setter
@Entity
@DynamicUpdate
public class Journalpost extends Registrering {
  private String journalpostIri; // Legacy, automatically set to external id
  private String arkivskaper; // Legacy
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

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "saksmappe_id")
  private Saksmappe saksmappe;

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

  // Legacy
  private Long journalpostId;


  public Journalpost() {
    super();
    this.entity = "journalpost";
  }

  @PrePersist
  public void prePersist() {
    super.prePersist();

    // Populate required legacy fields
    this.setJournalpostIri(this.getExternalId());

    // Saksmappe is required, no need to check for null
    Saksmappe saksmappe = this.getSaksmappe();
    this.setSaksmappeIri(saksmappe.getExternalId());

    // TODO: Virksomhet
  }
}

package no.einnsyn.apiv3.entities.saksmappe.models;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.mappe.models.Mappe;
import no.einnsyn.apiv3.utils.IdGenerator;

@Getter
@Setter
@Entity
@DynamicUpdate
public class Saksmappe extends Mappe {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "saksmappe_seq")
  @SequenceGenerator(name = "saksmappe_seq", sequenceName = "saksmappe_seq", allocationSize = 1)
  private Integer saksmappeId;

  private Integer saksaar;

  private Integer sakssekvensnummer;

  private LocalDate saksdato;

  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "saksmappe")
  private List<Journalpost> journalpost = new ArrayList<Journalpost>();


  // Legacy fields
  private String saksmappeIri;

  private String administrativEnhet;


  /**
   * Helper that adds a journalpost to the list of journalposts and sets the saksmappe on the
   * journalpost
   * 
   * @param journalpost
   */
  public void addJournalpost(Journalpost journalpost) {
    this.journalpost.add(journalpost);
    journalpost.setSaksmappe(this);
  }

  @PrePersist
  public void prePersist() {
    super.prePersist();

    if (this.getId() == null) {
      this.setId(IdGenerator.generate("saksmappe"));
    }

    // Populate required legacy fields
    this.setSaksmappeIri(this.getExternalId());
  }
}

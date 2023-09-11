package no.einnsyn.apiv3.entities.saksmappe.models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.DynamicUpdate;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.mappe.models.Mappe;

@Getter
@Setter
@Entity
@DynamicUpdate
public class Saksmappe extends Mappe {

  private Integer saksaar;

  private Integer sakssekvensnummer;

  private LocalDate saksdato;

  // TODO: ExpandableField
  private String administrativEnhet;

  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "saksmappe")
  private List<Journalpost> journalpost = new ArrayList<Journalpost>();

  public Saksmappe() {
    super();
    this.entity = "saksmappe";
  }

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

  // Legacy fields
  private String saksmappeIri;
  private Long saksmappeId;


  @PrePersist
  public void prePersist() {
    super.prePersist();

    // Populate required legacy fields
    this.setSaksmappeIri(this.getExternalId());
  }
}

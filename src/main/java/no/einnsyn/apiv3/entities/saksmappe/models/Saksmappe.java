package no.einnsyn.apiv3.entities.saksmappe.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.indexable.Indexable;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.mappe.models.Mappe;
import org.hibernate.annotations.Generated;

@Getter
@Setter
@Entity
public class Saksmappe extends Mappe implements Indexable {

  @Generated
  @Column(name = "saksmappe_id", unique = true)
  private Integer saksmappeId;

  private Integer saksaar;

  private Integer sakssekvensnummer;

  private LocalDate saksdato;

  @OneToMany(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH},
      mappedBy = "saksmappe")
  private List<Journalpost> journalpost;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "administrativ_enhet_id", referencedColumnName = "id")
  private Enhet administrativEnhetObjekt;

  private String administrativEnhet;

  private Instant lastIndexed;

  // Legacy
  private String saksmappeIri;

  /**
   * Helper that adds a journalpost to the list of journalposts and sets the saksmappe on the
   * journalpost
   *
   * @param jp
   */
  public void addJournalpost(Journalpost jp) {
    if (journalpost == null) {
      journalpost = new ArrayList<>();
    }
    journalpost.add(jp);
    jp.setSaksmappe(this);
  }

  @PrePersist
  public void prePersistSaksmappe() {
    // Populate required legacy fields. Use id as a replacement for IRIs
    saksmappeIri = this.id;

    // Update legacy value "arkivskaper"
    if (this.arkivskaper == null && administrativEnhetObjekt != null) {
      this.arkivskaper = administrativEnhetObjekt.getIri();
    }

    // Set Journalenhet as fallback for administrativEnhetObjekt
    if (administrativEnhetObjekt == null) {
      administrativEnhetObjekt = this.journalenhet;
    }
  }
}

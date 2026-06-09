package no.einnsyn.backend.entities.saksmappe.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
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
import no.einnsyn.backend.entities.enhet.models.Enhet;
import no.einnsyn.backend.entities.mappe.models.Mappe;
import no.einnsyn.backend.entities.matrikkelnummer.models.Matrikkelnummer;
import no.einnsyn.backend.utils.IRIMatcher;
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "administrativ_enhet__id")
  private Enhet administrativEnhetObjekt;

  private String administrativEnhet;

  // lastIndexed should not be updated through JPA
  @Column(insertable = false, updatable = false)
  private Instant lastIndexed;

  // Legacy
  @Column(name = "saksmappe_iri")
  private String legacyIri;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "saksmappe")
  @OrderBy("id ASC")
  private List<Matrikkelnummer> matrikkelnummer;

  @Override
  public void addMatrikkelnummer(Matrikkelnummer matrikkelnummer) {
    if (this.matrikkelnummer == null) {
      this.matrikkelnummer = new ArrayList<>();
    }
    if (!this.matrikkelnummer.contains(matrikkelnummer)) {
      matrikkelnummer.setSaksmappe(this);
      this.matrikkelnummer.add(matrikkelnummer);
    }
  }

  @PrePersist
  @Override
  protected void prePersist() {
    // Try to update arkivskaper before super.prePersist()
    if (administrativEnhetObjekt != null
        && !administrativEnhetObjekt.getIri().equals(arkivskaper)) {
      setArkivskaper(administrativEnhetObjekt.getIri());
    }
    super.prePersist();

    // Populate required legacy fields. Use id as a replacement for IRIs
    if (legacyIri == null) {
      if (externalId != null && IRIMatcher.matches(externalId)) {
        legacyIri = externalId;
      } else {
        legacyIri = "http://" + id;
        // The legacy API requires an externalId
        if (externalId == null) {
          externalId = legacyIri;
        }
      }
    }

    // Set Journalenhet as fallback for administrativEnhetObjekt
    if (administrativEnhetObjekt == null) {
      setAdministrativEnhetObjekt(journalenhet);
    }
  }

  @PreUpdate
  @Override
  protected void preUpdate() {
    super.preUpdate();

    // Keep saksmappeIri in sync with externalId
    if (externalId != null && !externalId.equals(legacyIri)) {
      legacyIri = externalId;
    }

    // Keep arkivskaper in sync with administrativEnhetObjekt
    if (administrativEnhetObjekt != null
        && !administrativEnhetObjekt.getIri().equals(arkivskaper)) {
      setArkivskaper(administrativEnhetObjekt.getIri());
    }
  }
}

package no.einnsyn.backend.entities.saksmappe.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.indexable.Indexable;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import no.einnsyn.backend.entities.mappe.models.Mappe;
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
  private String saksmappeIri;

  @PrePersist
  @Override
  protected void prePersist() {
    // Try to update arkivskaper before super.prePersist()
    updateArkivskaper();
    super.prePersist();

    // Populate required legacy fields. Use id as a replacement for IRIs
    if (saksmappeIri == null) {
      if (externalId != null && IRIMatcher.matches(externalId)) {
        saksmappeIri = externalId;
      } else {
        saksmappeIri = "http://" + id;
        // The legacy API requires an externalId
        if (externalId == null) {
          externalId = saksmappeIri;
        }
      }
    }

    // Set Journalenhet as fallback for administrativEnhetObjekt
    if (administrativEnhetObjekt == null) {
      setAdministrativEnhetObjekt(journalenhet);
    }
  }

  @PreUpdate
  private void updateArkivskaper() {
    if (administrativEnhetObjekt != null
        && !administrativEnhetObjekt.getIri().equals(arkivskaper)) {
      setArkivskaper(administrativEnhetObjekt.getIri());
    }
  }
}

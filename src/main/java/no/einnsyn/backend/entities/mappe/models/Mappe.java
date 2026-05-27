package no.einnsyn.backend.entities.mappe.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.hasslug.HasSlug;
import no.einnsyn.backend.entities.arkiv.models.Arkiv;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBase;
import no.einnsyn.backend.entities.arkivdel.models.Arkivdel;
import no.einnsyn.backend.entities.klasse.models.Klasse;
import no.einnsyn.backend.entities.matrikkelnummer.models.Matrikkelnummer;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public abstract class Mappe extends ArkivBase implements HasSlug {

  protected String slug;

  protected String offentligTittel;

  protected String offentligTittelSensitiv;

  protected String beskrivelse;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "klasse_id", referencedColumnName = "klasse_id")
  protected Klasse parentKlasse;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "arkiv_id", referencedColumnName = "arkiv_id")
  protected Arkiv parentArkiv;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "arkivdel_id", referencedColumnName = "arkivdel_id")
  protected Arkivdel parentArkivdel;

  protected Instant publisertDato;

  @LastModifiedDate protected Instant oppdatertDato;

  // Legacy, IRI of administrativEnhet (or journalenhet as fallback)
  protected String arkivskaper; // Legacy

  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(
      name = "mappe__id",
      referencedColumnName = "_id",
      insertable = false,
      updatable = false)
  @OrderBy("id ASC")
  protected List<Matrikkelnummer> matrikkelnummer;

  public void addMatrikkelnummer(Matrikkelnummer matrikkelnummer) {
    if (this.matrikkelnummer == null) {
      this.matrikkelnummer = new ArrayList<>();
    }
    if (!this.matrikkelnummer.contains(matrikkelnummer)) {
      matrikkelnummer.setMappeId(getId());
      matrikkelnummer.setRegistreringId(null);
      this.matrikkelnummer.add(matrikkelnummer);
    }
  }

  @PrePersist
  @Override
  protected void prePersist() {
    super.prePersist();

    if (getArkivskaper() == null) {
      setArkivskaper(journalenhet.getIri());
    }
  }
}

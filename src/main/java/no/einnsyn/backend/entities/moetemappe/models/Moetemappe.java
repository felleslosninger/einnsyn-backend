package no.einnsyn.backend.entities.moetemappe.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.indexable.Indexable;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import no.einnsyn.backend.entities.mappe.models.Mappe;
import no.einnsyn.backend.entities.moetedokument.models.Moetedokument;
import no.einnsyn.backend.entities.moetesak.models.Moetesak;
import no.einnsyn.backend.utils.IRIMatcher;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Generated;

@Getter
@Setter
@Entity
@Table(name = "møtemappe")
public class Moetemappe extends Mappe implements Indexable {

  @Generated
  @Column(name = "møtemappe_id", unique = true)
  private Integer moetemappeId;

  // Legacy
  @Column(name = "møtemappe_iri")
  private String moetemappeIri;

  @Column(name = "møtenummer")
  private String moetenummer;

  private String utvalg;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "utvalg__id")
  private Enhet utvalgObjekt;

  @Column(name = "møtedato")
  private Instant moetedato;

  @Column(name = "møtested")
  private String moetested;

  private String videolink;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "moetemappe")
  @Filter(name = "accessibleOrAdminFilter")
  @Filter(name = "accessibleFilter")
  @OrderBy("id ASC")
  private List<Moetesak> moetesak;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "moetemappe")
  @Filter(name = "accessibleOrAdminFilter")
  @Filter(name = "accessibleFilter")
  @OrderBy("id ASC")
  private List<Moetedokument> moetedokument;

  // lastIndexed should not be updated through JPA
  @Column(insertable = false, updatable = false)
  private Instant lastIndexed;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "referanse_forrige_moete__id")
  @Filter(name = "accessibleOrAdminFilter")
  @Filter(name = "accessibleFilter")
  private Moetemappe referanseForrigeMoete;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "referanse_neste_moete__id")
  @Filter(name = "accessibleOrAdminFilter")
  @Filter(name = "accessibleFilter")
  private Moetemappe referanseNesteMoete;

  /**
   * Helper that adds a moetedokument to the list of moetedokumentregistreringer and sets the
   * moetemappe on the moetedokument
   *
   * @param ms the moetesak to add
   */
  public void addMoetesak(Moetesak ms) {
    if (moetesak == null) {
      moetesak = new ArrayList<>();
    }
    if (!moetesak.contains(ms)) {
      moetesak.add(ms);
      ms.setMoetemappe(this);
    }
  }

  /**
   * Helper that adds a moetedokument to the list of moetedokumentregistreringer and sets the
   * moetemappe on the moetedokument
   *
   * @param md the moetedokument to add
   */
  public void addMoetedokument(Moetedokument md) {
    if (moetedokument == null) {
      moetedokument = new ArrayList<>();
    }
    moetedokument.add(md);
    md.setMoetemappe(this);
  }

  @PrePersist
  @Override
  protected void prePersist() {
    // Try to update arkivskaper before super.prePersist()
    updateArkivskaper();
    super.prePersist();

    if (moetemappeIri == null) {
      if (externalId != null && IRIMatcher.matches(externalId)) {
        moetemappeIri = externalId;
      } else {
        moetemappeIri = "http://" + id;
        // The legacy API requires an externalId
        if (externalId == null) {
          externalId = moetemappeIri;
        }
      }
    }
  }

  @PreUpdate
  private void updateArkivskaper() {
    if (utvalgObjekt != null && !utvalgObjekt.getIri().equals(arkivskaper)) {
      setArkivskaper(utvalgObjekt.getIri());
    }
  }
}

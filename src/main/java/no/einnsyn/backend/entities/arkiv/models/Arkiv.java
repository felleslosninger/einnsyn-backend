package no.einnsyn.backend.entities.arkiv.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBase;
import no.einnsyn.backend.utils.IRIMatcher;
import org.hibernate.annotations.Generated;

@Getter
@Setter
@Entity
public class Arkiv extends ArkivBase {

  @Generated
  @Column(name = "arkiv_id", unique = true)
  private Integer arkivId;

  private String tittel;

  private String arkivIri;

  @ManyToOne
  @JoinColumn(name = "parentarkiv_id", referencedColumnName = "arkiv_id")
  private Arkiv parent;

  private Instant publisertDato;

  @PrePersist
  @Override
  protected void prePersist() {
    super.prePersist();

    // Populate required legacy fields. Use id as a replacement for IRIs
    if (arkivIri == null) {
      if (externalId != null && IRIMatcher.matches(externalId)) {
        arkivIri = externalId;
      } else {
        arkivIri = "http://" + id;
        // The legacy API requires an externalId
        if (externalId == null) {
          externalId = arkivIri;
        }
      }
    }
  }
}

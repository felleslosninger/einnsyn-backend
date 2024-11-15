package no.einnsyn.apiv3.entities.arkivdel.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkiv.models.Arkiv;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBase;
import no.einnsyn.apiv3.utils.IRIMatcher;
import org.hibernate.annotations.Generated;

@Getter
@Setter
@Entity
public class Arkivdel extends ArkivBase {

  @Generated
  @Column(name = "arkivdel_id", unique = true)
  private Integer arkivdelId;

  private String arkivdelIri;

  private String tittel;

  @ManyToOne
  @JoinColumn(name = "arkiv_id", referencedColumnName = "arkiv_id")
  private Arkiv parent;

  private Instant publisertDato;

  @PrePersist
  @Override
  protected void prePersist() {
    super.prePersist();

    // Populate required legacy fields. Use id as a replacement for IRIs
    if (arkivdelIri == null) {
      if (externalId != null && IRIMatcher.matches(externalId)) {
        arkivdelIri = externalId;
      } else {
        arkivdelIri = "http://" + id;
        // The legacy API requires an externalId
        if (externalId == null) {
          externalId = arkivdelIri;
        }
      }
    }
  }
}

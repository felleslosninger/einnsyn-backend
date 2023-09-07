package no.einnsyn.apiv3.entities.mappe.models;

import java.time.Instant;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkiv.Arkiv;
import no.einnsyn.apiv3.entities.arkivdel.Arkivdel;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObject;
import no.einnsyn.apiv3.entities.klasse.Klasse;

@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public abstract class Mappe extends EinnsynObject {

  private String offentligTittel;

  private String offentligTittelSensitiv;

  private String beskrivelse;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "klasse_id")
  private Klasse klasse;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "arkiv_id")
  private Arkiv arkiv;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "arkivdel_id")
  private Arkivdel arkivdel;

  private String arkivskaper; // Legacy?

  private Instant publisertDato;

  @LastModifiedDate
  private Instant oppdatertDato;

  // private ExpandableField<Virksomhet> virksomhet;

  // Legacy, to be removed (replaced with virksomhet-id)
  private String virksomhetIri;

  @PrePersist
  public void prePersist() {
    super.prePersist();
    // TODO: Generate a slug based on offentligTittel (and possibly arkivskaper?)
  }
}

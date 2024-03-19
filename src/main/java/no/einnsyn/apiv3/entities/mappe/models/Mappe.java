package no.einnsyn.apiv3.entities.mappe.models;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkiv.models.Arkiv;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBase;
import no.einnsyn.apiv3.entities.arkivdel.models.Arkivdel;
import no.einnsyn.apiv3.entities.klasse.models.Klasse;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public abstract class Mappe extends ArkivBase {

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

  @PrePersist
  public void prePersistMappe() {

    if (arkivskaper == null) {
      setArkivskaper(this.journalenhet.getIri());
    }
  }
}

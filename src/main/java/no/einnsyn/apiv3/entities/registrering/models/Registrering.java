package no.einnsyn.apiv3.entities.registrering.models;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBase;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public abstract class Registrering extends ArkivBase {

  protected String offentligTittel;

  protected String offentligTittelSensitiv;

  protected String beskrivelse;

  protected Instant publisertDato;

  // Legacy
  @LastModifiedDate protected Instant oppdatertDato;

  // Legacy, IRI of administrativEnhet (or journalenhet as fallback)
  protected String arkivskaper;

  @PrePersist
  public void prePersist() {

    if (arkivskaper == null) {
      arkivskaper = this.journalenhet.getIri();
    }
  }
}

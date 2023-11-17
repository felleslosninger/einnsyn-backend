package no.einnsyn.apiv3.entities.registrering.models;

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
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObject;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;

@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public abstract class Registrering extends EinnsynObject {

  private String offentligTittel;

  private String offentligTittelSensitiv;

  private Instant publisertDato;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "administrativ_enhet_id")
  private Enhet administrativEnhetObjekt;

  private String administrativEnhet;


  // Legacy
  @LastModifiedDate
  private Instant oppdatertDato;

  // Legacy
  private String virksomhetIri;

  // Legacy?
  private String systemId;

  // Legacy, IRI of administrativEnhet (or journalenhet as fallback)
  private String arkivskaper;


  @PrePersist
  public void prePersist() {
    // Journalenhet is called "virksomhet" on the old codebase
    Enhet journalenhet = getJournalenhet();
    setVirksomhetIri(journalenhet.getIri());

    // Set Journalenhet as fallback for administrativEnhetObjekt
    if (getAdministrativEnhetObjekt() == null) {
      setAdministrativEnhetObjekt(this.getJournalenhet());
    }

    // Update legacy value "arkivskaper"
    if (getArkivskaper() == null && administrativEnhetObjekt != null) {
      setArkivskaper(administrativEnhetObjekt.getIri());
    }

    if (getArkivskaper() == null) {
      setArkivskaper(journalenhet.getIri());
    }
  }
}

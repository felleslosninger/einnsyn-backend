package no.einnsyn.backend.entities.registrering.models;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.time.Instant;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.hasslug.HasSlug;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBase;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import no.einnsyn.backend.entities.matrikkelnummer.models.Matrikkelnummer;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public abstract class Registrering extends ArkivBase implements HasSlug {

  protected String slug;

  protected String offentligTittel;

  protected String offentligTittelSensitiv;

  protected String beskrivelse;

  protected Instant publisertDato;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "avhendet_til__id")
  protected Enhet avhendetTil;

  // Legacy
  @LastModifiedDate protected Instant oppdatertDato;

  // Legacy, IRI of administrativEnhet (or journalenhet as fallback)
  protected String arkivskaper;

  public abstract List<Matrikkelnummer> getMatrikkelnummer();

  public abstract void addMatrikkelnummer(Matrikkelnummer matrikkelnummer);

  @PrePersist
  @Override
  protected void prePersist() {
    super.prePersist();

    if (arkivskaper == null) {
      setArkivskaper(journalenhet.getIri());
    }
  }
}

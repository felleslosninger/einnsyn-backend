package no.einnsyn.apiv3.entities.arkivbase.models;

import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.base.models.Base;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;

/**
 * Base class for all eInnsyn objects, containing metadata fields that are common to all objects.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class ArkivBase extends Base {

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "journalenhet__id")
  protected Enhet journalenhet;

  protected String systemId;

  // Legacy
  protected String virksomhetIri;

  @PrePersist
  public void prePersistArkivBase() {
    // Journalenhet is called "virksomhet" in the old codebase
    if (journalenhet != null) {
      setVirksomhetIri(journalenhet.getIri());
    }
  }
}

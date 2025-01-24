package no.einnsyn.backend.entities.arkivbase.models;

import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.base.models.Base;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import org.hibernate.annotations.Filter;

/**
 * Base class for all eInnsyn objects, containing metadata fields that are common to all objects.
 */
@Filter(
    name = "combinedFilter",
    condition =
        "(current_date > COALESCE($FILTER_PLACEHOLDER$._accessible_after, current_date - interval '1 day') "
            + "OR $FILTER_PLACEHOLDER$.journalenhet__id in (:journalenhet, 'default'))")
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
  @Override
  protected void prePersist() {
    super.prePersist();

    // Journalenhet is called "virksomhet" in the old codebase
    if (journalenhet != null) {
      setVirksomhetIri(journalenhet.getIri());
    }
  }
}

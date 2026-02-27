package no.einnsyn.backend.entities.base.models;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.utils.id.IdGenerator;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Base class for all eInnsyn objects, containing metadata fields that are common to all objects.
 */
@FilterDef(
    name = "accessibleFilter",
    applyToLoadByKey = true,
    defaultCondition =
        """
        $FILTER_PLACEHOLDER$._accessible_after <= NOW()
        """)
@Filter(name = "accessibleFilter")
@MappedSuperclass
@Getter
@Setter
public abstract class Base {

  // An ID that can be exposed to the public
  @Id
  @NotNull
  @Column(name = "_id")
  protected String id;

  // Replaces old IRIs, the client's reference id
  @Column(name = "_external_id")
  protected String externalId;

  @Column(name = "_created")
  protected Instant created;

  @UpdateTimestamp
  @Column(name = "_updated")
  protected Instant updated;

  @Column(name = "_accessible_after")
  protected Instant accessibleAfter;

  @Version protected Long lockVersion;

  @PrePersist
  protected void prePersist() {
    setId(IdGenerator.generateId(getClass()));
    var now = Instant.now();
    setCreated(now);
    setUpdated(now);

    if (getAccessibleAfter() == null) {
      setAccessibleAfter(now);
    }
  }

  @PreUpdate
  protected void preUpdate() {
    setUpdated(Instant.now());
  }
}

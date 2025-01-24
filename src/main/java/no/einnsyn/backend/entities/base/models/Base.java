package no.einnsyn.backend.entities.base.models;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.utils.idgenerator.IdGenerator;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Base class for all eInnsyn objects, containing metadata fields that are common to all objects.
 */
@FilterDef(
    name = "combinedFilter",
    applyToLoadByKey = true,
    parameters =
        @ParamDef(name = "journalenhet", type = String.class, resolver = Base.DefSupply.class),
    defaultCondition = "COALESCE(:journalenhet, 'default') = 'default'")
@FilterDef(
    name = "accessibilityFilter",
    applyToLoadByKey = true,
    defaultCondition =
        "current_date > COALESCE($FILTER_PLACEHOLDER$._accessible_after, current_date - interval '1 day') ")
@Filter(name = "accessibilityFilter")
@MappedSuperclass
@Getter
@Setter
public abstract class Base {

  protected static class DefSupply implements Supplier<String> {
    @Override
    public String get() {
      return "'default'";
    }
  }

  // An ID that can be exposed to the public
  @Id
  @NotNull
  @Column(name = "_id")
  protected String id;

  // Replaces old IRIs, the client's reference id
  @Column(name = "_external_id")
  protected String externalId;

  @CreationTimestamp
  @Column(name = "_created")
  protected Instant created;

  @UpdateTimestamp
  @Column(name = "_updated")
  protected Instant updated;

  @Column(name = "_accessible_after")
  protected LocalDate accessibleAfter;

  @Version protected Long lockVersion;

  @PrePersist
  protected void prePersist() {
    setId(IdGenerator.generateId(getClass()));
    if (accessibleAfter == null) {
      setAccessibleAfter(LocalDate.now().minusDays(1));
    }
  }

  @PreUpdate
  protected void preUpdate() {
    setUpdated(Instant.now());
    if (accessibleAfter == null) {
      setAccessibleAfter(LocalDate.now().minusDays(1));
    }
  }
}

package no.einnsyn.apiv3.entities.base.models;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.utils.idgenerator.IdGenerator;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Base class for all eInnsyn objects, containing metadata fields that are common to all objects.
 */
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

  @CreationTimestamp
  @Column(name = "_created")
  protected Instant created;

  @UpdateTimestamp
  @Column(name = "_updated")
  protected Instant updated;

  @Version protected Long lockVersion;

  @PrePersist
  public void generateId() {
    this.setId(IdGenerator.generateId(this.getClass()));
  }
}

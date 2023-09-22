package no.einnsyn.apiv3.entities.einnsynobject.models;

import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.utils.IdGenerator;

/**
 * Base class for all eInnsyn objects, containing metadata fields that are common to all objects.
 */
@MappedSuperclass
@Getter
@Setter
@DynamicUpdate
public abstract class EinnsynObject {

  // An ID that can be exposed to the public
  @NotNull
  @Column(name = "_id")
  private String id;

  // Replaces old IRIs, the client's reference id
  @Column(name = "_external_id")
  private String externalId;

  @CreationTimestamp
  @Column(name = "_created")
  private Instant created;

  @UpdateTimestamp
  @Column(name = "_updated")
  private Instant updated;

  @Version
  private Long lockVersion;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "journalenhet_id")
  private Enhet journalenhet;


  @PrePersist
  public void prePersist() {
    if (this.getId() == null) {
      this.setId(IdGenerator.generate(this.getClass()));
    }
  }
}

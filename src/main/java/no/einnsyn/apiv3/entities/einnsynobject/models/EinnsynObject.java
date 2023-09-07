package no.einnsyn.apiv3.entities.einnsynobject.models;

import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.utils.IdGenerator;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@DynamicUpdate
public class EinnsynObject {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "einnsyn_entity_seq")
  @SequenceGenerator(name = "einnsyn_entity_seq", sequenceName = "einnsyn_entity_seq",
      allocationSize = 1)
  private Long internalId;

  private String id; // An ID that can be exposed to the public

  private String externalId; // Replaces old IRIs, the client's reference id

  @Version
  private Long lockVersion;

  @NotNull
  private String entity;

  @CreationTimestamp
  private Instant created;

  @UpdateTimestamp
  private Instant updated;

  @PrePersist
  public void prePersist() {
    if (this.id == null) {
      this.id = IdGenerator.generate(this.entity);
    }
  }

}

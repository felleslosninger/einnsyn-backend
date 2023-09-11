package no.einnsyn.apiv3.entities.einnsynobject.models;

import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Base class for all eInnsyn objects, containing metadata fields that are common to all objects.
 */
// @Inheritance(strategy = InheritanceType.JOINED)
@MappedSuperclass
@Getter
@Setter
@DynamicUpdate
public class EinnsynObject {

  /*
   * @Id
   * 
   * @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "einnsyn_entity_seq")
   * 
   * @SequenceGenerator(name = "einnsyn_entity_seq", sequenceName = "einnsyn_entity_seq",
   * allocationSize = 1) private Long internalId;
   */

  // An ID that can be exposed to the public
  @NotNull
  private String id;

  // Replaces old IRIs, the client's reference id
  private String externalId;

  @Version
  private Long lockVersion;

  @CreationTimestamp
  private Instant created;

  @UpdateTimestamp
  private Instant updated;

}

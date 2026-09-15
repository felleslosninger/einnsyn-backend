package no.einnsyn.backend.entities.apikey.models;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.base.models.Base;
import no.einnsyn.backend.entities.bruker.models.Bruker;
import no.einnsyn.backend.entities.enhet.models.Enhet;

@Getter
@Setter
@Entity
public class ApiKey extends Base {

  private String name;

  /** SHA-256 hash of the secret key. The plaintext is never stored. */
  private String secret;

  /**
   * The plaintext secret key. Only set on the instance that was just created, so it can be returned
   * once in the create response. It is never persisted and never part of a request DTO.
   */
  @Transient private String secretKey;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn
  private Enhet enhet;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn
  private Bruker bruker;

  private Instant expiresAt;
}

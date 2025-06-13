package no.einnsyn.backend.entities.apikey.models;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

  private String secret;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn
  private Enhet enhet;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn
  private Bruker bruker;

  private Instant expiresAt;
}

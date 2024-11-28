package no.einnsyn.backend.entities.apikey.models;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.base.models.Base;
import no.einnsyn.backend.entities.enhet.models.Enhet;

@Getter
@Setter
@Entity
public class ApiKey extends Base {

  private String name;

  private String secret;

  @ManyToOne(fetch = FetchType.EAGER)
  @NotNull
  @JoinColumn
  private Enhet enhet;
}

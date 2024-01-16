package no.einnsyn.apiv3.entities.lagretsoek.models;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.base.models.Base;

@Getter
@Setter
@Entity
public class LagretSoek extends Base {

  private String query;
}

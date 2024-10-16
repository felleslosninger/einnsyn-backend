package no.einnsyn.apiv3.entities.lagretsak.models;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.base.models.Base;

@Getter
@Setter
@Entity
public class LagretSak extends Base {

  private String query;
}

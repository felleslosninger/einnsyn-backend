package no.einnsyn.apiv3.entities.innsynskravdel.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class InnsynskravDelStatus {

  @NotNull private Date opprettetDato;

  @NotNull
  @Enumerated(EnumType.STRING)
  private InnsynskravDelStatusValue status;

  @NotNull private boolean systemgenerert;
}

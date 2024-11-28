package no.einnsyn.apiv3.entities.innsynskrav.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class InnsynskravStatus {

  @NotNull private Date opprettetDato;

  @NotNull
  @Enumerated(EnumType.STRING)
  private InnsynskravStatusValue status;

  @NotNull private boolean systemgenerert;
}

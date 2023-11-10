package no.einnsyn.apiv3.entities.skjerming.models;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;
import no.einnsyn.apiv3.features.validation.validationgroups.Insert;

@Getter
@Setter
public class SkjermingJSON extends EinnsynObjectJSON {

  @NotNull(groups = {Insert.class})
  private String tilgangsrestriksjon;

  private String skjermingshjemmel;
}

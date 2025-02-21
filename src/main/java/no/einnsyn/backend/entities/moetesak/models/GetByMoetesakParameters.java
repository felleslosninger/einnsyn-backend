// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.moetesak.models;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.queryparameters.models.GetParameters;
import no.einnsyn.backend.validation.validationgroups.Insert;

@Getter
@Setter
public class GetByMoetesakParameters extends GetParameters {
  @NotBlank(groups = {Insert.class})
  protected String id;

  @NotBlank(groups = {Insert.class})
  protected String moetesakId;
}

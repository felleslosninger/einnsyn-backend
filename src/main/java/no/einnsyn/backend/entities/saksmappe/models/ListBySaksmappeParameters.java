// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.saksmappe.models;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.validation.validationgroups.Insert;

@Getter
@Setter
public class ListBySaksmappeParameters extends ListParameters {
  @NotBlank(groups = {Insert.class})
  protected String id;

  @NotBlank(groups = {Insert.class})
  protected String saksmappeId;
}

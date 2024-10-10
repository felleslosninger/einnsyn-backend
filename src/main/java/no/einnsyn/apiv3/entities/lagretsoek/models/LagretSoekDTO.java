// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.lagretsoek.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.base.models.BaseDTO;
import no.einnsyn.apiv3.entities.bruker.models.BrukerDTO;
import no.einnsyn.apiv3.validation.nossn.NoSSN;
import no.einnsyn.apiv3.validation.validationgroups.Insert;
import no.einnsyn.apiv3.validation.validationgroups.Update;

@Getter
@Setter
public class LagretSoekDTO extends BaseDTO {

  @Size(max = 500)
  final String entity = "LagretSoek";

  @Null(groups = {Insert.class, Update.class})
  ExpandableField<BrukerDTO> bruker;

  @Size(max = 500)
  @NoSSN
  @NotBlank(groups = {Insert.class})
  String label;

  Boolean subscribe;

  @Size(max = 500)
  @NoSSN
  @NotBlank(groups = {Insert.class})
  String legacyQuery;
}

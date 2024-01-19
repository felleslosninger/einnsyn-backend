// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.lagretsak.models;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.base.models.BaseDTO;
import no.einnsyn.apiv3.entities.bruker.models.BrukerDTO;

@Getter
@Setter
public class LagretSakDTO extends BaseDTO {

  @Size(max = 500)
  final String entity = "LagretSak";

  ExpandableField<BrukerDTO> bruker;

  Boolean varsling;
}

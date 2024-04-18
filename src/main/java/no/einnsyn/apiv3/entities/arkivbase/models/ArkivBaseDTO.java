// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.arkivbase.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.base.models.BaseDTO;
import no.einnsyn.apiv3.entities.enhet.models.EnhetDTO;
import no.einnsyn.apiv3.validation.nossn.NoSSN;

@Getter
@Setter
public abstract class ArkivBaseDTO extends BaseDTO {

  @Size(max = 500)
  @NoSSN
  String systemId;

  @Valid ExpandableField<EnhetDTO> journalenhet;
}

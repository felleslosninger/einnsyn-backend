// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.backend.entities.arkivbase.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.enhet.EnhetService;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;

@Getter
@Setter
public abstract class ArkivBaseDTO extends BaseDTO {

  @Size(max = 500)
  String systemId;

  @ExpandableObject(
      service = EnhetService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<EnhetDTO> journalenhet;
}

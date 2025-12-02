// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.arkivdel.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.arkiv.ArkivService;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;

/** Arkivdel */
@Getter
@Setter
public class ArkivdelDTO extends ArkivBaseDTO {
  protected final String entity = "Arkivdel";

  /** The title of the Arkivdel. */
  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String tittel;

  /** The parent Arkiv to which this Arkivdel belongs. */
  @ExpandableObject(
      service = ArkivService.class,
      groups = {Insert.class, Update.class})
  @Valid
  @Null(groups = {Insert.class, Update.class})
  protected ExpandableField<ArkivDTO> arkiv;
}

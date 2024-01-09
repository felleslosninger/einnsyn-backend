// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.mappe.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.apiv3.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.klasse.models.KlasseDTO;
import no.einnsyn.apiv3.entities.mappe.models.MappeDTO;
import no.einnsyn.apiv3.features.validation.nossn.NoSSN;
import no.einnsyn.apiv3.features.validation.validationgroups.Insert;

@Getter
@Setter
public abstract class MappeDTO extends ArkivBaseDTO {

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = { Insert.class })
  private String offentligTittel;

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = { Insert.class })
  private String offentligTittelSensitiv;

  @Size(max = 500)
  @NoSSN
  private String beskrivelse;

  @Size(max = 500)
  @NoSSN
  private String noekkelord;

  @Valid
  private ExpandableField<UnionPropertyParent> parent;
}

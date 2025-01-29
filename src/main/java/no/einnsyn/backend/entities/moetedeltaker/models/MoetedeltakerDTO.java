// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.entities.moetedeltaker.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;

@Getter
@Setter
public class MoetedeltakerDTO extends ArkivBaseDTO {
  final String entity = "Moetedeltaker";

  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  String moetedeltakerNavn;

  @NoSSN
  @Size(max = 500)
  String moetedeltakerFunksjon;
}

// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.utredning.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.apiv3.entities.moetesaksbeskrivelse.models.MoetesaksbeskrivelseDTO;
import no.einnsyn.apiv3.validation.validationgroups.Insert;

@Getter
@Setter
public class UtredningDTO extends ArkivBaseDTO {

  @Size(max = 500)
  final String entity = "Utredning";

  @NotNull(groups = {Insert.class})
  @Valid
  ExpandableField<MoetesaksbeskrivelseDTO> saksbeskrivelse;

  @NotNull(groups = {Insert.class})
  @Valid
  ExpandableField<MoetesaksbeskrivelseDTO> innstilling;

  @Valid List<ExpandableField<DokumentbeskrivelseDTO>> utredningsdokumenter;
}

// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.behandlingsprotokoll.models;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.apiv3.features.validation.nossn.NoSSN;
import no.einnsyn.apiv3.features.validation.validationgroups.Insert;
import no.einnsyn.apiv3.features.validation.validationgroups.Update;

@Getter
@Setter
public class BehandlingsprotokollDTO extends ArkivBaseDTO {

  @Size(max = 500)
  @Null(groups = { Insert.class, Update.class })
  private final String entity = "Behandlingsprotokoll";

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = { Insert.class })
  private String tekstInnhold;

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = { Insert.class })
  private String tekstFormat;

  @NotNull(groups = { Insert.class })
  private Double test;
}

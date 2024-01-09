// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.registrering.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartDTO;
import no.einnsyn.apiv3.entities.skjerming.models.SkjermingDTO;
import no.einnsyn.apiv3.features.validation.NoSSN;
import no.einnsyn.apiv3.features.validation.validationGroups.Insert;

@Getter
@Setter
public abstract class RegistreringDTO extends ArkivBaseDTO {

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

  @Valid
  private ExpandableField<SkjermingDTO> skjerming;

  @Valid
  private List<ExpandableField<KorrespondansepartDTO>> korrespondansepart;

  @Valid
  private List<ExpandableField<DokumentbeskrivelseDTO>> dokumentbeskrivelse;
}

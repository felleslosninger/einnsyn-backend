// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.registrering.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartDTO;
import no.einnsyn.apiv3.validation.isodatetime.IsoDateTime;
import no.einnsyn.apiv3.validation.nossn.NoSSN;
import no.einnsyn.apiv3.validation.validationgroups.Insert;

@Getter
@Setter
public abstract class RegistreringDTO extends ArkivBaseDTO {

  @Size(max = 500)
  @NoSSN
  @NotBlank(groups = {Insert.class})
  String offentligTittel;

  @Size(max = 500)
  @NoSSN
  @NotBlank(groups = {Insert.class})
  String offentligTittelSensitiv;

  @Size(max = 500)
  @NoSSN
  String beskrivelse;

  @Size(max = 500)
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  String publisertDato;

  @Valid List<ExpandableField<KorrespondansepartDTO>> korrespondansepart;

  @Valid List<ExpandableField<DokumentbeskrivelseDTO>> dokumentbeskrivelse;
}

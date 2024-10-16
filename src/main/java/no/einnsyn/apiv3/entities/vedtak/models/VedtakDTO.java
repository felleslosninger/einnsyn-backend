// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.vedtak.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.apiv3.entities.behandlingsprotokoll.models.BehandlingsprotokollDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.apiv3.entities.moetesaksbeskrivelse.models.MoetesaksbeskrivelseDTO;
import no.einnsyn.apiv3.entities.votering.models.VoteringDTO;
import no.einnsyn.apiv3.validation.isodatetime.IsoDateTime;
import no.einnsyn.apiv3.validation.validationgroups.Insert;

@Getter
@Setter
public class VedtakDTO extends ArkivBaseDTO {

  @Size(max = 500)
  final String entity = "Vedtak";

  @NotNull(groups = {Insert.class})
  @Valid
  ExpandableField<MoetesaksbeskrivelseDTO> vedtakstekst;

  @Valid List<ExpandableField<VoteringDTO>> votering;

  @Valid ExpandableField<BehandlingsprotokollDTO> behandlingsprotokoll;

  @Valid List<ExpandableField<DokumentbeskrivelseDTO>> vedtaksdokument;

  @Size(max = 500)
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE)
  @NotBlank(groups = {Insert.class})
  String dato;
}

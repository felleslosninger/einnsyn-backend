// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.vedtak.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.apiv3.entities.behandlingsprotokoll.models.BehandlingsprotokollDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.moetesaksbeskrivelse.models.MoetesaksbeskrivelseDTO;
import no.einnsyn.apiv3.entities.votering.models.VoteringDTO;
import no.einnsyn.apiv3.features.validation.NoSSN;
import no.einnsyn.apiv3.features.validation.validationGroups.Insert;
import no.einnsyn.apiv3.features.validation.validationGroups.Update;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class VedtakDTO extends ArkivBaseDTO {

  @Size(max = 500)
  @Null(groups = { Insert.class, Update.class })
  private final String entity = "Vedtak";

  @NotNull(groups = { Insert.class })
  @Valid
  private ExpandableField<MoetesaksbeskrivelseDTO> vedtakstekst;

  @Valid
  private List<ExpandableField<VoteringDTO>> votering;

  @Valid
  private ExpandableField<BehandlingsprotokollDTO> behandlingsprotokoll;

  @Valid
  private List<ExpandableField<DokumentbeskrivelseDTO>> vedtaksdokumenter;

  @Size(max = 500)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  @NotNull(groups = { Insert.class })
  private String dato;
}

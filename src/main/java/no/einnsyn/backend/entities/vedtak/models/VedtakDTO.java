// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.backend.entities.vedtak.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.entities.behandlingsprotokoll.BehandlingsprotokollService;
import no.einnsyn.backend.entities.behandlingsprotokoll.models.BehandlingsprotokollDTO;
import no.einnsyn.backend.entities.dokumentbeskrivelse.DokumentbeskrivelseService;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.backend.entities.moetesaksbeskrivelse.MoetesaksbeskrivelseService;
import no.einnsyn.backend.entities.moetesaksbeskrivelse.models.MoetesaksbeskrivelseDTO;
import no.einnsyn.backend.entities.votering.VoteringService;
import no.einnsyn.backend.entities.votering.models.VoteringDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.isodatetime.IsoDateTime;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;

@Getter
@Setter
public class VedtakDTO extends ArkivBaseDTO {

  @Size(max = 500)
  final String entity = "Vedtak";

  @ExpandableObject(
      service = MoetesaksbeskrivelseService.class,
      groups = {Insert.class, Update.class})
  @NotNull(groups = {Insert.class})
  @Valid
  ExpandableField<MoetesaksbeskrivelseDTO> vedtakstekst;

  @ExpandableObject(
      service = VoteringService.class,
      groups = {Insert.class, Update.class})
  @Valid
  List<ExpandableField<VoteringDTO>> votering;

  @ExpandableObject(
      service = BehandlingsprotokollService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<BehandlingsprotokollDTO> behandlingsprotokoll;

  @ExpandableObject(
      service = DokumentbeskrivelseService.class,
      groups = {Insert.class, Update.class})
  @Valid
  List<ExpandableField<DokumentbeskrivelseDTO>> vedtaksdokument;

  @Size(max = 500)
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE)
  @NotBlank(groups = {Insert.class})
  String dato;
}

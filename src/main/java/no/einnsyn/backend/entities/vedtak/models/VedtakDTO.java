// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.vedtak.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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

/** Vedtak */
@Getter
@Setter
public class VedtakDTO extends ArkivBaseDTO {
  protected final String entity = "Vedtak";

  @ExpandableObject(
      service = MoetesaksbeskrivelseService.class,
      groups = {Insert.class, Update.class})
  @Valid
  @NotNull(groups = {Insert.class})
  protected ExpandableField<MoetesaksbeskrivelseDTO> vedtakstekst;

  @ExpandableObject(
      service = VoteringService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected List<ExpandableField<VoteringDTO>> votering;

  @ExpandableObject(
      service = BehandlingsprotokollService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected ExpandableField<BehandlingsprotokollDTO> behandlingsprotokoll;

  @ExpandableObject(
      service = DokumentbeskrivelseService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected List<ExpandableField<DokumentbeskrivelseDTO>> vedtaksdokument;

  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE)
  @NotNull(groups = {Insert.class})
  protected String dato;
}

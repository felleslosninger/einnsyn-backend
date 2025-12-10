// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.utredning.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.entities.dokumentbeskrivelse.DokumentbeskrivelseService;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.backend.entities.moetesaksbeskrivelse.MoetesaksbeskrivelseService;
import no.einnsyn.backend.entities.moetesaksbeskrivelse.models.MoetesaksbeskrivelseDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;

/** Represents a report or investigation related to a meeting case. */
@Getter
@Setter
public class UtredningDTO extends ArkivBaseDTO {
  protected final String entity = "Utredning";

  /** The description of the case. */
  @ExpandableObject(
      service = MoetesaksbeskrivelseService.class,
      groups = {Insert.class, Update.class})
  @Valid
  @NotNull(groups = {Insert.class})
  protected ExpandableField<MoetesaksbeskrivelseDTO> saksbeskrivelse;

  /** The recommendation or proposition. */
  @ExpandableObject(
      service = MoetesaksbeskrivelseService.class,
      groups = {Insert.class, Update.class})
  @Valid
  @NotNull(groups = {Insert.class})
  protected ExpandableField<MoetesaksbeskrivelseDTO> innstilling;

  /** Documents that are part of the investigation. */
  @ExpandableObject(
      service = DokumentbeskrivelseService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected List<ExpandableField<DokumentbeskrivelseDTO>> utredningsdokument;
}

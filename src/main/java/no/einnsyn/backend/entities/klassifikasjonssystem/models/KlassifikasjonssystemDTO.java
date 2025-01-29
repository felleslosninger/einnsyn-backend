// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.entities.klassifikasjonssystem.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.entities.arkivdel.ArkivdelService;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;

/** Klassifikasjonssystem */
@Getter
@Setter
public class KlassifikasjonssystemDTO extends ArkivBaseDTO {
  final String entity = "Klassifikasjonssystem";

  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  String tittel;

  /** The parent arkivdel. */
  @ExpandableObject(
      service = ArkivdelService.class,
      groups = {Insert.class, Update.class})
  @Valid
  @Null(groups = {Insert.class, Update.class})
  ExpandableField<ArkivdelDTO> arkivdel;
}

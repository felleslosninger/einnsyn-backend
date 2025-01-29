// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.entities.klasse.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.entities.arkivdel.ArkivdelService;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.klasse.KlasseService;
import no.einnsyn.backend.entities.klassifikasjonssystem.KlassifikasjonssystemService;
import no.einnsyn.backend.entities.klassifikasjonssystem.models.KlassifikasjonssystemDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;

@Getter
@Setter
public class KlasseDTO extends ArkivBaseDTO {
  final String entity = "Klasse";

  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  String tittel;

  @ExpandableObject(
      service = KlassifikasjonssystemService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<KlassifikasjonssystemDTO> klassifikasjonssystem;

  @ExpandableObject(
      service = KlasseService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<KlasseDTO> klasse;

  @ExpandableObject(
      service = ArkivdelService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<ArkivdelDTO> arkivdel;
}

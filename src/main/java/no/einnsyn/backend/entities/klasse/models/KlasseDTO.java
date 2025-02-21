// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

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

/** Klasse */
@Getter
@Setter
public class KlasseDTO extends ArkivBaseDTO {
  protected final String entity = "Klasse";

  /** The title of the class. */
  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String tittel;

  /** An optional parent klassifikasjonssystem */
  @ExpandableObject(
      service = KlassifikasjonssystemService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected ExpandableField<KlassifikasjonssystemDTO> klassifikasjonssystem;

  /** An optional parent klasse */
  @ExpandableObject(
      service = KlasseService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected ExpandableField<KlasseDTO> klasse;

  /** An optional parent arkivdel (non-standard field, due to legacy data) */
  @ExpandableObject(
      service = ArkivdelService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected ExpandableField<ArkivdelDTO> arkivdel;
}

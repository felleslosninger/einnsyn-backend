// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.mappe.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.entities.arkivdel.ArkivdelService;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.klasse.KlasseService;
import no.einnsyn.backend.entities.klasse.models.KlasseDTO;
import no.einnsyn.backend.entities.moetemappe.MoetemappeService;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.saksmappe.SaksmappeService;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.isodatetime.IsoDateTime;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;

/**
 * An abstract base model for case files (Saksmappe) and meeting records (Moetemappe). It contains
 * common properties for these folder-like structures.
 */
@Getter
@Setter
public class MappeDTO extends ArkivBaseDTO {
  /** A URL-friendly unique slug for the resource. */
  @Pattern(regexp = "^[a-z0-9\\-]+$")
  protected String slug;

  /** The title of the Mappe, with sensitive information redacted. */
  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String offentligTittel;

  /** The title of the Mappe, with sensitive information included. */
  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String offentligTittelSensitiv;

  @Size(max = 1000)
  @NoSSN
  protected String beskrivelse;

  @NoSSN
  @Size(max = 500)
  protected String noekkelord;

  /**
   * The date the resource was published. This field is updated automatically, but can be set
   * manually by admins.
   */
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  protected String publisertDato;

  /**
   * The date the resource was last updated. This field is updated automatically, but can be set
   * manually by admins.
   */
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  protected String oppdatertDato;

  /** An optional Klasse for this Mappe. */
  @ExpandableObject(
      service = KlasseService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected ExpandableField<KlasseDTO> klasse;

  /** If this Mappe is the child of a Saksmappe, this field will contain the parent Saksmappe. */
  @ExpandableObject(
      service = SaksmappeService.class,
      groups = {Insert.class, Update.class})
  @Valid
  @Null(groups = {Insert.class, Update.class})
  protected ExpandableField<SaksmappeDTO> saksmappe;

  /** If this Mappe is the child of a Moetemappe, this field will contain the parent Moetemappe. */
  @ExpandableObject(
      service = MoetemappeService.class,
      groups = {Insert.class, Update.class})
  @Valid
  @Null(groups = {Insert.class, Update.class})
  protected ExpandableField<MoetemappeDTO> moetemappe;

  /**
   * If this Mappe is not a child of a Saksmappe or Moetemappe, this field will contain the parent
   * Arkivdel.
   */
  @ExpandableObject(
      service = ArkivdelService.class,
      groups = {Insert.class, Update.class})
  @Valid
  @Null(groups = {Insert.class, Update.class})
  protected ExpandableField<ArkivdelDTO> arkivdel;
}

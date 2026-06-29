// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.matrikkelnummer.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.entities.journalpost.JournalpostService;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.moetedokument.MoetedokumentService;
import no.einnsyn.backend.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.backend.entities.moetemappe.MoetemappeService;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.moetesak.MoetesakService;
import no.einnsyn.backend.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.backend.entities.saksmappe.SaksmappeService;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;

/**
 * Identifies a property unit (matrikkelenhet) in the Norwegian cadastre, following Kartverket's
 * standard format.
 */
@Getter
@Setter
public class MatrikkelnummerDTO extends ArkivBaseDTO {
  protected final String entity = "Matrikkelnummer";

  /** Four-digit municipality number (kommunenummer). */
  @Pattern(regexp = "^[0-9]{4}$")
  @NotBlank(groups = {Insert.class})
  protected String kommunenummer;

  /** Garden number (gaardsnummer). */
  @Min(0)
  @NotNull(groups = {Insert.class})
  protected Integer gaardsnummer;

  /** Bruk number (bruksnummer). */
  @Min(0)
  @NotNull(groups = {Insert.class})
  protected Integer bruksnummer;

  /** Leasehold number (festenummer). 0 means no leasehold. */
  @Min(0)
  protected Integer festenummer;

  /** Section number (seksjonsnummer). 0 means no section. */
  @Min(0)
  protected Integer seksjonsnummer;

  /** The Saksmappe this Matrikkelnummer is associated with, if any. */
  @ExpandableObject(
      service = SaksmappeService.class,
      groups = {Insert.class, Update.class})
  @Valid
  @Null(groups = {Insert.class, Update.class})
  protected ExpandableField<SaksmappeDTO> saksmappe;

  /** The Moetemappe this Matrikkelnummer is associated with, if any. */
  @ExpandableObject(
      service = MoetemappeService.class,
      groups = {Insert.class, Update.class})
  @Valid
  @Null(groups = {Insert.class, Update.class})
  protected ExpandableField<MoetemappeDTO> moetemappe;

  /** The Journalpost this Matrikkelnummer is associated with, if any. */
  @ExpandableObject(
      service = JournalpostService.class,
      groups = {Insert.class, Update.class})
  @Valid
  @Null(groups = {Insert.class, Update.class})
  protected ExpandableField<JournalpostDTO> journalpost;

  /** The Moetesak this Matrikkelnummer is associated with, if any. */
  @ExpandableObject(
      service = MoetesakService.class,
      groups = {Insert.class, Update.class})
  @Valid
  @Null(groups = {Insert.class, Update.class})
  protected ExpandableField<MoetesakDTO> moetesak;

  /** The Moetedokument this Matrikkelnummer is associated with, if any. */
  @ExpandableObject(
      service = MoetedokumentService.class,
      groups = {Insert.class, Update.class})
  @Valid
  @Null(groups = {Insert.class, Update.class})
  protected ExpandableField<MoetedokumentDTO> moetedokument;
}

// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.dokumentbeskrivelse.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.entities.dokumentobjekt.DokumentobjektService;
import no.einnsyn.backend.entities.dokumentobjekt.models.DokumentobjektDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;

/**
 * Represents the metadata for a document. It is connected to a registry entry and describes a
 * single document.
 */
@Getter
@Setter
public class DokumentbeskrivelseDTO extends ArkivBaseDTO {
  protected final String entity = "Dokumentbeskrivelse";

  /** The title of the document, with sensitive information redacted. */
  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String tittel;

  /** The title of the document, with sensitive information included. */
  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String tittelSensitiv;

  /** The document number within the parent registry entry. */
  @NotNull(groups = {Insert.class})
  protected Integer dokumentnummer;

  /** The type of document (e.g., 'letter', 'invoice'). */
  @NoSSN
  @Size(max = 500)
  protected String dokumenttype;

  /**
   * Describes the document's role in relation to the registry entry (e.g., 'main document',
   * 'attachment').
   */
  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String tilknyttetRegistreringSom;

  /** The associated electronic document(s). */
  @ExpandableObject(
      service = DokumentobjektService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected List<ExpandableField<DokumentobjektDTO>> dokumentobjekt;
}

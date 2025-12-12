// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.moetedokument.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.moetemappe.MoetemappeService;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.registrering.models.RegistreringDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;

/** Represents a document related to a meeting, such as an agenda or minutes. */
@Getter
@Setter
public class MoetedokumentDTO extends RegistreringDTO {
  protected final String entity = "Moetedokument";

  /** The type of meeting document (e.g., 'Agenda', 'Minutes'). */
  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String moetedokumenttype;

  /** The case officer responsible for the document. */
  @NoSSN
  @Size(max = 500)
  protected String saksbehandler;

  /** The case officer responsible for the document, including sensitive information. */
  @NoSSN
  @Size(max = 500)
  protected String saksbehandlerSensitiv;

  /** The meeting this document belongs to. */
  @ExpandableObject(
      service = MoetemappeService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected ExpandableField<MoetemappeDTO> moetemappe;
}

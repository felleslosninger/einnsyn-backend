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

/** Moetedokument */
@Getter
@Setter
public class MoetedokumentDTO extends RegistreringDTO {
  protected final String entity = "Moetedokument";

  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String moetedokumenttype;

  @NoSSN
  @Size(max = 500)
  protected String saksbehandler;

  @NoSSN
  @Size(max = 500)
  protected String saksbehandlerSensitiv;

  @ExpandableObject(
      service = MoetemappeService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected ExpandableField<MoetemappeDTO> moetemappe;
}

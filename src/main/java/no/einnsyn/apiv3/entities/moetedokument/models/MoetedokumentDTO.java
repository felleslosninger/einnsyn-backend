// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.moetedokument.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.moetemappe.MoetemappeService;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.registrering.models.RegistreringDTO;
import no.einnsyn.apiv3.validation.expandableobject.ExpandableObject;
import no.einnsyn.apiv3.validation.nossn.NoSSN;
import no.einnsyn.apiv3.validation.validationgroups.Insert;
import no.einnsyn.apiv3.validation.validationgroups.Update;

@Getter
@Setter
public class MoetedokumentDTO extends RegistreringDTO {

  @Size(max = 500)
  final String entity = "Moetedokument";

  @Size(max = 500)
  @NoSSN
  @NotBlank(groups = {Insert.class})
  String moetedokumenttype;

  @Size(max = 500)
  @NoSSN
  String saksbehandler;

  @Size(max = 500)
  @NoSSN
  String saksbehandlerSensitiv;

  @ExpandableObject(
      service = MoetemappeService.class,
      groups = {Insert.class, Update.class})
  @Null(groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<MoetemappeDTO> moetemappe;
}

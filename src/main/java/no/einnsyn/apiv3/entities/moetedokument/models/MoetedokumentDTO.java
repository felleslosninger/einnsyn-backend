// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.moetedokument.models;

import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.registrering.models.RegistreringDTO;
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
  String moetedokumenttype;

  List<ExpandableField<DokumentbeskrivelseDTO>> dokumentbeskrivelse;

  @Null(groups = {Insert.class, Update.class})
  ExpandableField<MoetemappeDTO> moetemappe;
}

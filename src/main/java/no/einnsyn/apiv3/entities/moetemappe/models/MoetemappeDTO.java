// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.moetemappe.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.enhet.models.EnhetDTO;
import no.einnsyn.apiv3.entities.mappe.models.MappeDTO;
import no.einnsyn.apiv3.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.apiv3.validation.isodatetime.IsoDateTime;
import no.einnsyn.apiv3.validation.nossn.NoSSN;
import no.einnsyn.apiv3.validation.validationgroups.Insert;

@Getter
@Setter
public class MoetemappeDTO extends MappeDTO {

  @Size(max = 500)
  final String entity = "Moetemappe";

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = {Insert.class})
  String moetenummer;

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = {Insert.class})
  String utvalg;

  @Valid ExpandableField<EnhetDTO> utvalgObjekt;

  @Size(max = 500)
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  @NotNull(groups = {Insert.class})
  String moetedato;

  @Size(max = 500)
  @NoSSN
  String moetested;

  @Size(max = 5000)
  @NoSSN
  String videoLink;

  @Valid ExpandableField<MoetemappeDTO> referanseForrigeMoete;

  @Valid ExpandableField<MoetemappeDTO> referanseNesteMoete;

  @Valid List<ExpandableField<MoetedokumentDTO>> moetedokument;

  @Valid List<ExpandableField<MoetesakDTO>> moetesak;
}

// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.moetemappe.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.enhet.models.EnhetDTO;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.mappe.models.MappeDTO;
import no.einnsyn.apiv3.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.apiv3.features.validation.nossn.NoSSN;
import no.einnsyn.apiv3.features.validation.validationgroups.Insert;
import no.einnsyn.apiv3.features.validation.validationgroups.Update;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class MoetemappeDTO extends MappeDTO {

  @Size(max = 500)
  @Null(groups = {Insert.class, Update.class})
  private final String entity = "Moetemappe";

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = {Insert.class})
  private String moetenummer;

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = {Insert.class})
  private String utvalg;

  @Valid
  private ExpandableField<EnhetDTO> utvalgObjekt;

  @Size(max = 500)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  @NotNull(groups = {Insert.class})
  private String moetedato;

  @Size(max = 500)
  @NoSSN
  private String moetested;

  @Size(max = 5000)
  @NoSSN
  private String videoLink;

  @Valid
  private ExpandableField<MoetemappeDTO> referanseForrigeMoete;

  @Valid
  private ExpandableField<MoetemappeDTO> referanseNesteMoete;

  @Valid
  private List<ExpandableField<MoetedokumentDTO>> moetedokument;

  @Valid
  private List<ExpandableField<MoetesakDTO>> moetesak;
}

// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.innsynskrav.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.base.models.BaseDTO;
import no.einnsyn.apiv3.entities.bruker.models.BrukerDTO;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelDTO;
import no.einnsyn.apiv3.features.validation.nossn.NoSSN;
import no.einnsyn.apiv3.features.validation.validationgroups.Insert;
import no.einnsyn.apiv3.features.validation.validationgroups.Update;

@Getter
@Setter
public class InnsynskravDTO extends BaseDTO {

  @Size(max = 500)
  @Null(groups = {Insert.class, Update.class})
  private final String entity = "Innsynskrav";

  @Size(max = 500)
  @Email
  @NotNull(groups = {Insert.class})
  private String email;

  @NotNull(groups = {Insert.class})
  @Valid
  private List<ExpandableField<InnsynskravDelDTO>> innsynskravDel;

  @Null(groups = {Insert.class, Update.class})
  private Boolean verified;

  @Valid
  private ExpandableField<BrukerDTO> bruker;

  @Size(max = 500)
  private LanguageEnum language;

  public enum LanguageEnum {
    nb, nn, en, se,
  }
}

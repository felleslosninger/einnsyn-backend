// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.bruker.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.base.models.BaseDTO;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelDTO;
import no.einnsyn.apiv3.entities.lagretsoek.models.LagretSoekDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.apiv3.features.validation.NoSSN;
import no.einnsyn.apiv3.features.validation.validationGroups.Insert;
import no.einnsyn.apiv3.features.validation.validationGroups.Update;

@Getter
@Setter
public class BrukerDTO extends BaseDTO {

  @Size(max = 500)
  @Null(groups = { Insert.class, Update.class })
  private final String entity = "Bruker";

  @Size(max = 500)
  @Email
  @NotNull(groups = { Insert.class })
  private String email;

  @Null(groups = { Insert.class, Update.class })
  private Boolean active;

  @Size(max = 500)
  private LanguageEnum language = "nb";

  @Null(groups = { Insert.class, Update.class })
  @Valid
  private List<ExpandableField<SaksmappeDTO>> lagretSak;

  @Null(groups = { Insert.class, Update.class })
  @Valid
  private List<ExpandableField<LagretSoekDTO>> lagretSoek;

  @Null(groups = { Insert.class, Update.class })
  @Valid
  private List<ExpandableField<InnsynskravDTO>> innsynskrav;

  @Null(groups = { Insert.class, Update.class })
  @Valid
  private List<ExpandableField<InnsynskravDelDTO>> innsynskravDel;

  public enum LanguageEnum {
    nb,
    nn,
    en,
    se,
  }
}

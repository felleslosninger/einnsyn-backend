package no.einnsyn.apiv3.entities.bruker.models;

import java.util.List;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravJSON;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelJSON;
import no.einnsyn.apiv3.features.validation.password.Password;
import no.einnsyn.apiv3.features.validation.validationGroups.Insert;
import no.einnsyn.apiv3.features.validation.validationGroups.Update;
import no.einnsyn.apiv3.responses.ResponseList;


@Getter
@Setter
public class BrukerJSON extends EinnsynObjectJSON {

  @Email
  @NotNull(groups = {Insert.class})
  // @Null(groups = {Update.class})
  private String email;

  // TODO: @Null for serialization groups
  @Null(groups = {Update.class})
  @NotNull(groups = {Insert.class})
  @Password
  private String password;

  private Boolean active;

  private String language = "nb";

  @Null(groups = {Insert.class, Update.class})
  private List<ExpandableField<InnsynskravJSON>> innsynskrav;

  @Null(groups = {Insert.class, Update.class})
  private ResponseList<ExpandableField<InnsynskravDelJSON>> innsynskravDel;

  // @Null(groups = {Insert.class, Update.class})
  // private List<ExpandableField<LagretSakJSON>> lagretSak = new ArrayList<>();

  // @Null(groups = {Insert.class, Update.class})
  // private List<ExpandableField<LagretSoekJSON>> lagretSoek = new ArrayList<>();
}

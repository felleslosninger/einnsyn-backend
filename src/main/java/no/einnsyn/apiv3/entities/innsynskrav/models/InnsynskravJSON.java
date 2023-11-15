package no.einnsyn.apiv3.entities.innsynskrav.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.hibernate.sql.Update;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelJSON;
import no.einnsyn.apiv3.features.validation.NewObject.NewObject;
import no.einnsyn.apiv3.features.validation.validationGroups.Insert;

@Getter
@Setter
public class InnsynskravJSON extends EinnsynObjectJSON {

  private String entity = "Innsynskrav";

  @NotNull(groups = {Insert.class})
  private String epost;

  @Null(groups = {Insert.class, Update.class})
  private Date opprettetDato;

  @Null(groups = {Insert.class, Update.class})
  private Date sendtTilVirksomhet;

  @Null(groups = {Insert.class, Update.class})
  private String verificationSecret;

  @Null(groups = {Insert.class, Update.class})
  private Boolean verified;

  private String language; // TODO: Enum

  @Valid
  @NotEmpty(groups = {Insert.class})
  @NewObject
  private List<ExpandableField<InnsynskravDelJSON>> innsynskravDel = new ArrayList<>();

}

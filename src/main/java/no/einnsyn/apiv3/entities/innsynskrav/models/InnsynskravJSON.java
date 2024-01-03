package no.einnsyn.apiv3.entities.innsynskrav.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Null;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.authentication.bruker.models.BrukerUserDetails;
import no.einnsyn.apiv3.entities.bruker.models.BrukerJSON;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelJSON;
import no.einnsyn.apiv3.features.validation.NewObject.NewObject;
import no.einnsyn.apiv3.features.validation.validationGroups.Insert;
import org.hibernate.sql.Update;
import org.springframework.security.core.context.SecurityContextHolder;

@Getter
@Setter
public class InnsynskravJSON extends EinnsynObjectJSON {

  private String entity = "Innsynskrav";

  private String email;

  @AssertTrue(
      groups = {Insert.class},
      message = "Email is requred when not logged in")
  private boolean isEmail() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return ((authentication != null && authentication.getPrincipal() instanceof BrukerUserDetails)
        || email != null);
  }

  @Null(groups = {Insert.class, Update.class})
  private Date opprettetDato;

  @Null(groups = {Insert.class, Update.class})
  private Date sendtTilVirksomhet;

  @Null(groups = {Insert.class, Update.class})
  private String verificationSecret;

  @Null(groups = {Insert.class, Update.class})
  private Boolean verified;

  private String language; // TODO: Enum

  @Null(groups = {Insert.class, Update.class})
  private ExpandableField<BrukerJSON> bruker;

  @Valid
  @NotEmpty(groups = {Insert.class})
  @NewObject
  private List<ExpandableField<InnsynskravDelJSON>> innsynskravDel = new ArrayList<>();
}

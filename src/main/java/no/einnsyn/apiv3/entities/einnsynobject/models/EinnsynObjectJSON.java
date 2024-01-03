package no.einnsyn.apiv3.entities.einnsynobject.models;

import jakarta.validation.constraints.Null;
import java.time.Instant;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.enhet.models.EnhetJSON;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.features.validation.ExistingObject.ExistingObject;
import no.einnsyn.apiv3.features.validation.validationGroups.Insert;
import no.einnsyn.apiv3.features.validation.validationGroups.Update;

@Getter
@Setter
public class EinnsynObjectJSON {

  @Null(groups = {Insert.class})
  private String id;

  private String externalId;

  @Null(groups = {Insert.class, Update.class})
  private Instant created;

  @Null(groups = {Insert.class, Update.class})
  private Instant updated;

  @ExistingObject(type = Enhet.class)
  @Null(groups = {Insert.class, Update.class}) // This is gotten from the security context
  private ExpandableField<EnhetJSON> journalenhet;

  @Null(groups = {Insert.class, Update.class})
  private Boolean deleted;

  // Fields that should be indexed to ES
  // These should ideally be renamed and / or changed, but are kept for backwards compatibility
  @Null(groups = {Insert.class, Update.class})
  private List<String> type;

  @Null(groups = {Insert.class, Update.class})
  private List<String> arkivskaperNavn;

  @Null(groups = {Insert.class, Update.class})
  private String arkivskaperSorteringNavn;

  @Null(groups = {Insert.class, Update.class})
  private List<String> arkivskaperTransitive;
}

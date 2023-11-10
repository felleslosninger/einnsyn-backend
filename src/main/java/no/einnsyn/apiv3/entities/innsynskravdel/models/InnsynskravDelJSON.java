package no.einnsyn.apiv3.entities.innsynskravdel.models;

import java.time.Instant;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;
import no.einnsyn.apiv3.entities.enhet.models.EnhetJSON;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravJSON;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostJSON;
import no.einnsyn.apiv3.features.validation.ExistingObject.ExistingObject;
import no.einnsyn.apiv3.features.validation.validationgroups.Insert;
import no.einnsyn.apiv3.features.validation.validationgroups.Update;

@Getter
@Setter
public class InnsynskravDelJSON extends EinnsynObjectJSON {

  @NotNull(groups = {Insert.class})
  @ExistingObject(type = Journalpost.class)
  private ExpandableField<JournalpostJSON> journalpost;

  // InnsynskravDel will always be inserted through an Innsynskrav. The `innsynskrav`-property will
  // be set in the InnsynskravService.
  @Null(groups = {Insert.class, Update.class})
  private ExpandableField<InnsynskravJSON> innsynskrav;

  // The `enhet`-property will be set in the InnsynskravService.
  @Null(groups = {Insert.class, Update.class})
  private ExpandableField<EnhetJSON> enhet;

  @Null(groups = {Insert.class, Update.class})
  private Instant sent;

}

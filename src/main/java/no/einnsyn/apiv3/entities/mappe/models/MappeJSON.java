package no.einnsyn.apiv3.entities.mappe.models;

import java.time.Instant;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.enhet.models.EnhetJSON;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.features.validation.ExistingObject.ExistingObject;
import no.einnsyn.apiv3.features.validation.NoSSN.NoSSN;
import no.einnsyn.apiv3.features.validation.validationGroups.Insert;

@Getter
@Setter
public class MappeJSON extends EinnsynObjectJSON {

  @NoSSN
  @NotNull(groups = Insert.class)
  private String offentligTittel;

  @NoSSN
  @NotNull(groups = Insert.class)
  private String offentligTittelSensitiv;

  @NoSSN
  private String beskrivelse;

  // TODO: This should be gotten from the authentication, and possibly put in einnsynobjekt?
  @ExistingObject(type = Enhet.class)
  @NotNull(groups = Insert.class)
  private ExpandableField<EnhetJSON> virksomhet;

  // @ExpandableField
  // @NotNull(groups = InsertValidationGroup.class)
  // private parent Long;

  private Instant publisertDato;

  // Legacy?
  private String arkivskaper;

  // Legacy ElasticSearch name
  private String offentligTittel_SENSITIV;
}

package no.einnsyn.apiv3.entities.mappe.models;

import java.time.Instant;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;
import no.einnsyn.apiv3.features.validation.NoSSN.NoSSN;
import no.einnsyn.apiv3.features.validation.validationGroups.InsertValidationGroup;

@Getter
@Setter
public class MappeJSON extends EinnsynObjectJSON {

  @NoSSN
  @NotNull(groups = InsertValidationGroup.class)
  private String offentligTittel;

  @NoSSN
  @NotNull(groups = InsertValidationGroup.class)
  private String offentligTittelSensitiv;

  @NoSSN
  private String beskrivelse;

  // @ExpandableField
  // @NotNull(groups = InsertValidationGroup.class)
  // private parent Long;

  private Instant publisertDato;

  // Legacy?
  private String arkivskaper;

  // Legacy, to be removed? Difference between this and parent?
  @NotNull(groups = InsertValidationGroup.class)
  private String virksomhetIri;
}

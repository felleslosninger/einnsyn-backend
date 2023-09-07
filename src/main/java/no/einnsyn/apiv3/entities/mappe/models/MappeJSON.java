package no.einnsyn.apiv3.entities.mappe.models;

import java.time.Instant;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;
import no.einnsyn.apiv3.validationGroups.InsertValidationGroup;

@Getter
@Setter
public class MappeJSON extends EinnsynObjectJSON {

  @NotNull(groups = InsertValidationGroup.class)
  private String offentligTittel;

  @NotNull(groups = InsertValidationGroup.class)
  private String offentligTittelSensitiv;

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

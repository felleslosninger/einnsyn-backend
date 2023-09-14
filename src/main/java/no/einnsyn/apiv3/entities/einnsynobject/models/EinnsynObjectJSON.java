package no.einnsyn.apiv3.entities.einnsynobject.models;

import java.time.Instant;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.features.validation.validationGroups.InsertValidationGroup;
import no.einnsyn.apiv3.features.validation.validationGroups.UpdateValidationGroup;

@Getter
@Setter
public class EinnsynObjectJSON {

  @Null(groups = {InsertValidationGroup.class})
  private String id;

  private String externalId;

  @Null(groups = {InsertValidationGroup.class, UpdateValidationGroup.class})
  private String entity;

  @Null(groups = {InsertValidationGroup.class, UpdateValidationGroup.class})
  private Instant created;

  @Null(groups = {InsertValidationGroup.class, UpdateValidationGroup.class})
  private Instant updated;
}

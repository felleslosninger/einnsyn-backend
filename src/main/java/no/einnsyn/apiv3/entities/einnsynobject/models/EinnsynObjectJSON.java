package no.einnsyn.apiv3.entities.einnsynobject.models;

import java.time.Instant;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.validationGroups.InsertValidationGroup;
import no.einnsyn.apiv3.validationGroups.UpdateValidationGroup;

@Getter
@Setter
public class EinnsynObjectJSON {

  @NotNull(groups = {UpdateValidationGroup.class})
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

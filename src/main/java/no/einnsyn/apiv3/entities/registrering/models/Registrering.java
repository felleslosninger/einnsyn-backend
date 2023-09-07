package no.einnsyn.apiv3.entities.registrering.models;

import java.time.Instant;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObject;
import no.einnsyn.apiv3.validationGroups.InsertValidationGroup;

@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public abstract class Registrering extends EinnsynObject {

  @NotNull(groups = {InsertValidationGroup.class})
  private String offentligTittel;

  @NotNull(groups = {InsertValidationGroup.class})
  private String offentligTittelSensitiv;

  private Instant publisertDato;

  // Legacy
  @LastModifiedDate
  private Instant oppdatertDato;

  // Legacy
  @NotNull(groups = {InsertValidationGroup.class})
  private String virksomhetIri;

  // Legacy?
  private String systemId;
}

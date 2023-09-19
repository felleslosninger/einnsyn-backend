package no.einnsyn.apiv3.entities.dokumentbeskrivelse.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.enhet.models.EnhetJSON;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.features.validation.ExistingObject.ExistingObject;
import no.einnsyn.apiv3.features.validation.validationGroups.Insert;

@Getter
@Setter
public class DokumentbeskrivelseJSON extends EinnsynObjectJSON {

  private String systemId;

  private Integer dokumentnummer;

  @NotNull(groups = {Insert.class})
  private String tilknyttetRegistreringSom;

  private String dokumenttype;

  private String tittel;

  private String tittelSensitiv;

  @NotNull(groups = {Insert.class})
  @ExistingObject(type = Enhet.class)
  @Valid
  private ExpandableField<EnhetJSON> virksomhet;
}

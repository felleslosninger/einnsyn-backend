package no.einnsyn.apiv3.entities.korrespondansepart.models;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostJSON;
import no.einnsyn.apiv3.features.validation.ExistingObject.ExistingObject;
import no.einnsyn.apiv3.features.validation.validationGroups.Insert;
import no.einnsyn.apiv3.features.validation.validationGroups.KorrespondansepartInsert;

@Getter
@Setter
public class KorrespondansepartJSON extends EinnsynObjectJSON {

  @NotNull(groups = {Insert.class})
  private String korrespondanseparttype;

  @NotNull(groups = {Insert.class})
  private String navn;

  @NotNull(groups = {Insert.class})
  private String navnSensitiv;

  @NotNull(groups = {KorrespondansepartInsert.class})
  @ExistingObject(type = Journalpost.class)
  private ExpandableField<JournalpostJSON> journalpost;

  private String administrativEnhet;

  private String saksbehandler;

  private String epostadresse;

  private String postnummer;

  private Boolean erBehandlingsansvarlig = false;
}

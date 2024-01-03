package no.einnsyn.apiv3.entities.dokumentbeskrivelse.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.dokumentobjekt.models.DokumentobjektJSON;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.features.validation.NewObject.NewObject;
import no.einnsyn.apiv3.features.validation.NoSSN.NoSSN;
import no.einnsyn.apiv3.features.validation.validationGroups.Insert;

@Getter
@Setter
public class DokumentbeskrivelseJSON extends EinnsynObjectJSON {

  private String entity = "Dokumentbeskrivelse";

  private String systemId;

  private Integer dokumentnummer;

  @NotNull(groups = {Insert.class})
  private String tilknyttetRegistreringSom;

  private String dokumenttype;

  @NoSSN private String tittel;

  @NoSSN private String tittelSensitiv;

  @NewObject(groups = {Insert.class})
  @Valid
  private List<ExpandableField<DokumentobjektJSON>> dokumentobjekt = new ArrayList<>();
}

package no.einnsyn.apiv3.entities.dokumentobjekt.models;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseJSON;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.features.validation.validationGroups.Insert;
import org.hibernate.validator.constraints.URL;

@Getter
@Setter
public class DokumentobjektJSON extends EinnsynObjectJSON {

  private String entity = "Dokumentobjekt";

  private String systemId;

  @NotNull(groups = {Insert.class})
  @URL(protocol = "https", message = "Must be a valid HTTPS url")
  private String referanseDokumentfil;

  private String dokumentFormat;

  private String sjekksum;

  private String sjekksumalgoritme;

  private ExpandableField<DokumentbeskrivelseJSON> dokumentbeskrivelse;
}

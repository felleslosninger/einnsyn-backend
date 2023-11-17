package no.einnsyn.apiv3.entities.registrering.models;

import java.time.Instant;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;
import no.einnsyn.apiv3.entities.enhet.models.EnhetJSON;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.features.validation.NoSSN.NoSSN;
import no.einnsyn.apiv3.features.validation.validationGroups.Insert;

@Getter
@Setter
public class RegistreringJSON extends EinnsynObjectJSON {

  @NoSSN
  @NotNull(groups = {Insert.class})
  private String offentligTittel;

  @NoSSN
  @NotNull(groups = {Insert.class})
  private String offentligTittelSensitiv;

  private Instant publisertDato;

  private String administrativEnhet;

  private ExpandableField<EnhetJSON> administrativEnhetObjekt;


  // Legacy ElasticSearch name
  private String offentligTittel_SENSITIV;

  // Legacy ElasticSearch name
  private String arkivskaper;

}

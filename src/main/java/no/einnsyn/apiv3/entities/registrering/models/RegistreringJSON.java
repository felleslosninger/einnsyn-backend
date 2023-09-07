package no.einnsyn.apiv3.entities.registrering.models;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;

@Getter
@Setter
public class RegistreringJSON extends EinnsynObjectJSON {

  private String offentligTittel;

  private String offentligTittelSensitiv;

  private Instant publisertDato;

  // private ExpandableField<Virksomhet> virksomhet;
}

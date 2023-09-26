package no.einnsyn.apiv3.entities.korrespondansepart.models;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;
import no.einnsyn.apiv3.features.validation.validationGroups.Insert;

@Getter
@Setter
public class KorrespondansepartJSON extends EinnsynObjectJSON {

  @NotNull(groups = {Insert.class})
  private String korrespondanseparttype;

  @NotNull(groups = {Insert.class})
  private String navn;

  @NotNull(groups = {Insert.class})
  private String navnSensitiv;

  private String administrativEnhet;

  private String saksbehandler;

  private String epostadresse;

  private String postnummer;

  private Boolean erBehandlingsansvarlig = false;
}

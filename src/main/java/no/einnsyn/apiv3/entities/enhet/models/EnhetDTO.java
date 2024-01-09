// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.enhet.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.base.models.BaseDTO;
import no.einnsyn.apiv3.entities.enhet.models.EnhetDTO;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.features.validation.nossn.NoSSN;
import no.einnsyn.apiv3.features.validation.validationgroups.Insert;
import no.einnsyn.apiv3.features.validation.validationgroups.Update;

@Getter
@Setter
public class EnhetDTO extends BaseDTO {

  @Size(max = 500)
  @Null(groups = { Insert.class, Update.class })
  private final String entity = "Enhet";

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = { Insert.class })
  private String navn;

  @Size(max = 500)
  @NoSSN
  private String navnNynorsk;

  @Size(max = 500)
  @NoSSN
  private String navnEngelsk;

  @Size(max = 500)
  @NoSSN
  private String navnSami;

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = { Insert.class })
  private String orgnummer;

  @Size(max = 500)
  @NoSSN
  private String enhetskode;

  @Size(max = 500)
  @NoSSN
  private String kontaktpunktAdresse;

  @Size(max = 500)
  @Email
  @NotNull(groups = { Insert.class })
  private String kontaktpunktEpost;

  @Size(max = 500)
  @NoSSN
  private String kontaktpunktTelefon;

  @Size(max = 500)
  @Email
  @NotNull(groups = { Insert.class })
  private String innsynskravEpost;

  @Size(max = 500)
  @NotNull(groups = { Insert.class })
  private EnhetstypeEnum enhetstype;

  private Boolean skjult;

  private Boolean eFormidling;

  private Boolean teknisk;

  private Boolean skalKonvertereId;

  private Boolean skalMottaKvittering;

  @Size(max = 500)
  @NoSSN
  private String orderXmlVersjon;

  @Valid
  private ExpandableField<EnhetDTO> parent;

  public enum EnhetstypeEnum {
    VIRKSOMHET,
    UTVALG,
    AVDELING,
    ADMINISTRATIVENHET,
    SEKSJON,
    BYDEL,
    KOMMUNE,
  }
}

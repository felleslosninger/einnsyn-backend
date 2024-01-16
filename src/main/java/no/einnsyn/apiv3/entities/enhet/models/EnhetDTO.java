// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.enhet.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.base.models.BaseDTO;
import no.einnsyn.apiv3.validation.nossn.NoSSN;
import no.einnsyn.apiv3.validation.validationgroups.Insert;
import no.einnsyn.apiv3.validation.validationgroups.Update;
import no.einnsyn.apiv3.validation.validenum.ValidEnum;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class EnhetDTO extends BaseDTO {

  @Size(max = 500)
  @Null(groups = {Insert.class, Update.class})
  final String entity = "Enhet";

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = {Insert.class})
  String navn;

  @Size(max = 500)
  @NoSSN
  String navnNynorsk;

  @Size(max = 500)
  @NoSSN
  String navnEngelsk;

  @Size(max = 500)
  @NoSSN
  String navnSami;

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = {Insert.class})
  String orgnummer;

  @Size(max = 500)
  @NoSSN
  String enhetskode;

  @Size(max = 500)
  @NoSSN
  String kontaktpunktAdresse;

  @Size(max = 500)
  @Email
  @NotNull(groups = {Insert.class})
  String kontaktpunktEpost;

  @Size(max = 500)
  @NoSSN
  String kontaktpunktTelefon;

  @Size(max = 500)
  @Email
  @NotNull(groups = {Insert.class})
  String innsynskravEpost;

  @Size(max = 500)
  @ValidEnum(enumClass = EnhetstypeEnum.class)
  @NotNull(groups = {Insert.class})
  String enhetstype;

  @Size(max = 500)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  String avsluttetDato;

  Boolean skjult;

  Boolean eFormidling;

  Boolean teknisk;

  Boolean skalKonvertereId;

  Boolean skalMottaKvittering;

  Boolean visToppnode;

  Integer orderXmlVersjon;

  List<ExpandableField<EnhetDTO>> underenhet;

  @Valid ExpandableField<EnhetDTO> parent;
}

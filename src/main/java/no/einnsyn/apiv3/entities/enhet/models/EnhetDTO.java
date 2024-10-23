// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.enhet.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.base.models.BaseDTO;
import no.einnsyn.apiv3.entities.enhet.EnhetService;
import no.einnsyn.apiv3.validation.expandableobject.ExpandableObject;
import no.einnsyn.apiv3.validation.isodatetime.IsoDateTime;
import no.einnsyn.apiv3.validation.nossn.NoSSN;
import no.einnsyn.apiv3.validation.validationgroups.Insert;
import no.einnsyn.apiv3.validation.validationgroups.Update;
import no.einnsyn.apiv3.validation.validenum.ValidEnum;

@Getter
@Setter
public class EnhetDTO extends BaseDTO {

  @Size(max = 500)
  final String entity = "Enhet";

  @Size(max = 500)
  @NoSSN
  @NotBlank(groups = {Insert.class})
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
  @NotBlank(groups = {Insert.class})
  String orgnummer;

  @Size(max = 500)
  @NoSSN
  String enhetskode;

  @Size(max = 500)
  @NoSSN
  String kontaktpunktAdresse;

  @Size(max = 500)
  @Email
  @NotBlank(groups = {Insert.class})
  String kontaktpunktEpost;

  @Size(max = 500)
  @NoSSN
  String kontaktpunktTelefon;

  @Size(max = 500)
  @Email
  @NotBlank(groups = {Insert.class})
  String innsynskravEpost;

  @Size(max = 500)
  @ValidEnum(enumClass = EnhetstypeEnum.class)
  @NotBlank(groups = {Insert.class})
  String enhetstype;

  @Size(max = 500)
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE)
  String avsluttetDato;

  Boolean skjult;

  Boolean eFormidling;

  Boolean teknisk;

  Boolean skalKonvertereId;

  Boolean skalMottaKvittering;

  Boolean visToppnode;

  Integer orderXmlVersjon;

  @ExpandableObject(
      service = EnhetService.class,
      groups = {Insert.class, Update.class})
  @Valid
  List<ExpandableField<EnhetDTO>> underenhet;

  @ExpandableObject(
      service = EnhetService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<EnhetDTO> handteresAv;

  @ExpandableObject(
      service = EnhetService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<EnhetDTO> parent;
}

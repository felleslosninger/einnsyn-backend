// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.korrespondansepart.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.apiv3.validation.nossn.NoSSN;
import no.einnsyn.apiv3.validation.validationgroups.Insert;
import no.einnsyn.apiv3.validation.validenum.ValidEnum;

@Getter
@Setter
public class KorrespondansepartDTO extends ArkivBaseDTO {

  @Size(max = 500)
  final String entity = "Korrespondansepart";

  @Size(max = 20000)
  @NoSSN
  @NotBlank(groups = {Insert.class})
  String korrespondansepartNavn;

  @Size(max = 20000)
  @NoSSN
  @NotBlank(groups = {Insert.class})
  String korrespondansepartNavnSensitiv;

  @Size(max = 500)
  @ValidEnum(enumClass = KorrespondanseparttypeEnum.class)
  @NotBlank(groups = {Insert.class})
  String korrespondanseparttype;

  @Size(max = 500)
  @NoSSN
  String legacyKorrespondanseparttype;

  @Size(max = 500)
  @NoSSN
  String saksbehandler;

  @Size(max = 500)
  @NoSSN
  String epostadresse;

  @Size(max = 500)
  @NoSSN
  String postnummer;

  Boolean erBehandlingsansvarlig;

  @Size(max = 500)
  @NoSSN
  String administrativEnhet;

  KorrespondansepartParentDTO parent;
}

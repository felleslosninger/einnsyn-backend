// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.backend.entities.korrespondansepart.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;

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
  @NoSSN
  @NotBlank(groups = {Insert.class})
  String korrespondanseparttype;

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

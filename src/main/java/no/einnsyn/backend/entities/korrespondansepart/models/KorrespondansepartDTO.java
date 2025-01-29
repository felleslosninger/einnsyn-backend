// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.entities.korrespondansepart.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;

@Getter
@Setter
public class KorrespondansepartDTO extends ArkivBaseDTO {
  final String entity = "Korrespondansepart";

  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  String korrespondansepartNavn;

  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  String korrespondansepartNavnSensitiv;

  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  String korrespondanseparttype;

  @NoSSN
  @Size(max = 500)
  String saksbehandler;

  @NoSSN
  @Size(max = 500)
  String epostadresse;

  @NoSSN
  @Size(max = 500)
  String postnummer;

  Boolean erBehandlingsansvarlig;

  @NoSSN
  @Size(max = 500)
  String administrativEnhet;

  @Null(groups = {Insert.class, Update.class})
  KorrespondansepartParent parent;
}

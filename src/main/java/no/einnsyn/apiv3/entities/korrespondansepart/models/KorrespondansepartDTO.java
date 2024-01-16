// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.korrespondansepart.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.validation.nossn.NoSSN;
import no.einnsyn.apiv3.validation.validationgroups.Insert;
import no.einnsyn.apiv3.validation.validationgroups.Update;

@Getter
@Setter
public class KorrespondansepartDTO extends ArkivBaseDTO {

  @Size(max = 500)
  @Null(groups = {Insert.class, Update.class})
  final String entity = "Korrespondansepart";

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = {Insert.class})
  String korrespondansepartNavn;

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = {Insert.class})
  String korrespondansepartNavnSensitiv;

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = {Insert.class})
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

  @NotNull(groups = {Insert.class})
  @Valid
  ExpandableField<JournalpostDTO> journalpost;
}

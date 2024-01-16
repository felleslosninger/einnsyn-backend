// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.dokumentbeskrivelse.models;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.apiv3.entities.dokumentobjekt.models.DokumentobjektDTO;
import no.einnsyn.apiv3.validation.nossn.NoSSN;
import no.einnsyn.apiv3.validation.validationgroups.Insert;
import no.einnsyn.apiv3.validation.validationgroups.Update;

@Getter
@Setter
public class DokumentbeskrivelseDTO extends ArkivBaseDTO {

  @Size(max = 500)
  @Null(groups = {Insert.class, Update.class})
  final String entity = "Dokumentbeskrivelse";

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = {Insert.class})
  String tittel;

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = {Insert.class})
  String tittelSensitiv;

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = {Insert.class})
  String dokumenttype;

  @NotNull(groups = {Insert.class})
  Integer dokumentnummer;

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = {Insert.class})
  String tilknyttetRegistreringSom;

  List<ExpandableField<DokumentobjektDTO>> dokumentobjekt;
}

// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.dokumentobjekt.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.apiv3.validation.nossn.NoSSN;
import no.einnsyn.apiv3.validation.validationgroups.Insert;
import no.einnsyn.apiv3.validation.validationgroups.Update;
import org.hibernate.validator.constraints.URL;

@Getter
@Setter
public class DokumentobjektDTO extends ArkivBaseDTO {

  @Size(max = 500)
  final String entity = "Dokumentobjekt";

  @Size(max = 500)
  @URL
  @NotBlank(groups = {Insert.class})
  String referanseDokumentfil;

  @Size(max = 500)
  @NoSSN
  String format;

  @Size(max = 500)
  String sjekksum;

  @Size(max = 500)
  @NoSSN
  String sjekksumAlgoritme;

  @Null(groups = {Insert.class, Update.class})
  ExpandableField<DokumentbeskrivelseDTO> dokumentbeskrivelse;
}

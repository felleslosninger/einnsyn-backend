// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.dokumentobjekt.models;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.apiv3.validation.nossn.NoSSN;
import no.einnsyn.apiv3.validation.validationgroups.Insert;
import no.einnsyn.apiv3.validation.validationgroups.Update;
import org.hibernate.validator.constraints.URL;

@Getter
@Setter
public class DokumentobjektDTO extends ArkivBaseDTO {

  @Size(max = 500)
  @Null(groups = {Insert.class, Update.class})
  final String entity = "Dokumentobjekt";

  @Size(max = 500)
  @URL
  @NotNull(groups = {Insert.class})
  String referanseDokumentfil;

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = {Insert.class})
  String format;

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = {Insert.class})
  String sjekksum;

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = {Insert.class})
  String sjekksumAlgoritme;
}

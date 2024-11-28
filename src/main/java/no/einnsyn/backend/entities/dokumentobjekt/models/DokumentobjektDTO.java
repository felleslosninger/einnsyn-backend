// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.backend.entities.dokumentobjekt.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.entities.dokumentbeskrivelse.DokumentbeskrivelseService;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;
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

  @ExpandableObject(
      service = DokumentbeskrivelseService.class,
      groups = {Insert.class, Update.class})
  @Null(groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<DokumentbeskrivelseDTO> dokumentbeskrivelse;
}

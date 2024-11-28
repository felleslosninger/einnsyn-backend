// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.backend.entities.dokumentbeskrivelse.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.entities.dokumentobjekt.DokumentobjektService;
import no.einnsyn.backend.entities.dokumentobjekt.models.DokumentobjektDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;

@Getter
@Setter
public class DokumentbeskrivelseDTO extends ArkivBaseDTO {

  @Size(max = 500)
  final String entity = "Dokumentbeskrivelse";

  @Size(max = 500)
  @NoSSN
  @NotBlank(groups = {Insert.class})
  String tittel;

  @Size(max = 500)
  @NoSSN
  @NotBlank(groups = {Insert.class})
  String tittelSensitiv;

  @NotNull(groups = {Insert.class})
  Integer dokumentnummer;

  @Size(max = 500)
  @NoSSN
  String dokumenttype;

  @Size(max = 500)
  @NoSSN
  @NotBlank(groups = {Insert.class})
  String tilknyttetRegistreringSom;

  @ExpandableObject(
      service = DokumentobjektService.class,
      groups = {Insert.class, Update.class})
  @Valid
  List<ExpandableField<DokumentobjektDTO>> dokumentobjekt;
}

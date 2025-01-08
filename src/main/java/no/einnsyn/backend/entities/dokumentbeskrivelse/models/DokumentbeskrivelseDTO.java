// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

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

/** Dokumentbeskrivelse */
@Getter
@Setter
public class DokumentbeskrivelseDTO extends ArkivBaseDTO {
  final String entity = "Dokumentbeskrivelse";

  /** The title of the document, with sensitive information redacted. */
  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  String tittel;

  /** The title of the document, with sensitive information included. */
  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  String tittelSensitiv;

  @NotNull(groups = {Insert.class})
  Integer dokumentnummer;

  @NoSSN
  @Size(max = 500)
  String dokumenttype;

  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  String tilknyttetRegistreringSom;

  @ExpandableObject(
      service = DokumentobjektService.class,
      groups = {Insert.class, Update.class})
  @Valid
  List<ExpandableField<DokumentobjektDTO>> dokumentobjekt;
}

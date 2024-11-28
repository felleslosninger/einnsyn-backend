// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.backend.entities.saksmappe.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.enhet.EnhetService;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import no.einnsyn.backend.entities.journalpost.JournalpostService;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.mappe.models.MappeDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.isodatetime.IsoDateTime;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;

@Getter
@Setter
public class SaksmappeDTO extends MappeDTO {

  @Size(max = 500)
  final String entity = "Saksmappe";

  @NotNull(groups = {Insert.class})
  Integer saksaar;

  @NotNull(groups = {Insert.class})
  Integer sakssekvensnummer;

  @Size(max = 500)
  @NoSSN
  @Null(groups = {Insert.class, Update.class})
  String saksnummer;

  @Size(max = 500)
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE)
  String saksdato;

  @ExpandableObject(
      service = JournalpostService.class,
      groups = {Insert.class, Update.class})
  @Valid
  List<ExpandableField<JournalpostDTO>> journalpost;

  @Size(max = 500)
  @NoSSN
  String administrativEnhet;

  @ExpandableObject(
      service = EnhetService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<EnhetDTO> administrativEnhetObjekt;
}

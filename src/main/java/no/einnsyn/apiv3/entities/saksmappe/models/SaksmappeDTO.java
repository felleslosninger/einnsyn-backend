// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.saksmappe.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.enhet.models.EnhetDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.mappe.models.MappeDTO;
import no.einnsyn.apiv3.validation.nossn.NoSSN;
import no.einnsyn.apiv3.validation.validationgroups.Insert;
import no.einnsyn.apiv3.validation.validationgroups.Update;
import org.springframework.format.annotation.DateTimeFormat;

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
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  String saksdato;

  @Valid List<ExpandableField<JournalpostDTO>> journalpost;

  @Size(max = 500)
  @NoSSN
  String administrativEnhet;

  @Valid ExpandableField<EnhetDTO> administrativEnhetObjekt;
}

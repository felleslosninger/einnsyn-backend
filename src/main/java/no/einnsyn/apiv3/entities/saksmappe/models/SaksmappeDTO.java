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
import no.einnsyn.apiv3.entities.enhet.models.EnhetDTO;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.mappe.models.MappeDTO;
import no.einnsyn.apiv3.features.validation.nossn.NoSSN;
import no.einnsyn.apiv3.features.validation.validationgroups.Insert;
import no.einnsyn.apiv3.features.validation.validationgroups.Update;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class SaksmappeDTO extends MappeDTO {

  @Size(max = 500)
  @Null(groups = { Insert.class, Update.class })
  private final String entity = "Saksmappe";

  @NotNull(groups = { Insert.class })
  private Long saksaar;

  @NotNull(groups = { Insert.class })
  private Long sakssekvensnummer;

  @Size(max = 500)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private String saksdato;

  @Valid
  private List<ExpandableField<JournalpostDTO>> journalpost;

  @Size(max = 500)
  @NoSSN
  private String administrativEnhet;

  @Valid
  private ExpandableField<EnhetDTO> administrativEnhetObjekt;
}
